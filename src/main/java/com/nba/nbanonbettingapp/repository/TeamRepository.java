package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}