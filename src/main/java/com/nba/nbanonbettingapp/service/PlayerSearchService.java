package com.nba.nbanonbettingapp.service;

import com.nba.nbanonbettingapp.dto.BdlPlayerDTO;
import com.nba.nbanonbettingapp.dto.BdlResponseDTO;
import com.nba.nbanonbettingapp.entity.Player;
import com.nba.nbanonbettingapp.entity.Team;
import com.nba.nbanonbettingapp.repository.NbaPlayerLookupRepository;
import com.nba.nbanonbettingapp.repository.PlayerRepository;
import com.nba.nbanonbettingapp.repository.TeamLogoLookupRepository;
import com.nba.nbanonbettingapp.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerSearchService {

    // Repositories for reading/writing Players and Teams in the database
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    // Service that calls the external Balldontlie API
    private final BalldontlieService balldontlieService;
    private final NbaPlayerLookupRepository nbaLookupRepository;
    private final TeamLogoLookupRepository teamLogoLookupRepository;


    // Dependency injection through constructor
    public PlayerSearchService(PlayerRepository playerRepository,
                               TeamRepository teamRepository,
                               BalldontlieService balldontlieService,
                               NbaPlayerLookupRepository nbaLookupRepository,
                               TeamLogoLookupRepository teamLogoLookupRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.balldontlieService = balldontlieService;
        this.nbaLookupRepository = nbaLookupRepository;
        this.teamLogoLookupRepository =teamLogoLookupRepository;
    }

    /**
     * Search for players by name.
     * 1) Try database first
     * 2) If DB is empty, call Balldontlie API
     * 3) Save missing players/teams into DB for future searches
     */
    public List<Player> search(String q) {

        // Normalize input: handle null, remove extra spaces
        String query = q == null ? "" : q.trim();
        if (query.length() < 2) return List.of();

        // Split query by spaces (like this "LeBron James" -> ["LeBron", "James"])
        String[] parts = query.trim().split("\\s+");

        List<Player> dbResults;

        // If user typed first + last name, search both fields
        if (parts.length >= 2) {
            String first = parts[0];
            String last = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            // Try normal "first last"
            dbResults = playerRepository
                    .findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
                            first, last
                    );
            // This is for try "last first" if user typed reversed order
            if (dbResults.isEmpty()) {
                dbResults = playerRepository
                        .findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(last, first);
            }
        } else {
            // Otherwise search either first or last name, limit results to 20
            dbResults = playerRepository
                    .findTop20ByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrderByLastNameAsc(
                            query, query
                    );
        }
        // If the database already has matches, return them immediately
        if (!dbResults.isEmpty()) return dbResults;

        //Call API if DB empty
        BdlResponseDTO<BdlPlayerDTO> response;
        if (parts.length >= 2) {
            String first = parts[0];
            String last = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            response = balldontlieService.searchPlayersByName(first, last);
        } else {
            response = balldontlieService.searchPlayers(query);
        }
        List<BdlPlayerDTO> apiPlayers = response.data();

        if (apiPlayers == null || apiPlayers.isEmpty()) { // If API returns nothing, return empty list
            return List.of();
        }

        // Save players safely (and teams too) so future searches hit DB
        List<Player> result = new ArrayList<>();

        for (BdlPlayerDTO dto : apiPlayers) {

            // External API player id is required for saving/uniqueness
            Long apiId = (dto.id() == null) ? null : dto.id().longValue();
            if (apiId == null) continue;

            // Avoid duplicates: if player already exists by externalApiId, reuse it.
            // Otherwise create a new Player record and save it.
            Player player = playerRepository
                    .findByExternalApiId(apiId)
                    .orElseGet(() -> {

                        Player p = new Player();
                        p.setExternalApiId(apiId);
                        p.setFirstName(dto.firstName());
                        p.setLastName(dto.lastName());
                        p.setPosition(dto.position());
                        p.setHeight(dto.height());
                        p.setWeight(dto.weight());
                        p.setJerseyNumber(dto.jerseyNumber());
                        p.setIsActive(true);
                        p.setCreatedAt(OffsetDateTime.now());
                        p.setCollege(dto.college());
                        p.setDraftYear(dto.draftYear());
                        p.setDraftRound(dto.draftRound());
                        p.setDraftNumber(dto.draftNumber());

                        // If API includes a team, ensure the team exists in DB too
                        // Then connect player.team_id to that team record.
                        if (dto.team() != null && dto.team().id() != null) {

                            Long teamApiId = dto.team().id().longValue();

                            Team team = teamRepository
                                    .findByExternalApiId(teamApiId)
                                    .orElseGet(() -> {
                                        Team t = new Team();
                                        t.setExternalApiId(teamApiId);
                                        t.setTeamName(dto.team().fullName());
                                        t.setCity(dto.team().city());
                                        t.setAbbreviation(dto.team().abbreviation());
                                        t.setConference(dto.team().conference());
                                        t.setDivision(dto.team().division());
                                        t.setCreatedAt(OffsetDateTime.now());
                                        if (t.getAbbreviation() != null) {
                                            teamLogoLookupRepository
                                                    .findByAbbreviationIgnoreCase(t.getAbbreviation())
                                                    .ifPresent(l -> t.setNbaTeamId(l.getNbaTeamId()));
                                        }

                                        return teamRepository.save(t);
                                    });
                            // Backfill nbaTeamId even for existing teams
                            if (team.getNbaTeamId() == null && team.getAbbreviation() != null) {
                                teamLogoLookupRepository
                                        .findByAbbreviationIgnoreCase(team.getAbbreviation())
                                        .ifPresent(l -> {
                                            team.setNbaTeamId(l.getNbaTeamId());
                                            teamRepository.save(team);
                                        });
                            }
                            // Link player to team (sets team_id FK)
                            p.setTeam(team); //sets team_id
                        }
                        String fullName = (dto.firstName() + " " + dto.lastName()).trim();

                        // Try exact name match first
                        nbaLookupRepository.findFirstByPlayerNameIgnoreCase(fullName)
                                .ifPresent(l -> p.setNbaPlayerId(l.getNbaPlayerId()));
                        // Save new team and return it
                        return playerRepository.save(p);
                    });
            // Add existing or newly-created player to final response list
            result.add(player);
        }

        return result;
    }
}