package com.nba.nbanonbettingapp.service;

import com.nba.nbanonbettingapp.dto.BdlAdvancedStatsDTO;
import com.nba.nbanonbettingapp.dto.BdlResponseDTO;
import com.nba.nbanonbettingapp.entity.Game;
import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.entity.PlayerAdvancedStats;
import com.nba.nbanonbettingapp.repository.GameRepository;
import com.nba.nbanonbettingapp.repository.PlayerAdvancedStatsRepository;
import com.nba.nbanonbettingapp.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fetches per-game advanced stats from BallDontLie and persists them into
 * the player_advanced_stats table.
 *
 * Called from PlayerStatsService after each box score upsert
 * Tries /nba/v2/stats/advanced first, falls back to v1 for pre-2015 games
 */
@Service
public class AdvancedStatsIngestionService {

    private static final Logger log = LoggerFactory.getLogger(AdvancedStatsIngestionService.class);

    private static final int BATCH_SIZE = 50;

    private final BalldontlieService balldontlieService;
    private final PlayerAdvancedStatsRepository advancedStatsRepository;
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;

    public AdvancedStatsIngestionService(BalldontlieService balldontlieService,
                                         PlayerAdvancedStatsRepository advancedStatsRepository,
                                         PlayerRepository playerRepository,
                                         GameRepository gameRepository) {
        this.balldontlieService = balldontlieService;
        this.advancedStatsRepository = advancedStatsRepository;
        this.playerRepository = playerRepository;
        this.gameRepository = gameRepository;
    }

