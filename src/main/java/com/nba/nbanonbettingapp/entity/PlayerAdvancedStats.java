package com.nba.nbanonbettingapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Stores per-game advanced stats for a player.
 *
 * Populated by AdvancedStatsIngestionService which is called from
 * PlayerStatsService after each box score upsert.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "player_advanced_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_adv_player_game",
                columnNames = {"player_id", "game_id"}
        )
)
public class PlayerAdvancedStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "external_adv_id", unique = true)
    private Long externalAdvId;

    // ── Relationships ─────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;


    /** USG% — #2 predictor. Fraction of team possessions used by this player. */
    @Column(name = "usage_percentage")
    private Double usagePercentage;

    /** TS% — #3 predictor. Scoring efficiency across 2PT, 3PT, and FT. */
    @Column(name = "true_shooting_percentage")
    private Double trueShootingPercentage;

    /** Pace — #4 predictor. Game-tempo multiplier for the projection formula. */
    @Column(name = "pace")
    private Double pace;

    /** Offensive rating — player impact quality signal used in EWMA weighting. */
    @Column(name = "offensive_rating")
    private Double offensiveRating;

    /** Defensive rating — player impact quality signal. */
    @Column(name = "defensive_rating")
    private Double defensiveRating;

    @Column(name = "net_rating")
    private Double netRating;

    /** PIE — Player Impact Estimate. Overall impact proxy. */
    @Column(name = "pie")
    private Double pie;

    // Prop-type-specific rate stats
    // Used for the opponent adjustment on AST and REB props.

    @Column(name = "assist_percentage")
    private Double assistPercentage;

    @Column(name = "rebound_percentage")
    private Double reboundPercentage;

    @Column(name = "offensive_rebound_percentage")
    private Double offensiveReboundPercentage;

    @Column(name = "defensive_rebound_percentage")
    private Double defensiveReboundPercentage;

    // Metadata

    @Column(name = "data_version", length = 2)
    private String dataVersion;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;
}