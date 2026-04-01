package com.nba.nbanonbettingapp.service;

import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.entity.PlayerGameStatistic;
import com.nba.nbanonbettingapp.entity.Team;
import com.nba.nbanonbettingapp.entity.TeamDefenseVsPosition;
import com.nba.nbanonbettingapp.repository.GameRepository;
import com.nba.nbanonbettingapp.repository.PlayerGameStatisticRepository;
import com.nba.nbanonbettingapp.repository.TeamDefenseVsPositionRepository;
import com.nba.nbanonbettingapp.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds Defense-vs-Position (DvP) matchup scores from data already in the DB.
 *
 * Run manually via POST /api/admin/dvp/refresh, and automatically nightly
 * via @Scheduled after games have been ingested.
 */
@Service
public class DvPIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DvPIngestionService.class);

    private static final int CURRENT_SEASON = 2025;

    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final PlayerGameStatisticRepository statRepository;
    private final TeamDefenseVsPositionRepository dvpRepository;

    public DvPIngestionService(TeamRepository teamRepository,
                               GameRepository gameRepository,
                               PlayerGameStatisticRepository statRepository,
                               TeamDefenseVsPositionRepository dvpRepository) {
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.statRepository = statRepository;
        this.dvpRepository = dvpRepository;
    }


    public void refreshCurrentSeason() {
        refresh(CURRENT_SEASON);
    }

    /**
     * Runs nightly at 3 AM after game stats have been ingested.
     * Keeps DvP profiles current without any manual intervention.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledRefresh() {
        log.info("DvP: starting nightly refresh for season {}", CURRENT_SEASON);
        refreshCurrentSeason();
        log.info("DvP: nightly refresh complete");
    }

    private void refresh(int season) {
        List<Team> allTeams = teamRepository.findAll();
        log.info("DvP: refreshing {} teams for season {}", allTeams.size(), season);

        for (Team team : allTeams) {
            try {
                computeAndSaveForTeam(team, season);
            } catch (Exception e) {
                log.warn("DvP: failed for team={} — {}", team.getTeamName(), e.getMessage());
            }
        }
    }

    /**
     * For one team: finds all games they played, collects stats for opposing
     * players in those games, groups by position, computes averages, saves.
     */
    private void computeAndSaveForTeam(Team team, int season) {

        List<com.nba.nbanonbettingapp.entity.Game> games =
                gameRepository.findBySeasonYearAndPostseasonFalse(season);

        List<Long> teamGameIds = games.stream()
                .filter(g -> {
                    boolean home = g.getHomeTeam() != null &&
                            g.getHomeTeam().getTeamId().equals(team.getTeamId());
                    boolean away = g.getAwayTeam() != null &&
                            g.getAwayTeam().getTeamId().equals(team.getTeamId());
                    return home || away;
                })
                .map(com.nba.nbanonbettingapp.entity.Game::getGameId)
                .toList();

        if (teamGameIds.isEmpty()) return;

        // Collect all player stat rows for opposing players in those games
        // "opposing" means the stat's team_id != this team's team_id
        Map<String, List<PlayerGameStatistic>> byPosition = new HashMap<>();
        byPosition.put("G", new ArrayList<>());
        byPosition.put("F", new ArrayList<>());
        byPosition.put("C", new ArrayList<>());

        for (Long gameId : teamGameIds) {
            // Get all player stats for this game
            List<PlayerGameStatistic> gameStats = statRepository
                    .findByPlayer_PlayerIdOrderByGame_GameDateDesc(
                            -1L, PageRequest.of(0, 1)) // placeholder — we need a different query
                    .stream().toList();
        }

        Map<String, StatAccumulator> accumulators = new HashMap<>();
        accumulators.put("G", new StatAccumulator());
        accumulators.put("F", new StatAccumulator());
        accumulators.put("C", new StatAccumulator());

        for (com.nba.nbanonbettingapp.entity.Game game : games) {
            if (!teamGameIds.contains(game.getGameId())) continue;

            // Get all stats for this game from DB
            for (PlayerGameStatistic stat : game.getPlayerStats()) {

                // Skip DNP rows
                String min = stat.getMinutesPlayed();
                if (min == null || min.isBlank() ||
                        min.equals("0") || min.equals("00") ||
                        min.equals("0:00") || min.equals("00:00")) continue;

                // Filters only opponents
                if (stat.getTeam() != null &&
                        stat.getTeam().getTeamId().equals(team.getTeamId())) continue;

                // Determine position group
                Player player = stat.getPlayer();
                if (player == null || player.getPosition() == null) continue;
                String posGroup = normalizePosition(player.getPosition());
                if (posGroup == null) continue;

                accumulators.get(posGroup).add(stat);
            }
        }

        // Save one DvP row per position
        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<String, StatAccumulator> entry : accumulators.entrySet()) {
            String position = entry.getKey();
            StatAccumulator acc = entry.getValue();
            if (acc.count == 0) continue;

            TeamDefenseVsPosition dvp = dvpRepository
                    .findByTeam_TeamIdAndSeasonAndPosition(team.getTeamId(), season, position)
                    .orElseGet(TeamDefenseVsPosition::new);

            dvp.setTeam(team);
            dvp.setSeason(season);
            dvp.setPosition(position);
            dvp.setPtsAllowed(round(acc.avgPts()));
            dvp.setRebAllowed(round(acc.avgReb()));
            dvp.setAstAllowed(round(acc.avgAst()));
            dvp.setFg3mAllowed(round(acc.avgFg3m()));
            dvp.setStlAllowed(round(acc.avgStl()));
            dvp.setBlkAllowed(round(acc.avgBlk()));
            dvp.setTovAllowed(round(acc.avgTov()));
            dvp.setUpdatedAt(now);

            dvpRepository.save(dvp);
        }

        log.debug("DvP: saved G/F/C rows for team={} season={}", team.getTeamName(), season);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Maps BDL position strings to three groups.
     */
    private String normalizePosition(String pos) {
        if (pos == null) return null;
        String p = pos.toUpperCase().trim();
        if (p.startsWith("G") || p.equals("PG") || p.equals("SG")) return "G";
        if (p.startsWith("F") || p.equals("SF") || p.equals("PF")) return "F";
        if (p.startsWith("C"))                                        return "C";
        // Handle combo positions like "G-F" → take first
        if (p.contains("-")) return normalizePosition(p.split("-")[0]);
        return null;
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }


    private static class StatAccumulator {
        int count = 0;
        double pts = 0, reb = 0, ast = 0, fg3m = 0, stl = 0, blk = 0, tov = 0;

        void add(PlayerGameStatistic s) {
            count++;
            pts  += s.getPointsScored()        != null ? s.getPointsScored()        : 0;
            reb  += s.getTotalRebounds()        != null ? s.getTotalRebounds()        : 0;
            ast  += s.getAssists()              != null ? s.getAssists()              : 0;
            fg3m += s.getThreePointShotsMade()  != null ? s.getThreePointShotsMade()  : 0;
            stl  += s.getSteals()               != null ? s.getSteals()               : 0;
            blk  += s.getBlocks()               != null ? s.getBlocks()               : 0;
            tov  += s.getTurnovers()            != null ? s.getTurnovers()            : 0;
        }

        double avgPts()  { return count > 0 ? pts  / count : 0; }
        double avgReb()  { return count > 0 ? reb  / count : 0; }
        double avgAst()  { return count > 0 ? ast  / count : 0; }
        double avgFg3m() { return count > 0 ? fg3m / count : 0; }
        double avgStl()  { return count > 0 ? stl  / count : 0; }
        double avgBlk()  { return count > 0 ? blk  / count : 0; }
        double avgTov()  { return count > 0 ? tov  / count : 0; }
    }
}