    /**
     * Entry point called by PlayerStatsService after box score upserts complete.
     *
     * @param playerApiId      BallDontLie player ID
     * @param gameExternalIds  List of BDL game IDs whose box scores were just upserted
     */
    public void fetchAndUpsert(Long playerApiId, List<Long> gameExternalIds) {
        if (gameExternalIds == null || gameExternalIds.isEmpty()) return;

        Player player = playerRepository.findByExternalApiId(playerApiId).orElse(null);
        if (player == null) {
            log.warn("AdvancedStatsIngestion: player not found for apiId={}", playerApiId);
            return;
        }

        // Filter out games we already have advanced stats for
        List<Long> missing = gameExternalIds.stream()
                .filter(gameExtId -> {
                    Optional<Game> game = gameRepository.findByExternalApiId(gameExtId);
                    return game.isEmpty() ||
                            !advancedStatsRepository.existsByPlayer_PlayerIdAndGame_GameId(
                                    player.getPlayerId(), game.get().getGameId());
                })
                .toList();

        if (missing.isEmpty()) return;

        // Fetch all advanced stats rows for this player + these game IDs
        List<BdlAdvancedStatsDTO> fetched = fetchAllPages(playerApiId, missing);

        OffsetDateTime now = OffsetDateTime.now();
        for (BdlAdvancedStatsDTO dto : fetched) {
            try {
                upsertRow(dto, player, now);
            } catch (Exception e) {
                log.warn("AdvancedStatsIngestion: failed to upsert row id={} — {}", dto.id(), e.getMessage());
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Paginates through all pages of advanced stats for this player + game list.
     */
    private List<BdlAdvancedStatsDTO> fetchAllPages(Long playerApiId, List<Long> gameIds) {
        List<BdlAdvancedStatsDTO> all = new ArrayList<>();

        for (int i = 0; i < gameIds.size(); i += BATCH_SIZE) {
            List<Long> batch = gameIds.subList(i, Math.min(i + BATCH_SIZE, gameIds.size()));

            List<BdlAdvancedStatsDTO> batchResults = fetchBatchWithFallback(playerApiId, batch);
            all.addAll(batchResults);
        }

        return all;
    }

    /**
     * Fetches one batch of game IDs, trying v2 then falling back to v1 if empty.
     * Paginates within the batch using cursor until all pages are consumed.
     */
    private List<BdlAdvancedStatsDTO> fetchBatchWithFallback(Long playerApiId, List<Long> gameIds) {
        List<BdlAdvancedStatsDTO> v2Results = fetchPaginatedBatch(playerApiId, gameIds, "v2");

        if (!v2Results.isEmpty()) {
            return v2Results;
        }

        log.debug("AdvancedStatsIngestion: v2 returned empty for batch, trying v1 fallback");
        return fetchPaginatedBatch(playerApiId, gameIds, "v1");
    }

    /**
     * Fetches all pages for a single batch using cursor pagination.
     */
    private List<BdlAdvancedStatsDTO> fetchPaginatedBatch(Long playerApiId,
                                                          List<Long> gameIds,
                                                          String version) {
        List<BdlAdvancedStatsDTO> results = new ArrayList<>();
        Integer cursor = null;

        do {
            BdlResponseDTO<BdlAdvancedStatsDTO> page;
            try {
                page = "v2".equals(version)
                        ? balldontlieService.getAdvancedStatsByPlayerAndGamesV2(playerApiId, gameIds, cursor)
                        : balldontlieService.getAdvancedStatsByPlayerAndGamesV1(playerApiId, gameIds, cursor);
            } catch (Exception e) {
                log.warn("AdvancedStatsIngestion: API call failed ({}): {}", version, e.getMessage());
                break;
            }

            if (page == null || page.data() == null || page.data().isEmpty()) break;

            // Filter to period 0 only — BDL returns per-quarter rows for the same game
            // which would violate the (player_id, game_id) unique constraint
            List<BdlAdvancedStatsDTO> fullGameRows = page.data().stream()
                    .filter(dto -> dto.period() == null || dto.period() == 0)
                    .toList();

            results.addAll(fullGameRows.stream()
                    .map(dto -> tagVersion(dto, version))
                    .toList());

            cursor = (page.meta() != null) ? page.meta().nextCursor() : null;

        } while (cursor != null);

        return results;
    }

    private BdlAdvancedStatsDTO tagVersion(BdlAdvancedStatsDTO dto, String version) {
        // Version is applied during upsertRow — this passthrough exists for clarity
        return dto;
    }

    private void upsertRow(BdlAdvancedStatsDTO dto, Player player, OffsetDateTime now) {
        upsertRow(dto, player, now, "v2"); // default — overridden by batch context
    }

    private void upsertRow(BdlAdvancedStatsDTO dto, Player player, OffsetDateTime now, String version) {
        if (dto.id() == null) return;

        // Skip DNP rows — BDL returns a row but usage_percentage will be null
        if (dto.usagePercentage() == null) {
            return;
        }

        if (dto.game() == null || dto.game().id() == null) return;
        Game game = gameRepository.findByExternalApiId(dto.game().id().longValue()).orElse(null);
        if (game == null) {
            log.warn("AdvancedStatsIngestion: game not found for external id={}", dto.game().id());
            return;
        }

        // Find existing row by BDL id (upsert key) or create new
        PlayerAdvancedStats row = advancedStatsRepository
                .findByExternalAdvId(dto.id().longValue())
                .orElseGet(PlayerAdvancedStats::new);

        row.setExternalAdvId(dto.id().longValue());
        row.setPlayer(player);
        row.setGame(game);

        // Formula inputs
        row.setUsagePercentage(dto.usagePercentage());
        row.setTrueShootingPercentage(dto.trueShootingPercentage());
        row.setPace(dto.pace());
        row.setOffensiveRating(dto.offensiveRating());
        row.setDefensiveRating(dto.defensiveRating());
        row.setNetRating(dto.netRating());
        row.setPie(dto.pie());

        // Prop-type rate stats
        row.setAssistPercentage(dto.assistPercentage());
        row.setReboundPercentage(dto.reboundPercentage());
        row.setOffensiveReboundPercentage(dto.offensiveReboundPercentage());
        row.setDefensiveReboundPercentage(dto.defensiveReboundPercentage());

        row.setDataVersion(version);
        row.setSyncedAt(now);
        if (row.getCreatedAt() == null) row.setCreatedAt(now);

        advancedStatsRepository.save(row);
    }

    /**
     * Overloaded entry for batch fetch with explicit version tracking.
     */
    void upsertBatch(List<BdlAdvancedStatsDTO> dtos, Player player, String version) {
        OffsetDateTime now = OffsetDateTime.now();
        for (BdlAdvancedStatsDTO dto : dtos) {
            try {
                upsertRow(dto, player, now, version);
            } catch (Exception e) {
                log.warn("AdvancedStatsIngestion: failed to upsert row id={} — {}", dto.id(), e.getMessage());
            }
        }
    }
}