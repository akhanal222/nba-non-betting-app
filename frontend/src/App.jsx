import "./App.css";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import NavBar from "./components/NavBar";
import PlayerCard from "./components/PlayerCard";
import AnalyzePanel from "./components/AnalyzePanel";

const API = {
  test:           "http://localhost:8080/test",
  teams:          "http://localhost:8080/teams",
  playerSearch:   (q) => `http://localhost:8080/api/players/search?q=${encodeURIComponent(q)}`,
  upcomingGames:  "http://localhost:8080/bdl/games/upcoming?days=2",
  completedGames: "http://localhost:8080/bdl/games/completed?days=2",
};

const NBA_ID_OVERRIDES = { UTA: 1610612762, NOP: 1610612740 };

function teamLogoByAbbr(abbr) {
  if (!abbr) return "";
  if (NBA_ID_OVERRIDES[abbr.toUpperCase()])
    return `https://cdn.nba.com/logos/nba/${NBA_ID_OVERRIDES[abbr.toUpperCase()]}/primary/L/logo.svg`;
  return `https://a.espncdn.com/i/teamlogos/nba/500/${abbr.toLowerCase()}.png`;
}

function formatGameDate(dateStr) {
  if (!dateStr) return "";
  const dateOnly = dateStr.substring(0, 10); // YYYY-MM-DD
  const [year, month, day] = dateOnly.split("-");

  const d = new Date(year, month - 1, day);
  return d.toLocaleDateString("en-US", {
    weekday: "short",
    month: "short",
    day: "numeric"
  });
}
function formatGameTime(status) {
  if (!status) return "TBD";

  const gameDate = new Date(status);
  if (Number.isNaN(gameDate.getTime())) return "TBD";

  return new Intl.DateTimeFormat("en-US", {
    hour: "numeric",
    minute: "2-digit",
    timeZone: "America/New_York",
  }).format(gameDate);

}

