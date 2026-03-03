package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByExternalApiId(Long externalApiId);
}