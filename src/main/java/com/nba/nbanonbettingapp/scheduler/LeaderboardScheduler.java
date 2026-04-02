package com.nba.nbanonbettingapp.scheduler;

import com.nba.nbanonbettingapp.service.PlayerStatLeaderboardService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardScheduler {

    private final PlayerStatLeaderboardService leaderboardService;

    public LeaderboardScheduler(PlayerStatLeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    // Runs every day at 3 AM
    @Scheduled(cron = "0 0 3 * * *")
    public void refreshLeaderboardsDaily() {
        int season = 2025; // you can make this dynamic later

        leaderboardService.refreshLeaderboard(season, "PTS");
        leaderboardService.refreshLeaderboard(season, "REB");
        leaderboardService.refreshLeaderboard(season, "AST");

        System.out.println("Leaderboard refreshed at 3 AM");
    }
}