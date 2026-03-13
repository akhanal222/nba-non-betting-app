package com.nba.nbanonbettingapp.dto;

import java.util.List;

/**
 * Response DTO returned by both the Head-to-Head and Recent Stats analysis endpoints.
 *
 * Generic across all stat types — statType indicates which stat was analyzed
 * (e.g. "pts", "ast", "reb") and statValue in GameLineResult holds the value
 * for that stat in each game.
 */
public record HeadToHeadResultDTO(

        // Display name of the player being analyzed
        String playerName,

        // Display name of the opponent team, or "All Opponents" for recent stats
        String opponentTeamName,

        // The stat type analyzed (e.g. "pts", "ast", "reb", "stl", "blk", "turnover")
        String statType,

        // The sportsbook line the user entered (e.g. 15.5)
        double statLine,

        // One entry per game, newest first
        List<GameLineResult> games,

        // Mean stat value across all returned games
        double average,

        // Population standard deviation — measures consistency
        double standardDeviation,

        // Number of games where statValue > statLine
        int hitCount,

        // Total games found
        int totalGames,

        // hitCount / totalGames, rounded to 2 decimal places
        double hitRate

) {

    /**
     * Represents a single game result.
     *
     * @param date       Game date (format: "YYYY-MM-DD")
     * @param statValue  Value of the analyzed stat in this game (e.g. assists, points)
     * @param hitLine    true if statValue > statLine
     */
    public record GameLineResult(
            String date,
            int statValue,
            boolean hitLine
    ) {}
}