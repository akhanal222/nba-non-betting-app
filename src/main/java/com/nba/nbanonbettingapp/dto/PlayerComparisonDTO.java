package com.nba.nbanonbettingapp.dto;

import java.util.List;

/**
 * Response DTO returned by GET /api/comparison/compare
 *
 * Contains a full profile for each of the two selected players,
 * including bio info, this season's averages, and career averages.
 */
public record PlayerComparisonDTO(
        PlayerProfile playerOne,
        PlayerProfile playerTwo
) {

    /**
     * Full profile for one player in the comparison.
     */
    public record PlayerProfile(

            // --- Identity ---
            Long   externalApiId,
            String firstName,
            String lastName,
            String imageUrl,

            // --- Bio Info ---
            String position,
            String teamName,
            String height,
            String weight,
            String college,
            String team,
            String jerseyNumber,
            Integer draftYear,
            Integer draftRound,
            Integer draftNumber,

            // --- Season Stats (current season) ---
            SeasonAverages seasonAverages,

            // --- Career Stats (all-time averages across all seasons) ---
            SeasonAverages careerAverages
    ) {}

    /**
     * Averaged stats for a season or career.
     */
    public record SeasonAverages(
            Integer season,          // null for career averages
            Integer gamesPlayed,
            Double minutesPerGame,
            Double  pts,
            Double  reb,
            Double  ast,
            Double  stl,
            Double  blk,
            Double  turnover,
            Double  fgPct,
            Double  fg3Pct,
            Double  ftPct,
            Double  fg3m
    ) {}
}