package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.NbaPlayerLookup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NbaPlayerLookupRepository extends JpaRepository<NbaPlayerLookup, Long> {

    Optional<NbaPlayerLookup> findFirstByPlayerNameIgnoreCase(String playerName);

    // Check for name + position matching)
    Optional<NbaPlayerLookup> findFirstByPlayerNameIgnoreCaseAndPosition(String playerName, String position);
}