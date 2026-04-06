package com.nba.nbanonbettingapp.service;

import com.nba.nbanonbettingapp.dto.EWMASnapshot;
import com.nba.nbanonbettingapp.dto.PropPredictResponseDTO;
import com.nba.nbanonbettingapp.dto.StatType;
import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.entity.PlayerGameStatistic;
import com.nba.nbanonbettingapp.entity.TeamDefenseVsPosition;
import com.nba.nbanonbettingapp.repository.GameRepository;
import com.nba.nbanonbettingapp.repository.PlayerGameStatisticRepository;
import com.nba.nbanonbettingapp.repository.PlayerRepository;
import com.nba.nbanonbettingapp.repository.TeamDefenseVsPositionRepository;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Core of the prop prediction engine.
 *
 * Implements the projection formula from the research:
 *   projected_stat = EWMA_per_min × projected_minutes × pace_adj × opponent_adj × rest_adj
 *
 * Then converts the projection to over/under probabilities using:
 *   - PoissonDistribution for discrete counting stats (reb, ast, stl, blk, fg3m, turnover)
 *   - NormalDistribution for points (higher variance, more continuous)
 */
@Service
public class PropProjectionService {

    private static final Logger log = LoggerFactory.getLogger(PropProjectionService.class);

    private static final int CURRENT_SEASON = 2025;

    // Minimum stdDev floor — prevents division edge cases and unrealistic
    private static final double MIN_STD_DEV = 1.5;

    private final PlayerRepository playerRepository;
    private final PlayerGameStatisticRepository statRepository;
    private final GameRepository gameRepository;
    private final TeamDefenseVsPositionRepository dvpRepository;
    private final EWMACalculatorService ewmaCalculatorService;

    public PropProjectionService(PlayerRepository playerRepository,
                                 PlayerGameStatisticRepository statRepository,
                                 GameRepository gameRepository,
                                 TeamDefenseVsPositionRepository dvpRepository,
                                 EWMACalculatorService ewmaCalculatorService) {
        this.playerRepository = playerRepository;
        this.statRepository = statRepository;
        this.gameRepository = gameRepository;
        this.dvpRepository = dvpRepository;
        this.ewmaCalculatorService = ewmaCalculatorService;
    }


    /**
     * Generates a prop prediction for a player against a specific opponent.
     *
     * @param playerApiId     BDL external_api_id of the player
     * @param opponentTeamApiId BDL external_api_id of the opposing team
     * @param statTypeStr     Stat to predict: "pts", "reb", "ast", "stl", "blk", "turnover", "fg3m"
     * @param line            The sportsbook line (e.g. 25.5)
     */
    public PropPredictResponseDTO predict(Long playerApiId,
                                          Long opponentTeamApiId,
                                          String statTypeStr,
                                          double line) {

        StatType statType = StatType.from(statTypeStr);

        // Load player
        Player player = playerRepository.findByExternalApiId(playerApiId)
                .orElseThrow(() -> new RuntimeException(
                        "Player not found. Search for them first at /api/players/search"));
        String playerName = player.getFirstName() + " " + player.getLastName();

        // Step 1: Get EWMA snapshot
        EWMASnapshot ewma = ewmaCalculatorService.getOrCompute(playerApiId);
        if (ewma == null) {
            throw new RuntimeException(
                    "No EWMA data for " + playerName +
                            ". Hit /stats/player/external/" + playerApiId + " first to populate stats.");
        }

        // Step 2: Get the per-minute rate for the requested stat
        double perMinRate = extractPerMinRate(ewma, statType);

        // Step 3: Projected minutes (EWMA average minutes)
        double projectedMinutes = ewma.avgMinutes() != null ? ewma.avgMinutes() : 30.0;

        // Step 4: Pace adjustment
        // Uses avgPace from EWMA — compares to league average pace (~100)
        double paceAdj = computePaceAdjustment(ewma);

        // Step 5: Opponent adjustment
        // How much this team allows to players of this position vs league average
        double opponentAdj = computeOpponentAdjustment(
                opponentTeamApiId, player.getPosition(), statType);

        // Step 6: Rest adjustment — from games table
        double restAdj = computeRestAdjustment(player);

        // Step 7: Final projection
        double projectedValue = perMinRate * projectedMinutes * paceAdj * opponentAdj * restAdj;
        projectedValue = Math.max(0, projectedValue); // floor at 0

        // Step 8: Historical stdDev for the same stat (from recent box scores)
        double stdDev = computeStdDev(player, statType);

        // Step 9: Probability distribution
        double[] probs = computeProbabilities(statType, projectedValue, stdDev, line);
        double overProb  = probs[0];
        double underProb = probs[1];

        // Step 10: Confidence score
        double confidence = Math.min(1.0, ewma.gamesUsed() / 15.0);

        log.debug("PropPrediction: player={} stat={} line={} projected={} over={}%",
                playerName, statTypeStr, line,
                round(projectedValue), round(overProb * 100));

        return new PropPredictResponseDTO(
                playerName,
                statTypeStr,
                line,
                round(projectedValue),
                round(overProb),
                round(underProb),
                round(confidence),
                ewma.lowConfidence(),
                round(paceAdj),
                round(opponentAdj),
                round(restAdj),
                round(perMinRate),
                round(projectedMinutes),
                ewma.usgPct() != null ? round(ewma.usgPct()) : 0.0,
                ewma.tsPct()  != null ? round(ewma.tsPct())  : 0.0,
                ewma.gamesUsed(),
                round(stdDev)
        );
    }


