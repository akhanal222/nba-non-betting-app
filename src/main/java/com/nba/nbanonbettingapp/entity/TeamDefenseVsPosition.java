package com.nba.nbanonbettingapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Stores how many stats each team allows per game, broken down by
 * the opponent player's position (G, F, C).
 *
 * Populated by DvPIngestionService from BDL's opponent averages endpoint.
 * Used by PropProjectionService as the opponent_adj in the projection formula.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "team_defense_vs_position",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_dvp",
                columnNames = {"team_id", "season", "position"}
        )
)
public class TeamDefenseVsPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "season", nullable = false)
    private Integer season;

    @Column(name = "position", nullable = false, length = 5)
    private String position;

    // Stats allowed per game to this position

    @Column(name = "pts_allowed")
    private Double ptsAllowed;

    @Column(name = "reb_allowed")
    private Double rebAllowed;

    @Column(name = "ast_allowed")
    private Double astAllowed;

    @Column(name = "fg3m_allowed")
    private Double fg3mAllowed;

    @Column(name = "stl_allowed")
    private Double stlAllowed;

    @Column(name = "blk_allowed")
    private Double blkAllowed;

    @Column(name = "tov_allowed")
    private Double tovAllowed;

    // Team-level defensive rating from BDL opponent averages endpoint
    @Column(name = "def_rating")
    private Double defRating;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}