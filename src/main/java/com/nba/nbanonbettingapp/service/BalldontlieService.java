package com.nba.nbanonbettingapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.nbanonbettingapp.dto.BdlGameDTO;
import com.nba.nbanonbettingapp.dto.BdlLineupDTO;
import com.nba.nbanonbettingapp.dto.BdlPlayerDTO;
import com.nba.nbanonbettingapp.dto.BdlResponseDTO;
import com.nba.nbanonbettingapp.dto.BdlStatDTO;
import com.nba.nbanonbettingapp.dto.PlayerWithImageDTO;
import com.nba.nbanonbettingapp.entity.NbaPlayerLookup;
import com.nba.nbanonbettingapp.repository.NbaPlayerLookupRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import java.util.List;
@Service
public class BalldontlieService {

    private final RestClient client;
    private final NbaPlayerLookupRepository nbaLookupRepository;

    public BalldontlieService(RestClient balldontlieRestClient,
                              NbaPlayerLookupRepository nbaLookupRepository) {
        this.client = balldontlieRestClient;
        this.nbaLookupRepository = nbaLookupRepository;
    }

    // This is for getting player Information
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
     * @param teamApiId  BallDontLie team ID
     * @param perPage    Results per page
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

        BdlResponseDTO<BdlGameDTO> response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("start_date", start.toString())
                        .queryParam("end_date", end.toString())
                        .queryParam("per_page", 100)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.data() == null) {
            return response;
        }

        var filteredGames = response.data().stream()
                .filter(game -> game.date() != null && game.date().length() >= 10)
                .filter(game -> {
                    LocalDate gameDate = LocalDate.parse(game.date().substring(0, 10));
                    return !gameDate.isBefore(start);
                })
                .toList();

        return new BdlResponseDTO<>(filteredGames, response.meta());
    }
    public BdlResponseDTO<BdlLineupDTO> getLineupsByGameId(Long gameId) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/lineups")
                        .queryParam("game_ids[]", gameId)
                        .queryParam("per_page", 100)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
    public BdlResponseDTO<BdlStatDTO> getStatsByGameId(Long gameId) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stats")
                        .queryParam("game_ids[]", gameId)
                        .queryParam("per_page", 100)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
    public BdlResponseDTO<BdlGameDTO> getRecentCompletedGames(int days) {
        if (days <= 0) days = 2;

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        BdlResponseDTO<BdlGameDTO> response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("start_date", start.toString())
                        .queryParam("end_date", end.toString())
                        .queryParam("per_page", 100)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.data() == null) {
            return response;
        }

        var completedGames = response.data().stream()
                .filter(game -> game.status() != null)
                .filter(game -> game.status().equalsIgnoreCase("Final"))
                .toList();

        return new BdlResponseDTO<>(completedGames, response.meta());
    }

    // This gets active players of a specific team and adds nbaPlayerId + imageUrl
    // endpoint : http:localhost:8080/bdl/teams/1/players (1 is the team id of the external api)
    public BdlResponseDTO<PlayerWithImageDTO> getPlayersByTeamId(Long teamId) {
        BdlResponseDTO<BdlPlayerDTO> response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players/active")
                        .queryParam("team_ids[]", teamId)
                        .queryParam("per_page", 100)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (response == null || response.data() == null) {
            return new BdlResponseDTO<>(List.of(), null);
        }

        List<PlayerWithImageDTO> players = response.data().stream()
                .map(dto -> {
                    Long nbaPlayerId = findNbaPlayerId(dto.firstName(), dto.lastName());
                    String imageUrl = buildImageUrl(nbaPlayerId);

                    return new PlayerWithImageDTO(
                            dto.id() != null ? dto.id().longValue() : null,
                            dto.firstName(),
                            dto.lastName(),
                            dto.position(),
                            dto.height(),
                            dto.weight(),
                            dto.jerseyNumber(),
                            nbaPlayerId,
                            imageUrl
                    );
                })
                .toList();

        return new BdlResponseDTO<>(players, response.meta());
    }

    // This is to find the top perfomer of the certain season so it can be used in the player nav bar tab
    public JsonNode getTop20SeasonLeaders(String statType, int season) {
        JsonNode root = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/leaders")
                        .queryParam("season", season)
                        .queryParam("stat_type", statType)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        if (root == null || !root.has("data") || !root.get("data").isArray()) {
            return root;
        }

        ArrayNode top20 = ((ArrayNode) root.get("data")).deepCopy();
        while (top20.size() > 20) {
            top20.remove(top20.size() - 1);
        }
        for (JsonNode leaderNode : top20) {
            JsonNode playerNode = leaderNode.get("player");
            if (!(playerNode instanceof ObjectNode playerObject)) {
                continue;
            }

            String firstName = playerNode.path("first_name").asText(null);
            String lastName = playerNode.path("last_name").asText(null);

            Long nbaPlayerId = findNbaPlayerId(firstName, lastName);
            String imageUrl = buildImageUrl(nbaPlayerId);

            if (nbaPlayerId != null) {
                playerObject.put("nbaPlayerId", nbaPlayerId);
            } else {
                playerObject.putNull("nbaPlayerId");
            }

            if (imageUrl != null) {
                playerObject.put("imageUrl", imageUrl);
            } else {
                playerObject.putNull("imageUrl");
            }

            if (!playerObject.path("team_id").isMissingNode() && !playerObject.path("team_id").isNull()) {
                long teamId = playerObject.path("team_id").asLong();
                JsonNode teamNode = getTeamById(teamId);

                if (teamNode != null && !teamNode.isNull()) {
                    playerObject.set("team", teamNode);
                } else {
                    playerObject.putNull("team");
                }
            } else {
                playerObject.putNull("team");
            }
        }

        ((ObjectNode) root).set("data", top20);
        return root;
    }
    private Long findNbaPlayerId(String firstName, String lastName) {
        if (firstName == null || lastName == null) {
            return null;
        }

        String fullName = (firstName + " " + lastName).trim();

        return nbaLookupRepository
                .findFirstByPlayerNameIgnoreCase(fullName)
                .map(NbaPlayerLookup::getNbaPlayerId)
                .orElse(null);
    }

    private String buildImageUrl(Long nbaPlayerId) {
        if (nbaPlayerId == null) {
            return null;
        }

        return "https://cdn.nba.com/headshots/nba/latest/1040x760/" + nbaPlayerId + ".png";
    }
}