package com.nba.nbanonbettingapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nba.nbanonbettingapp.dto.BdlGameDTO;
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
 * Analyzes how a player has historically performed against a specific opponent.
 * Computes average, standard deviation, and hit rate against a user-provided stat line.
 *
 * Step A: Call /games?team_ids[]=<opponentId> to get all game IDs the opponent played in.
 *      This endpoint returns home_team/visitor_team correctly.
 *
 *Step B: Call /stats?player_ids[]=<playerId>&game_ids[]=id1&game_ids[]=id2...
 *      This returns only the player's stats in those specific games.
 */

@Service
public class HeadToHeadService {

    private final PlayerRepository playerRepository;
    private final BalldontlieService balldontlieService;

    //Seasons to scan for opponent games. 5 seasons gives plenty of matchup history.
    //BallDontLie season = the year the season started (2025 = 2025-26 season).
    private static final List<Integer> SEASONS = List.of(2025, 2024, 2023, 2022, 2021);

    //Max game IDs per /stats call — keeps URLs well within safe length limits.
    private static final int GAME_ID_BATCH_SIZE = 50;

    public HeadToHeadService(PlayerRepository playerRepository,
                             BalldontlieService balldontlieService) {
        this.playerRepository = playerRepository;
        this.balldontlieService = balldontlieService;
    }

