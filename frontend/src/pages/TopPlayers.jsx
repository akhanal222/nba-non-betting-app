import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import NavBar from "../components/Navbar.jsx";
import PlayerCard from "../components/Playercard.jsx";
import { API } from "../api";
const LAST_FULL_SEASON = 2025;
const STAT_OPTIONS = [
    { key: "pts", label: "PTS" },
    { key: "reb", label: "REB" },
    { key: "ast", label: "AST" },
];

const UI = {
    pageBg: "#0b1020",
    surface: "#121a2e",
    surfaceAlt: "#0f1629",
    border: "#2c395c",
    textPrimary: "#f4f7ff",
    textSecondary: "#c4cee6",
    textMuted: "#a5b2d1",
    accent: "#5d84ff",
};

export default function TopPlayers() {
    const [players, setPlayers] = useState([]);
    const [topPlayers, setTopPlayers] = useState([]);
    const [suggestions, setSuggestions] = useState([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [loading, setLoading] = useState(true);
    const [activePage, setActivePage] = useState("PLAYERS");
    const navigate = useNavigate();
    const [teams, setTeams] = useState([]);
    const [q, setQ] = useState("");
    const [statType, setStatType] = useState("pts");
    const [showSearchCards, setShowSearchCards] = useState(false);

    const resolveTeamByName = (teamName) => {
        if (!teamName) return null;

        return (
            teams.find(
                (team) => team.teamName?.toLowerCase() === String(teamName).toLowerCase()
            ) ?? null
        );
    };

    const hydratePlayerDetails = async (basePlayer) => {
        const fullName = `${basePlayer.firstName ?? ""} ${basePlayer.lastName ?? ""}`.trim();
        if (fullName.length < 2) return basePlayer;

        try {
            const res = await fetch(API.playerSearch(fullName));
            if (!res.ok) return basePlayer;

            const candidates = await res.json();
            if (!Array.isArray(candidates) || candidates.length === 0) return basePlayer;

            const matched = candidates.find(
                (p) => p?.externalApiId && p.externalApiId === basePlayer.externalApiId
            ) ?? candidates[0];

            const nbaPlayerId = matched?.nbaPlayerId ?? basePlayer.nbaPlayerId ?? null;

            return {
                ...basePlayer,
                isActive: matched?.isActive ?? matched?.is_active ?? basePlayer.isActive ?? null,
                position: basePlayer.position ?? matched?.position ?? null,
                height: matched?.height ?? null,
                weight: matched?.weight ?? null,
                jerseyNumber: matched?.jerseyNumber ?? null,
                nbaPlayerId,
                imageUrl: nbaPlayerId
                    ? `https://cdn.nba.com/headshots/nba/latest/1040x760/${nbaPlayerId}.png`
                    : null,
                team: {
                    abbreviation: basePlayer.team?.abbreviation ?? matched?.team?.abbreviation ?? null,
                    teamName: basePlayer.team?.teamName ?? matched?.team?.teamName ?? null,
                    city: basePlayer.team?.city ?? matched?.team?.city ?? null,
                    conference: basePlayer.team?.conference ?? matched?.team?.conference ?? null,
                    division: basePlayer.team?.division ?? matched?.team?.division ?? null,
                    nbaTeamId: basePlayer.team?.nbaTeamId ?? matched?.team?.nbaTeamId ?? null,
                },
            };
        } catch {
            return basePlayer;
        }
    };

    useEffect(() => {
        fetch(API.teams)
            .then(r => r.json())
            .then(data => setTeams(data.data || data))
            .catch(() => {});
    }, []);

    useEffect(() => {
        let cancelled = false;

        async function loadPlayers() {
            setLoading(true);
            try {
                const res = await fetch(API.leaderboard(statType, LAST_FULL_SEASON));
                if (!res.ok) {
                    throw new Error(`Leaderboard request failed with status ${res.status}`);
                }

                const data = await res.json();
                const rows = Array.isArray(data) ? data : [];

                const mappedPlayers = rows.map((entry, index) => {
                    const matchedTeam = resolveTeamByName(entry?.teamName);

                    return {
                        playerId: entry?.playerId ?? index,
                        externalApiId: entry?.externalApiId ?? null,
                        firstName: entry?.firstName ?? "",
                        lastName: entry?.lastName ?? "",
                        position: entry?.position ?? null,
                        height: null,
                        weight: null,
                        jerseyNumber: null,
                        nbaPlayerId: null,
                        isActive: true,
                        imageUrl: null,
                        rank: entry?.rank ?? index + 1,
                        statistic: entry?.seasonAvg ?? "—",
                        avgLast5: entry?.last5Avg ?? "—",
                        gamesPlayed: entry?.gamesPlayed ?? "—",
                        team: {
                            abbreviation: matchedTeam?.abbreviation ?? null,
                            teamName: entry?.teamName ?? matchedTeam?.teamName ?? null,
                            city: matchedTeam?.city ?? null,
                            conference: matchedTeam?.conference ?? null,
                            division: matchedTeam?.division ?? null,
                            nbaTeamId: matchedTeam?.nbaTeamId ?? null,
                        },
                    };
                });

                const enrichedPlayers = await Promise.all(
                    mappedPlayers.map((player) => hydratePlayerDetails(player))
                );

                if (!cancelled) {
                    setPlayers(enrichedPlayers);
                    setTopPlayers(enrichedPlayers);
                    setShowSearchCards(false);
                    setLoading(false);
                }
            } catch (error) {
                console.error("Failed to load top players:", error);
                if (!cancelled) {
                    setPlayers([]);
                    setTopPlayers([]);
                    setLoading(false);
                }
            }
        }

        loadPlayers();
        return () => {
            cancelled = true;
        };
    }, [teams, statType]);

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
            setShowSearchCards(false);
            return;
        }

        setLoading(true);
        setShowSuggestions(false);
        try {
            const res = await fetch(API.playerSearch(query));
            const data = await res.json();
            const mappedPlayers = (Array.isArray(data) ? data : []).map((player) => ({
                playerId: player.playerId,
                externalApiId: player.externalApiId ?? player.playerId ?? null,
                firstName: player.firstName ?? "",
                lastName: player.lastName ?? "",
                isActive: true,
                position: player.position ?? null,
                height: player.height ?? null,
                weight: player.weight ?? null,
                jerseyNumber: player.jerseyNumber ?? null,
                nbaPlayerId: player.nbaPlayerId ?? null,
                imageUrl: player.nbaPlayerId
                    ? `https://cdn.nba.com/headshots/nba/latest/1040x760/${player.nbaPlayerId}.png`
                    : null,
                rank: "_",
                statistic: "—",
                avgLast5: "—",
                team: {
                    abbreviation: player.team?.abbreviation ?? null,
                    teamName: player.team?.teamName ?? null,
                    city: player.team?.city ?? null,
                    conference: player.team?.conference ?? null,
                    division: player.team?.division ?? null,
                    nbaTeamId: player.team?.nbaTeamId ?? null,
                },
            }));

            setPlayers(mappedPlayers);
            setShowSearchCards(true);
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
            externalApiId: player.externalApiId ?? player.playerId ?? null,
            firstName: player.firstName ?? "",
            lastName: player.lastName ?? "",
            isActive: true,
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
                nbaTeamId: player.team?.nbaTeamId ?? null,
            },
        }]);
        setShowSuggestions(false);
        setSuggestions([]);
        setShowSearchCards(true);
    };

    return (
        <div style={{ minHeight: "100vh", background: UI.pageBg, color: UI.textPrimary }}>
            <NavBar
                activePage={activePage}
                setActivePage={setActivePage}
                teams={teams}
                onTeamClick={(team) =>
                    navigate(`/team/${team.teamId}/players`, { state: { team } })
                }
            />

            <div style={{ padding: "40px", alignContent: "center" }}>

                <h1 style={{
                    color: "#a5b2d1",
                    fontSize: "30px",
                    letterSpacing: "1px",
                    fontweight: "600",
                    textTransform: "uppercase",
                    textAlign: "center",
                    padding: "50px",
                    fontFamily: "'Outfit', sans-serif"
                }}
                >Top Players Last Season </h1>

                <div
                    style={{
                        display: "flex",
                        gap: 12,
                        justifyContent: "center",
                        marginBottom: 24,
                    }}
                >
                    {STAT_OPTIONS.map((stat) => (
                        <button
                            key={stat.key}
                            onClick={() => setStatType(stat.key)}
                            style={{
                                padding: "10px 18px",
                                borderRadius: 10,
                                border: `1px solid ${UI.border}`,
                                background: statType === stat.key ? UI.accent : UI.surface,
                                color: UI.textPrimary,
                                fontWeight: 700,
                                cursor: "pointer",
                                transition: "all 0.2s",
                            }}
                        >
                            {stat.label}
                        </button>
                    ))}
                </div>

                <div className="player-search-shell">
                    <div style={{
                        display: "flex",
                        alignItems: "center",
                        background: UI.surfaceAlt,
                        border: `1.5px solid ${UI.border}`,
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
                                color: UI.textPrimary,
                                fontSize: "0.9rem",
                                padding: "13px 0",
                                fontFamily: "inherit"
                            }}
                        />
                        <button
                            onClick={searchPlayers}
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


                {loading ? (
                    <p style={{ color: UI.textSecondary }}>Loading players...</p>
                ) : players.length === 0 ? (
                    <p style={{ color: UI.textSecondary }}>No players found.</p>
                ) : (
                    <div className="player-grid">
                        {players.map((player) => (
                            showSearchCards ? (
                                <PlayerCard
                                    key={player.playerId}
                                    player={player}
                                    selected={false}
                                    onAnalyze={(p) => navigate(`/players/${p.playerId}`, { state: { player: p } })}
                                />
                            ) : (
                                <LeaderboardCard
                                    key={player.playerId}
                                    player={player}
                                    onClick={(p) =>
                                        navigate(`/players/${p.playerId}`, { state: { player: p } })
                                    }
                                />
                            )
                        ))}
                    </div>
                )}

            </div>
        </div>
    );
}