    /**
     * Extracts the correct per-minute rate from the EWMA snapshot
     * based on the stat type requested.
     */
    private double extractPerMinRate(EWMASnapshot ewma, StatType stat) {
        return switch (stat) {
            case PTS      -> ewma.ptsPerMin()  != null ? ewma.ptsPerMin()  : 0.0;
            case REB      -> ewma.rebPerMin()  != null ? ewma.rebPerMin()  : 0.0;
            case AST      -> ewma.astPerMin()  != null ? ewma.astPerMin()  : 0.0;
            case STL      -> ewma.stlPerMin()  != null ? ewma.stlPerMin()  : 0.0;
            case BLK      -> ewma.blkPerMin()  != null ? ewma.blkPerMin()  : 0.0;
            case FG3M     -> ewma.fg3mPerMin() != null ? ewma.fg3mPerMin() : 0.0;
            case TURNOVER -> ewma.tovPerMin()  != null ? ewma.tovPerMin()  : 0.0;
            // Combos: sum the component per-minute rates
            case PR  -> safe(ewma.ptsPerMin())  + safe(ewma.rebPerMin());
            case PA  -> safe(ewma.ptsPerMin())  + safe(ewma.astPerMin());
            case RA  -> safe(ewma.rebPerMin())  + safe(ewma.astPerMin());
            case PRA -> safe(ewma.ptsPerMin())  + safe(ewma.rebPerMin()) + safe(ewma.astPerMin());
        };
    }

    private double safe(Double v) { return v != null ? v : 0.0; }

    /**
     * Pace adjustment.
     * Formula: avgPace / LEAGUE_AVG_PACE
     */
    private double computePaceAdjustment(EWMASnapshot ewma) {
        if (ewma.avgPace() == null || ewma.avgPace() <= 0) return 1.0;
        double leagueAvgPace = 99.5;
        double adj = ewma.avgPace() / leagueAvgPace;
        // Cap adjustment to prevent extreme outliers
        return Math.min(1.15, Math.max(0.85, adj));
    }

    private double computeOpponentAdjustment(Long opponentTeamApiId,
                                             String playerPosition,
                                             StatType stat) {
        if (stat.isCombo()) {
            List<StatType> components = switch (stat) {
                case PR  -> List.of(StatType.PTS, StatType.REB);
                case PA  -> List.of(StatType.PTS, StatType.AST);
                case RA  -> List.of(StatType.REB, StatType.AST);
                case PRA -> List.of(StatType.PTS, StatType.REB, StatType.AST);
                default  -> List.of(stat);
            };
            return components.stream()
                    .mapToDouble(c -> computeSingleOpponentAdjustment(opponentTeamApiId, playerPosition, c))
                    .average()
                    .orElse(1.0);
        }
        return computeSingleOpponentAdjustment(opponentTeamApiId, playerPosition, stat);
    }
    /**
     * Opponent adjustment — how much this team allows to this position vs league average.
     * Formula: 1 + (leagueAvgAllowed - teamAllowed) / leagueAvgAllowed
     */
    private double computeSingleOpponentAdjustment(Long opponentTeamApiId,
                                             String playerPosition,
                                             StatType stat) {
        if (opponentTeamApiId == null || playerPosition == null) return 1.0;

        // Normalize position to G/F/C
        String posGroup = normalizePosition(playerPosition);
        if (posGroup == null) return 1.0;

        // Find opponent team's internal ID
        Optional<com.nba.nbanonbettingapp.entity.Team> oppTeam =
                playerRepository.findAll().stream()
                        .filter(p -> false) // placeholder — use team repo
                        .map(p -> (com.nba.nbanonbettingapp.entity.Team) null)
                        .findFirst();

        // Look up from dvp table using the team's external API id
        List<TeamDefenseVsPosition> allRows = dvpRepository.findBySeason(CURRENT_SEASON);

        // Find the row for this opponent + position
        Optional<TeamDefenseVsPosition> dvpOpt = allRows.stream()
                .filter(d -> d.getTeam() != null &&
                        d.getTeam().getExternalApiId() != null &&
                        d.getTeam().getExternalApiId().equals(opponentTeamApiId) &&
                        posGroup.equals(d.getPosition()))
                .findFirst();

        if (dvpOpt.isEmpty()) {
            log.debug("DvP: no data for opponent={} pos={}, using neutral adj",
                    opponentTeamApiId, posGroup);
            return 1.0;
        }

        // Compute league average for this stat and position
        double leagueAvg = allRows.stream()
                .filter(d -> posGroup.equals(d.getPosition()))
                .mapToDouble(d -> extractDvpStat(d, stat))
                .filter(v -> v > 0)
                .average()
                .orElse(0.0);

        if (leagueAvg <= 0) return 1.0;

        double teamAllowed = extractDvpStat(dvpOpt.get(), stat);
        double adj = 1.0 + (leagueAvg - teamAllowed) / leagueAvg;

        return Math.min(1.20, Math.max(0.80, adj));
    }

