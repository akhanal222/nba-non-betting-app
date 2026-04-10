package com.nba.nbanonbettingapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.nba.nbanonbettingapp.dto.*;
import com.nba.nbanonbettingapp.service.BalldontlieService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PathVariable;
import com.fasterxml.jackson.databind.JsonNode;
import com.nba.nbanonbettingapp.dto.PlayerWithImageDTO;

@RestController
@RequestMapping("/bdl")
public class BalldontlieController {

    private final BalldontlieService service;

    public BalldontlieController(BalldontlieService service) {
        this.service = service;
    }

    // Search Specific Player
//    example endpoint : http://localhost:8080/bdl/players/search?q=lebron
    @GetMapping("/players/search")
    public BdlResponseDTO<BdlPlayerDTO> search(@RequestParam("q") String q) {
        return service.searchPlayers(q);
    }
    //     Search specific games
// example endpoint : http://localhost:8080/bdl/games?startDate=2025-01-01&endDate=2025-01-31
    @GetMapping("/games")
    public Object getGames() {
        return service.getGames();
    }
    // Get specific game by id
    // example endpoint : http://localhost:8080/bdl/games/12345 (12345 is the game id of the external api)
    @GetMapping("/games/{id}")
    public JsonNode getGameById(@PathVariable Long id) {
        return service.getGameById(id);
    }

    //get the lineups of a specific game by id
    // example endpoint : http://localhost:8080/bdl/games/12345/lineups (12345 is the game id of the external api)
    @GetMapping("/games/{id}/lineups")
    public BdlResponseDTO<BdlLineupDTO> getGameLineups(@PathVariable Long id) {
        return service.getLineupsByGameId(id);
    }

    // Get all teams in the NBA
    // example endpoint : http://localhost:8080/bdl/teams
    @GetMapping("/teams")
    public Object getTeams(){
        return service.getTeams();
    }

    // Get the upcomming games
    //http://localhost:8080/bdl/games/upcoming?days=7
    @GetMapping("/games/upcoming")
    public BdlResponseDTO<BdlGameDTO> upcomingGames(
            @RequestParam(defaultValue = "3") int days
    ) {
        return service.getUpcomingGames(days);
    }
    // Get the games stats
    //examole endpoint : http://localhost:8080/bdl/games/12345/stats (12345 is the game id of the external api)
    @GetMapping("/games/{id}/stats")
    public BdlResponseDTO<BdlStatDTO> getGameStats(@PathVariable Long id) {
        return service.getStatsByGameId(id);
    }

    // Get the recently completed games
    // example endpoint : http://localhost:8080/bdl/games/completed?days=7
    @GetMapping("/games/completed")
    public BdlResponseDTO<BdlGameDTO> completedGames(
            @RequestParam(defaultValue = "2") int days
    ) {
        return service.getRecentCompletedGames(days);
    }
    // This is the endpoint for getting players of a team
    //http:localhost:8080/bdl/teams/1/players (1 is the team id of the external api)
    @GetMapping("/teams/{id}/players")
    public BdlResponseDTO<PlayerWithImageDTO> getPlayersByTeam(@PathVariable Long id) {
        return service.getPlayersByTeamId(id);
    }

    @GetMapping("/players/{id}/injuries")
    public JsonNode getPlayerInjuries(@PathVariable Long id) {
        return service.getPlayerInjuries(id);
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
    // This endPoint is to import the games for a specific season into our database.
    // It will be used to populate our database with historical data.
    // Endpoint : http://localhost:8080/bdl/games/import-season?season=2025
    @GetMapping("/games/import-season")
    public String importSeasonGames(@RequestParam int season) {
        service.importSeasonGames(season);
        return "Imported season games for season " + season;
    }

}