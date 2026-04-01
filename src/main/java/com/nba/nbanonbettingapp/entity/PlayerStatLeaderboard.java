package com.nba.nbanonbettingapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "player_stat_leaderboard",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"player_id", "season_year", "stat_type"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStatLeaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leaderboard_id")
    private Long leaderboardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "season_year", nullable = false)
    private Integer seasonYear;

    @Column(name = "stat_type", nullable = false, length = 10)
    private String statType;   // PTS, REB, AST

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "season_avg", nullable = false, precision = 6, scale = 2)
    private BigDecimal seasonAvg;

    @Column(name = "last5_avg", nullable = false, precision = 6, scale = 2)
    private BigDecimal last5Avg;

    @Column(name = "games_played")
    private Integer gamesPlayed;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}