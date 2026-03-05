package com.nba.nbanonbettingapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "team_lookup")
public class TeamLogoLookup {

    @Id
    @Column(name = "nba_team_id")
    private Long nbaTeamId;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "abbreviation")
    private String abbreviation;

    @Transient
    public String getLogoUrl() {
        return "https://cdn.nba.com/logos/nba/" + nbaTeamId + "/primary/L/logo.svg";
    }
}