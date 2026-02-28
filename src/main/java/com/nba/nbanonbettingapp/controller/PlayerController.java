package com.nba.nbanonbettingapp.controller;

import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.service.PlayerSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerSearchService playerSearchService;

    // Constructor injection
    public PlayerController(PlayerSearchService playerSearchService) {
        this.playerSearchService = playerSearchService;
    }

    //GET endpoint to search for players by name.
    @GetMapping("/search")
    public List<Player> search(@RequestParam String q) {
        return playerSearchService.search(q);
    }
}