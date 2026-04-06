package com.nba.nbanonbettingapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.nbanonbettingapp.dto.*;
import com.nba.nbanonbettingapp.entity.AiExplanation;
import com.nba.nbanonbettingapp.entity.AnalyticsSnapshot;
import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.repository.AiExplanationRepository;
import com.nba.nbanonbettingapp.repository.AnalyticsSnapshotRepository;
import com.nba.nbanonbettingapp.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GeminiService is the AI explanation layer for the NBA analytics app.
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + GEMINI_MODEL + ":generateContent";

    private final String geminiApiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PlayerRepository playerRepository;
    private final AnalyticsSnapshotRepository snapshotRepository;
    private final AiExplanationRepository explanationRepository;

    public GeminiService(
            @Value("${gemini.api.key}") String geminiApiKey,
            PlayerRepository playerRepository,
            AnalyticsSnapshotRepository snapshotRepository,
            AiExplanationRepository explanationRepository,
            ObjectMapper objectMapper
    ) {
        this.geminiApiKey = geminiApiKey;
        this.playerRepository = playerRepository;
        this.snapshotRepository = snapshotRepository;
        this.explanationRepository = explanationRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    // Public Entry Points

    /**
     * Generates a plain-English explanation of a prop prediction result.
     */
    public AiExplanationResponseDTO explainPropPrediction(
            Long playerApiId,
            PropPredictResponseDTO prediction,
            boolean forceRefresh
    ) {
        Player player = resolvePlayer(playerApiId);
        String metricType = "PROP_PREDICTION_" + prediction.statType().toUpperCase();
        String paramsJson = toJson(Map.of(
                "statType", prediction.statType(),
                "line", prediction.line(),
                "opponentAdj", prediction.opponentAdjustment()
        ));

        if (!forceRefresh) {
            Optional<AiExplanationResponseDTO> cached =
                    getCachedExplanation(player, "PROP_PREDICTION", paramsJson);
            if (cached.isPresent()) return cached.get();
        }

        String prompt = buildPropPredictionPrompt(prediction);
        return generateAndPersist(player, "PROP_PREDICTION", paramsJson, toJson(prediction), prompt);
    }

    /**
     * Generates a plain-English explanation of a head-to-head or recent stats result.
     *
     * Works for both /api/matchup/analyze (player vs specific opponent)
     * and /stats/recent/analyze (player vs all opponents) since both
     * return HeadToHeadResultDTO.
     */
    public AiExplanationResponseDTO explainHeadToHead(
            Long playerApiId,
            HeadToHeadResultDTO result,
            boolean forceRefresh,
            String analysisType
    ) {
        Player player = resolvePlayer(playerApiId);
        // Use different metric types so they cache separately
        String metricType = "RECENT".equalsIgnoreCase(analysisType)
                ? "RECENT_FORM"
                : "HEAD_TO_HEAD";

        String paramsJson = toJson(Map.of(
                "statType", result.statType(),
                "statLine", result.statLine(),
                "opponentTeamName", result.opponentTeamName(),
                "limit", result.totalGames()
        ));

        if (!forceRefresh) {
            Optional<AiExplanationResponseDTO> cached =
                    getCachedExplanation(player, metricType, paramsJson);
            if (cached.isPresent()) return cached.get();
        }

        String prompt = buildHeadToHeadPrompt(result);
        return generateAndPersist(player, metricType, paramsJson, toJson(result), prompt);
    }

    /**
     * Generates a plain-English comparison of two players.
     */
    public PlayerComparisonExplanationDTO explainPlayerComparisonStructured(
            Long playerOneApiId,
            PlayerComparisonDTO comparison,
            boolean forceRefresh
    ) {
        Player player = resolvePlayer(playerOneApiId);
        String paramsJson = toJson(Map.of(
                "playerOne", comparison.playerOne().firstName() + " " + comparison.playerOne().lastName(),
                "playerTwo", comparison.playerTwo().firstName() + " " + comparison.playerTwo().lastName()
        ));

        if (!forceRefresh) {
            Optional<AnalyticsSnapshot> snapshotOpt =
                    snapshotRepository.findTopByPlayer_PlayerIdAndMetricTypeOrderByComputedAtDesc(
                            player.getPlayerId(), "PLAYER_COMPARISON");

            if (snapshotOpt.isPresent()) {
                Optional<AiExplanation> cachedEx =
                        explanationRepository.findTopBySnapshot_SnapshotIdOrderByCreatedAtDesc(
                                snapshotOpt.get().getSnapshotId());

                if (cachedEx.isPresent()) {
                    AiExplanation ex = cachedEx.get();
                    try {
                        String cleaned = ex.getGeneratedResponse()
                                .replace("```json", "").replace("```", "").trim();
                        Map<?, ?> parsed = objectMapper.readValue(cleaned, Map.class);

                        return new PlayerComparisonExplanationDTO(
                                ex.getExplanationId(),
                                player.getFirstName() + " " + player.getLastName(),
                                "PLAYER_COMPARISON",
                                (String) parsed.get("seasonExplanation"),
                                (String) parsed.get("careerExplanation"),
                                (String) parsed.get("bottomLine"),
                                ex.getModelUsed(),
                                ex.getCreatedAt(),
                                true
                        );
                    } catch (Exception e) {
                        // Cached response unparseable — fall through and regenerate
                        log.warn("Cached comparison response could not be parsed, regenerating", e);
                    }
                }
            }
        }
        String prompt = buildPlayerComparisonPrompt(comparison);
        String rawResponse = callGemini(prompt);

        String cleaned = rawResponse.replace("```json", "").replace("```", "").trim();

        try {
            Map<?, ?> parsed = objectMapper.readValue(cleaned, Map.class);

            String seasonExplanation = (String) parsed.get("seasonExplanation");
            String careerExplanation = (String) parsed.get("careerExplanation");
            String bottomLine        = (String) parsed.get("bottomLine");

            AnalyticsSnapshot snapshot = new AnalyticsSnapshot();
            snapshot.setPlayer(player);
            snapshot.setMetricType("PLAYER_COMPARISON");
            snapshot.setParameters(paramsJson);
            snapshot.setResultData(toJson(comparison));
            snapshot.setComputedAt(OffsetDateTime.now());
            snapshot = snapshotRepository.save(snapshot);

            AiExplanation explanation = new AiExplanation();
            explanation.setSnapshot(snapshot);
            explanation.setPlayer(player);
            explanation.setPromptText(prompt);
            explanation.setGeneratedResponse(rawResponse);
            explanation.setModelUsed(GEMINI_MODEL);
            explanation.setCreatedAt(OffsetDateTime.now());
            explanation = explanationRepository.save(explanation);

            return new PlayerComparisonExplanationDTO(
                    explanation.getExplanationId(),
                    player.getFirstName() + " " + player.getLastName(),
                    "PLAYER_COMPARISON",
                    seasonExplanation,
                    careerExplanation,
                    bottomLine,
                    GEMINI_MODEL,
                    explanation.getCreatedAt(),
                    false
            );

        } catch (Exception e) {
            log.error("Failed to parse structured Gemini response: {}", cleaned, e);
            throw new RuntimeException("Gemini returned malformed JSON for comparison", e);
        }
    }

    // Prompt Builders

    /**
     * Builds the Gemini prompt for a prop prediction.
     */
    private String buildPropPredictionPrompt(PropPredictResponseDTO p) {
        String overUnder = p.overProbability() > p.underProbability() ? "OVER" : "UNDER";
        double dominantProb = Math.max(p.overProbability(), p.underProbability());
        String confidenceNote = p.lowConfidence()
                ? " Note: this is a low-confidence prediction because we have limited recent game data."
                : "";

        return """
                You are a friendly NBA analyst explaining a stat prediction to someone who watches NBA games
                but doesn't know advanced statistics. Use simple, clear language. No jargon. Be direct.
                
                Here is what our model calculated for %s:
                
                Stat being predicted: %s (the line is %.1f)
                Our model projects they will get: %.1f %s
                Probability they go OVER %.1f: %.0f%%
                Probability they go UNDER %.1f: %.0f%%
                
                How the model arrived at this (explain each factor simply):
                - Recent form (EWMA): Based on their last %d games, they average %.2f %s per minute played
                - Minutes expected: %.1f minutes
                - Pace of game adjustment: %.2f (1.0 = neutral; above 1.0 means faster-paced game = more opportunities)
                - Opponent defensive adjustment: %.2f (above 1.0 = weak defense; below 1.0 = tough defense)
                - Rest adjustment: %.2f (above 1.0 = well-rested; below 1.0 = playing on short rest)
                - Usage rate (USG%%): %.1f%% — this means roughly %.0f%% of their team's plays run through them
                - True Shooting %% (TS%%): %.1f%% — their overall scoring efficiency including free throws and 3-pointers
                - Variability (std dev): %.1f — how consistent they are game to game%s
                
                Write 3-4 sentences explaining this prediction in plain English for a casual fan.
                Tell them what the model thinks will happen and WHY (using the factors above).
                Mention whether the defense is tough or soft, how hot the player has been, and
                give them a bottom-line take on the OVER/UNDER. Do NOT use the words "EWMA",
                "standard deviation", "USG%%", "TS%%", or any statistical jargon — explain those
                concepts in plain words instead.
                """.formatted(
                p.playerName(),
                p.statType(), p.line(),
                p.projectedValue(), p.statType(),
                p.line(), p.overProbability() * 100,
                p.line(), p.underProbability() * 100,
                p.gamesUsed(), p.ewmaPerMinute(), p.statType(),
                p.projectedMinutes(),
                p.paceAdjustment(),
                p.opponentAdjustment(),
                p.restAdjustment(),
                p.usgPct() * 100, p.usgPct() * 100,
                p.tsPct() * 100,
                p.stdDev(),
                confidenceNote
        );
    }

    /**
     * Builds the Gemini prompt for a head-to-head or recent form analysis.
     */
    private String buildHeadToHeadPrompt(HeadToHeadResultDTO r) {
        boolean isMatchup = !"All Opponents".equals(r.opponentTeamName());
        String context = isMatchup
                ? "against the " + r.opponentTeamName()
                : "across their most recent games";

        StringBuilder gameLog = new StringBuilder();
        List<HeadToHeadResultDTO.GameLineResult> games = r.games();
        for (int i = 0; i < games.size(); i++) {
            HeadToHeadResultDTO.GameLineResult g = games.get(i);
            gameLog.append(String.format("  Game %d (%s): %d %s — %s the line of %.1f%n",
                    i + 1, g.date(), g.statValue(), r.statType(),
                    g.hitLine() ? "OVER" : "UNDER", r.statLine()));
        }

        return """
                You are a friendly NBA analyst explaining historical performance data to a casual fan.
                Use simple language. Be concise and engaging. No jargon.
                
                Player: %s
                Stat analyzed: %s
                Prop line being evaluated: %.1f
                Context: %s
                
                Game-by-game results (newest first):
                %s
                Summary statistics:
                - Average %s %s: %.1f
                - Consistency (std dev): %.1f (lower = more consistent)
                - Hit rate vs the %.1f line: %d out of %d games (%.0f%%)
                
                Write 3-4 sentences for a casual fan explaining:
                1. How this player has been performing %s
                2. Whether they tend to go over or under this kind of line
                3. Any notable trend you see in the game log (hot streak, cold stretch, outlier game)
                4. A plain-English bottom-line take
                
                Do NOT say "standard deviation" — say "consistency" or "how reliable they are".
                Do NOT use any statistical jargon. Keep it conversational.
                """.formatted(
                r.playerName(),
                r.statType(),
                r.statLine(),
                context,
                gameLog,
                r.statType(), context, r.average(),
                r.standardDeviation(),
                r.statLine(), r.hitCount(), r.totalGames(), r.hitRate() * 100,
                context
        );
    }

    /**
     * Builds the Gemini prompt for a player vs player comparison.
     */
    private String buildPlayerComparisonPrompt(PlayerComparisonDTO c) {
        PlayerComparisonDTO.PlayerProfile p1 = c.playerOne();
        PlayerComparisonDTO.PlayerProfile p2 = c.playerTwo();

        String p1SeasonLine = formatSeasonLine(p1);
        String p2SeasonLine = formatSeasonLine(p2);
        String p1CareerLine = formatCareerLine(p1);
        String p2CareerLine = formatCareerLine(p2);

        return """
        You are a friendly NBA analyst comparing two players for a casual fan.
        Use plain English. No jargon. Focus heavily on the actual numbers.

        Player 1: %s %s
          Position: %s | Team: %s
          THIS SEASON averages: %s
          CAREER averages: %s

        Player 2: %s %s
          Position: %s | Team: %s
          THIS SEASON averages: %s
          CAREER averages: %s

        Respond ONLY with a valid JSON object in exactly this format, no markdown, no backticks:
        {
          "seasonExplanation": "2-3 sentences comparing their CURRENT SEASON stats head-to-head. Be specific with numbers. Who scores more, who rebounds better, who passes more.",
          "careerExplanation": "2-3 sentences comparing their CAREER averages. For rookies whose career IS this season, note that explicitly.",
          "bottomLine": "One sentence on what makes each player uniquely valuable to their team based purely on the stats."
        }

        Rules:
        - Lead with stats, not backstory or draft history.
        - Be specific — say 'averaging 26 points' not 'scoring well'.
        - Do NOT use jargon like standard deviation, USG%%, TS%%, or advanced stats.
        - Return ONLY the JSON object. No preamble, no explanation outside the JSON.
        """.formatted(
                p1.firstName(), p1.lastName(), p1.position(), teamName(p1),
                p1SeasonLine, p1CareerLine,
                p2.firstName(), p2.lastName(), p2.position(), teamName(p2),
                p2SeasonLine, p2CareerLine
        );
    }

    // Gemini API Call

    /**
     * Calls the Gemini generateContent REST API with the given prompt text.
     *
     * Uses RestClient rather than introducing a Gemini SDK dependency
     */
    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        String url = GEMINI_URL + "?key=" + geminiApiKey;

        try {
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new RuntimeException("Gemini returned a null response body");
            }

            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException("Gemini returned no candidates");
            }

            Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);

            String text = (String) firstPart.get("text");
            if (text == null || text.isBlank()) {
                throw new RuntimeException("Gemini returned empty text");
            }
            return text.trim();

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get explanation from Gemini: " + e.getMessage(), e);
        }
    }

    // Persistence & Caching

    /**
     * Calls Gemini, then saves the snapshot + explanation to DB.
     * Returns the final AiExplanationResponseDTO with cached=false.
     */
    private AiExplanationResponseDTO generateAndPersist(
            Player player,
            String metricType,
            String paramsJson,
            String resultDataJson,
            String prompt
    ) {
        log.info("Calling Gemini for player={} metricType={}", player.getPlayerId(), metricType);

        String generatedText = callGemini(prompt);

        // Save snapshot
        AnalyticsSnapshot snapshot = new AnalyticsSnapshot();
        snapshot.setPlayer(player);
        snapshot.setMetricType(metricType);
        snapshot.setParameters(paramsJson);
        snapshot.setResultData(resultDataJson);
        snapshot.setComputedAt(OffsetDateTime.now());
        snapshot = snapshotRepository.save(snapshot);

        // Save explanation
        AiExplanation explanation = new AiExplanation();
        explanation.setSnapshot(snapshot);
        explanation.setPlayer(player);
        explanation.setPromptText(prompt);
        explanation.setGeneratedResponse(generatedText);
        explanation.setModelUsed(GEMINI_MODEL);
        explanation.setCreatedAt(OffsetDateTime.now());
        explanation = explanationRepository.save(explanation);

        log.info("Saved AI explanation id={} for snapshotId={}", explanation.getExplanationId(), snapshot.getSnapshotId());

        return new AiExplanationResponseDTO(
                explanation.getExplanationId(),
                player.getFirstName() + " " + player.getLastName(),
                metricType,
                generatedText,
                GEMINI_MODEL,
                explanation.getCreatedAt(),
                false
        );
    }

    /**
     * Looks up an existing snapshot+explanation pair for this player/metric/params combo.
     * Returns empty if nothing is cached, triggering a fresh Gemini call.
     */
    private Optional<AiExplanationResponseDTO> getCachedExplanation(
            Player player, String metricType, String paramsJson
    ) {
        Optional<AnalyticsSnapshot> snapshotOpt =
                snapshotRepository.findTopByPlayer_PlayerIdAndMetricTypeOrderByComputedAtDesc(
                        player.getPlayerId(), metricType);

        if (snapshotOpt.isEmpty()) return Optional.empty();

        AnalyticsSnapshot snapshot = snapshotOpt.get();
        Optional<AiExplanation> explanationOpt =
                explanationRepository.findTopBySnapshot_SnapshotIdOrderByCreatedAtDesc(snapshot.getSnapshotId());

        if (explanationOpt.isEmpty()) return Optional.empty();

        AiExplanation ex = explanationOpt.get();
        log.info("Returning cached AI explanation id={} for player={}", ex.getExplanationId(), player.getPlayerId());

        return Optional.of(new AiExplanationResponseDTO(
                ex.getExplanationId(),
                player.getFirstName() + " " + player.getLastName(),
                metricType,
                ex.getGeneratedResponse(),
                ex.getModelUsed(),
                ex.getCreatedAt(),
                true
        ));
    }

    // Helper Utilities

    private Player resolvePlayer(Long playerApiId) {
        return playerRepository.findByExternalApiId(playerApiId)
                .orElseThrow(() -> new RuntimeException(
                        "Player not found with externalApiId: " + playerApiId +
                                ". Search for them first at /api/players/search"));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return "{}";
        }
    }

    private String formatSeasonLine(PlayerComparisonDTO.PlayerProfile p) {
        if (p.seasonAverages() == null) return "No current season data available";
        PlayerComparisonDTO.SeasonAverages s = p.seasonAverages();
        return String.format("%.1f pts, %.1f reb, %.1f ast, %.1f%% FG, %.1f%% 3P%% in %.1f min",
                nvlD(s.pts()), nvlD(s.reb()), nvlD(s.ast()),
                nvlD(s.fgPct()) * 100, nvlD(s.fg3Pct()) * 100, nvlD(s.minutesPerGame()));
    }

    private String formatCareerLine(PlayerComparisonDTO.PlayerProfile p) {
        if (p.careerAverages() == null) return "No career data available";
        PlayerComparisonDTO.SeasonAverages s = p.careerAverages();
        return String.format("%.1f pts, %.1f reb, %.1f ast, %.1f%% FG over career",
                nvlD(s.pts()), nvlD(s.reb()), nvlD(s.ast()), nvlD(s.fgPct()) * 100);
    }

    private String teamName(PlayerComparisonDTO.PlayerProfile p) {
        return p.teamName() != null ? p.teamName() : "Unknown Team";
    }

    private String nvl(Object value) {
        return value != null ? value.toString() : "N/A";
    }

    private double nvlD(Double value) {
        return value != null ? value : 0.0;
    }
}