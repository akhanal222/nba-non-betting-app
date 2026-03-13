package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.dto.HeadToHeadResultDTO;
import com.nba.nbanonbettingapp.service.HeadToHeadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HeadToHeadController
 *
 * Exposes the opponent-specific matchup analysis endpoint.
 *
 * Endpoint: GET /api/matchup/analyze
 */
@RestController
@RequestMapping("/api/matchup")
public class HeadToHeadController {

    private final HeadToHeadService headToHeadService;

    public HeadToHeadController(HeadToHeadService headToHeadService) {
        this.headToHeadService = headToHeadService;
    }

    /**
     * Analyze a player's historical performance against a specific opponent.
     *
     * @param playerApiId     BallDontLie player ID (external_api_id)
     * @param opponentApiId   BallDontLie team ID for the opponent
     * @param statLine        Prediction stats line (e.g. 15.5)
     * @param limit           Number of most-recent matchup games to analyze (default 5)
     * @param includePlayoffs Whether to include postseason games (default true)
     *
     * Examples:
     *   GET /api/matchup/analyze?playerApiId=17553995&opponentApiId=8&statLine=15.5&limit=5
     *   GET /api/matchup/analyze?playerApiId=17553995&opponentApiId=8&statLine=15.5&limit=5&includePlayoffs=false
     */
    @GetMapping("/analyze")
    public ResponseEntity<HeadToHeadResultDTO> analyze(
            @RequestParam Long playerApiId,
            @RequestParam Long opponentApiId,
            @RequestParam double statLine,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "true") boolean includePlayoffs,
            @RequestParam(defaultValue = "pts") String statType
    ) {
        HeadToHeadResultDTO result =
                headToHeadService.analyze(playerApiId, opponentApiId, statLine, limit, includePlayoffs, statType);
        return ResponseEntity.ok(result);
    }
}