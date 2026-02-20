package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.dto.BdlPlayerDTO;
import com.nba.nbanonbettingapp.dto.BdlResponseDTO;
import com.nba.nbanonbettingapp.service.BalldontlieService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BalldontlieController {

    private final BalldontlieService service;

    public BalldontlieController(BalldontlieService service) {
        this.service = service;
    }

    // Search Specific Player
    @GetMapping("/bdl/players/search")
    public BdlResponseDTO<BdlPlayerDTO> search(@RequestParam("q") String q) {
        return service.searchPlayers(q);
    }
    // Search specific games
    @GetMapping("/bdl/games")
    public Object getGames() {
        return service.getGames();
    }

    // Get all teams in the NBA
    @GetMapping("/bdl/teams")
    public Object getTeams(){
        return service.getTeams();
    }

}