package com.nba.nbanonbettingapp.dto;

import java.time.OffsetDateTime;

/**
 * Response DTO returned by all AI explanation endpoints.
 */
public record AiExplanationResponseDTO(

        Long explanationId,

        String playerName,

        String analysisType,

        String explanation,

        String modelUsed,

        OffsetDateTime generatedAt,

        boolean cached

) {}