package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.TeamLogoLookup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamLogoLookupRepository extends JpaRepository<TeamLogoLookup, Long> {
    Optional<TeamLogoLookup> findByAbbreviationIgnoreCase(String abbreviation);
}