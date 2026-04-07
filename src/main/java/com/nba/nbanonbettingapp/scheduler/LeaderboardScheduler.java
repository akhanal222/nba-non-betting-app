package com.nba.nbanonbettingapp.scheduler;

import com.nba.nbanonbettingapp.service.PlayerStatLeaderboardService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class LeaderboardScheduler {

    private final PlayerStatLeaderboardService leaderboardService;

    public LeaderboardScheduler(PlayerStatLeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    // Runs every day at 3 AM New York time.
    @Scheduled(cron = "0 0 3 * * *", zone = "America/New_York")
    public void refreshLeaderboardsDaily() {
        int season = ZonedDateTime.now(ZoneId.of("America/New_York")).getYear();

        leaderboardService.refreshLeaderboard(season, "PTS");
        leaderboardService.refreshLeaderboard(season, "REB");
        leaderboardService.refreshLeaderboard(season, "AST");

        System.out.println("Leaderboard refreshed at 3 AM ET for season " + season);
    }
}