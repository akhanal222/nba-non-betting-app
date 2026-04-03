package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.service.DvPIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin endpoint to trigger DvP data population.
 *
 * Run this ONCE after setting up phase 3 to populate team_defense_vs_position.
 * After that, the nightly @Scheduled job keeps it current automatically.
 *
 * POST http://localhost:8080/api/admin/dvp/refresh
 */
@RestController
@RequestMapping("/api/admin/dvp")
public class DvPAdminController {

    private final DvPIngestionService dvpIngestionService;

    public DvPAdminController(DvPIngestionService dvpIngestionService) {
        this.dvpIngestionService = dvpIngestionService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh() {
        dvpIngestionService.refreshCurrentSeason();
        return ResponseEntity.ok("DvP refresh complete for current season.");
    }
}