package com.nba.nbanonbettingapp.dto;

public record BdlStatDTO(
        Integer pts,
        Integer reb,
        Integer ast,
        Integer stl,
        Integer blk,
        Integer turnover,
        String min,
        Integer fgm,
        Integer fga,
        Integer fg3m,
        Integer fg3a,
        Integer ftm,
        Integer fta,
        BdlGameDTO game,
        BdlPlayerDTO player,
        BdlTeamDTO team
) {}
