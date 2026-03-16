package com.nba.nbanonbettingapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.nba.nbanonbettingapp.dto.*;
import com.nba.nbanonbettingapp.service.BalldontlieService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PathVariable;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/bdl")
public class BalldontlieController {

    private final BalldontlieService service;

    public BalldontlieController(BalldontlieService service) {
        this.service = service;
    }

    // Search Specific Player
    @GetMapping("/players/search")
    public BdlResponseDTO<BdlPlayerDTO> search(@RequestParam("q") String q) {
        return service.searchPlayers(q);
    }
    //     Search specific games
    @GetMapping("/games")
    public Object getGames() {
        return service.getGames();
    }
    @GetMapping("/games/{id}")
    public JsonNode getGameById(@PathVariable Long id) {
        return service.getGameById(id);
    }

    @GetMapping("/games/{id}/lineups")
    public BdlResponseDTO<BdlLineupDTO> getGameLineups(@PathVariable Long id) {
        return service.getLineupsByGameId(id);
    }

    // Get all teams in the NBA
    @GetMapping("/teams")
    public Object getTeams(){
        return service.getTeams();
    }
    //http://localhost:8080/bdl/games/upcoming?days=7
    @GetMapping("/games/upcoming")
    public BdlResponseDTO<BdlGameDTO> upcomingGames(
            @RequestParam(defaultValue = "3") int days
    ) {
        return service.getUpcomingGames(days);
    }
    @GetMapping("/games/{id}/stats")
    public BdlResponseDTO<BdlStatDTO> getGameStats(@PathVariable Long id) {
        return service.getStatsByGameId(id);
    }
    @GetMapping("/games/completed")
    public BdlResponseDTO<BdlGameDTO> completedGames(
            @RequestParam(defaultValue = "2") int days
    ) {
        return service.getRecentCompletedGames(days);
    }
    // This is the endpoint for getting players of a team
    //http:localhost:8080/bdl/teams/1/players (1 is the team id of the external api)
    @GetMapping("/teams/{id}/players")
    public BdlResponseDTO<BdlPlayerDTO> getPlayersByTeam(@PathVariable Long id) {
        return service.getPlayersByTeamId(id);
    }
    // This is the endpoint for getting the top 20 season leaders for a specific stat type and season
    //http:localhost:8080/bdl/leaders/top20?statType=pts&season=2025
    @GetMapping("/leaders/top20")
    public JsonNode getTop20SeasonLeaders(
            @RequestParam String statType,
            @RequestParam int season
    ) {
        return service.getTop20SeasonLeaders(statType, season);
    }
}