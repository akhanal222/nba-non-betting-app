package com.nba.nbanonbettingapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BdlTeamDTO(
        Integer id,
        String abbreviation,
        String city,
        String conference,
        String division,
        String name,
        @JsonProperty("full_name") String fullName
) {}