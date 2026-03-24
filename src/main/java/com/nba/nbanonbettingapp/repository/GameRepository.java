package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByExternalApiId(Long externalApiId);
    List<Game> findBySeasonYearAndPostseasonFalse(Integer seasonYear);
}