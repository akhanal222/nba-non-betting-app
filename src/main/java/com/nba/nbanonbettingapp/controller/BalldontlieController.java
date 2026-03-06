package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.dto.BdlGameDTO;
import com.nba.nbanonbettingapp.dto.BdlPlayerDTO;
import com.nba.nbanonbettingapp.dto.BdlResponseDTO;
import com.nba.nbanonbettingapp.service.BalldontlieService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

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
}