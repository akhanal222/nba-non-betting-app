package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.TeamStanding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamStandingRepository extends JpaRepository<TeamStanding, Long> {
    void deleteBySeasonYear(Integer seasonYear);
    List<TeamStanding> findBySeasonYearOrderByWinsDescPointDifferenceDesc(Integer seasonYear);
}