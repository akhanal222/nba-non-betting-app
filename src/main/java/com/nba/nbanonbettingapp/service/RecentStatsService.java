package com.nba.nbanonbettingapp.service;

import com.nba.nbanonbettingapp.dto.BdlResponseDTO;
import com.nba.nbanonbettingapp.dto.BdlStatDTO;
import com.nba.nbanonbettingapp.dto.HeadToHeadResultDTO;
import com.nba.nbanonbettingapp.dto.StatType;
import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *   Analyzes a player's performance across their most recent N games,
 *   regardless of opponent. Computes the same analytics as HeadToHeadService
 *   (average, standard deviation, hit rate) but without opponent filtering.
 *
 *   Calls /stats?player_ids[]=<playerId> with a large per_page, then filters,
 *   sorts newest-first, and trims to `limit`. Same DNP filter as HeadToHeadService.
 *   Postseason games are optionally excluded via the includePlayoffs flag.
 */
@Service
public class RecentStatsService {

    // Fetch stats from this date forward — covers ~4 seasons of history.
    // BallDontLie /stats respects start_date/end_date filters reliably.
    private static final String STATS_START_DATE = "2021-10-01";

    private static final int FETCH_PER_PAGE = 100;

    private final PlayerRepository playerRepository;
    private final BalldontlieService balldontlieService;

    public RecentStatsService(PlayerRepository playerRepository,
                              BalldontlieService balldontlieService) {
        this.playerRepository = playerRepository;
        this.balldontlieService = balldontlieService;
    }

    /**
     * Analyze a player's performance across their most recent N games.
     *
     * @param playerApiId     BallDontLie player ID (external_api_id on Player entity)
     * @param statLine        Sportsbook line entered by the user (e.g. 15.5)
     * @param limit           How many most-recent games to analyze (e.g. 5, 10, 15)
     * @param includePlayoffs Whether to include postseason games (default true)
     */
    public HeadToHeadResultDTO analyze(Long playerApiId, double statLine,
                                       int limit, boolean includePlayoffs, String statType) {

        // Step 1: Resolve player name from DB (read-only)
        Player player = playerRepository.findByExternalApiId(playerApiId)
                .orElseThrow(() -> new RuntimeException(
                        "Player not found with externalApiId: " + playerApiId +
                                ". Make sure the player has been searched first."));

        String playerName = player.getFirstName() + " " + player.getLastName();
        StatType stat = StatType.from(statType);

        // Step 2: Fetch all stats since STATS_START_DATE via pagination
        // BallDontLie returns oldest-first, so we must paginate through everything
        // and sort newest-first afterward. We cannot stop early by count because
        // the most recent games are on the last pages.
        List<BdlStatDTO> allStats = new ArrayList<>();
        Integer cursor = null;
        do {
            BdlResponseDTO<BdlStatDTO> response =
                    balldontlieService.getStatsByPlayerSince(
                            playerApiId, STATS_START_DATE, FETCH_PER_PAGE, cursor);

            List<BdlStatDTO> page = response.data();
            if (page == null || page.isEmpty()) break;

            allStats.addAll(page);
            cursor = (response.meta() != null) ? response.meta().nextCursor() : null;

        } while (cursor != null);

        if (allStats.isEmpty()) {
            throw new RuntimeException(
                    "No stats found for " + playerName +
                            ". The player may not have any recorded game data.");
        }

        // Step 3: Filter DNPs, optionally filter postseason, sort, trim
        List<BdlStatDTO> recentGames = allStats.stream()
                .filter(s -> s.game() != null && s.game().date() != null)
                .filter(s -> s.min() != null && !s.min().isBlank()
                        && !s.min().equals("0") && !s.min().equals("00")
                        && !s.min().equals("0:00") && !s.min().equals("00:00"))
                .filter(s -> includePlayoffs || !Boolean.TRUE.equals(s.game().postseason()))
                .sorted(Comparator.comparing((BdlStatDTO s) -> s.game().date()).reversed())
                .limit(limit)
                .toList();

        if (recentGames.isEmpty()) {
            throw new RuntimeException(
                    "No qualifying games found for " + playerName +
                            " after applying filters. Try increasing the limit or enabling playoffs.");
        }

        // Step 4: Extract stat values — null-safe
        List<Integer> points = recentGames.stream()
                .map(s -> { Integer v = stat.extract(s); return v != null ? v : 0; })
                .toList();

        // Step 5: Compute analytics
        double average = computeAverage(points);
        double stdDev  = computeStdDev(points, average);
        long   hits    = points.stream().filter(p -> p > statLine).count();
        double hitRate = (double) hits / points.size();

        // Step 6: Build per-game result list
        List<HeadToHeadResultDTO.GameLineResult> gameResults = recentGames.stream()
                .map(s -> {
                    String date = s.game().date();
                    int    val  = stat.extract(s) != null ? stat.extract(s) : 0;
                    return new HeadToHeadResultDTO.GameLineResult(date, val, val > statLine);
                })
                .toList();

        // Step 7: Assemble and return
        // opponentTeamName is "All Opponents" — no specific matchup filtering applied
        return new HeadToHeadResultDTO(
                playerName,
                "All Opponents",
                stat.getKey(),
                statLine,
                gameResults,
                round(average),
                round(stdDev),
                (int) hits,
                points.size(),
                round(hitRate)
        );
    }

    private double computeAverage(List<Integer> values) {
        return values.stream().mapToInt(i -> i).average().orElse(0.0);
    }

    private double computeStdDev(List<Integer> values, double average) {
        double variance = values.stream()
                .mapToDouble(p -> Math.pow(p - average, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}