package com.nba.nbanonbettingapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BdlPlayerDTO(
        Integer id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String position,
        BdlTeamDTO team
) {}