export default function App() {
  const navigate = useNavigate();

  const [activePage,setActivePage]= useState("Home");
  const [teams,setTeams]= useState([]);
  const [q,setQ]= useState("");
  const [players,setPlayers]= useState([]);
  const [loadingPlayers,setLoadingPlayers] = useState(false);
  const [hasSearched,setHasSearched] = useState(false);
  const [selectedPlayer, setSelectedPlayer] = useState(null);

  const [upcomingGames,setUpcomingGames]= useState([]);
  const [loadingUpcoming,setLoadingUpcoming]= useState(false);

  const [completedGames, setCompletedGames] = useState([]);
  const [loadingCompleted,setLoadingCompleted] = useState(false);

  const [hoveredGame,setHoveredGame]= useState(null);

  useEffect(() => {
    fetch(API.test).catch(() => {});

    fetch(API.teams)
        .then(r => r.json())
        .then(data => setTeams(data.data || data))
        .catch(() => {});

    // Upcoming
    setLoadingUpcoming(true);
    fetch(API.upcomingGames)
        .then(r => r.json())
        .then(data => { setUpcomingGames(Array.isArray(data) ? data : data.data || []); setLoadingUpcoming(false); })
        .catch(() => setLoadingUpcoming(false));

    // Completed
    setLoadingCompleted(true);
    fetch(API.completedGames)
        .then(r => r.json())
        .then(data => { setCompletedGames(Array.isArray(data) ? data : data.data || []); setLoadingCompleted(false); })
        .catch(() => setLoadingCompleted(false));
  }, []);

  const searchPlayers = () => {
    const query = q.trim();
    if (query.length < 2) return;
    setLoadingPlayers(true);
    setHasSearched(true);
    fetch(API.playerSearch(query))
        .then(r => r.json())
        .then(data => { setPlayers(Array.isArray(data) ? data : []); setLoadingPlayers(false); })
        .catch(() => setLoadingPlayers(false));
  };

  const filteredPlayers = players;

  const handleGameClick = (game) => navigate(`/game/${game.id}`, { state: { game } });

  const handleTeamClick = (team) => {
    navigate(`/team/${team.teamId}/players`, { state: { team } });
  };

  return (
      <div style={{ minHeight: "100vh", background: "#0a0c14", fontFamily: "system-ui, -apple-system, sans-serif" }}>

        <NavBar activePage={activePage} setActivePage={setActivePage} teams={teams} onTeamClick={handleTeamClick} />

        {/* ── Upcoming Games ── */}
        <section className="games-section">
          <h2>Upcoming Games</h2>
          {loadingUpcoming ? (
              <p style={{ color: "#444", fontSize: "0.85rem" }}>Loading...</p>
          ) : upcomingGames.length === 0 ? (
              <p style={{ color: "#444", fontSize: "0.85rem" }}>No upcoming games found.</p>
          ) : (
              <div className="games-scroll">
                {upcomingGames.map((game, i) => (
                    <GameCard
                        key={game.id ?? i}
                        game={game}
                        hovered={hoveredGame === (game.id ?? i)}
                        onHover={() => setHoveredGame(game.id ?? i)}
                        onLeave={() => setHoveredGame(null)}
                        onClick={() => handleGameClick(game)}
                        // badge="View Lineup"
                        badgeColor="#4f7cff"
                    />
                ))}
              </div>
          )}
        </section>

        {/* ── Completed Games ── */}
        <section className="games-section">
          <h2 style={{ display: "flex", alignItems: "center", gap: 10 }}>
            Recent Results
            <span style={{
              fontSize: "0.6rem", fontWeight: 700, letterSpacing: "0.12em",
              textTransform: "uppercase", color: "#22c55e",
              background: "#22c55e18", border: "1px solid #22c55e30",
              borderRadius: 4, padding: "2px 7px",
            }}>
            Final
          </span>
          </h2>

          {loadingCompleted ? (
              <p style={{ color: "#444", fontSize: "0.85rem" }}>Loading...</p>
          ) : completedGames.length === 0 ? (
              <p style={{ color: "#444", fontSize: "0.85rem" }}>No recent completed games.</p>
          ) : (
              <div className="games-scroll">
                {completedGames.map((game, i) => (
                    <GameCard
                        key={game.id ?? i}
                        game={game}
                        hovered={hoveredGame === `c-${game.id ?? i}`}
                        onHover={() => setHoveredGame(`c-${game.id ?? i}`)}
                        onLeave={() => setHoveredGame(null)}
                        onClick={() => handleGameClick(game)}
                        // badge="View Stats"
                        badgeColor="#22c55e"
                        showScore
                    />
                ))}
              </div>
          )}
        </section>

        {/* ── Player Search ── */}
        <main style={{ padding: "40px 0", textAlign: "left" }}>
          <h1 style={{ fontSize: "1.9rem", fontWeight: 800, color: "#fff", marginBottom: 22, textAlign: "center"}}>
            Select Players
          </h1>

          <div style={{
            display: "flex", alignItems: "center",
            background: "#111620", border: "1.5px solid #1e2333",
            borderRadius: 10, padding: "0 16px",
            maxWidth: 480, margin: "0 auto 22px auto",
          }}>
            <input
                value={q}
                onChange={e => setQ(e.target.value)}
                onKeyDown={e => e.key === "Enter" && searchPlayers()}
                placeholder="Search by player name...."
                style={{ flex: 1, background: "transparent", border: "none", outline: "none", color: "#ccc", fontSize: "0.9rem", padding: "13px 0", fontFamily: "inherit" }}
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

          {loadingPlayers ? (
              <p style={{ color: "#555", textAlign: "center" }}>Searching...</p>
          ) : filteredPlayers.length > 0 ? (
              <div className="player-grid">
                {filteredPlayers.map(player => (
                    <PlayerCard
                        key={player.playerId}
                        player={player}
                        selected={selectedPlayer?.playerId === player.playerId}
                        onAnalyze={p => setSelectedPlayer(selectedPlayer?.playerId === p.playerId ? null : p)}
                    />
                ))}
              </div>
          ) : hasSearched ? (
              <p style={{ color: "#444", textAlign: "center" }}>No players found.</p>
          ) : null}

          {selectedPlayer && <AnalyzePanel player={selectedPlayer} />}
        </main>
      </div>
  );
}

