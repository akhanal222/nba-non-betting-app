package com.nba.nbanonbettingapp.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.nbanonbettingapp.dto.*;
import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HeadToHeadService.
 *
 * Pure unit tests — no Spring context, no database, no real API calls.
 *
 * HOW THE SERVICE WORKS (and therefore how mocks are structured):
 *   1. playerRepository.findByExternalApiId()  — get player name
 *   2. balldontlieService.getTeamById()         — get opponent name (1 call)
 *   3. balldontlieService.getGamesByTeam()      — get all game IDs for the opponent
 *   4. balldontlieService.getStatsByPlayerAndGames() — get player stats in those games
 *
 * Tests must mock steps 2, 3, and 4. Step 1 is mocked via playerRepository.
 * getGameById() is NEVER called — that was the old broken approach.
 *
 * How to run:
 *   ./mvnw test -Dtest=HeadToHeadServiceTest
 */
@ExtendWith(MockitoExtension.class)
class HeadToHeadServiceTest {

    @Mock private PlayerRepository     playerRepository;
    @Mock private BalldontlieService   balldontlieService;
    @InjectMocks private HeadToHeadService headToHeadService;

    private static final Long   PLAYER_API_ID   = 17553995L; // Austin Reaves
    private static final Long   OPPONENT_API_ID = 8L;        // Denver Nuggets
    private static final double STAT_LINE       = 15.5;
    private static final int    LIMIT           = 5;

    // Fake game IDs representing Denver's schedule
    private static final List<Long> DENVER_GAME_IDS = List.of(101L, 102L, 103L, 104L, 105L);

    private Player mockPlayer;

    private static final ObjectNode DENVER_TEAM_NODE = JsonNodeFactory.instance.objectNode()
            .put("id", 8)
            .put("full_name", "Denver Nuggets")
            .put("abbreviation", "DEN");

