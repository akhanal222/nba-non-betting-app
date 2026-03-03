package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.PlayerGameStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PlayerGameStatisticRepository extends JpaRepository<PlayerGameStatistic, Long> {
    // Recent stats for a player, ordered by game date (newest first), with dynamic limit
    List<PlayerGameStatistic> findByPlayer_PlayerIdOrderByGame_GameDateDesc(Long playerId, Pageable pageable);
    Optional<PlayerGameStatistic> findByGame_GameIdAndPlayer_PlayerId(Long gameId, Long playerId);
}