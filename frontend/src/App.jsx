import "./App.css";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import NavBar from "./components/Navbar.jsx";
import PlayerCard from "./components/Playercard.jsx";
import { API, DEMO_MODE } from "./api";

const UI = {
  pageBg: "#0b1020",
  surface: "#121a2e",
  border: "#2c395c",
  textPrimary: "#f4f7ff",
  textSecondary: "#c4cee6",
  textMuted: "#a5b2d1",
  accent: "#5d84ff",
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
  // if (Number.isNaN(gameDate.getTime())) return "Live";
  if (Number.isNaN(gameDate.getTime())) return ""; // Place holder for the Demo presentation for final uncomment the above line and comment this line


  return new Intl.DateTimeFormat("en-US", {
    hour: "numeric",
    minute: "2-digit",
    timeZone: "America/New_York",
  }).format(gameDate);

}

function playerHeadshotUrl(nbaPlayerId) {
  if (!nbaPlayerId) return "";
  return `https://cdn.nba.com/headshots/nba/latest/1040x760/${nbaPlayerId}.png`;
}

function normalizePlayer(player) {
  return {
    ...player,
    playerId: player?.playerId ?? player?.externalApiId ?? null,
    externalApiId: player?.externalApiId ?? player?.playerId ?? null,
    firstName: player?.firstName ?? "",
    lastName: player?.lastName ?? "",
    position: player?.position ?? null,
    jerseyNumber: player?.jerseyNumber ?? null,
    nbaPlayerId: player?.nbaPlayerId ?? null,
    team: player?.team ?? {},
  };
}

const HOME_DEMO_SNAPSHOT_KEY = "nba-homepage-demo-snapshot";