    /**
     * Rest adjustment based on days since last game.
     */
    private double computeRestAdjustment(Player player) {
        if (player.getTeam() == null) return 1.0;

        Long teamId = player.getTeam().getTeamId();
        LocalDate today = LocalDate.now();

        // Find the most recent completed game for this team
        List<com.nba.nbanonbettingapp.entity.Game> recentGames =
                gameRepository.findBySeasonYearAndPostseasonFalse(CURRENT_SEASON)
                        .stream()
                        .filter(g -> {
                            boolean home = g.getHomeTeam() != null &&
                                    g.getHomeTeam().getTeamId().equals(teamId);
                            boolean away = g.getAwayTeam() != null &&
                                    g.getAwayTeam().getTeamId().equals(teamId);
                            return (home || away) && g.getGameDate() != null &&
                                    g.getGameDate().isBefore(today);
                        })
                        .sorted((a, b) -> b.getGameDate().compareTo(a.getGameDate()))
                        .limit(1)
                        .toList();

        if (recentGames.isEmpty()) return 1.0;

        long daysRest = today.toEpochDay() - recentGames.get(0).getGameDate().toEpochDay();

        if (daysRest == 0) return 0.93; // back-to-back
        if (daysRest >= 2) return 1.02; // extra rest
        return 1.00;                    // normal 1-day rest
    }

    private double computeStdDev(Player player, StatType statType) {
        if (statType.isCombo()) {
            // Combine individual stdDevs: sqrt(σ_a² + σ_b² + ...)
            List<StatType> components = switch (statType) {
                case PR  -> List.of(StatType.PTS, StatType.REB);
                case PA  -> List.of(StatType.PTS, StatType.AST);
                case RA  -> List.of(StatType.REB, StatType.AST);
                case PRA -> List.of(StatType.PTS, StatType.REB, StatType.AST);
                default  -> List.of(statType);
            };
            double sumOfSquares = components.stream()
                    .mapToDouble(c -> {
                        double sd = computeSingleStdDev(player, c);
                        return sd * sd;
                    })
                    .sum();
            return Math.max(MIN_STD_DEV, Math.sqrt(sumOfSquares));
        }
        return computeSingleStdDev(player, statType);
    }
    /**
     * Computes historical standard deviation for this stat
     * from the player's last 20 qualifying games.
     * Used as σ in the probability distribution.
     */
    private double computeSingleStdDev(Player player, StatType stat) {
        List<PlayerGameStatistic> recent = statRepository
                .findByPlayer_PlayerIdOrderByGame_GameDateDesc(
                        player.getPlayerId(), PageRequest.of(0, 20));

        List<Integer> values = recent.stream()
                .filter(s -> {
                    String min = s.getMinutesPlayed();
                    return min != null && !min.isBlank()
                            && !min.equals("0") && !min.equals("00")
                            && !min.equals("0:00") && !min.equals("00:00");
                })
                .map(s -> extractStatValue(s, stat))
                .toList();

        if (values.size() < 2) return MIN_STD_DEV;

        double avg = values.stream().mapToInt(i -> i).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - avg, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        return Math.max(MIN_STD_DEV, stdDev);
    }

