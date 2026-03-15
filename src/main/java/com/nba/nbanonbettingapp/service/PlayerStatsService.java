package com.nba.nbanonbettingapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nba.nbanonbettingapp.entity.Game;
import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.entity.PlayerGameStatistic;
import com.nba.nbanonbettingapp.entity.Team;
import com.nba.nbanonbettingapp.repository.GameRepository;
import com.nba.nbanonbettingapp.repository.PlayerGameStatisticRepository;
import com.nba.nbanonbettingapp.repository.PlayerRepository;
import com.nba.nbanonbettingapp.repository.TeamRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import com.nba.nbanonbettingapp.dto.HeadToHeadResultDTO;
import com.nba.nbanonbettingapp.dto.StatType;

@Service
public class PlayerStatsService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final PlayerGameStatisticRepository statRepository;
    private final BalldontlieService balldontlieService;

    public PlayerStatsService(PlayerRepository playerRepository,
                              TeamRepository teamRepository,
                              GameRepository gameRepository,
                              PlayerGameStatisticRepository statRepository,
                              BalldontlieService balldontlieService) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.statRepository = statRepository;
        this.balldontlieService = balldontlieService;
    }

    /**
     * Main endpoint helper:
     * - user passes Balldontlie player id (external_api_id)
     * - we find the Player row in DB
     * - then load recent stats (DB-first, else API then store)
     */
    public List<PlayerGameStatistic> getRecentStatsByExternalApiId(Long externalApiId, int limit) {
        Player player = playerRepository.findByExternalApiId(externalApiId)
                .orElseThrow(() -> new RuntimeException("Player not found by externalApiId: " + externalApiId));

        return getRecentStats(player.getPlayerId(), limit);
    }

    /**
     * DB-first:
     * 1) Try DB stats for this player (limit)
     * 2) If empty, call Balldontlie /stats, store stats + store missing games/teams
     * 3) Return from DB
     */
    public List<PlayerGameStatistic> getRecentStats(Long playerId, int limit) {

        limit = normalizeLimit(limit);

        // Try DB first
        List<PlayerGameStatistic> db = statRepository.findByPlayer_PlayerIdOrderByGame_GameDateDesc(
                playerId, PageRequest.of(0, limit)
        );

        OffsetDateTime lastSync = statRepository
                .findLatestSyncedAtByPlayerId(playerId)
                .orElse(null);

        boolean stale = lastSync == null ||
                Duration.between(lastSync, OffsetDateTime.now()).toHours() >= 12;

        // If stats are fresh AND we already have enough rows, return DB
        if (!stale && db.size() >= limit) {
            return db;
        }

        // Need to fetch more (or DB empty)
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found in DB: " + playerId));

        Long playerApiId = player.getExternalApiId();
        if (playerApiId == null) return db; // return what we have

        // Paginate through all pages — BallDontLie returns oldest-first so we need
        // all pages to find the most recent games
        List<com.nba.nbanonbettingapp.dto.BdlStatDTO> apiStats = new java.util.ArrayList<>();
        Integer cursor = null;
        do {
            var resp = balldontlieService.getStatsByPlayerSince(playerApiId, "2021-10-01", 100, cursor);
            var page = resp.data();
            if (page == null || page.isEmpty()) break;
            apiStats.addAll(page);
            cursor = (resp.meta() != null) ? resp.meta().nextCursor() : null;
        } while (cursor != null);

        if (apiStats.isEmpty()) return db;

        // Sort newest first and take up to "limit"
        var sorted = apiStats.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.game() != null && s.game().id() != null)
                .sorted(Comparator.comparing((com.nba.nbanonbettingapp.dto.BdlStatDTO s) -> parseLocalDateSafe(s.game().date()))
                        .reversed())
                .limit(limit)
                .toList();

        // Save stats + games (+ teams) for up to limit games
        OffsetDateTime now = OffsetDateTime.now();
        for (var s : sorted) {

            Long gameApiId = s.game().id().longValue();
            Game game = upsertGameWithTeams(gameApiId);

            Team statTeam = null;
            if (s.team() != null && s.team().id() != null) {
                Long teamApiId = s.team().id().longValue();
                statTeam = teamRepository.findByExternalApiId(teamApiId)
                        .orElseGet(() -> {
                            Team t = new Team();
                            t.setExternalApiId(teamApiId);
                            t.setTeamName(s.team().fullName());
                            t.setCity(s.team().city());
                            t.setAbbreviation(s.team().abbreviation());
                            t.setConference(s.team().conference());
                            t.setDivision(s.team().division());
                            t.setCreatedAt(OffsetDateTime.now());
                            return teamRepository.save(t);
                        });
            }

            PlayerGameStatistic stat = statRepository
                    .findByGame_GameIdAndPlayer_PlayerId(game.getGameId(), player.getPlayerId())
                    .orElseGet(PlayerGameStatistic::new);

            stat.setGame(game);
            stat.setPlayer(player);
            stat.setTeam(statTeam);

            stat.setMinutesPlayed(s.min());
            stat.setPointsScored(s.pts());
            stat.setTotalRebounds(s.reb());
            stat.setAssists(s.ast());
            stat.setSteals(s.stl());
            stat.setBlocks(s.blk());
            stat.setTurnovers(s.turnover());
            stat.setFieldGoalsMade(s.fgm());
            stat.setFieldGoalsAttempted(s.fga());
            stat.setThreePointShotsMade(s.fg3m());
            stat.setThreePointShotsAttempted(s.fg3a());
            stat.setFreeThrowsMade(s.ftm());
            stat.setFreeThrowsAttempted(s.fta());

            if (stat.getCreatedAt() == null) stat.setCreatedAt(now);
            stat.setSyncedAt(now);

            statRepository.save(stat);
        }

        // Return again from DB
        return statRepository.findByPlayer_PlayerIdOrderByGame_GameDateDesc(
                playerId, PageRequest.of(0, limit)
        );
    }

    /**
     * Analyzes a player's recent N games and computes prop analytics.
     *
     * Reuses getRecentStatsByExternalApiId() for DB-first data fetching and storage,
     * then applies DNP filter, postseason filter, StatType extraction,
     * and computes average, stdDev, and hit rate.
     *
     * @param playerApiId     BallDontLie player ID (external_api_id)
     * @param statLine        Sportsbook line (e.g. 15.5)
     * @param limit           Number of recent games (5, 10, or 15)
     * @param includePlayoffs Whether to include postseason games
     * @param statType        Stat to analyze: pts, ast, reb, stl, blk, turnover, fg3m
     */
    public HeadToHeadResultDTO analyzeRecentStats(Long playerApiId, double statLine,
                                                  int limit, boolean includePlayoffs,
                                                  String statType) {

        // Step 1: Reuse existing method — fetches, stores, returns from DB
        List<PlayerGameStatistic> recentStats =
                getRecentStatsByExternalApiId(playerApiId, limit);

        // Step 2: Resolve player name
        Player player = playerRepository.findByExternalApiId(playerApiId)
                .orElseThrow(() -> new RuntimeException(
                        "Player not found with externalApiId: " + playerApiId));
        String playerName = player.getFirstName() + " " + player.getLastName();

        // Step 3: Resolve stat type
        StatType stat = StatType.from(statType);

        // Step 4: Apply DNP filter (zero minutes) and optional postseason filter
        List<PlayerGameStatistic> filteredStats = recentStats.stream()
                .filter(s -> {
                    String min = s.getMinutesPlayed();
                    return min != null && !min.isBlank()
                            && !min.equals("0") && !min.equals("00")
                            && !min.equals("0:00") && !min.equals("00:00");
                })
                .filter(s -> includePlayoffs ||
                        s.getGame() == null ||
                        !Boolean.TRUE.equals(s.getGame().getPostseason()))
                .toList();

        if (filteredStats.isEmpty()) {
            throw new RuntimeException(
                    "No qualifying games found for " + playerName +
                            " after applying filters. Try increasing the limit or enabling playoffs.");
        }

        // Step 5: Extract stat values and compute analytics
        List<Integer> values = filteredStats.stream()
                .map(s -> extractStatValue(stat, s))
                .toList();

        double average  = values.stream().mapToInt(i -> i).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(p -> Math.pow(p - average, 2))
                .average().orElse(0.0);
        double stdDev   = Math.sqrt(variance);
        long   hits     = values.stream().filter(v -> v > statLine).count();
        double hitRate  = (double) hits / values.size();

        // Step 6: Build per-game result list
        List<HeadToHeadResultDTO.GameLineResult> gameResults = filteredStats.stream()
                .map(s -> {
                    String date = s.getGame() != null && s.getGame().getGameDate() != null
                            ? s.getGame().getGameDate().toString() : "unknown";
                    int val = extractStatValue(stat, s);
                    return new HeadToHeadResultDTO.GameLineResult(date, val, val > statLine);
                })
                .toList();

        // Step 7: Return analytics DTO
        return new HeadToHeadResultDTO(
                playerName,
                "All Opponents",
                stat.getKey(),
                statLine,
                gameResults,
                round(average),
                round(stdDev),
                (int) hits,
                values.size(),
                round(hitRate)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

    private int normalizeLimit(int limit) {
        if (limit != 5 && limit != 10 && limit != 15) return 5;
        return limit;
    }

    /**
     * Extracts the correct stat value from a PlayerGameStatistic
     * based on the requested StatType.
     */
    private int extractStatValue(StatType stat, PlayerGameStatistic s) {
        return switch (stat) {
            case PTS      -> s.getPointsScored()        != null ? s.getPointsScored()        : 0;
            case REB      -> s.getTotalRebounds()        != null ? s.getTotalRebounds()        : 0;
            case AST      -> s.getAssists()              != null ? s.getAssists()              : 0;
            case STL      -> s.getSteals()               != null ? s.getSteals()               : 0;
            case BLK      -> s.getBlocks()               != null ? s.getBlocks()               : 0;
            case TURNOVER -> s.getTurnovers()            != null ? s.getTurnovers()            : 0;
            case FG3M     -> s.getThreePointShotsMade()  != null ? s.getThreePointShotsMade()  : 0;
        };
    }

    /** Rounds to 2 decimal places for clean API output. */
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Ensures a Game row exists AND has homeTeam + awayTeam.
     * Calls Balldontlie /games/{id} only if needed.
     */
    private Game upsertGameWithTeams(Long gameApiId) {

        Game existing = gameRepository.findByExternalApiId(gameApiId).orElse(null);
        if (existing != null && existing.getHomeTeam() != null && existing.getAwayTeam() != null) {
            return existing;
        }

        // call /games/{id} to get full details with home_team + visitor_team
        JsonNode fullGame = balldontlieService.getGameById(gameApiId);

        JsonNode homeNode = fullGame.get("home_team");

        // some versions use visitor_team, some use away_team so this will fix the problem
        JsonNode awayNode = fullGame.has("visitor_team")
                ? fullGame.get("visitor_team")
                : fullGame.get("away_team");

        Team home = upsertTeamFromJson(homeNode);
        Team away = upsertTeamFromJson(awayNode);

        if (home == null || away == null) {
            throw new RuntimeException("Game " + gameApiId + " missing home/away teams in API response");
        }

        Game g = (existing != null) ? existing : new Game();
        g.setExternalApiId(gameApiId);

        String dateStr = fullGame.path("date").asText(null);
        g.setGameDate(parseLocalDateSafe(dateStr));

        if (!fullGame.path("season").isMissingNode() && !fullGame.path("season").isNull()) {
            g.setSeasonYear(fullGame.path("season").asInt());
        }

        // Optional fields if present in JSON
        if (!fullGame.path("home_team_score").isMissingNode() && !fullGame.path("home_team_score").isNull()) {
            g.setHomeTeamScore(fullGame.path("home_team_score").asInt());
        }
        if (!fullGame.path("visitor_team_score").isMissingNode() && !fullGame.path("visitor_team_score").isNull()) {
            g.setAwayTeamScore(fullGame.path("visitor_team_score").asInt());
        }
        if (!fullGame.path("status").isMissingNode() && !fullGame.path("status").isNull()) {
            g.setGameStatus(fullGame.path("status").asText());
        }
        // Store postseason flag for playoff filtering in analytics
        if (!fullGame.path("postseason").isMissingNode() && !fullGame.path("postseason").isNull()) {
            g.setPostseason(fullGame.path("postseason").asBoolean());
        }

        g.setHomeTeam(home);
        g.setAwayTeam(away);

        if (g.getCreatedAt() == null) g.setCreatedAt(OffsetDateTime.now());

        return gameRepository.save(g);
    }

    /**
     * Upserts a Team using JSON from /games/{id}.
     * Returns existing team if found by externalApiId,
     * otherwise creates and saves a new Team.
     */
    private Team upsertTeamFromJson(JsonNode teamNode) {
        if (teamNode == null || teamNode.isNull()) return null;
        JsonNode idNode = teamNode.get("id");
        if (idNode == null || idNode.isNull()) return null;

        Long apiId = idNode.asLong();

        return teamRepository.findByExternalApiId(apiId).orElseGet(() -> {
            Team t = new Team();
            t.setExternalApiId(apiId);
            t.setTeamName(teamNode.path("full_name").asText(null));
            t.setCity(teamNode.path("city").asText(null));
            t.setAbbreviation(teamNode.path("abbreviation").asText(null));
            t.setConference(teamNode.path("conference").asText(null));
            t.setDivision(teamNode.path("division").asText(null));
            t.setCreatedAt(OffsetDateTime.now());
            return teamRepository.save(t);
        });
    }

    /**
     * Parses a date string into LocalDate.
     * Handles both plain dates ("YYYY-MM-DD")
     * and ISO date-time strings ("YYYY-MM-DDTHH:MM:SSZ").
     * Returns null if parsing fails.
     */
    private LocalDate parseLocalDateSafe(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;

        try {
            // Plain date "YYYY-MM-DD"
            if (dateStr.length() == 10) {
                return LocalDate.parse(dateStr);
            }
            // Full ISO datetime
            return OffsetDateTime.parse(dateStr).toLocalDate();
        } catch (Exception e) {
            try {
                if (dateStr.length() >= 10) {
                    return LocalDate.parse(dateStr.substring(0, 10));
                }
            } catch (Exception ignored) {
            }
            return null;
        }
    }
}