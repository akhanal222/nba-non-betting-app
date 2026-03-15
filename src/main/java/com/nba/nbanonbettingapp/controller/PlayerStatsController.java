package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.entity.PlayerGameStatistic;
import com.nba.nbanonbettingapp.service.PlayerStatsService;
import org.springframework.web.bind.annotation.*;
import com.nba.nbanonbettingapp.dto.HeadToHeadResultDTO;
import org.springframework.http.ResponseEntity;

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
    @GetMapping("/recent/analyze")
    public ResponseEntity<HeadToHeadResultDTO> analyzeRecentStats(
            @RequestParam Long playerApiId,
            @RequestParam double statLine,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "true") boolean includePlayoffs,
            @RequestParam(defaultValue = "pts") String statType
    ) {
        HeadToHeadResultDTO result = playerStatsService.analyzeRecentStats(
                playerApiId, statLine, limit, includePlayoffs, statType);
        return ResponseEntity.ok(result);
    }
}