    /**
     * Converts the projected value to over/under probabilities.
     */
    private double[] computeProbabilities(StatType statType, double projectedValue,
                                          double stdDev, double line) {
        if (projectedValue <= 0) return new double[]{0.5, 0.5};

        try {
            if (statType == StatType.PTS || statType.isCombo()) {
                // Points: Normal distribution
                NormalDistribution nd = new NormalDistribution(projectedValue, stdDev);
                double overProb  = 1.0 - nd.cumulativeProbability(line);
                double underProb = nd.cumulativeProbability(line);
                return new double[]{
                        Math.min(0.99, Math.max(0.01, overProb)),
                        Math.min(0.99, Math.max(0.01, underProb))
                };
            } else {
                // Discrete counting stats: Poisson distribution
                double lambda = Math.max(0.1, projectedValue);
                PoissonDistribution pd = new PoissonDistribution(lambda);
                // P(over line) = P(X > floor(line)) = 1 - P(X <= floor(line))
                double overProb  = 1.0 - pd.cumulativeProbability((int) Math.floor(line));
                double underProb = pd.cumulativeProbability((int) Math.floor(line));
                return new double[]{
                        Math.min(0.99, Math.max(0.01, overProb)),
                        Math.min(0.99, Math.max(0.01, underProb))
                };
            }
        } catch (Exception e) {
            log.warn("PropProjection: probability computation failed: {}", e.getMessage());
            return new double[]{0.5, 0.5};
        }
    }


    private int extractStatValue(PlayerGameStatistic s, StatType stat) {
        return switch (stat) {
            case PTS      -> s.getPointsScored()       != null ? s.getPointsScored()       : 0;
            case REB      -> s.getTotalRebounds()       != null ? s.getTotalRebounds()       : 0;
            case AST      -> s.getAssists()             != null ? s.getAssists()             : 0;
            case STL      -> s.getSteals()              != null ? s.getSteals()              : 0;
            case BLK      -> s.getBlocks()              != null ? s.getBlocks()              : 0;
            case FG3M     -> s.getThreePointShotsMade() != null ? s.getThreePointShotsMade() : 0;
            case TURNOVER -> s.getTurnovers()           != null ? s.getTurnovers()           : 0;
            case PR  -> (s.getPointsScored()  != null ? s.getPointsScored()  : 0)
                    + (s.getTotalRebounds() != null ? s.getTotalRebounds() : 0);
            case PA  -> (s.getPointsScored()  != null ? s.getPointsScored()  : 0)
                    + (s.getAssists()       != null ? s.getAssists()       : 0);
            case RA  -> (s.getTotalRebounds() != null ? s.getTotalRebounds() : 0)
                    + (s.getAssists()       != null ? s.getAssists()       : 0);
            case PRA -> (s.getPointsScored()  != null ? s.getPointsScored()  : 0)
                    + (s.getTotalRebounds() != null ? s.getTotalRebounds() : 0)
                    + (s.getAssists()       != null ? s.getAssists()       : 0);
        };
    }

    private double extractDvpStat(TeamDefenseVsPosition dvp, StatType stat) {
        return switch (stat) {
            case PTS      -> dvp.getPtsAllowed()  != null ? dvp.getPtsAllowed()  : 0.0;
            case REB      -> dvp.getRebAllowed()  != null ? dvp.getRebAllowed()  : 0.0;
            case AST      -> dvp.getAstAllowed()  != null ? dvp.getAstAllowed()  : 0.0;
            case STL      -> dvp.getStlAllowed()  != null ? dvp.getStlAllowed()  : 0.0;
            case BLK      -> dvp.getBlkAllowed()  != null ? dvp.getBlkAllowed()  : 0.0;
            case FG3M     -> dvp.getFg3mAllowed() != null ? dvp.getFg3mAllowed() : 0.0;
            case TURNOVER -> dvp.getTovAllowed()  != null ? dvp.getTovAllowed()  : 0.0;
            // Combos are decomposed before this method is called — should never reach here
            case PR, PA, RA, PRA -> 0.0;
        };
    }

    private String normalizePosition(String pos) {
        if (pos == null) return null;
        String p = pos.toUpperCase().trim();
        if (p.startsWith("G") || p.equals("PG") || p.equals("SG")) return "G";
        if (p.startsWith("F") || p.equals("SF") || p.equals("PF")) return "F";
        if (p.startsWith("C"))                                        return "C";
        if (p.contains("-")) return normalizePosition(p.split("-")[0]);
        return null;
    }

    private double round(double val) {
        return Math.round(val * 1000.0) / 1000.0;
    }
}