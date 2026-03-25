package com.nba.nbanonbettingapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BdlMetaDTO(
        @JsonProperty("next_cursor") Integer nextCursor,
        @JsonProperty("per_page") Integer perPage,
        @JsonProperty("current_Page") Integer currentPage,
        @JsonProperty("next_Page") Integer nextPage,
        @JsonProperty("total_Count")  Integer totalCount
) {}