    @BeforeEach
    void setUp() {
        mockPlayer = new Player();
        mockPlayer.setPlayerId(1L);
        mockPlayer.setExternalApiId(PLAYER_API_ID);
        mockPlayer.setFirstName("Austin");
        mockPlayer.setLastName("Reaves");
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Builds a BdlStatDTO for a game the player appeared in.
     * game.id doesn't need to match the Denver game IDs exactly in tests since
     * the filtering is done server-side (mocked) not in-memory.
     */
    private BdlStatDTO makeStat(int points, String gameDate) {
        BdlGameDTO game = new BdlGameDTO(1, gameDate, 2024, false, null, null, null, null, null );
        BdlTeamDTO playerTeam = new BdlTeamDTO(
                14, "LAL", "Los Angeles", "West", "Pacific", "Lakers", "Los Angeles Lakers"
        );
        return new BdlStatDTO(
                points, 5, 3, 1, 0, 2, "32:00", 6, 13, 2, 5, 3, 4,
                game, null, playerTeam
        );
    }

    /**
     * Builds a BdlStatDTO with null pts but real minutes played.
     * Simulates a rare case where a player played but pts was not recorded by the API.
     * Uses a real minutes value so the DNP filter does NOT remove this entry —
     * the service should treat null pts as 0 and include it in analytics.
     */
    private BdlStatDTO makeStatNullPts(String gameDate) {
        BdlGameDTO game = new BdlGameDTO(1, gameDate, 2024, false, null, null, null, null, null );
        BdlTeamDTO playerTeam = new BdlTeamDTO(
                14, "LAL", "Los Angeles", "West", "Pacific", "Lakers", "Los Angeles Lakers"
        );
        return new BdlStatDTO(
                null, null, null, null, null, null, "28:00",
                null, null, null, null, null, null,
                game, null, playerTeam
        );
    }

    /**
     * Builds a fake BdlGameDTO representing a Denver game.
     * Only the id field matters for HeadToHeadService — it just collects IDs.
     */
    private BdlGameDTO makeDenverGame(int gameId) {
        BdlTeamDTO denver = new BdlTeamDTO(8, "DEN", "Denver", "West", "Northwest", "Nuggets", "Denver Nuggets");
        BdlTeamDTO lakers = new BdlTeamDTO(14, "LAL", "Los Angeles", "West", "Pacific", "Lakers", "Los Angeles Lakers");
        return new BdlGameDTO(gameId, "2024-01-01", 2024, false, null, null,null,  denver, lakers);
    }

    private BdlResponseDTO<BdlStatDTO> makeStatPage(List<BdlStatDTO> stats, Integer nextCursor) {
        return new BdlResponseDTO<>(stats, new BdlMetaDTO(nextCursor, 100, 1, null, stats.size()));
    }

    private BdlResponseDTO<BdlGameDTO> makeGamePage(List<BdlGameDTO> games, Integer nextCursor) {
        return new BdlResponseDTO<>(games, new BdlMetaDTO(nextCursor, 100, 1, null, games.size()));
    }

    /**
     * Standard mock setup used by most tests.
     * Mocks the three shared dependencies: player DB lookup, team name, and Denver game IDs.
     */
    private void mockStandardSetup(List<BdlStatDTO> statsToReturn) {
        // Step 1: player found in DB
        when(playerRepository.findByExternalApiId(PLAYER_API_ID))
                .thenReturn(Optional.of(mockPlayer));

        // Step 2: team name lookup
        when(balldontlieService.getTeamById(OPPONENT_API_ID))
                .thenReturn(DENVER_TEAM_NODE);

        // Step 3: Denver's games (single page, 5 fake game IDs)
        List<BdlGameDTO> denverGames = List.of(
                makeDenverGame(101), makeDenverGame(102), makeDenverGame(103),
                makeDenverGame(104), makeDenverGame(105)
        );
        when(balldontlieService.getGamesByTeam(eq(OPPONENT_API_ID), anyList(), anyInt(), isNull()))
                .thenReturn(makeGamePage(denverGames, null));

        // Step 4: player's stats in those games
        when(balldontlieService.getStatsByPlayerAndGames(
                eq(PLAYER_API_ID), anyList(), isNull()))
                .thenReturn(makeStatPage(statsToReturn, null));
    }

    // =========================================================================
    // TEST 1 — Core Math (spec example: 18, 16, 22, 14, 19 vs 15.5)
    // =========================================================================

    @Test
    void analyze_coreExample_correctAverageAndHitRate() {
        mockStandardSetup(List.of(
                makeStat(18, "2025-01-14"),
                makeStat(16, "2024-11-22"),
                makeStat(22, "2024-03-10"),
                makeStat(14, "2023-12-01"),
                makeStat(19, "2023-10-28")
        ));

        HeadToHeadResultDTO result = headToHeadService.analyze(
                PLAYER_API_ID, OPPONENT_API_ID, STAT_LINE, LIMIT,
                true, "pts"
        );

        assertEquals("Austin Reaves",  result.playerName());
        assertEquals("Denver Nuggets", result.opponentTeamName());
        assertEquals(15.5,             result.statLine());
        assertEquals(5,                result.totalGames());
        assertEquals(17.8,             result.average(),  0.01);
        assertEquals(4,                result.hitCount());
        assertEquals(0.80,             result.hitRate(),   0.01);

        // Newest-first ordering
        assertEquals("2025-01-14", result.games().get(0).date());
        assertEquals("2023-10-28", result.games().get(4).date());

        assertTrue(result.games().get(0).hitLine());   // 18 > 15.5 ✓
        assertFalse(result.games().get(3).hitLine());  // 14 < 15.5 ✗

        // Verify call chain — each step called exactly once
        verify(balldontlieService, times(1)).getTeamById(OPPONENT_API_ID);
        verify(balldontlieService, times(1)).getGamesByTeam(eq(OPPONENT_API_ID), anyList(), anyInt(), isNull());
        verify(balldontlieService, times(1)).getStatsByPlayerAndGames(eq(PLAYER_API_ID), anyList(), isNull());
        verify(balldontlieService, never()).getGameById(anyLong()); // old approach never used
    }

    // =========================================================================
    // TEST 2 — All 5 games hit the line
    // =========================================================================

    @Test
    void analyze_allGamesHitLine_hitRateIsOneHundredPercent() {
        mockStandardSetup(List.of(
                makeStat(20, "2025-01-10"),
                makeStat(25, "2024-12-05"),
                makeStat(18, "2024-11-01"),
                makeStat(30, "2024-03-20"),
                makeStat(22, "2023-11-15")
        ));

        HeadToHeadResultDTO result = headToHeadService.analyze(
                PLAYER_API_ID, OPPONENT_API_ID, STAT_LINE, LIMIT,
                true, "pts"
        );

        assertEquals(5,   result.hitCount());
        assertEquals(1.0, result.hitRate(), 0.01);
        result.games().forEach(g -> assertTrue(g.hitLine()));
    }

    // =========================================================================
    // TEST 3 — No games hit the line
    // =========================================================================

    @Test
    void analyze_noGamesHitLine_hitRateIsZero() {
        mockStandardSetup(List.of(
                makeStat(10, "2025-01-10"),
                makeStat(8,  "2024-12-05"),
                makeStat(12, "2024-11-01"),
                makeStat(6,  "2024-03-20"),
                makeStat(14, "2023-11-15")
        ));

        HeadToHeadResultDTO result = headToHeadService.analyze(
                PLAYER_API_ID, OPPONENT_API_ID, STAT_LINE, LIMIT,
                true, "pts"
        );

        assertEquals(0,   result.hitCount());
        assertEquals(0.0, result.hitRate(), 0.01);
        result.games().forEach(g -> assertFalse(g.hitLine()));
    }

    // =========================================================================
    // TEST 4 — Boundary: exactly on the line is NOT a hit
    // =========================================================================

    @Test
    void analyze_pointsExactlyOnLine_doesNotCountAsHit() {
        mockStandardSetup(List.of(
                makeStat(20, "2025-01-10"),  // exactly on 20.0 — NOT a hit
                makeStat(21, "2024-12-05")   // one over — IS a hit
        ));

        HeadToHeadResultDTO result = headToHeadService.analyze(
                PLAYER_API_ID, OPPONENT_API_ID, 20.0, 2,
                true, "pts"
        );

        assertEquals(1, result.hitCount());
        assertFalse(result.games().get(0).hitLine());
        assertTrue(result.games().get(1).hitLine());
    }

    // =========================================================================
    // TEST 5 — Pagination: opponent games span two pages
    // =========================================================================

    /**
     * Verifies that getGamesByTeam is called twice when there are two pages
     * of opponent games, and that all game IDs from both pages are passed
     * to getStatsByPlayerAndGames.
     */
    @Test
    void analyze_opponentGamesSpanTwoPages_allGameIdsCollected() {
        when(playerRepository.findByExternalApiId(PLAYER_API_ID))
                .thenReturn(Optional.of(mockPlayer));
        when(balldontlieService.getTeamById(OPPONENT_API_ID))
                .thenReturn(DENVER_TEAM_NODE);

        // Page 1 of Denver games — has a next cursor
        when(balldontlieService.getGamesByTeam(eq(OPPONENT_API_ID), anyList(), anyInt(), isNull()))
                .thenReturn(makeGamePage(List.of(
                        makeDenverGame(201), makeDenverGame(202), makeDenverGame(203)
                ), 500));

        // Page 2 of Denver games — no more cursor
        when(balldontlieService.getGamesByTeam(eq(OPPONENT_API_ID), anyList(), anyInt(), eq(500)))
                .thenReturn(makeGamePage(List.of(
                        makeDenverGame(204), makeDenverGame(205)
                ), null));

        // Stats call — all 5 game IDs from both pages are passed
        when(balldontlieService.getStatsByPlayerAndGames(
                eq(PLAYER_API_ID), anyList(), isNull()))
                .thenReturn(makeStatPage(List.of(
                        makeStat(18, "2025-01-14"),
                        makeStat(22, "2024-11-10"),
                        makeStat(14, "2024-03-05")
                ), null));

        HeadToHeadResultDTO result = headToHeadService.analyze(
                PLAYER_API_ID, OPPONENT_API_ID, STAT_LINE, LIMIT,
                true, "pts"
        );

        assertEquals(3, result.totalGames());

        // Both pages of games were fetched
        verify(balldontlieService, times(1)).getGamesByTeam(eq(OPPONENT_API_ID), anyList(), anyInt(), isNull());
        verify(balldontlieService, times(1)).getGamesByTeam(eq(OPPONENT_API_ID), anyList(), anyInt(), eq(500));
    }

    // =========================================================================
    // TEST 6 — Player not found in DB
    // =========================================================================

    @Test
    void analyze_playerNotInDatabase_throwsBeforeAnyApiCall() {
        when(playerRepository.findByExternalApiId(PLAYER_API_ID))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                headToHeadService.analyze(PLAYER_API_ID, OPPONENT_API_ID, STAT_LINE, LIMIT, true, "pts")
        );

        assertTrue(ex.getMessage().contains("Player not found"));
        verify(balldontlieService, never()).getTeamById(anyLong());
        verify(balldontlieService, never()).getGamesByTeam(anyLong(), anyList(), anyInt(), any());
        verify(balldontlieService, never()).getStatsByPlayerAndGames(anyLong(), anyList(), any());
        verify(balldontlieService, never()).getGameById(anyLong());
    }

