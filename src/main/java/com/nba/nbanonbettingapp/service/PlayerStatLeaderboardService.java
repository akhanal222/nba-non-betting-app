package com.nba.nbanonbettingapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nba.nbanonbettingapp.dto.BdlResponseDTO;
import com.nba.nbanonbettingapp.dto.BdlSeasonAverageDTO;
import com.nba.nbanonbettingapp.dto.PlayerStatLeaderboardDTO;
import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.entity.PlayerGameStatistic;
import com.nba.nbanonbettingapp.entity.PlayerStatLeaderboard;
import com.nba.nbanonbettingapp.repository.PlayerGameStatisticRepository;
import com.nba.nbanonbettingapp.repository.PlayerRepository;
import com.nba.nbanonbettingapp.repository.PlayerStatLeaderboardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class PlayerStatLeaderboardService {

    private final PlayerStatLeaderboardRepository leaderboardRepository;
    private final PlayerRepository playerRepository;
    private final PlayerGameStatisticRepository playerGameStatisticRepository;
    private final BalldontlieService balldontlieService;

    public PlayerStatLeaderboardService(PlayerStatLeaderboardRepository leaderboardRepository,
                                        PlayerRepository playerRepository,
                                        PlayerGameStatisticRepository playerGameStatisticRepository,
                                        BalldontlieService balldontlieService) {
        this.leaderboardRepository = leaderboardRepository;
        this.playerRepository = playerRepository;
        this.playerGameStatisticRepository = playerGameStatisticRepository;
        this.balldontlieService = balldontlieService;
    }

    public List<PlayerStatLeaderboardDTO> getLeaderboard(Integer seasonYear, String statType) {
        return leaderboardRepository
                .findBySeasonYearAndStatTypeOrderByRankAsc(seasonYear, statType.toUpperCase())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void refreshLeaderboard(Integer seasonYear, String statType) {
        // Keep API and DB stat keys in their expected formats.
        String apiStatType = statType.toLowerCase();
        String dbStatType = statType.toUpperCase();

        // Rebuild this leaderboard slice so ranks/averages are fully fresh.
        leaderboardRepository.deleteBySeasonYearAndStatType(seasonYear, dbStatType);
        leaderboardRepository.flush();


        JsonNode root = balldontlieService.getTop20SeasonLeaders(apiStatType, seasonYear);

        // Fail fast if upstream payload is not usable.
        if (root == null || !root.has("data") || !root.get("data").isArray()) {
            throw new RuntimeException("Could not load top 20 leaders for " + dbStatType);
        }

        int rank = 1;
        Set<Long> addedPlayerIds = new HashSet<>();

        for (JsonNode leaderNode : root.get("data")) {
            JsonNode playerNode = leaderNode.path("player");

            Long externalApiId = playerNode.path("id").isMissingNode() || playerNode.path("id").isNull()
                    ? null
                    : playerNode.path("id").asLong();

            if (externalApiId == null) {
                continue;
            }

            // Reuse existing player row when present, otherwise create a minimal record.
            Optional<Player> playerOpt = playerRepository.findByExternalApiId(externalApiId);

            Player player;

            if (playerOpt.isPresent()) {
                player = playerOpt.get();
            } else {
                player = Player.builder()
                        .externalApiId(externalApiId)
                        .firstName(playerNode.path("first_name").asText(null))
                        .lastName(playerNode.path("last_name").asText(null))
                        .position(playerNode.path("position").asText(null))
                        .createdAt(OffsetDateTime.now())
                        .build();

                player = playerRepository.save(player);
            }

            // skip duplicate player in same refresh
            if (addedPlayerIds.contains(player.getPlayerId())) {
                continue;
            }

            addedPlayerIds.add(player.getPlayerId());

            // Store both season-long and recent-form metrics for the leaderboard card.
            BigDecimal seasonAvg = fetchSeasonAverageForStat(externalApiId, seasonYear, dbStatType);
            BigDecimal last5Avg = calculateLast5AverageFromDb(player.getPlayerId(), seasonYear, dbStatType);
            Integer gamesPlayed = countSeasonGamesFromDb(player.getPlayerId(), seasonYear);

            PlayerStatLeaderboard row = PlayerStatLeaderboard.builder()
                    .player(player)
                    .seasonYear(seasonYear)
                    .statType(dbStatType)
                    .rank(rank)
                    .seasonAvg(seasonAvg)
                    .last5Avg(last5Avg)
                    .gamesPlayed(gamesPlayed)
                    .updatedAt(OffsetDateTime.now())
                    .build();

            leaderboardRepository.save(row);
            rank++;
        }
    }

    private BigDecimal fetchSeasonAverageForStat(Long externalApiId, Integer seasonYear, String statType) {
        // Season averages still come from Balldontlie and are cached into leaderboard rows.
        BdlResponseDTO<BdlSeasonAverageDTO> response =
                balldontlieService.getSeasonAverages(externalApiId, seasonYear);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BdlSeasonAverageDTO avg = response.data().get(0);
        Double value = extractSeasonAverageValue(avg, statType);

        if (value == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private Double extractSeasonAverageValue(BdlSeasonAverageDTO avg, String statType) {
        return switch (statType) {
            case "PTS" -> avg.pts();
            case "REB" -> avg.reb();
            case "AST" -> avg.ast();
            default -> null;
        };
    }

    private BigDecimal calculateLast5AverageFromDb(Long playerId, Integer seasonYear, String statType) {
        // Last-5 trend is computed from already-saved game stats in our DB.
        List<PlayerGameStatistic> last5 = playerGameStatisticRepository
                .findTop5ByPlayer_PlayerIdAndGame_SeasonYearOrderByGame_GameDateDesc(playerId, seasonYear);

        if (last5 == null || last5.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double average = last5.stream()
                .mapToDouble(stat -> getStatValue(stat, statType))
                .average()
                .orElse(0.0);

        return BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
    }

    private double getStatValue(PlayerGameStatistic stat, String statType) {
        return switch (statType) {
            case "PTS" -> stat.getPointsScored() != null ? stat.getPointsScored() : 0;
            case "REB" -> stat.getTotalRebounds() != null ? stat.getTotalRebounds() : 0;
            case "AST" -> stat.getAssists() != null ? stat.getAssists() : 0;
            default -> 0;
        };
    }

    private Integer countSeasonGamesFromDb(Long playerId, Integer seasonYear) {
        return playerGameStatisticRepository.countByPlayer_PlayerIdAndGame_SeasonYear(playerId, seasonYear);
    }

    private PlayerStatLeaderboardDTO toDTO(PlayerStatLeaderboard row) {
        return PlayerStatLeaderboardDTO.builder()
                .playerId(row.getPlayer().getPlayerId())
                .externalApiId(row.getPlayer().getExternalApiId())
                .firstName(row.getPlayer().getFirstName())
                .lastName(row.getPlayer().getLastName())
                .fullName(row.getPlayer().getFirstName() + " " + row.getPlayer().getLastName())
                .teamName(row.getPlayer().getTeam() != null ? row.getPlayer().getTeam().getTeamName() : null)
                .position(row.getPlayer().getPosition())
                .seasonYear(row.getSeasonYear())
                .statType(row.getStatType())
                .rank(row.getRank())
                .seasonAvg(row.getSeasonAvg() != null ? row.getSeasonAvg().doubleValue() : null)
                .last5Avg(row.getLast5Avg() != null ? row.getLast5Avg().doubleValue() : null)
                .gamesPlayed(row.getGamesPlayed())
                .build();
    }
}