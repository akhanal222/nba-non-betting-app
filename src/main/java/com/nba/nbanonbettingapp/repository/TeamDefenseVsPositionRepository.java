package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.TeamDefenseVsPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamDefenseVsPositionRepository extends JpaRepository<TeamDefenseVsPosition, Long> {

    /** Lookup for a specific matchup — team, season, position */
    Optional<TeamDefenseVsPosition> findByTeam_TeamIdAndSeasonAndPosition(
            Long teamId, Integer season, String position);

    /** All rows for a given season — used to compute league averages */
    List<TeamDefenseVsPosition> findBySeason(Integer season);

    /** All rows for a team across seasons */
    List<TeamDefenseVsPosition> findByTeam_TeamId(Long teamId);
}