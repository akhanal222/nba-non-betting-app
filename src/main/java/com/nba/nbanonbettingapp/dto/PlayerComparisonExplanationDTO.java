package com.nba.nbanonbettingapp.dto;
import java.time.OffsetDateTime;

public record PlayerComparisonExplanationDTO(
        Long explanationId,
        String playerName,
        String analysisType,
        String seasonExplanation,
        String careerExplanation,
        String bottomLine,
        String modelUsed,
        OffsetDateTime generatedAt,
        boolean cached
) {}