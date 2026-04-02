package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.dto.PlayerStatLeaderboardDTO;
import com.nba.nbanonbettingapp.service.PlayerStatLeaderboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
public class PlayerStatLeaderboardController {

    private final PlayerStatLeaderboardService leaderboardService;

    public PlayerStatLeaderboardController(PlayerStatLeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }
//     Post endpoint this will run at 3am daily by Sheduler
    //http://localhost:8080/leaderboard/refresh/PTS?season=2025
    //http://localhost:8080/leaderboard/refresh/REB?season=2025
    //http://localhost:8080/leaderboard/refresh/AST?season=2025

//    Get endpoint for the frontend :
//    http://localhost:8080/leaderboard/PTS?season=2025
//    http://localhost:8080/leaderboard/REB?season=2025
//    http://localhost:8080/leaderboard/AST?season=2025

    @PostMapping("/refresh/{statType}")
    public String refreshLeaderboard(
            @PathVariable String statType,
            @RequestParam Integer season
    ) {
        leaderboardService.refreshLeaderboard(season, statType);
        return "Leaderboard refreshed for " + statType.toUpperCase() + " season " + season;
    }
    @GetMapping("/{statType}")
    public List<PlayerStatLeaderboardDTO> getLeaderboard(
            @PathVariable String statType,
            @RequestParam Integer season
    ) {
        return leaderboardService.getLeaderboard(season, statType);
    }
}