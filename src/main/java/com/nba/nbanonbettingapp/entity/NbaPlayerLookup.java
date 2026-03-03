package com.nba.nbanonbettingapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "nba_player_lookup")
public class NbaPlayerLookup {

    @Id
    @Column(name = "nba_player_id")
    private Long nbaPlayerId;

    @Column(name = "player_name", nullable = false)
    private String playerName;

    @Column(name = "position")
    private String position;
}