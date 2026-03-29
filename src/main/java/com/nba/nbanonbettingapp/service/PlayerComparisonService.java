package com.nba.nbanonbettingapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.nbanonbettingapp.dto.BdlResponseDTO;
import com.nba.nbanonbettingapp.dto.BdlSeasonAverageDTO;
import com.nba.nbanonbettingapp.dto.PlayerComparisonDTO;
import com.nba.nbanonbettingapp.dto.PlayerComparisonDTO.PlayerProfile;
import com.nba.nbanonbettingapp.dto.PlayerComparisonDTO.SeasonAverages;
import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class PlayerComparisonService {

    private static final int CURRENT_SEASON     = 2025;
    private static final int CAREER_START_SEASON = 1996;

    private final PlayerRepository playerRepository;
    private final BalldontlieService balldontlieService;
    private final ObjectMapper objectMapper;

    public PlayerComparisonService(PlayerRepository playerRepository,
                                   BalldontlieService balldontlieService,
                                   ObjectMapper objectMapper) {
        this.playerRepository = playerRepository;
        this.balldontlieService = balldontlieService;
        this.objectMapper = objectMapper;
    }

    /**
     * Builds a full comparison between two players identified by their
     * BallDontLie external API IDs.
     */
    public PlayerComparisonDTO compare(Long playerOneApiId, Long playerTwoApiId) {
        PlayerProfile profileOne = buildProfile(playerOneApiId);
        PlayerProfile profileTwo = buildProfile(playerTwoApiId);
        return new PlayerComparisonDTO(profileOne, profileTwo);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private PlayerProfile buildProfile(Long apiId) {

        Player p = playerRepository.findByExternalApiId(apiId)
                .orElseThrow(() -> new RuntimeException(
                        "Player not found in DB for externalApiId: " + apiId +
                                ". Please search for this player first so they are saved to the database."));

        // Current season averages
        SeasonAverages seasonAvg = fetchSeasonAverages(apiId, CURRENT_SEASON);

        // Career averages - DB cache first, parallel BDL fetch on miss
        SeasonAverages careerAvg = getOrComputeCareerAverages(p, apiId);

        String imageUrl = p.getNbaPlayerId() != null
                ? "https://cdn.nba.com/headshots/nba/latest/1040x760/" + p.getNbaPlayerId() + ".png"
                : null;

        String teamName = p.getTeam() != null ? p.getTeam().getTeamName() : null;

        return new PlayerProfile(
                apiId,
                p.getFirstName(),
                p.getLastName(),
                imageUrl,
                p.getPosition(),
                p.getHeight(),
                p.getWeight(),
                p.getCollege(),
                teamName,
                p.getJerseyNumber(),
                p.getDraftYear(),
                p.getDraftRound(),
                p.getDraftNumber(),
                seasonAvg,
                careerAvg
        );
    }

    /**
     * DB-first career averages.
     *
     * Cache hit  -> deserialize JSON from players.career_averages_json and return.
     * Cache miss -> fire 30 parallel BDL calls, compute weighted averages,
     *               serialize, persist on Player row, return result.
     */
    private SeasonAverages getOrComputeCareerAverages(Player player, Long apiId) {

        // Cache hit
        if (player.getCareerAveragesJson() != null) {
            try {
                return objectMapper.readValue(
                        player.getCareerAveragesJson(), SeasonAverages.class);
            } catch (Exception e) {
                // Corrupt cache entry - fall through and recompute
            }
        }

        // Cache miss - fetch in parallel and compute
        SeasonAverages computed = fetchCareerAveragesInParallel(apiId);

        // Persist so the next request is instant
        if (computed != null) {
            try {
                player.setCareerAveragesJson(objectMapper.writeValueAsString(computed));
                player.setCareerAveragesCachedAt(OffsetDateTime.now());
                playerRepository.save(player);
            } catch (Exception e) {
                // Non-fatal - still return the computed result even if caching fails
            }
        }

        return computed;
    }

    /**
     * Fires all season requests (1996-2025) simultaneously using CompletableFuture.
     */
    private SeasonAverages fetchCareerAveragesInParallel(Long playerApiId) {

        List<CompletableFuture<BdlSeasonAverageDTO>> futures = new ArrayList<>();

        for (int season = CAREER_START_SEASON; season <= CURRENT_SEASON; season++) {
            final int s = season;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    BdlResponseDTO<BdlSeasonAverageDTO> resp =
                            balldontlieService.getSeasonAverages(playerApiId, s);
                    if (resp != null && resp.data() != null && !resp.data().isEmpty()) {
                        return resp.data().get(0);
                    }
                } catch (Exception ignored) {
                    // One failed season should not abort the entire career average
                }
                return null;
            }));
        }

        // Block until all 30 complete, collect non-null results
        List<BdlSeasonAverageDTO> seasons = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        return computeWeightedCareerAverages(seasons);
    }

    /**
     * Computes weighted career averages across all seasons.
     */
    private SeasonAverages computeWeightedCareerAverages(List<BdlSeasonAverageDTO> seasons) {
        if (seasons == null || seasons.isEmpty()) return null;

        int totalGames = seasons.stream()
                .mapToInt(s -> s.gamesPlayed() != null ? s.gamesPlayed() : 0)
                .sum();

        if (totalGames == 0) return null;

        double pts      = weightedAvg(seasons, BdlSeasonAverageDTO::pts,      totalGames);
        double reb      = weightedAvg(seasons, BdlSeasonAverageDTO::reb,      totalGames);
        double ast      = weightedAvg(seasons, BdlSeasonAverageDTO::ast,      totalGames);
        double stl      = weightedAvg(seasons, BdlSeasonAverageDTO::stl,      totalGames);
        double blk      = weightedAvg(seasons, BdlSeasonAverageDTO::blk,      totalGames);
        double turnover = weightedAvg(seasons, BdlSeasonAverageDTO::turnover, totalGames);
        double fg3m     = weightedAvg(seasons, BdlSeasonAverageDTO::fg3m,     totalGames);

        // Shooting percentages from totals (made / attempted) not averaged pcts
        double totalFgm  = weightedSum(seasons, BdlSeasonAverageDTO::fgm,  totalGames);
        double totalFga  = weightedSum(seasons, BdlSeasonAverageDTO::fga,  totalGames);
        double totalFg3m = weightedSum(seasons, BdlSeasonAverageDTO::fg3m, totalGames);
        double totalFg3a = weightedSum(seasons, BdlSeasonAverageDTO::fg3a, totalGames);
        double totalFtm  = weightedSum(seasons, BdlSeasonAverageDTO::ftm,  totalGames);
        double totalFta  = weightedSum(seasons, BdlSeasonAverageDTO::fta,  totalGames);

        double fgPct  = totalFga  > 0 ? totalFgm  / totalFga  : 0;
        double fg3Pct = totalFg3a > 0 ? totalFg3m / totalFg3a : 0;
        double ftPct  = totalFta  > 0 ? totalFtm  / totalFta  : 0;

        return new SeasonAverages(
                null,       // null = career, not a single season
                totalGames,
                null,       // minutes not aggregated at career level
                round(pts),    round(reb),    round(ast),
                round(stl),    round(blk),    round(turnover),
                round(fgPct),  round(fg3Pct), round(ftPct),
                round(fg3m)
        );
    }

    // -------------------------------------------------------------------------
    // Math helpers
    // -------------------------------------------------------------------------

    private double weightedAvg(List<BdlSeasonAverageDTO> seasons,
                               java.util.function.Function<BdlSeasonAverageDTO, Double> extractor,
                               int totalGames) {
        double sum = 0.0;
        for (BdlSeasonAverageDTO s : seasons) {
            Double val = extractor.apply(s);
            int games  = s.gamesPlayed() != null ? s.gamesPlayed() : 0;
            if (val != null) sum += val * games;
        }
        return totalGames > 0 ? sum / totalGames : 0.0;
    }

    private double weightedSum(List<BdlSeasonAverageDTO> seasons,
                               java.util.function.Function<BdlSeasonAverageDTO, Double> extractor,
                               int totalGames) {
        double sum = 0.0;
        for (BdlSeasonAverageDTO s : seasons) {
            Double val = extractor.apply(s);
            int games  = s.gamesPlayed() != null ? s.gamesPlayed() : 0;
            if (val != null) sum += val * games;
        }
        return sum;
    }

    private SeasonAverages fetchSeasonAverages(Long playerApiId, int season) {
        BdlResponseDTO<BdlSeasonAverageDTO> response =
                balldontlieService.getSeasonAverages(playerApiId, season);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            return null;
        }
        return mapToSeasonAverages(response.data().get(0));
    }

    private SeasonAverages mapToSeasonAverages(BdlSeasonAverageDTO avg) {
        return new SeasonAverages(
                avg.season(),
                avg.gamesPlayed(),
                avg.min(),
                round(avg.pts()),    round(avg.reb()),    round(avg.ast()),
                round(avg.stl()),    round(avg.blk()),    round(avg.turnover()),
                round(avg.fgPct()),  round(avg.fg3Pct()), round(avg.ftPct()),
                round(avg.fg3m())
        );
    }

    private Double round(Double val) {
        if (val == null) return null;
        return Math.round(val * 100.0) / 100.0;
    }
}