// ── Reusable game card ────────────────────────────────────────────────────────
function GameCard({ game, hovered, onHover, onLeave, onClick, badge, badgeColor, showScore }) {
  const homeAbbr    = game.home_team?.abbreviation;
  const visitorAbbr = game.visitor_team?.abbreviation;
  const homeScore   = game.home_team_score;
  const visitorScore= game.visitor_team_score;
  const homeWon     = showScore && homeScore != null && visitorScore != null && homeScore > visitorScore;
  const visitorWon  = showScore && homeScore != null && visitorScore != null && visitorScore > homeScore;
  const gameTime = formatGameTime(game.status);
  return (
      <div
          className="game-card"
          onClick={onClick}
          onMouseEnter={onHover}
          onMouseLeave={onLeave}
          style={{
            cursor: "pointer", position: "relative",
            border: `1.5px solid ${hovered ? `${badgeColor}50` : "transparent"}`,
            background: hovered ? "#111620" : undefined,
            transform: hovered ? "translateY(-3px)" : "none",
            transition: "all 0.15s",
          }}
      >
        {/* Hover badge */}
        {hovered && (
            <div style={{
              position: "absolute", top: 8, right: 8,
              fontSize: "0.56rem", fontWeight: 700, letterSpacing: "0.08em",
              textTransform: "uppercase", color: badgeColor,
              background: `${badgeColor}18`, border: `1px solid ${badgeColor}30`,
              borderRadius: 4, padding: "2px 6px",
            }}>
              {badge}
            </div>
        )}

        <div className="game-date">{formatGameDate(game.date)}</div>

        {!showScore && (
            <>
              <div className="game-date">{gameTime}</div>
            </>
        )}


        <div className="game-matchup">
          {/* Home team */}
          <div className="game-team">
            <img src={teamLogoByAbbr(homeAbbr)} alt={homeAbbr} onError={e => { e.target.style.display = "none"; }} />
            <span className="game-team-abbr" style={{ color: homeWon ? "#fff" : undefined, fontWeight: homeWon ? 900 : undefined }}>
            {homeAbbr ?? game.home_team?.full_name ?? "—"}
          </span>
          </div>

          {/* Score or VS */}
          {showScore && homeScore != null && visitorScore != null ? (
              <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 2 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                  <span style={{ fontSize: "1.1rem", fontWeight: 900, color: homeWon ? "#fff" : "#555" }}>{homeScore}</span>
                  <span style={{ fontSize: "0.65rem", color: "#333", fontWeight: 700 }}>—</span>
                  <span style={{ fontSize: "1.1rem", fontWeight: 900, color: visitorWon ? "#fff" : "#555" }}>{visitorScore}</span>
                </div>
                <span style={{ fontSize: "0.56rem", color: "#22c55e", fontWeight: 700, letterSpacing: "0.1em", textTransform: "uppercase" }}>Final</span>
              </div>
          ) : (
              <span className="game-vs">VS</span>
          )}

          {/* Visitor team */}
          <div className="game-team">
            <img src={teamLogoByAbbr(visitorAbbr)} alt={visitorAbbr} onError={e => { e.target.style.display = "none"; }} />
            <span className="game-team-abbr" style={{ color: visitorWon ? "#fff" : undefined, fontWeight: visitorWon ? 900 : undefined }}>
            {visitorAbbr ?? game.visitor_team?.full_name ?? "—"}
          </span>
          </div>
        </div>
      </div>
  );
}