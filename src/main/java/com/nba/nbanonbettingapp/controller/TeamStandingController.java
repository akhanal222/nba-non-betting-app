package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.entity.TeamStanding;
import com.nba.nbanonbettingapp.service.TeamStandingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/standings") // Base url for all endpoints in this controller
public class TeamStandingController {

    private final TeamStandingService teamStandingService;

    public TeamStandingController(TeamStandingService teamStandingService) {
        this.teamStandingService = teamStandingService;
    }
    // This endpoint is to post the season info to the team standing table
    // This calculate all the points of the team
    // Endpoint : http://localhost:8080/standings/refresh/2025
    @PostMapping("/refresh/{seasonYear}")
    public String refreshStandings(@PathVariable Integer seasonYear) {
        teamStandingService.refreshStandings(seasonYear);
        return "Standings refreshed for season " + seasonYear;
    }

    // This endpoint is to get the standings of the team by season year
    // endpoint : http://localhost:8080/standings/2025
    @GetMapping("/{seasonYear}")
    public List<TeamStanding> getStandings(@PathVariable Integer seasonYear) {
        return teamStandingService.getStandingsBySeason(seasonYear);
    }
}