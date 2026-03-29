package com.nba.nbanonbettingapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps a single season's averages from BallDontLie /season_averages/general endpoint.
 */
public record BdlSeasonAverageDTO(
        Integer season,
        @JsonProperty("season_type") String seasonType,
        Stats stats
) {

    /**
     * Nested stats object — all per-game averages live here.
     * Field names match BDL's exact JSON keys.
     */
    public record Stats(
            Integer gp,
            Double min,
            Double pts,
            Double reb,
            Double ast,
            Double stl,
            Double blk,
            Double tov,
            Double fgm,
            Double fga,
            @JsonProperty("fg_pct")  Double fgPct,
            Double fg3m,
            Double fg3a,
            @JsonProperty("fg3_pct") Double fg3Pct,
            Double ftm,
            Double fta,
            @JsonProperty("ft_pct")  Double ftPct,
            Double oreb,
            Double dreb
    ) {}

    // ── Convenience accessors so the rest of the service code stays clean ────
    // These delegate into the nested stats object with null safety.

    public Integer gamesPlayed() {
        return stats != null ? stats.gp() : null;
    }

    public Double min()      { return stats != null ? stats.min()    : null; }
    public Double pts()      { return stats != null ? stats.pts()    : null; }
    public Double reb()      { return stats != null ? stats.reb()    : null; }
    public Double ast()      { return stats != null ? stats.ast()    : null; }
    public Double stl()      { return stats != null ? stats.stl()    : null; }
    public Double blk()      { return stats != null ? stats.blk()    : null; }
    public Double turnover() { return stats != null ? stats.tov()    : null; }
    public Double fgm()      { return stats != null ? stats.fgm()    : null; }
    public Double fga()      { return stats != null ? stats.fga()    : null; }
    public Double fgPct()    { return stats != null ? stats.fgPct()  : null; }
    public Double fg3m()     { return stats != null ? stats.fg3m()   : null; }
    public Double fg3a()     { return stats != null ? stats.fg3a()   : null; }
    public Double fg3Pct()   { return stats != null ? stats.fg3Pct() : null; }
    public Double ftm()      { return stats != null ? stats.ftm()    : null; }
    public Double fta()      { return stats != null ? stats.fta()    : null; }
    public Double ftPct()    { return stats != null ? stats.ftPct()  : null; }
}