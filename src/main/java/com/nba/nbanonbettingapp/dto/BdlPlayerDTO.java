package com.nba.nbanonbettingapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BdlPlayerDTO(
        Integer id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String height,
        String weight,
        String position,
        @JsonProperty("jersey_number") String jerseyNumber,
        String college,
        @JsonProperty("draft_year") Integer draftYear,
        @JsonProperty("draft_round") Integer draftRound,
        @JsonProperty("draft_number") Integer draftNumber,
        BdlTeamDTO team
) {}