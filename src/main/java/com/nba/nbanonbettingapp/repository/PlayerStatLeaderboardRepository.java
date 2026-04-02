package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.PlayerStatLeaderboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerStatLeaderboardRepository extends JpaRepository<PlayerStatLeaderboard, Long> {

    List<PlayerStatLeaderboard> findBySeasonYearAndStatTypeOrderByRankAsc(Integer seasonYear, String statType);

    Optional<PlayerStatLeaderboard> findByPlayer_PlayerIdAndSeasonYearAndStatType(
            Long playerId,
            Integer seasonYear,
            String statType
    );

    void deleteBySeasonYearAndStatType(Integer seasonYear, String statType);
}