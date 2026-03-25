package com.nba.nbanonbettingapp.service;

import com.nba.nbanonbettingapp.entity.Game;
import com.nba.nbanonbettingapp.entity.Team;
import com.nba.nbanonbettingapp.entity.TeamStanding;
import com.nba.nbanonbettingapp.repository.GameRepository;
import com.nba.nbanonbettingapp.repository.TeamStandingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TeamStandingService {

    private final GameRepository gameRepository;
    private final TeamStandingRepository teamStandingRepository;

    public TeamStandingService(GameRepository gameRepository,
                               TeamStandingRepository teamStandingRepository) {
        this.gameRepository = gameRepository;
        this.teamStandingRepository = teamStandingRepository;
    }

    private TeamStanding createEmptyStanding(Team team, Integer seasonYear) {
        TeamStanding standing = new TeamStanding();
        standing.setTeam(team);
        standing.setSeasonYear(seasonYear);
        standing.setGamesPlayed(0);
        standing.setWins(0);
        standing.setLosses(0);
        standing.setWinPercentage(0.0);
        standing.setPointsFor(0);
        standing.setPointsAgainst(0);
        standing.setPointDifference(0);
        return standing;
    }

    @Transactional
    public void refreshStandings(Integer seasonYear) {
        List<Game> games = gameRepository.findBySeasonYearAndPostseasonFalse(seasonYear);

        Map<Long, TeamStanding> standingsMap = new HashMap<>();

        for (Game game : games) {
            Team homeTeam = game.getHomeTeam();
            Team awayTeam = game.getAwayTeam();

            if (homeTeam == null || awayTeam == null) continue;

            Integer homeScore = game.getHomeTeamScore();
            Integer awayScore = game.getAwayTeamScore();

            if (homeScore == null || awayScore == null) continue;

            Long homeTeamId = homeTeam.getTeamId();
            Long awayTeamId = awayTeam.getTeamId();

            standingsMap.putIfAbsent(homeTeamId, createEmptyStanding(homeTeam, seasonYear));
            standingsMap.putIfAbsent(awayTeamId, createEmptyStanding(awayTeam, seasonYear));

            TeamStanding homeStanding = standingsMap.get(homeTeamId);
            TeamStanding awayStanding = standingsMap.get(awayTeamId);

            homeStanding.setGamesPlayed(homeStanding.getGamesPlayed() + 1);
            awayStanding.setGamesPlayed(awayStanding.getGamesPlayed() + 1);

            homeStanding.setPointsFor(homeStanding.getPointsFor() + homeScore);
            homeStanding.setPointsAgainst(homeStanding.getPointsAgainst() + awayScore);

            awayStanding.setPointsFor(awayStanding.getPointsFor() + awayScore);
            awayStanding.setPointsAgainst(awayStanding.getPointsAgainst() + homeScore);

            if (homeScore > awayScore) {
                homeStanding.setWins(homeStanding.getWins() + 1);
                awayStanding.setLosses(awayStanding.getLosses() + 1);
            } else if (awayScore > homeScore) {
                awayStanding.setWins(awayStanding.getWins() + 1);
                homeStanding.setLosses(homeStanding.getLosses() + 1);
            }
        }

        for (TeamStanding standing : standingsMap.values()) {
            if (standing.getGamesPlayed() > 0) {
                standing.setWinPercentage((double) standing.getWins() / standing.getGamesPlayed());
            } else {
                standing.setWinPercentage(0.0);
            }

            standing.setPointDifference(standing.getPointsFor() - standing.getPointsAgainst());
            standing.setUpdatedAt(OffsetDateTime.now());
        }

        teamStandingRepository.deleteBySeasonYear(seasonYear);
        teamStandingRepository.flush();
        teamStandingRepository.saveAll(standingsMap.values());
    }

    public List<TeamStanding> getStandingsBySeason(Integer seasonYear) {
        return teamStandingRepository.findBySeasonYearOrderByWinsDescPointDifferenceDesc(seasonYear);
    }
}