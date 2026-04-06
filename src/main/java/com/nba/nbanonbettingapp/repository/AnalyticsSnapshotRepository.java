package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.AnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, Long> {

    /**
     * Finds the most recent snapshot for a player with a given metric type and
     * parameters fingerprint. Used to check if we can return a cached explanation
     * instead of calling Gemini again.
     */
    Optional<AnalyticsSnapshot> findTopByPlayer_PlayerIdAndMetricTypeOrderByComputedAtDesc(
            Long playerId,
            String metricType
    );
}