package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.dto.PlayerComparisonDTO;
import com.nba.nbanonbettingapp.service.PlayerComparisonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the Player vs Player comparison endpoint.
 *
 * Endpoint: GET /api/comparison/compare
 *
 * Example:
 *   http://localhost:8080/api/comparison/compare?playerOneApiId=237&playerTwoApiId=132
 *   (LeBron James vs Luka Doncic)
 */
@RestController
@RequestMapping("/api/comparison")
public class PlayerComparisonController {

    private final PlayerComparisonService comparisonService;

    public PlayerComparisonController(PlayerComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    /**
     * Returns a side-by-side player comparison including bio info,
     * current season averages, and career averages for both players.
     *
     * @param playerOneApiId BallDontLie external API ID for player one
     * @param playerTwoApiId BallDontLie external API ID for player two
     * @return PlayerComparisonDTO with both profiles
     */
    @GetMapping("/compare")
    public PlayerComparisonDTO compare(
            @RequestParam Long playerOneApiId,
            @RequestParam Long playerTwoApiId
    ) {
        return comparisonService.compare(playerOneApiId, playerTwoApiId);
    }
}