package com.nba.nbanonbettingapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "player_game_statistics")
public class PlayerGameStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statistic_id")
    private Long statisticId;

    @Column(name = "minutes_played")
    private String minutesPlayed;

    @Column(name = "points_scored")
    private Integer pointsScored;

    @Column(name = "total_rebounds")
    private Integer totalRebounds;

    @Column(name = "assists")
    private Integer assists;

    @Column(name = "steals")
    private Integer steals;

    @Column(name = "blocks")
    private Integer blocks;

    @Column(name = "turnovers")
    private Integer turnovers;

    @Column(name = "field_goals_made")
    private Integer fieldGoalsMade;

    @Column(name = "field_goals_attempted")
    private Integer fieldGoalsAttempted;

    @Column(name = "three_point_shots_made")
    private Integer threePointShotsMade;

    @Column(name = "three_point_shots_attempted")
    private Integer threePointShotsAttempted;

    @Column(name = "free_throws_made")
    private Integer freeThrowsMade;

    @Column(name = "free_throws_attempted")
    private Integer freeThrowsAttempted;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // Many stats -> one game
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    // Many stats -> one player
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    // Many stats -> one team
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

}