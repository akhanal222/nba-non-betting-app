package com.nba.nbanonbettingapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.nbanonbettingapp.dto.EWMASnapshot;
import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.entity.PlayerAdvancedStats;
import com.nba.nbanonbettingapp.entity.PlayerGameStatistic;
import com.nba.nbanonbettingapp.repository.PlayerAdvancedStatsRepository;
import com.nba.nbanonbettingapp.repository.PlayerGameStatisticRepository;
import com.nba.nbanonbettingapp.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes 20-game exponentially weighted moving averages (EWMA) for a player
 * and caches the result in players.ewma_json.
 */
@Service
public class EWMACalculatorService {

    private static final Logger log = LoggerFactory.getLogger(EWMACalculatorService.class);

    // Number of recent games to include in the EWMA window.
    private static final int WINDOW_SIZE = 20;

    // Exponential decay factor.
    private static final double LAMBDA = 0.1;

    // Recompute threshold — if cached value is older than this, recompute.
    private static final int CACHE_HOURS = 24;

    // Minimum games required before returning a result.
    // Below this threshold, lowConfidence = true in the snapshot.
    private static final int MIN_GAMES_THRESHOLD = 5;

    private final PlayerRepository playerRepository;
    private final PlayerGameStatisticRepository statRepository;
    private final PlayerAdvancedStatsRepository advancedStatsRepository;
    private final ObjectMapper objectMapper;

    public EWMACalculatorService(PlayerRepository playerRepository,
                                 PlayerGameStatisticRepository statRepository,
                                 PlayerAdvancedStatsRepository advancedStatsRepository,
                                 ObjectMapper objectMapper) {
        this.playerRepository = playerRepository;
        this.statRepository = statRepository;
        this.advancedStatsRepository = advancedStatsRepository;
        this.objectMapper = objectMapper;
    }


    /**
     * Returns the EWMA snapshot for a player, using cache when fresh.
     *
     * @param playerApiId BallDontLie external_api_id
     * @return EWMASnapshot or null if the player has no qualifying game data
     */
    public EWMASnapshot getOrCompute(Long playerApiId) {
        Player player = playerRepository.findByExternalApiId(playerApiId).orElse(null);
        if (player == null) {
            log.warn("EWMACalculator: player not found for apiId={}", playerApiId);
            return null;
        }
        return getOrComputeForPlayer(player);
    }

    public EWMASnapshot getOrComputeByPlayerId(Long playerId) {
        Player player = playerRepository.findById(playerId).orElse(null);
        if (player == null) {
            log.warn("EWMACalculator: player not found for playerId={}", playerId);
            return null;
        }
        return getOrComputeForPlayer(player);
    }

    /**
     * Forces a recompute regardless of cache age.
     * Useful after a new game is ingested.
     */
    public EWMASnapshot forceRecompute(Long playerApiId) {
        Player player = playerRepository.findByExternalApiId(playerApiId).orElse(null);
        if (player == null) return null;
        return computeAndCache(player);
    }


    private EWMASnapshot getOrComputeForPlayer(Player player) {

        // Cache hit — return if fresh
        if (player.getEwmaJson() != null && player.getEwmaComputedAt() != null) {
            boolean fresh = player.getEwmaComputedAt()
                    .isAfter(OffsetDateTime.now().minusHours(CACHE_HOURS));
            if (fresh) {
                try {
                    return objectMapper.readValue(player.getEwmaJson(), EWMASnapshot.class);
                } catch (Exception e) {
                    log.warn("EWMACalculator: corrupt cache for player={}, recomputing", player.getPlayerId());
                }
            }
        }
        return computeAndCache(player);
    }

    private EWMASnapshot computeAndCache(Player player) {
        EWMASnapshot snapshot = compute(player.getPlayerId());

        if (snapshot != null) {
            try {
                player.setEwmaJson(objectMapper.writeValueAsString(snapshot));
                player.setEwmaComputedAt(OffsetDateTime.now());
                playerRepository.save(player);
            } catch (Exception e) {
                log.warn("EWMACalculator: failed to cache snapshot for player={}: {}",
                        player.getPlayerId(), e.getMessage());
            }
        }

        return snapshot;
    }

