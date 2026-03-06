import "./App.css";
import { useEffect, useState } from "react";
import NavBar from "./components/NavBar";
import PlayerCard from "./components/PlayerCard";
import AnalyzePanel from "./components/AnalyzePanel";

// All the Endpoint(Testing backend, get all the teams, and search player)
const API = {
  test: "http://localhost:8080/test",
  teams: "http://localhost:8080/teams",
  playerSearch: (q) => `http://localhost:8080/api/players/search?q=${encodeURIComponent(q)}`,
  upcomingGames: "http://localhost:8080/bdl/games/upcoming?days=2",
};

// Filter options (This is for next week )
const TEAM_FILTERS = ["All Teams", "Lakers", "Warriors", "Celtics", "Suns", "Nets"];


export default function App() {
  const [activePage, setActivePage] = useState("Home");
  const [teams, setTeams] = useState([]);
  const [q, setQ] = useState("");
  const [players, setPlayers] = useState([]);
  const [loadingPlayers, setLoadingPlayers] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [selectedTeam, setSelectedTeam] = useState("All Teams");
  const [selectedPlayer, setSelectedPlayer] = useState(null);
  const [upcomingGames, setUpcomingGames] = useState([]);
  const [loadingGames, setLoadingGames] = useState(false);

  // Checking if backend is running or not
  useEffect(() => {
    fetch(API.test).catch(() => {});
    fetch(API.teams)
      .then((res) => res.json())
      .then((data) => setTeams(data.data || data))
      .catch(() => {});
    // Fetch upcoming games
    setLoadingGames(true);
    fetch(API.upcomingGames)
        .then((res) => res.json())
        .then((data) => {
          setUpcomingGames(Array.isArray(data) ? data : data.data || []);
          setLoadingGames(false);
        })
        .catch(() => setLoadingGames(false));
  }, []);


  // Search players by name
  const searchPlayers = () => {
    const query = q.trim();
    if (query.length < 2) return;
    setLoadingPlayers(true);
    setHasSearched(true);
    fetch(API.playerSearch(query))
      .then((res) => res.json())
      .then((data) => { setPlayers(Array.isArray(data) ? data : []); setLoadingPlayers(false); })
      .catch(() => setLoadingPlayers(false));
  };

  // Filter by team only ( This is placeholder for next week work )
  const filteredPlayers = players.filter((p) => {
    if (selectedTeam !== "All Teams") {
      const abbr = (p.team?.abbreviation ?? "").toLowerCase();
      const name = (p.team?.teamName ?? "").toLowerCase();
      if (!abbr.includes(selectedTeam.toLowerCase()) && !name.includes(selectedTeam.toLowerCase())) return false;
    }
    return true;
  });
  const teamLogoUrl = (nbaTeamId) =>
      nbaTeamId ? `https://cdn.nba.com/logos/nba/${nbaTeamId}/primary/L/logo.svg` : "";

  const NBA_ID_OVERRIDES = {
    UTA: 1610612762,
    NOP: 1610612740,
  };

  const teamLogoByAbbr = (abbr) => {
    if (!abbr) return "";
    if (NBA_ID_OVERRIDES[abbr.toUpperCase()]) {
      const id = NBA_ID_OVERRIDES[abbr.toUpperCase()];
      return `https://cdn.nba.com/logos/nba/${id}/primary/L/logo.svg`;
    }
    return `https://a.espncdn.com/i/teamlogos/nba/500/${abbr.toLowerCase()}.png`;
  };

  // Helper: format date nicely
  const formatGameDate = (dateStr) => {
    if (!dateStr) return "";
    const d = new Date(dateStr);
    return d.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" });
  };

  return (
    <>

      <div style={{ minHeight: "100vh", background: "#0a0c14", fontFamily: "system-ui, -apple-system, sans-serif" }}>

        {/* import navbar with button */}
        <NavBar
          activePage={activePage}
          setActivePage={setActivePage}
          teams={teams}
        />
        {/* Upcoming Games Section */}
        <section className="games-section">
          <h2>Upcoming Games</h2>

          {loadingGames ? (
              <p style={{ color: "#444", fontSize: "0.85rem" }}>Loading games...</p>
          ) : upcomingGames.length === 0 ? (
              <p style={{ color: "#444", fontSize: "0.85rem" }}>No upcoming games found.</p>
          ) : (
              <div className="games-scroll">
                {upcomingGames.map((game, i) => (
                    <div className="game-card" key={game.id ?? i}>
                      <div className="game-date">{formatGameDate(game.date)}</div>
                      <div className="game-matchup">
                        <div className="game-team">
                          <img
                              src={teamLogoByAbbr(game.home_team?.abbreviation)}
                              alt={game.home_team?.abbreviation}
                              onError={(e) => { e.target.style.display = "none"; }}
                          />
                          <span className="game-team-abbr">
                          {game.home_team?.abbreviation ?? game.home_team?.full_name ?? "—"}
                        </span>
                        </div>
                        <span className="game-vs">VS</span>
                        <div className="game-team">
                          <img
                              src={teamLogoByAbbr(game.visitor_team?.abbreviation)}
                              alt={game.visitor_team?.abbreviation}
                              onError={(e) => { e.target.style.display = "none"; }}
                          />
                          <span className="game-team-abbr">
                          {game.visitor_team?.abbreviation ?? game.visitor_team?.full_name ?? "—"}
                        </span>
                        </div>
                      </div>
                      {game.status && (
                          <div className="game-time">{game.status}</div>
                      )}
                    </div>
                ))}
              </div>
          )}
        </section>


        {/* Heading  */}
        <main style={{ padding: "40px 0", textAlign: "left" }}>

          <h1 style={{ fontSize: "1.9rem", fontWeight: 800, color: "#fff", marginBottom: 22,textAlign: "center" }}>
            Select Players
          </h1>

          {/* Player Search Bar */}
          <div style={{
            display: "flex",
            alignItems: "center",
            background: "#111620",
            border: "1.5px solid #1e2333",
            borderRadius: 10,
            padding: "0 16px",
            maxWidth: 480,
            margin: "0 auto 22px auto"
          }}>
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && searchPlayers()}
              placeholder="Search by player name...."
              style={{
                flex: 1, background: "transparent", border: "none", outline: "none",
                color: "#ccc", fontSize: "0.9rem", padding: "13px 0", fontFamily: "inherit",
              }}
            />
            <button onClick={searchPlayers}
              style={{ background: "none", border: "none", cursor: "pointer", color: "#555", display: "flex", alignItems: "center" }}
              onMouseEnter={e => e.currentTarget.style.color = "#4f7cff"}
              onMouseLeave={e => e.currentTarget.style.color = "#555"}
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
            </button>
          </div>

          {/* ── Player Grid ── */}
          {loadingPlayers ? (
            <p style={{ color: "#555" }}>Searching...</p>
          ) : filteredPlayers.length > 0 ? (
            <div className="player-grid">
              {filteredPlayers.map((player) => (
                <PlayerCard
                  key={player.playerId}
                  player={player}
                  selected={selectedPlayer?.playerId === player.playerId}
                  onAnalyze={(p) => setSelectedPlayer(selectedPlayer?.playerId === p.playerId ? null : p)}
                />
              ))}
            </div>
          ) : hasSearched ? (
            <p style={{ color: "#444" }}>No players found.</p>
          ) : null}

          {/* Detail Panel of the player */}
          {selectedPlayer && <AnalyzePanel player={selectedPlayer} />}

        </main>
      </div>
    </>
  );
}