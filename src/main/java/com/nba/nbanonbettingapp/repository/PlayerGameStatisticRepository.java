package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.PlayerGameStatistic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerGameStatisticRepository extends JpaRepository<PlayerGameStatistic, Long> {
}