import "./App.css";
import { useEffect, useState } from "react";
import NavBar from "./components/NavBar";
import PlayerCard from "./components/PlayerCard";
import AnalyzePanel from "./components/AnalyzePanel";

// All the Endpoint(Testing backend, get all the teams, and search player)
const API = {
  test: "http://localhost:8080/test",
  teams: "http://localhost:8080/bdl/teams",
  playerSearch: (q) => `http://localhost:8080/api/players/search?q=${encodeURIComponent(q)}`,
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

  // Checking if backend is running or not
  useEffect(() => {
    fetch(API.test).catch(() => {});
    fetch(API.teams)
      .then((res) => res.json())
      .then((data) => setTeams(data.data || data))
      .catch(() => {});
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

  return (
    <>
      <style>{`
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { background: #0a0c14; font-family: system-ui, -apple-system, sans-serif; }

        .player-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
          gap: 24px;
        }
        .pill {
          padding: 7px 18px; border-radius: 999px;
          border: 1.5px solid #2a2f44; background: transparent; color: #888;
          font-size: 0.85rem; font-weight: 600; cursor: pointer;
          transition: all 0.15s; letter-spacing: 0.04em; white-space: nowrap;
          font-family: inherit;
        }
        .pill:hover  { border-color: #4f7cff; color: #fff; }
        .pill.active { background: #2a3be0; border-color: #2a3be0; color: #fff; }
        .nav-btn {
          background: none; border: none; cursor: pointer;
          font-size: 0.85rem; font-weight: 700; letter-spacing: 0.12em;
          color: #555; transition: color 0.15s; padding: 4px 0; font-family: inherit;
        }
        .nav-btn:hover  { color: #fff; }
        .nav-btn.active { color: #4f7cff; }
        .tab-btn {
          padding: 7px 22px; border-radius: 8px; border: none; cursor: pointer;
          font-size: 0.9rem; font-weight: 700; letter-spacing: 0.05em;
          transition: all 0.15s; font-family: inherit;
        }
        .tab-btn.active   { background: #2a3be0; color: #fff; }
        .tab-btn.inactive { background: #1a1f2e; color: #666; }
      `}</style>

      <div style={{ minHeight: "100vh", background: "#0a0c14", fontFamily: "system-ui, -apple-system, sans-serif" }}>

        {/* import navbar with button */}
        <NavBar
          activePage={activePage}
          setActivePage={setActivePage}
          teams={teams}
        />

        {/* Heading  */}
        <main style={{ padding: "40px 0", textAlign: "left" }}>

          <h1 style={{ fontSize: "1.9rem", fontWeight: 800, color: "#fff", marginBottom: 22 }}>
            Select Players
          </h1>

          {/* Player Search Bar */}
          <div style={{
            display: "flex", alignItems: "center",
            background: "#111620", border: "1.5px solid #1e2333",
            borderRadius: 10, padding: "0 16px",
            maxWidth: 480, marginBottom: 22,
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