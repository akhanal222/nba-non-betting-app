package com.nba.nbanonbettingapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BdlMetaDTO(
        @JsonProperty("next_cursor") Integer nextCursor,
        @JsonProperty("per_page") Integer perPage
) {}