import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import NavBar from "../components/Navbar.jsx";
import AnalyzePanel from "../components/Analyzepanel.jsx";

const API_BASE = "http://localhost:8080";
const API = {
    teams: "http://localhost:8080/teams",
};
const LAST_FULL_SEASON = 2025;

export default function TopPlayers() {
    const [players, setPlayers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activePage, setActivePage] = useState("PLAYERS");
    const navigate = useNavigate();
    const [teams, setTeams] = useState([])

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
            } catch (error) {
                console.error("Failed to load top players:", error);
                setPlayers([]);
            } finally {
                setLoading(false);
            }
        }

        loadPlayers();
    }, [teams]);

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
