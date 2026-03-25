import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import NavBar from "../components/Navbar.jsx";
import AnalyzePanel from "../components/Analyzepanel.jsx";

const API_BASE = "http://localhost:8080";
const API = {
    teams: "http://localhost:8080/teams",
    playerSearch: (q) => `http://localhost:8080/api/players/search?q=${encodeURIComponent(q)}`,
};
const LAST_FULL_SEASON = 2025;

export default function TopPlayers() {
    const [players, setPlayers] = useState([]);
    const [topPlayers, setTopPlayers] = useState([]);
    const [suggestions, setSuggestions] = useState([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [loading, setLoading] = useState(true);
    const [activePage, setActivePage] = useState("PLAYERS");
    const navigate = useNavigate();
    const [teams, setTeams] = useState([])
    const [q, setQ] = useState("");

    useEffect(() => {
        fetch(API.teams)
            .then(r => r.json())
            .then(data => setTeams(data.data || data))
            .catch(() => {});
    }, []);

    useEffect(() => {
        if (teams.length === 0) return;

        async function loadPlayers() {
            setLoading(true);
            try {
                const res = await fetch(
                    `${API_BASE}/bdl/leaders/top20?statType=pts&season=${LAST_FULL_SEASON}`
                );

                const data = await res.json();
                const rawPlayers = Array.isArray(data?.data) ? data.data : [];

                const mappedPlayers = rawPlayers.map((entry, index) => {
                    const p = entry?.player ?? {};
                    const matchedTeam =
                        teams.find((team) => team.externalApiId === p.team_id) ?? null;

                    const statistic =
                        entry?.value ??
                        entry?.stat_value ??
                        entry?.amount ??
                        entry?.leader_value ??
                        entry?.pts ??
                        entry?.points ??
                        p?.pts ??
                        p?.points ??
                        "—";

                    return {
                        playerId: p.id ?? index,
                        firstName: p.first_name ?? "",
                        lastName: p.last_name ?? "",
                        position: p.position ?? null,
                        height: p.height ?? null,
                        weight: p.weight ?? null,
                        jerseyNumber: p.jersey_number ?? null,
                        nbaPlayerId: p.nbaPlayerId ?? null,
                        imageUrl: p.imageUrl ?? null,
                        rank: entry?.rank ?? index + 1,
                        statistic,
                        team: {
                            abbreviation: matchedTeam?.abbreviation ?? null,
                            teamName: matchedTeam?.teamName ?? null,
                            city: matchedTeam?.city ?? null,
                            conference: matchedTeam?.conference ?? null,
                            division: matchedTeam?.division ?? null,
                        },
                    };
                });

                const playersWithLast5 = await Promise.all(
                    mappedPlayers.map(async (player) => {
                        try {
                            const statsRes = await fetch(
                                `${API_BASE}/stats/player/external/${player.playerId}?limit=5`
                            );
                            const statsData = await statsRes.json();
                            const games = Array.isArray(statsData) ? statsData : [];

                            const totalPoints = games.reduce(
                                (sum, game) => sum + (game.pointsScored ?? 0),
                                0
                            );

                            const avgLast5 = games.length
                                ? (totalPoints / games.length).toFixed(1)
                                : "- -";

                            return {
                                ...player,
                                avgLast5,
                            };
                        } catch {
                            return {
                                ...player,
                                avgLast5: "—",
                            };
                        }
                    })
                );

                setPlayers(playersWithLast5);
                setTopPlayers(playersWithLast5);
            } catch (error) {
                console.error("Failed to load top players:", error);
                setPlayers([]);
                setTopPlayers([]);
            } finally {
                setLoading(false);
            }
        }

        loadPlayers();
    }, [teams]);

    useEffect(() => {
        const query = q.trim();

        if (query.length < 2) {
            setSuggestions([]);
            setShowSuggestions(false);
            return;
        }

        const timeoutId = setTimeout(() => {
            fetch(API.playerSearch(query))
                .then((r) => r.json())
                .then((data) => {
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

    const searchPlayers = async () => {
        const query = q.trim();

        if (query.length < 2) {
            setPlayers(topPlayers);
            setShowSuggestions(false);
            return;
        }

        setLoading(true);
        setShowSuggestions(false);
        try {
            const res = await fetch(API.playerSearch(query));
            const data = await res.json();
            const mappedPlayers = (Array.isArray(data) ? data : []).map((player) => ({
                playerId: player.playerId,
                firstName: player.firstName ?? "",
                lastName: player.lastName ?? "",
                position: player.position ?? null,
                height: player.height ?? null,
                weight: player.weight ?? null,
                jerseyNumber: player.jerseyNumber ?? null,
                nbaPlayerId: player.nbaPlayerId ?? null,
                imageUrl: player.nbaPlayerId
                    ? `https://cdn.nba.com/headshots/nba/latest/1040x760/${player.nbaPlayerId}.png`
                    : null,
                rank: "—",
                statistic: "—",
                avgLast5: "—",
                team: {
                    abbreviation: player.team?.abbreviation ?? null,
                    teamName: player.team?.teamName ?? null,
                    city: player.team?.city ?? null,
                    conference: player.team?.conference ?? null,
                    division: player.team?.division ?? null,
                },
            }));

            setPlayers(mappedPlayers);
        } catch (error) {
            console.error("Failed to search players:", error);
            setPlayers([]);
        } finally {
            setLoading(false);
        }
    };

    const handleSuggestionSelect = (player) => {
        setQ(`${player.firstName} ${player.lastName}`.trim());
        setPlayers([{
            playerId: player.playerId,
            firstName: player.firstName ?? "",
            lastName: player.lastName ?? "",
            position: player.position ?? null,
            height: player.height ?? null,
            weight: player.weight ?? null,
            jerseyNumber: player.jerseyNumber ?? null,
            nbaPlayerId: player.nbaPlayerId ?? null,
            imageUrl: player.nbaPlayerId
                ? `https://cdn.nba.com/headshots/nba/latest/1040x760/${player.nbaPlayerId}.png`
                : null,
            rank: "—",
            statistic: "—",
            avgLast5: "—",
            team: {
                abbreviation: player.team?.abbreviation ?? null,
                teamName: player.team?.teamName ?? null,
                city: player.team?.city ?? null,
                conference: player.team?.conference ?? null,
                division: player.team?.division ?? null,
            },
        }]);
        setShowSuggestions(false);
        setSuggestions([]);
    };

    return (
        <div style={{ minHeight: "100vh", background: "#0a0c14", color: "#fff" }}>
            <NavBar
                activePage={activePage}
                setActivePage={setActivePage}
                teams={teams}
                onTeamClick={(team) =>
                    navigate(`/team/${team.teamId}/players`, { state: { team } })
                }
            />

            <div style={{ padding: "40px" , alignContent: "center"}}>

                <h1 style={{
                    color: "#fff",
                    fontSize: "36px",
                    fontWeight: "bold",
                    letterSpacing: "1px",
                    textTransform: "uppercase",
                    textAlign : "center",
                    padding: "50px"
                }}
                >Top 20 players Last Season </h1>

                <div className="player-search-shell">
                    <div style={{
                        display: "flex",
                        alignItems: "center",
                        background: "#111620",
                        border: "1.5px solid #1e2333",
                        borderRadius: 10,
                        padding: "0 16px",
                    }}>
                        <input
                            value={q}
                            onChange={e => setQ(e.target.value)}
                            onFocus={() => setShowSuggestions(suggestions.length > 0)}
                            onKeyDown={e => e.key === "Enter" && searchPlayers()}
                            placeholder="Search by player name..."
                            style={{
                                flex: 1,
                                background: "transparent",
                                border: "none",
                                outline: "none",
                                color: "#ccc",
                                fontSize: "0.9rem",
                                padding: "13px 0",
                                fontFamily: "inherit"
                            }}
                        />
                        <button
                            onClick={searchPlayers}
                            style={{ background: "none", border: "none", cursor: "pointer", color: "#555", display: "flex", alignItems: "center" }}
                            onMouseEnter={e => e.currentTarget.style.color = "#4f7cff"}
                            onMouseLeave={e => e.currentTarget.style.color = "#555"}
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
                                                    src={`https://cdn.nba.com/headshots/nba/latest/1040x760/${player.nbaPlayerId}.png`}
                                                    alt={`${player.firstName} ${player.lastName}`}
                                                    onError={(e) => { e.currentTarget.style.display = "none"; }}
                                                />
                                            ) : null}
                                            <span>
                                                {player.firstName?.[0] ?? ""}{player.lastName?.[0] ?? ""}
                                            </span>
                                        </div>
                                        <div className="player-suggestion-copy">
                                            <span style={{ color: "#fff", fontWeight: 600 }}>
                                                {player.firstName} {player.lastName}
                                            </span>
                                            <span style={{ color: "#666", fontSize: "0.75rem" }}>
                                                {player.team?.abbreviation ?? "—"} • {player.position ?? "—"}
                                            </span>
                                        </div>
                                    </div>
                                </button>
                            ))}
                        </div>
                    )}
                </div>


                {loading ? (
                    <p style={{ color: "#888" }}>Loading players...</p>
                ) : players.length === 0 ? (
                    <p style={{ color: "#888" }}>No players found.</p>
                ) : (
                    <div className="player-grid">
                        {players.map((player) => (
                            <LeaderboardCard
                                key={player.playerId}
                                player={player}
                                onClick={(p) =>
                                    navigate(`/players/${p.playerId}`, { state: { player: p } })
                                }
                            />
                        ))}
                    </div>
                )}

            </div>
        </div>
    );
}

function LeaderboardCard({ player, onClick, selected }) {
    return (
        <div
            style={{
                background: selected ? "#1a1f2e" : "#131720",
                border: `1.5px solid ${selected ? "#4f7cff" : "#1e2333"}`,
                borderRadius: 14,
                padding: 28,
                display: "flex",
                flexDirection: "column",
                gap: 12,
                boxShadow: selected ? "0 0 18px #4f7cff44" : "none",
            }}
        >
            <div
                style={{
                    color: "#4f7cff",
                    fontSize: "0.75rem",
                    fontWeight: 800,
                    letterSpacing: "0.08em",
                    textTransform: "uppercase",
                }}
            >
                Rank #{player.rank}
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                {player.nbaPlayerId ? (
                    <img
                        src={player.imageUrl}
                        alt={`${player.firstName} ${player.lastName}`}
                        style={{
                            width: 90,
                            height: 90,
                            borderRadius: "50%",
                            objectFit: "cover",
                            border: "2px solid #4f7cff",
                            flexShrink: 0,
                        }}
                        onError={(e) => {
                            e.currentTarget.style.display = "none";
                        }}
                    />
                ) : (
                    <div
                        style={{
                            width: 90,
                            height: 90,
                            borderRadius: "50%",
                            background: "#2a3be0",
                            flexShrink: 0,
                        }}
                    />
                )}

                <div>
                    <div style={{ color: "#fff", fontWeight: 700, fontSize: "1.1rem" }}>
                        {player.firstName} {player.lastName}
                    </div>
                    <div style={{ color: "#777", fontSize: "0.8rem" }}>
                        {player.team?.abbreviation ?? "—"}  {player.team?.teamName ?? "—"}
                    </div>
                </div>
            </div>

            <div
                style={{
                    display: "flex",
                    justifyContent: "center",
                    gap: 28,
                    marginTop: 10,
                }}
            >
                <Stat value={player.statistic} label="Statistic" />
                <Stat value={player.avgLast5} label="Last 5 Game Average" />
                <Stat value={player.rank} label="Ranking last season" />
            </div>

            <button
                onClick={() => onClick(player)}
                style={{
                    background: selected ? "#2a3be0" : "#1a1f2e",
                    border: selected ? "none" : "1px solid #2a2f44",
                    color: selected ? "#fff" : "#888",
                    borderRadius: 8,
                    padding: "12px 0",
                    fontSize: "1rem",
                    letterSpacing: "0.08em",
                    fontWeight: 600,
                    cursor: "pointer",
                    width: "100%",
                    marginTop: 10,
                }}
            >
                Detail
            </button>
        </div>
    );
}

function Stat({ value, label }) {
    return (
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
      <span style={{ color: "#4f7cff", fontWeight: 700, fontSize: "1.2rem" }}>
        {value ?? "—"}
      </span>
            <span style={{ color: "#666", fontSize: "0.72rem", letterSpacing: "0.08em" }}>
        {label}
      </span>
        </div>
    );
}
