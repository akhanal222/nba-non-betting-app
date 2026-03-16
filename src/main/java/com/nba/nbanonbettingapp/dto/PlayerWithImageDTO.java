package com.nba.nbanonbettingapp.dto;

public record PlayerWithImageDTO(
        Long id,
        String firstName,
        String lastName,
        String position,
        String height,
        String weight,
        String jerseyNumber,
        Long nbaPlayerId,
        String imageUrl
) {
}