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

        var resp = balldontlieService.getStatsByPlayerId(playerApiId, 100);
        var apiStats = resp.data();
        if (apiStats == null || apiStats.isEmpty()) return db;

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

    // Helpers Functions

    private int normalizeLimit(int limit) {
        if (limit != 5 && limit != 10 && limit != 15) return 5;
        return limit;
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