    /**
     * Analyze a player's historical performance against a specific opponent.
     *
     * @param playerApiId   BallDontLie player ID (external_api_id on Player entity)
     * @param opponentApiId BallDontLie team ID for the opponent
     * @param statLine      Prediction line entered by the user (e.g. 15.5)
     * @param limit           How many most-recent matchup games to analyze (default 5)
     * @param includePlayoffs Whether to include postseason games (default true)
     * @param statType        Stat to analyze: pts, ast, reb, stl, blk, turnover, fg3m
     */
    public HeadToHeadResultDTO analyze(Long playerApiId, Long opponentApiId,
                                       double statLine, int limit,
                                       boolean includePlayoffs, String statType) {

        //Step 1: Resolve player name from DB (read-only)
        Player player = playerRepository.findByExternalApiId(playerApiId)
                .orElseThrow(() -> new RuntimeException(
                        "Player not found with externalApiId: " + playerApiId +
                                ". Make sure the player has been searched first."));

        String playerName = player.getFirstName() + " " + player.getLastName();
        StatType stat = StatType.from(statType);

        //Step 2: Resolve opponent name
        String opponentTeamName = resolveTeamName(opponentApiId);

        //Step 3: Collect all game IDs the opponent has played in
        // /games?team_ids[]=<opponentId> returns full game objects with correct
        // home_team/visitor_team — we just need the IDs from each game.
        List<Long> opponentGameIds = fetchOpponentGameIds(opponentApiId, includePlayoffs);

        if (opponentGameIds.isEmpty()) {
            throw new RuntimeException(
                    "No games found for opponent team ID " + opponentApiId +
                            ". Check that the opponentApiId is a valid BallDontLie team ID.");
        }

        //Step 4: Fetch player stats in only those games (batched)
        // /stats?player_ids[]=X&game_ids[]=id1&game_ids[]=id2... is the correct
        // way to filter by opponent — BallDontLie handles it server-side.
        List<BdlStatDTO> allMatchups = fetchStatsByGameIds(playerApiId, opponentGameIds);

        if (allMatchups.isEmpty()) {
            throw new RuntimeException(
                    "No historical matchup data found for " + playerName +
                            " vs " + opponentTeamName +
                            ". The player may not have faced this team in the periods covered.");
        }

        //Step 5: Sort newest-first, trim to `limit` most recent games
        // Also filter out DNP entries (0 or null minutes) — injured/inactive games
        // skew the average and hit rate and shouldn't count as matchup data.
        // BallDontLie represents minutes as "0:00", "00:00", null, or "0" for DNPs.
        List<BdlStatDTO> matchups = allMatchups.stream()
                .filter(s -> s.game() != null && s.game().date() != null)
                .filter(s -> s.min() != null && !s.min().isBlank()
                        && !s.min().equals("0") && !s.min().equals("00")
                        && !s.min().equals("0:00") && !s.min().equals("00:00"))
                .sorted(Comparator.comparing((BdlStatDTO s) -> s.game().date()).reversed())
                .limit(limit)
                .toList();

        //Step 6: Extract stat values
        List<Integer> points = matchups.stream()
                .map(s -> { Integer v = stat.extract(s); return v != null ? v : 0; })
                .toList();

        //Step 7: Compute analytics
        double average = computeAverage(points);
        double stdDev  = computeStdDev(points, average);
        long   hits    = points.stream().filter(p -> p > statLine).count();
        double hitRate = (double) hits / points.size();

        //Step 8: Build per-game result list
        List<HeadToHeadResultDTO.GameLineResult> gameResults = matchups.stream()
                .map(s -> {
                    String date = s.game().date();
                    int    val  = stat.extract(s) != null ? stat.extract(s) : 0;
                    return new HeadToHeadResultDTO.GameLineResult(date, val, val > statLine);
                })
                .toList();

        //Step 9: Assemble and return
        return new HeadToHeadResultDTO(
                playerName,
                opponentTeamName,
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

    // ─────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pages through /games?team_ids[]=<opponentId> and collects all game IDs.
     * The /games endpoint correctly returns home_team and visitor_team objects.
     *
     * Collects all game IDs the opponent played in across the target seasons.
     *
     * Passes seasons[] to /games so we only get recent history, not 1976 data.
     * Each season is ~82 games, so 4 seasons = ~328 games across a few pages.
     */
    private List<Long> fetchOpponentGameIds(Long opponentApiId, boolean includePlayoffs) {
        List<Long> gameIds = new ArrayList<>();
        Integer cursor = null;

        //Loop handles pagination within the season-filtered result set
        while (true) {
            BdlResponseDTO<BdlGameDTO> response =
                    balldontlieService.getGamesByTeam(opponentApiId, SEASONS, 100, cursor);

            List<BdlGameDTO> page = response.data();
            if (page == null || page.isEmpty()) break;

            for (BdlGameDTO game : page) {
                if (game.id() == null) continue;
                // Filter out postseason games if not requested
                if (!includePlayoffs && Boolean.TRUE.equals(game.postseason())) continue;
                gameIds.add(game.id().longValue());
            }

            if (response.meta() == null || response.meta().nextCursor() == null) break;

            cursor = response.meta().nextCursor();
        }

        return gameIds;
    }

    /**
     * Fetches the player's stats in a given list of game IDs, batching requests
     * to stay within URL length limits (GAME_ID_BATCH_SIZE IDs per call).
     *
     * Each batch is a single /stats call. Results across all batches are combined.
     * Cursor pagination is used within each batch to handle edge cases where a
     * single batch returns more than one page (very unlikely but handled correctly).
     */
    private List<BdlStatDTO> fetchStatsByGameIds(Long playerApiId, List<Long> gameIds) {
        List<BdlStatDTO> collected = new ArrayList<>();

        //Process in batches of GAME_ID_BATCH_SIZE to avoid URL length limits
        for (int i = 0; i < gameIds.size(); i += GAME_ID_BATCH_SIZE) {
            List<Long> batch = gameIds.subList(i, Math.min(i + GAME_ID_BATCH_SIZE, gameIds.size()));

            //Paginate within this batch (in practice, one page is always enough)
            Integer cursor = null;
            do {
                BdlResponseDTO<BdlStatDTO> response =
                        balldontlieService.getStatsByPlayerAndGames(playerApiId, batch, cursor);

                List<BdlStatDTO> page = response.data();
                if (page == null || page.isEmpty()) break;

                collected.addAll(page);

                cursor = (response.meta() != null) ? response.meta().nextCursor() : null;

            } while (cursor != null);
        }

        return collected;
    }

    /** Resolves the opponent's display name via /teams/{id}.*/
    private String resolveTeamName(Long teamApiId) {
        try {
            JsonNode team = balldontlieService.getTeamById(teamApiId);
            if (team != null && team.has("full_name")) {
                return team.get("full_name").asText();
            }
        } catch (Exception e) {
            //Non-fatal — fall through to default
        }
        return "Unknown Opponent";
    }

    /** Mean */
    private double computeAverage(List<Integer> values) {
        return values.stream().mapToInt(i -> i).average().orElse(0.0);
    }

    /**
     * Population standard deviation.
     * Low = consistent vs this opponent. High = volatile.
     */
    private double computeStdDev(List<Integer> values, double average) {
        double variance = values.stream()
                .mapToDouble(p -> Math.pow(p - average, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    /** Rounds to 2 decimal places for clean API output. */
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}