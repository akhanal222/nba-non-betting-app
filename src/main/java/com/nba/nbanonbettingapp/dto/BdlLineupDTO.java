package com.nba.nbanonbettingapp.dto;

public record BdlLineupDTO(
        Integer id,
        Integer game_id,
        Boolean starter,
        String position,
        BdlPlayerDTO player,
        BdlTeamDTO team
) {}