package com.nba.nbanonbettingapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStatLeaderboardDTO {

    private Long playerId;
    private Long externalApiId;
    private String firstName;
    private String lastName;
    private String fullName;

    private String teamName;
    private String position;

    private Integer seasonYear;
    private String statType;
    private Integer rank;

    private Double seasonAvg;
    private Double last5Avg;
    private Integer gamesPlayed;
}