package com.nba.nbanonbettingapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "external_api_id")
    private Long externalApiId;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "city")
    private String city;

    @Column(name = "abbreviation")
    private String abbreviation;

    @Column(name = "conference")
    private String conference;

    @Column(name = "division")
    private String division;

    @Column(name = "nba_team_id")
    private Long nbaTeamId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // One team -> many players
    @JsonIgnore
    @OneToMany(mappedBy = "team")
    private List<Player> players = new ArrayList<>();

}