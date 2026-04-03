package com.nba.nbanonbettingapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Holds the result of a 20-game EWMA computation for one player.
 * Serialized to JSON and cached in players.ewma_json.
 *
 * @JsonIgnoreProperties prevents deserialization errors if fields are
 * added or removed from this record between cache writes and reads.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EWMASnapshot(

        // Per-minute rates for each prop stat type

        Double ptsPerMin,
        Double rebPerMin,
        Double astPerMin,
        Double stlPerMin,
        Double blkPerMin,
        Double fg3mPerMin,
        Double tovPerMin,

        // Weighted average of minutes played — used as projected_minutes baseline.

        Double avgMinutes,

        // Advanced stat EWMA, from player_advanced_stats

        Double usgPct,       // usage percentage — how many possessions player uses
        Double tsPct,        // true shooting % — scoring efficiency
        Double avgPace,      // average game pace over the window

        // Metadata

        int gamesUsed,       // how many games contributed to the window (max 20)
        boolean lowConfidence, // true if gamesUsed < 5
        String computedAt    // ISO timestamp — when this snapshot was computed

) {}