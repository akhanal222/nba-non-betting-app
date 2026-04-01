package com.nba.nbanonbettingapp.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
@AllArgsConstructor
@Builder
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "external_api_id", unique = true)
    private Long externalApiId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "position")
    private String position;

    @Column(name = "height")
    private String height;

    @Column(name = "weight")
    private String weight;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "jersey_number")
    private String jerseyNumber;

    @Column(name = "nba_player_id")
    private Long nbaPlayerId;

    @Column(name = "college")
    private String college;

    @Column(name = "draft_year")
    private Integer draftYear;

    @Column(name = "draft_round")
    private Integer draftRound;

    @Column(name = "draft_number")
    private Integer draftNumber;

    @Column(name = "career_averages_json", columnDefinition = "TEXT")
    private String careerAveragesJson;

    @Column(name = "career_averages_cached_at")
    private OffsetDateTime careerAveragesCachedAt;

    // Many players -> one team
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    // One player -> many stats
    @JsonBackReference
    @OneToMany(mappedBy = "player")
    private List<PlayerGameStatistic> gameStats = new ArrayList<>();

}