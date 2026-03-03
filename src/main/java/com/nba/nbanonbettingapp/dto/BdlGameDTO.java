package com.nba.nbanonbettingapp.dto;

public record BdlGameDTO(
        Integer id,
        String date,
        Integer season,
        BdlTeamDTO home_team,
        BdlTeamDTO visitor_team
) {}
