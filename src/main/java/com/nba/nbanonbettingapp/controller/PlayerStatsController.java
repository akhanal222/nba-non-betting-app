package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.entity.PlayerGameStatistic;
import com.nba.nbanonbettingapp.service.PlayerStatsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stats")
public class PlayerStatsController {

    private final PlayerStatsService playerStatsService;

    public PlayerStatsController(PlayerStatsService playerStatsService) {
        this.playerStatsService = playerStatsService;
    }

    @GetMapping("/player/external/{externalApiId}")
    public List<PlayerGameStatistic> getPlayerStatsByExternal(
            @PathVariable Long externalApiId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return playerStatsService.getRecentStatsByExternalApiId(externalApiId, limit);
    }
}