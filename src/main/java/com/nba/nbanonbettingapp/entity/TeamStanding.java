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
@Table(
        name = "team_standings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"team_id", "season"})
        }
)
public class TeamStanding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long standingId;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "season", nullable = false)
    private Integer seasonYear;

    @Column(name = "games_played")
    private Integer gamesPlayed = 0;

    @Column(name = "wins")
    private Integer wins = 0;

    @Column(name = "losses")
    private Integer losses = 0;

    @Column(name = "win_percentage")
    private Double winPercentage = 0.0;

    @Column(name = "points_for")
    private Integer pointsFor = 0;

    @Column(name = "points_against")
    private Integer pointsAgainst = 0;

    @Column(name = "point_difference")
    private Integer pointDifference = 0;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}