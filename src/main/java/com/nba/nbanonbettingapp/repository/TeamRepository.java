package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByExternalApiId(Long externalApiId);
}