function readHomepageSnapshot() {
  try {
    const raw = localStorage.getItem(HOME_DEMO_SNAPSHOT_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function saveHomepageSnapshot(snapshot) {
  try {
    localStorage.setItem(HOME_DEMO_SNAPSHOT_KEY, JSON.stringify(snapshot));
  } catch {
    // ignore storage failures
  }
}

export default function App() {
  const navigate = useNavigate();

  const [activePage,setActivePage]= useState("Home");
  const [teams,setTeams]= useState([]);
  const [q,setQ]= useState("");
  const [players,setPlayers]= useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loadingPlayers,setLoadingPlayers] = useState(false);
  const [hasSearched,setHasSearched] = useState(false);

  const [upcomingGames,setUpcomingGames]= useState([]);
  const [loadingUpcoming,setLoadingUpcoming]= useState(false);

  const [completedGames, setCompletedGames] = useState([]);
  const [loadingCompleted,setLoadingCompleted] = useState(false);

  const [hoveredGame,setHoveredGame]= useState(null);

  useEffect(() => {
    let cancelled = false;

    fetch(API.test).catch(() => {});

    fetch(API.teams)
        .then(r => r.json())
        .then(data => {
          const filteredTeams = (Array.isArray(data.data || data) ? (data.data || data) : [])
              .filter((team) => {
                const id = Number(team?.externalApiId ?? team?.teamId);
                return Number.isInteger(id) && id >= 1 && id <= 30;
              })
              .sort((a, b) => Number(a?.externalApiId ?? a?.teamId) - Number(b?.externalApiId ?? b?.teamId));

          if (!cancelled) setTeams(filteredTeams);
        })
        .catch(() => {});

    const loadGames = async () => {
      if (DEMO_MODE) {
        const snapshot = readHomepageSnapshot();
        if (snapshot?.upcomingGames && snapshot?.completedGames) {
          if (!cancelled) {
            setUpcomingGames(snapshot.upcomingGames);
            setCompletedGames(snapshot.completedGames);
            setLoadingUpcoming(false);
            setLoadingCompleted(false);
          }
          return;
        }
      }

      if (!cancelled) {
        setLoadingUpcoming(true);
        setLoadingCompleted(true);
      }

      try {
        const [upcomingRes, completedRes] = await Promise.all([
          fetch(API.upcomingGames),
          fetch(API.completedGames),
        ]);

        const [upcomingData, completedData] = await Promise.all([
          upcomingRes.json(),
          completedRes.json(),
        ]);

        const upcoming = Array.isArray(upcomingData) ? upcomingData : upcomingData.data || [];
        const completed = Array.isArray(completedData) ? completedData : completedData.data || [];

        if (!cancelled) {
          setUpcomingGames(upcoming);
          setCompletedGames(completed);
        }

        if (DEMO_MODE) {
          saveHomepageSnapshot({
            upcomingGames: upcoming,
            completedGames: completed,
            savedAt: new Date().toISOString(),
          });
        }
      } catch {
        if (!cancelled) {
          setUpcomingGames([]);
          setCompletedGames([]);
        }
      } finally {
        if (!cancelled) {
          setLoadingUpcoming(false);
          setLoadingCompleted(false);
        }
      }
    };

    loadGames();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const query = q.trim();

    if (query.length < 2) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    const timeoutId = setTimeout(() => {
      fetch(API.playerSearch(query))
          .then(r => r.json())
          .then(data => {
            const matches = Array.isArray(data) ? data.slice(0, 6) : [];
            setSuggestions(matches);
            setShowSuggestions(matches.length > 0);
          })
          .catch(() => {
            setSuggestions([]);
            setShowSuggestions(false);
          });
    }, 250);

    return () => clearTimeout(timeoutId);
  }, [q]);

  const searchPlayers = () => {
    const query = q.trim();
    if (query.length < 2) return;
    setLoadingPlayers(true);
    setHasSearched(true);
    setShowSuggestions(false);
    fetch(API.playerSearch(query))
        .then(r => r.json())
        .then(data => {
          const safePlayers = (Array.isArray(data) ? data : []).map(normalizePlayer);
          setPlayers(safePlayers);
          setLoadingPlayers(false);
        })
        .catch(() => setLoadingPlayers(false));
  };

  const handleSuggestionSelect = (player) => {
    setQ(`${player.firstName} ${player.lastName}`.trim());
    setPlayers([normalizePlayer(player)]);
    setHasSearched(true);
    setShowSuggestions(false);
    setSuggestions([]);
  };

  const filteredPlayers = players;

  const handleGameClick = (game) => navigate(`/game/${game.id}`, { state: { game } });

  const handleTeamClick = (team) => {
    navigate(`/team/${team.teamId}/players`, { state: { team } });
  };

  return (
      <div style={{ minHeight: "100vh", background: UI.pageBg, fontFamily: "system-ui, -apple-system, sans-serif" }}>

        <NavBar activePage={activePage} setActivePage={setActivePage} teams={teams} onTeamClick={handleTeamClick} />

        {/* ── Upcoming Games ── */}
        <section className="games-section">
          <h2>Upcoming Games</h2>
          {loadingUpcoming ? (
              <p style={{ color: UI.textSecondary, fontSize: "0.85rem" }}>Loading...</p>
          ) : upcomingGames.length === 0 ? (
              <p style={{ color: UI.textSecondary, fontSize: "0.85rem" }}>No upcoming games found.</p>
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
              <p style={{ color: UI.textSecondary, fontSize: "0.85rem" }}>Loading...</p>
          ) : completedGames.length === 0 ? (
              <p style={{ color: UI.textSecondary, fontSize: "0.85rem" }}>No recent completed games.</p>
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
          <h1 style={{
            fontFamily: "'Outfit', sans-serif", fontSize: "1.9rem", fontWeight: 800, color: UI.textPrimary, marginBottom: 22, textAlign: "center"}}>
            Search Players
          </h1>

          <div className="player-search-shell">
            <div style={{
              display: "flex", alignItems: "center",
              background: UI.surface, border: `1.5px solid ${UI.border}`,
              borderRadius: 10, padding: "0 16px",
            }}>
              <input
                  value={q}
                  onChange={e => setQ(e.target.value)}
                  onFocus={() => setShowSuggestions(suggestions.length > 0)}
                  onKeyDown={e => e.key === "Enter" && searchPlayers()}
                  placeholder="Search by player name...."
                  style={{ flex: 1, background: "transparent", border: "none", outline: "none", color: UI.textPrimary, fontSize: "0.9rem", padding: "13px 0", fontFamily: "inherit" }}
              />
              <button onClick={searchPlayers}
                      style={{ background: "none", border: "none", cursor: "pointer", color: UI.textMuted, display: "flex", alignItems: "center" }}
                      onMouseEnter={e => e.currentTarget.style.color = UI.accent}
                      onMouseLeave={e => e.currentTarget.style.color = UI.textMuted}
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
              </button>
            </div>

            {showSuggestions && (
                <div className="player-suggestions">
                  {suggestions.map((player) => (
                      <button
                          key={player.playerId}
                          className="player-suggestion-item"
                          onMouseDown={() => handleSuggestionSelect(player)}
                      >
                        <div className="player-suggestion-main">
                          <div className="player-suggestion-avatar">
                            {player.nbaPlayerId ? (
                                <img
                                    src={playerHeadshotUrl(player.nbaPlayerId)}
                                    alt={`${player.firstName} ${player.lastName}`}
                                    onError={(e) => { e.currentTarget.style.display = "none"; }}
                                />
                            ) : null}
                            <span>
                              {player.firstName?.[0] ?? ""}{player.lastName?.[0] ?? ""}
                            </span>
                          </div>
                          <div className="player-suggestion-copy">
                            <span style={{ color: UI.textPrimary, fontWeight: 600 }}>
                              {player.firstName} {player.lastName}
                            </span>
                            <span style={{ color: UI.textSecondary, fontSize: "0.75rem" }}>
                              {player.team?.abbreviation ?? "—"} • {player.position ?? "—"}
                            </span>
                          </div>
                        </div>
                      </button>
                  ))}
                </div>
            )}
          </div>

          {loadingPlayers ? (
              <p style={{ color: UI.textSecondary, textAlign: "center" }}>Searching...</p>
          ) : filteredPlayers.length > 0 ? (
              <div className="player-grid">
                {filteredPlayers.map(player => (
                    <PlayerCard
                        key={player.playerId}
                        player={player}
                        selected={false}
                        onAnalyze={(p) => navigate(`/players/${p.playerId}`, { state: { player: p } })}
                    />
                ))}
              </div>
          ) : hasSearched ? (
              <p style={{ color: UI.textSecondary, textAlign: "center" }}>No players found.</p>
          ) : null}
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
  const isLive = gameTime === "Live";
  return (
      <div
          className="game-card"
          onClick={onClick}
          onMouseEnter={onHover}
          onMouseLeave={onLeave}
          style={{
            cursor: "pointer", position: "relative",
            border: `1.5px solid ${hovered ? `${badgeColor}50` : "transparent"}`,
            background: hovered ? UI.surface : undefined,
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
              <div
                className="game-date"
                style={isLive ? { color: "#22c55e", fontWeight: 700 } : undefined}
              >
                {gameTime}
              </div>
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
                  <span style={{ fontSize: "1.1rem", fontWeight: 900, color: homeWon ? UI.textPrimary : UI.textMuted }}>{homeScore}</span>
                  <span style={{ fontSize: "0.65rem", color: "#333", fontWeight: 700 }}>—</span>
                  <span style={{ fontSize: "1.1rem", fontWeight: 900, color: visitorWon ? UI.textPrimary : UI.textMuted }}>{visitorScore}</span>
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