function LeaderboardCard({ player, onClick, selected }) {
    const [imgError, setImgError] = useState(false);
    const initials = `${player.firstName?.[0] ?? ""}${player.lastName?.[0] ?? ""}`.toUpperCase();

    useEffect(() => {
        setImgError(false);
    }, [player?.nbaPlayerId]);

    return (
        <div
            style={{
                background: selected ? "#1a2745" : UI.surface,
                border: `1.5px solid ${selected ? UI.accent : UI.border}`,
                borderRadius: 14,
                padding: 28,
                display: "flex",
                flexDirection: "column",
                gap: 12,
                height: "100%",
                boxShadow: selected ? "0 0 18px #4f7cff44" : "none",
            }}
        >
            <div
                style={{
                    color: "#9eb8ff",
                    fontSize: "0.75rem",
                    fontWeight: 800,
                    letterSpacing: "0.08em",
                    textTransform: "uppercase",
                }}
            >
                Rank #{player.rank}
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                {player.nbaPlayerId && player.imageUrl && !imgError ? (
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
                        onError={() => {
                            setImgError(true);
                        }}
                    />
                ) : (
                    <div
                        style={{
                            width: 90,
                            height: 90,
                            borderRadius: "50%",
                            background: "#2a3be0",
                            color: "#fff",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontWeight: 700,
                            fontSize: "1rem",
                            flexShrink: 0,
                        }}
                    >
                        {initials || "--"}
                    </div>
                )}

                <div style={{ minWidth: 0 }}>
                    <div style={{ color: UI.textPrimary, fontWeight: 700, fontSize: "1.1rem" }}>
                        {player.firstName} {player.lastName}
                    </div>
                    <div style={{ color: UI.textSecondary, fontSize: "0.8rem" }}>
                        {player.team?.abbreviation ?? "—"}  {player.team?.teamName ?? "—"}
                    </div>
                </div>
            </div>

            <div
                style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
                    alignItems: "start",
                    gap: 18,
                    marginTop: 10,
                }}
            >
                <Stat value={player.statistic} label="Last season AVG" />
                <Stat value={player.avgLast5} label="5 Game AVG" />
                <Stat value={player.gamesPlayed} label="Game Played This Season" />
            </div>

            <button
                onClick={() => onClick(player)}
                style={{
                    background: selected ? UI.accent : "#1b2644",
                    border: selected ? "none" : `1px solid ${UI.border}`,
                    color: UI.textPrimary,
                    borderRadius: 8,
                    padding: "12px 0",
                    fontSize: "1rem",
                    letterSpacing: "0.08em",
                    fontWeight: 600,
                    cursor: "pointer",
                    width: "100%",
                    marginTop: "auto",
                }}
            >
                Detail
            </button>
        </div>
    );
}

function Stat({ value, label }) {
    const isGamesPlayed = label === "Game Played This Season";
    const numericValue = Number(value);
    const highlightGold = isGamesPlayed && Number.isFinite(numericValue) && numericValue >= 65;

    return (
        <div
            style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "flex-start",
                minWidth: 0,
                textAlign: "center",
            }}
        >
      <span style={{ color: highlightGold ? "#d4af37" : "#9eb8ff", fontWeight: 700, fontSize: "1.2rem", lineHeight: 1.15 }}>
        {value ?? "—"}
      </span>
            <span
                style={{
                    color: UI.textMuted,
                    fontSize: "0.72rem",
                    letterSpacing: "0.08em",
                    lineHeight: 1.25,
                    marginTop: 6,
                }}
            >
        {label}
      </span>
        </div>
    );
}
