package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.AiExplanation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiExplanationRepository extends JpaRepository<AiExplanation, Long> {

    /**
     * Finds the most recent AI explanation for a given snapshot.
     * Used to return cached explanations without re-calling Gemini.
     */
    Optional<AiExplanation> findTopBySnapshot_SnapshotIdOrderByCreatedAtDesc(Long snapshotId);
}