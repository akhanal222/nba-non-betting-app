package com.nba.nbanonbettingapp.repository;

import com.nba.nbanonbettingapp.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findTop20ByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrderByLastNameAsc(
            String firstName, String lastName
    );
    List<Player> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
            String firstName, String lastName
    );

    Optional<Player> findByExternalApiId(Long externalApiId);
}