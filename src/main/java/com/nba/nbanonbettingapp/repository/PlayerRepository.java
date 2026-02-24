package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
}