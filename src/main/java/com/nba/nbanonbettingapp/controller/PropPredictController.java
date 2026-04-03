package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.dto.PropPredictResponseDTO;
import com.nba.nbanonbettingapp.service.PropProjectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes the prop prediction endpoint.
 * Supported statType values: pts, reb, ast, stl, blk, turnover, fg3m
 */
@RestController
@RequestMapping("/api/props")
public class PropPredictController {

    private final PropProjectionService propProjectionService;

    public PropPredictController(PropProjectionService propProjectionService) {
        this.propProjectionService = propProjectionService;
    }

    @GetMapping("/predict")
    public ResponseEntity<PropPredictResponseDTO> predict(
            @RequestParam Long playerApiId,
            @RequestParam Long opponentTeamApiId,
            @RequestParam String statType,
            @RequestParam double line
    ) {
        PropPredictResponseDTO result =
                propProjectionService.predict(playerApiId, opponentTeamApiId, statType, line);
        return ResponseEntity.ok(result);
    }
}