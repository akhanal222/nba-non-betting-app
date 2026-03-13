package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.dto.HeadToHeadResultDTO;
import com.nba.nbanonbettingapp.service.RecentStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RecentStatsController
 *
 * Exposes the general recent games analysis endpoint.
 * No opponent filtering — analyzes the player's last N games overall.
 *
 * Endpoint: GET /api/stats/recent
 */
@RestController
@RequestMapping("/api/stats")
public class RecentStatsController {

    private final RecentStatsService recentStatsService;

    public RecentStatsController(RecentStatsService recentStatsService) {
        this.recentStatsService = recentStatsService;
    }

    /**
     * Analyze a player's performance over their most recent N games.
     *
     * @param playerApiId     BallDontLie player ID (external_api_id)
     * @param statLine        Prediction stats line (e.g. 15.5)
     * @param limit           Number of recent games to analyze (default 5)
     * @param includePlayoffs Whether to include postseason games (default true)
     *
     * Example:
     *   GET /api/stats/recent?playerApiId=17553995&statLine=15.5&limit=10&includePlayoffs=false
     */
    @GetMapping("/recent")
    public ResponseEntity<HeadToHeadResultDTO> recentStats(
            @RequestParam Long playerApiId,
            @RequestParam double statLine,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "true") boolean includePlayoffs,
            @RequestParam(defaultValue = "pts") String statType
    ) {
        HeadToHeadResultDTO result =
                recentStatsService.analyze(playerApiId, statLine, limit, includePlayoffs, statType);
        return ResponseEntity.ok(result);
    }
}