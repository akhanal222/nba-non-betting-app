package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}