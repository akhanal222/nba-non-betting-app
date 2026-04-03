package com.nba.nbanonbettingapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the per-game advanced stats response from BallDontLie.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BdlAdvancedStatsDTO(

        Integer id,
        Integer period,

        NestedGame game,
        NestedPlayer player,
        NestedTeam team,

        // Core formula inputs

        @JsonProperty("usage_percentage")
        Double usagePercentage,

        @JsonProperty("true_shooting_percentage")
        Double trueShootingPercentage,

        Double pace,

        @JsonProperty("offensive_rating")
        Double offensiveRating,

        @JsonProperty("defensive_rating")
        Double defensiveRating,

        @JsonProperty("net_rating")
        Double netRating,

        Double pie,

        // Prop-type-specific rate stats

        @JsonProperty("assist_percentage")
        Double assistPercentage,

        @JsonProperty("rebound_percentage")
        Double reboundPercentage,

        @JsonProperty("offensive_rebound_percentage")
        Double offensiveReboundPercentage,

        @JsonProperty("defensive_rebound_percentage")
        Double defensiveReboundPercentage

) {

    // Nested record

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NestedGame(Integer id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NestedPlayer(Integer id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NestedTeam(Integer id) {}
}