    // =========================================================================
    // TEST 7 — No matchup history
    // =========================================================================

    @Test
    void analyze_noMatchupHistory_throwsRuntimeException() {
        when(playerRepository.findByExternalApiId(PLAYER_API_ID))
                .thenReturn(Optional.of(mockPlayer));
        when(balldontlieService.getTeamById(OPPONENT_API_ID))
                .thenReturn(DENVER_TEAM_NODE);
        when(balldontlieService.getGamesByTeam(eq(OPPONENT_API_ID), anyList(), anyInt(), isNull()))
                .thenReturn(makeGamePage(List.of(makeDenverGame(101)), null));
        when(balldontlieService.getStatsByPlayerAndGames(
                eq(PLAYER_API_ID), anyList(), isNull()))
                .thenReturn(makeStatPage(List.of(), null));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                headToHeadService.analyze(PLAYER_API_ID, OPPONENT_API_ID, STAT_LINE, LIMIT, true, "pts")
        );

        assertTrue(ex.getMessage().contains("No historical matchup data"));
    }

    // =========================================================================
    // TEST 8 — Null pts (DNP) treated as 0
    // =========================================================================

    @Test
    void analyze_nullPointsInStat_treatedAsZero() {
        mockStandardSetup(List.of(
                makeStat(20, "2025-01-14"),
                makeStatNullPts("2024-10-15")
        ));

        assertDoesNotThrow(() ->
                headToHeadService.analyze(PLAYER_API_ID, OPPONENT_API_ID, STAT_LINE, 2, true, "pts")
        );

        HeadToHeadResultDTO result = headToHeadService.analyze(
                PLAYER_API_ID, OPPONENT_API_ID, STAT_LINE, 2,
                true, "pts"
        );

        assertEquals(20, result.games().get(0).statValue());
        assertEquals(0,  result.games().get(1).statValue());
        assertFalse(result.games().get(1).hitLine());
    }

    // =========================================================================
    // TEST 9 — Newest-first sort: API returns oldest-first, we reverse
    // =========================================================================

    @Test
    void analyze_apiReturnsOldestFirst_resultIsNewestFirst() {
        // Deliberately oldest-first order (as the real API returns)
        mockStandardSetup(List.of(
                makeStat(5,  "2022-01-15"),
                makeStat(0,  "2022-04-03"),
                makeStat(31, "2022-04-10"),
                makeStat(8,  "2023-10-26"),
                makeStat(24, "2024-11-14")
        ));

        HeadToHeadResultDTO result = headToHeadService.analyze(
                PLAYER_API_ID, OPPONENT_API_ID, STAT_LINE, LIMIT,
                true, "pts"
        );

        assertEquals("2024-11-14", result.games().get(0).date());
        assertEquals(24,           result.games().get(0).statValue());
        assertEquals("2022-01-15", result.games().get(4).date());
        assertEquals(5,            result.games().get(4).statValue());
    }
}