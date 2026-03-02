package com.nba.nbanonbettingapp.service;

import com.nba.nbanonbettingapp.dto.BdlPlayerDTO;
import com.nba.nbanonbettingapp.dto.BdlResponseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
}