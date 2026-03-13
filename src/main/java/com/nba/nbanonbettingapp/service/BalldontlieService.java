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
import java.util.List;
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
     * Fetches all games for a given team, paginated by cursor.
     *
     * Uses team_ids[] filter — returns every game the team has played.
     * Each game object includes home_team and visitor_team with full details,
     * so the caller can extract game IDs for the /stats lookup.
     *
     * @param teamApiId  BallDontLie team ID (e.g. 8 for Denver Nuggets)
     * @param perPage    Results per page (max 100)
     * @param cursor     Pagination cursor, or null for first page
     */
    public BdlResponseDTO<BdlGameDTO> getGamesByTeam(Long teamApiId, List<Integer> seasons,
                                                     int perPage, Integer cursor) {
        return client.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/games")
                            .queryParam("team_ids[]", teamApiId)
                            .queryParam("per_page", perPage);
                    if (seasons != null) {
                        for (Integer season : seasons) {
                            builder = builder.queryParam("seasons[]", season);
                        }
                    }
                    if (cursor != null) {
                        builder = builder.queryParam("cursor", cursor);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /**
     * Fetches stats for a player filtered to a specific list of game IDs.
     *
     * This is the correct way to do opponent filtering on BallDontLie:
     *   1. Get the opponent's game IDs via getGamesByTeam()
     *   2. Pass those IDs here to get only the player's stats in those games
     *
     * URL length limit: don't pass more than ~50 game IDs at once.
     * HeadToHeadService handles batching automatically.
     *
     * @param playerApiId  BallDontLie player ID
     * @param gameIds      List of game IDs to filter by (max ~50 per call)
     * @param cursor       Pagination cursor, or null for first page
     */
    public BdlResponseDTO<BdlStatDTO> getStatsByPlayerAndGames(Long playerApiId,
                                                               List<Long> gameIds,
                                                               Integer cursor) {
        return client.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/stats")
                            .queryParam("player_ids[]", playerApiId)
                            .queryParam("per_page", 100);

                    for (Long gameId : gameIds) {
                        builder = builder.queryParam("game_ids[]", gameId);
                    }
                    if (cursor != null) {
                        builder = builder.queryParam("cursor", cursor);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /**
     * Fetches a single team by its BallDontLie team ID.
     * Used to resolve the opponent's display name (e.g. "Denver Nuggets").
     *
     * Response JSON: { "data": { "id": 8, "full_name": "Denver Nuggets", ... } }
     * Returns the unwrapped "data" node directly.
     *
     * @param teamApiId  BallDontLie team ID
     * @return           JsonNode of the team object, or null if not found
     */
    public JsonNode getTeamById(Long teamApiId) {
        JsonNode root = client.get()
                .uri(uriBuilder -> uriBuilder.path("/teams/{id}").build(teamApiId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (root != null && root.has("data")) {
            return root.get("data");
        }
        return root;
    }
    /**
     * Fetches stats for a player from a given start date onward.
     * Used by RecentStatsService — seasons[] on /stats is silently ignored by
     * BallDontLie, but start_date is respected reliably.
     *
     * @param playerApiId  BallDontLie player ID
     * @param startDate    Earliest date to include, format "YYYY-MM-DD"
     * @param perPage      Results per page (max 100)
     * @param cursor       Pagination cursor, or null for first page
     */
    public BdlResponseDTO<BdlStatDTO> getStatsByPlayerSince(Long playerApiId,
                                                            String startDate,
                                                            int perPage,
                                                            Integer cursor) {
        return client.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/stats")
                            .queryParam("player_ids[]", playerApiId)
                            .queryParam("start_date", startDate)
                            .queryParam("per_page", perPage);
                    if (cursor != null) {
                        builder = builder.queryParam("cursor", cursor);
                    }
                    return builder.build();
                })
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