package com.nba.nbanonbettingapp.dto;

public record BdlGameDTO(
        Integer id,
        String date,
        Integer season,
        String status,
        Integer home_team_score,
        Integer visitor_team_score,
        BdlTeamDTO home_team,
        BdlTeamDTO visitor_team
) {}
