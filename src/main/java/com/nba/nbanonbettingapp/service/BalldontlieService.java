package com.nba.nbanonbettingapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nba.nbanonbettingapp.dto.BdlPlayerDTO;
import com.nba.nbanonbettingapp.dto.BdlResponseDTO;
import com.nba.nbanonbettingapp.dto.BdlStatDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.nba.nbanonbettingapp.dto.BdlGameDTO;
import java.time.LocalDate;
@Service
public class BalldontlieService {

    private final RestClient client;

    public BalldontlieService(RestClient balldontlieRestClient) {
        this.client = balldontlieRestClient;
    }

//    This is for getting player Information
    /**
     * General player search using the "search" query parameter.
     * Supports partial name matching (single search input).
     */
    public BdlResponseDTO<BdlPlayerDTO> searchPlayers(String search) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .queryParam("search", search)
                        .queryParam("per_page", 25)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /**
     * Structured player search using first_name and last_name parameters.
     * Provides more precise filtering when full name is available.
     */
    public BdlResponseDTO<BdlPlayerDTO> searchPlayersByName(String firstName, String lastName) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .queryParam("first_name", firstName)
                        .queryParam("last_name", lastName)
                        .queryParam("per_page", 25)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

//     This is for getting all the games data
    /**
     * Fetches a limited list of games from the Balldontlie API.
     * Used to retrieve recent game data.
     */
    public BdlResponseDTO<Object> getGames() {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("per_page", 5)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
    /**
     * Fetches a limited list of teams from the Balldontlie API.
     * Used to retrieve team information.
     */
    public BdlResponseDTO<Object> getTeams() {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("per_page", 5)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
    /**
     * Fetch stats for a player from Balldontlie API.
     * We will request more than we need, then take last N.
     */
    public BdlResponseDTO<BdlStatDTO> getStatsByPlayerId(Long playerApiId, int perPage) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stats")
                        .queryParam("player_ids[]", playerApiId)
                        .queryParam("per_page", perPage) // set 50 or 100
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
    /**
     * Fetch stats for a player from Balldontlie API.
     * This is to find home team away team score because /stats endpoint don't have it
     */
    public JsonNode getGameById(Long gameApiId) {
        JsonNode root = client.get()
                .uri(uriBuilder -> uriBuilder.path("/games/{id}").build(gameApiId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (root != null && root.has("data")) {
            return root.get("data");
        }
        return root;
    }
    //http://localhost:8080/bdl/games/upcoming?days=7
    public BdlResponseDTO<BdlGameDTO> getUpcomingGames(int days) {
        if (days <= 0) days = 7;

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(days);

        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("start_date", start.toString())
                        .queryParam("end_date", end.toString())
                        .queryParam("per_page", 100)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}