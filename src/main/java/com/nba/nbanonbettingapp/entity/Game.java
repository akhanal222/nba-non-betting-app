package com.nba.nbanonbettingapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "external_api_id")
    private Long externalApiId;

    @Column(name = "game_date")
    private LocalDate gameDate;

    @Column(name = "season_year")
    private Integer seasonYear;

    @Column(name = "home_team_score")
    private Integer homeTeamScore;

    @Column(name = "away_team_score")
    private Integer awayTeamScore;

    @Column(name = "game_status")
    private String gameStatus;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // Many games -> one home team
    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    // Many games -> one away team
    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    // One game -> many player stats
    @OneToMany(mappedBy = "game")
    private List<PlayerGameStatistic> playerStats = new ArrayList<>();

}