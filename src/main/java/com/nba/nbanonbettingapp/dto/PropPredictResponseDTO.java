package com.nba.nbanonbettingapp.dto;

/**
 * Response returned by GET /api/props/predict.
 */
public record PropPredictResponseDTO(

        String playerName,
        String statType,
        double line,

        double projectedValue,
        double overProbability,
        double underProbability,
        double confidence,
        boolean lowConfidence,

        // Adjustment multipliers — 1.0 is neutral
        double paceAdjustment,
        double opponentAdjustment,
        double restAdjustment,

        // Model inputs exposed for the AI explanation layer
        double ewmaPerMinute,
        double projectedMinutes,
        double usgPct,
        double tsPct,
        int    gamesUsed,
        double stdDev

) {}