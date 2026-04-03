package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.PlayerAdvancedStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerAdvancedStatsRepository extends JpaRepository<PlayerAdvancedStats, Long> {

    /**
     * Upsert key lookup — find an existing row by the BDL advanced stat id.
     * Used by AdvancedStatsIngestionService to decide create vs update.
     */
    Optional<PlayerAdvancedStats> findByExternalAdvId(Long externalAdvId);

    /**
     * Returns the most recent N advanced stat rows for a player, newest first.
     * Used by EWMACalculatorService to fetch the 20-game window.
     */
    List<PlayerAdvancedStats> findByPlayer_PlayerIdOrderByGame_GameDateDesc(
            Long playerId, Pageable pageable
    );

    /**
     * Check whether we already have an advanced stats row for a specific game+player.
     * Allows AdvancedStatsIngestionService to skip games already persisted.
     */
    boolean existsByPlayer_PlayerIdAndGame_GameId(Long playerId, Long gameId);
}