    /**
     * Core computation — reads box score + advanced stats from DB,
     * applies EWMA weights, returns the snapshot.
     */
    private EWMASnapshot compute(Long playerId) {

        // Fetch up to WINDOW_SIZE most recent games — newest first
        List<PlayerGameStatistic> boxScores = statRepository
                .findByPlayer_PlayerIdOrderByGame_GameDateDesc(
                        playerId, PageRequest.of(0, WINDOW_SIZE));

        if (boxScores == null || boxScores.isEmpty()) return null;

        // Apply DNP filter
        List<PlayerGameStatistic> qualified = boxScores.stream()
                .filter(s -> {
                    String min = s.getMinutesPlayed();
                    return min != null && !min.isBlank()
                            && !min.equals("0") && !min.equals("00")
                            && !min.equals("0:00") && !min.equals("00:00");
                })
                .toList();

        if (qualified.isEmpty()) return null;

        // Build a lookup map: game_id → advanced stats row
        // Used to join pace, usg%, ts% onto each box score game
        List<Long> gameIds = qualified.stream()
                .filter(s -> s.getGame() != null)
                .map(s -> s.getGame().getGameId())
                .toList();

        Map<Long, PlayerAdvancedStats> advancedByGameId = advancedStatsRepository
                .findByPlayer_PlayerIdOrderByGame_GameDateDesc(
                        playerId, PageRequest.of(0, WINDOW_SIZE))
                .stream()
                .filter(a -> a.getGame() != null)
                .collect(Collectors.toMap(
                        a -> a.getGame().getGameId(),
                        a -> a,
                        (a, b) -> a
                ));

        // Pre-compute weights — index 0 = most recent game
        // weight[i] = (1 - λ)^i
        int n = qualified.size();
        double[] weights = new double[n];
        double weightSum = 0.0;
        for (int i = 0; i < n; i++) {
            weights[i] = Math.pow(1.0 - LAMBDA, i);
            weightSum += weights[i];
        }

        // Accumulators for weighted sums
        double wMinutes = 0, wPts = 0, wReb = 0, wAst = 0;
        double wStl = 0, wBlk = 0, wFg3m = 0, wTov = 0;
        double wUsg = 0, wTs = 0, wPace = 0;
        int advancedCount = 0; // tracks how many games had advanced stats

        for (int i = 0; i < n; i++) {
            PlayerGameStatistic s = qualified.get(i);
            double w = weights[i];

            double minutes = parseMinutes(s.getMinutesPlayed());
            if (minutes <= 0) continue; // safety — shouldn't reach here after DNP filter

            // Per-minute rates — all divided by minutes to make them pace-neutral
            wMinutes += minutes * w;
            wPts     += safeRate(s.getPointsScored(),       minutes) * w;
            wReb     += safeRate(s.getTotalRebounds(),      minutes) * w;
            wAst     += safeRate(s.getAssists(),            minutes) * w;
            wStl     += safeRate(s.getSteals(),             minutes) * w;
            wBlk     += safeRate(s.getBlocks(),             minutes) * w;
            wFg3m    += safeRate(s.getThreePointShotsMade(), minutes) * w;
            wTov     += safeRate(s.getTurnovers(),          minutes) * w;

            // Advanced stats — already rate stats, weight directly
            if (s.getGame() != null) {
                PlayerAdvancedStats adv = advancedByGameId.get(s.getGame().getGameId());
                if (adv != null) {
                    if (adv.getUsagePercentage()      != null) wUsg  += adv.getUsagePercentage()      * w;
                    if (adv.getTrueShootingPercentage() != null) wTs += adv.getTrueShootingPercentage() * w;
                    if (adv.getPace()                  != null) wPace += adv.getPace()                  * w;
                    advancedCount++;
                }
            }
        }

        // Normalize by total weight sum
        double avgMin  = round(wMinutes / weightSum);
        double ptsPerMin  = round(wPts  / weightSum);
        double rebPerMin  = round(wReb  / weightSum);
        double astPerMin  = round(wAst  / weightSum);
        double stlPerMin  = round(wStl  / weightSum);
        double blkPerMin  = round(wBlk  / weightSum);
        double fg3mPerMin = round(wFg3m / weightSum);
        double tovPerMin  = round(wTov  / weightSum);

        // Track the weight sum only for games that had advanced stats
        double advancedWeightSum = 0.0;
        for (int i = 0; i < n; i++) {
            PlayerGameStatistic s = qualified.get(i);
            if (s.getGame() != null && advancedByGameId.containsKey(s.getGame().getGameId())) {
                advancedWeightSum += weights[i];
            }
        }

        Double usgPct  = advancedWeightSum > 0 ? round(wUsg  / advancedWeightSum) : null;
        Double tsPct   = advancedWeightSum > 0 ? round(wTs   / advancedWeightSum) : null;
        Double avgPace = advancedWeightSum > 0 ? round(wPace / advancedWeightSum) : null;

        return new EWMASnapshot(
                ptsPerMin, rebPerMin, astPerMin, stlPerMin,
                blkPerMin, fg3mPerMin, tovPerMin,
                avgMin,
                usgPct, tsPct, avgPace,
                n,
                n < MIN_GAMES_THRESHOLD,
                OffsetDateTime.now().toString()
        );
    }


    /**
     * Parses the minutesPlayed string into a double.
     * Returns 0.0 if unparseable.
     */
    private double parseMinutes(String min) {
        if (min == null || min.isBlank()) return 0.0;
        try {
            if (min.contains(":")) {
                // "MM:SS" format
                String[] parts = min.split(":");
                return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]) / 60.0;
            }
            return Double.parseDouble(min);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Returns stat / minutes, or 0.0 if stat is null or minutes is 0.
     */
    private double safeRate(Integer stat, double minutes) {
        if (stat == null || minutes <= 0) return 0.0;
        return stat / minutes;
    }

    private double round(double val) {
        return Math.round(val * 10000.0) / 10000.0; // 4 decimal places for per-min rates
    }
}