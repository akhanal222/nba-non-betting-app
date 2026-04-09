package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.dto.*;
import com.nba.nbanonbettingapp.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI-powered plain-English explanations.
 *
 * All endpoints accept an optional `forceRefresh` param (default false).
 *
 * Endpoints:
 *   GET /api/ai/explain/prop         — explains a prop prediction
 *   GET /api/ai/explain/matchup      — explains a head-to-head or recent form analysis
 *   GET /api/ai/explain/comparison   — explains a player vs player comparison
 */
@RestController
@RequestMapping("/api/ai/explain")
public class AiExplanationController {

    private final GeminiService geminiService;
    private final PropProjectionService propProjectionService;
    private final HeadToHeadService headToHeadService;
    private final PlayerComparisonService playerComparisonService;
    private final PlayerStatsService playerStatsService;

    public AiExplanationController(
            GeminiService geminiService,
            PropProjectionService propProjectionService,
            HeadToHeadService headToHeadService,
            PlayerComparisonService playerComparisonService,
            PlayerStatsService playerStatsService
    ) {
        this.geminiService = geminiService;
        this.propProjectionService = propProjectionService;
        this.headToHeadService = headToHeadService;
        this.playerComparisonService = playerComparisonService;
        this.playerStatsService = playerStatsService;
    }

    /**
     * Generates a plain-English explanation of a prop prediction.
     *
     * Example:
     * GET /api/ai/explain/prop?playerApiId=237&opponentTeamApiId=6&statType=pts&line=22.5
     */
    @GetMapping("/prop")
    public ResponseEntity<AiExplanationResponseDTO> explainPropPrediction(
            @RequestParam Long playerApiId,
            @RequestParam Long opponentTeamApiId,
            @RequestParam String statType,
            @RequestParam double line,
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) {

        PropPredictResponseDTO prediction =
                propProjectionService.predict(playerApiId, opponentTeamApiId, statType, line);

        AiExplanationResponseDTO explanation =
                geminiService.explainPropPrediction(
                        playerApiId,
                        opponentTeamApiId,
                        prediction,
                        forceRefresh
                );

        return ResponseEntity.ok(explanation);
    }

    /**
     * Generates a plain-English explanation of a head-to-head matchup or
     * recent form analysis.
     *
     * Works for both analysis types since they share HeadToHeadResultDTO:
     *   - analysisType=MATCHUP  → runs /api/matchup/analyze logic (player vs specific opponent)
     *   - analysisType=RECENT   → runs /stats/recent/analyze logic (player vs all opponents)
     *
     * Example (matchup):
     * GET /api/ai/explain/matchup?playerApiId=175&opponentApiId=8&statLine=4.5
     *       &statType=reb&limit=7&analysisType=MATCHUP
     *
     * Example (recent form):
     * GET /api/ai/explain/matchup?playerApiId=17553995&statLine=15.5
     *       &statType=pts&limit=5&analysisType=RECENT
     */
    @GetMapping("/matchup")
    public ResponseEntity<AiExplanationResponseDTO> explainMatchup(
            @RequestParam Long playerApiId,
            @RequestParam(required = false) Long opponentApiId,
            @RequestParam double statLine,
            @RequestParam(defaultValue = "pts") String statType,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "true") boolean includePlayoffs,
            @RequestParam(defaultValue = "MATCHUP") String analysisType,
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) {
        HeadToHeadResultDTO result;

        if ("RECENT".equalsIgnoreCase(analysisType)) {
            // Recent form — player vs all opponents
            result = playerStatsService.analyzeRecentStats(playerApiId, statLine, limit,
                    includePlayoffs, statType);
        } else {
            // Head-to-head — player vs specific opponent
            if (opponentApiId == null) {
                return ResponseEntity.badRequest().build();
            }
            result = headToHeadService.analyze(playerApiId, opponentApiId, statLine,
                    limit, includePlayoffs, statType);
        }

        AiExplanationResponseDTO explanation =
                geminiService.explainHeadToHead(playerApiId, result, forceRefresh, analysisType);

        return ResponseEntity.ok(explanation);
    }

    /**
     * Generates a plain-English comparison of two players.
     *
     * Example:
     * GET /api/ai/explain/comparison?playerOneApiId=3547239&playerTwoApiId=1057263194
     */
    @GetMapping("/comparison")
    public ResponseEntity<PlayerComparisonExplanationDTO> explainComparison(
            @RequestParam Long playerOneApiId,
            @RequestParam Long playerTwoApiId,
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) {
        PlayerComparisonDTO comparison =
                playerComparisonService.compare(playerOneApiId, playerTwoApiId);

        PlayerComparisonExplanationDTO explanation =
                geminiService.explainPlayerComparisonStructured(playerOneApiId, comparison, forceRefresh);

        return ResponseEntity.ok(explanation);
    }
}