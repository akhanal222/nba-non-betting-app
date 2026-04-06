import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bar } from "react-chartjs-2";
import {
    BarElement,
    CategoryScale,
    Chart as ChartJS,
    Legend,
    LinearScale,
    Tooltip,
} from "chart.js";
import NavBar from "../components/Navbar.jsx";
import "../matchup.css";
import "../prediction.css";

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend);

const API = {
    teams: "http://localhost:8080/teams",
    playerSearch: (q) =>
        `http://localhost:8080/api/players/search?q=${encodeURIComponent(q)}`,
    playerStats: (apiId) =>
        `http://localhost:8080/stats/player/external/${apiId}`,
    predict: (playerApiId, opponentTeamApiId, statType, line) =>
        `http://localhost:8080/api/props/predict?playerApiId=${playerApiId}&opponentTeamApiId=${opponentTeamApiId}&statType=${statType}&line=${line}`,
};

const STAT_TYPES = [
    { value: "pts", label: "Points" },
    { value: "reb", label: "Rebounds" },
    { value: "ast", label: "Assists" },
    { value: "stl", label: "Steals" },
    { value: "blk", label: "Blocks" },
    { value: "turnover", label: "Turnovers" },
    { value: "fg3m", label: "3PT Made" },
    { value: "pr",       label: "Pts + Reb (P+R)" },
    { value: "pa",       label: "Pts + Ast (P+A)" },
    { value: "ra",       label: "Reb + Ast (R+A)" },
    { value: "pra",      label: "Pts + Reb + Ast (PRA)" },
];

const DEFAULT_LINES = {
    pts: "22.5",
    reb: "6.5",
    ast: "5.5",
    stl: "1.5",
    blk: "1.5",
    turnover: "2.5",
    fg3m: "2.5",
    pr:  "32.5",
    pa:  "30.5",
    ra:  "12.5",
    pra: "38.5",
};

const STAT_VALUE_KEYS = {
    pts: "pointsScored",
    reb: "totalRebounds",
    ast: "assists",
    stl: "steals",
    blk: "blocks",
    turnover: "turnovers",
    fg3m: "threePointShotsMade",
    pr:  ["pointsScored", "totalRebounds"],
    pa:  ["pointsScored", "assists"],
    ra:  ["totalRebounds", "assists"],
    pra: ["pointsScored", "totalRebounds", "assists"],
};

function playerHeadshot(id) {
    return id
        ? `https://cdn.nba.com/headshots/nba/latest/1040x760/${id}.png`
        : null;
}

function teamLogo(nbaTeamId) {
    return nbaTeamId
        ? `https://cdn.nba.com/logos/nba/${nbaTeamId}/primary/L/logo.svg`
        : "";
}

function normalizeTeamsResponse(payload) {
    const rawTeams = Array.isArray(payload?.data)
        ? payload.data
        : Array.isArray(payload)
          ? payload
          : [];

    return rawTeams
        .filter((team) => {
            const id = Number(team?.externalApiId ?? team?.teamId);
            return Number.isInteger(id) && id >= 1 && id <= 30;
        })
        .sort(
            (a, b) =>
                Number(a?.externalApiId ?? a?.teamId) -
                Number(b?.externalApiId ?? b?.teamId)
        );
}

function normalizePlayerSearchResponse(payload) {
    const rawPlayers = Array.isArray(payload?.data)
        ? payload.data
        : Array.isArray(payload)
          ? payload
          : [];

    return rawPlayers.slice(0, 6);
}

function getGameDate(game) {
    const rawDate = (
        game?.gameDate ??
        game?.game_date ??
        game?.date ??
        game?.game?.gameDate ??
        game?.game?.game_date ??
        game?.game?.date ??
        ""
    );

    if (!rawDate) return "";

    const normalized = rawDate.includes("T") ? rawDate : `${rawDate}T00:00:00`;
    const parsed = new Date(normalized);

    if (Number.isNaN(parsed.getTime())) return rawDate;

    return parsed
        .toLocaleDateString("en-US", {
            month: "short",
            day: "numeric",
        })
        .toUpperCase();
}

function getRecentGameTeams(game) {
    const matchupGame = game?.game ?? null;
    const playerTeam = game?.team ?? null;
    const homeTeam = matchupGame?.homeTeam ?? matchupGame?.home_team ?? null;
    const awayTeam = matchupGame?.awayTeam ?? matchupGame?.away_team ?? null;
    const playerTeamId = Number(playerTeam?.teamId ?? playerTeam?.id);
    const homeTeamId = Number(homeTeam?.teamId ?? homeTeam?.id);
    const awayTeamId = Number(awayTeam?.teamId ?? awayTeam?.id);

    if (Number.isFinite(playerTeamId) && playerTeamId === homeTeamId) {
        return { playerTeam: homeTeam, opponentTeam: awayTeam };
    }

    if (Number.isFinite(playerTeamId) && playerTeamId === awayTeamId) {
        return { playerTeam: awayTeam, opponentTeam: homeTeam };
    }

    return { playerTeam: playerTeam ?? homeTeam, opponentTeam: awayTeam ?? homeTeam };
}

function PredictionRecentGameRow({ game, statType, line, index }) {
    const keys = STAT_VALUE_KEYS[statType] ?? [];
    const rawValue = keys.length > 0
        ? keys.reduce((sum, k) => sum + (game?.[k] ?? 0), 0)
        : undefined;
    const value = rawValue ?? "—";
    const numericValue = parseFloat(rawValue);
    const lineNumber = parseFloat(line);
    const hit =
        !Number.isNaN(numericValue) &&
        !Number.isNaN(lineNumber) &&
        numericValue > lineNumber;
    const { playerTeam, opponentTeam } = getRecentGameTeams(game);

    return (
        <div className="flex items-center gap-10 px-10 py-8 bg-[#0a0e1c] border border-[#1a2540] hover:border-[#253660] transition-colors">
            <span className="text-[white] text-xs font-mono w-5 text-right">
                {index + 1}
            </span>

            <span className="text-[white] text-sm font-mono w-20">
                {getGameDate(game) || "—"}
            </span>

            <div className="flex items-center gap-2 w-24">
                {playerTeam?.nbaTeamId && (
                    <img
                        src={teamLogo(playerTeam.nbaTeamId)}
                        alt=""
                        className="w-7 h-7 object-contain"
                        onError={(e) => {
                            e.currentTarget.style.display = "none";
                        }}
                    />
                )}
                <span className="text-[white] text-xs font-semibold uppercase tracking-wide">
                    {playerTeam?.abbreviation ?? playerTeam?.teamName ?? ""}
                </span>
                <span className="text-[white] text-xs">vs</span>
                {opponentTeam?.nbaTeamId && (
                    <img
                        src={teamLogo(opponentTeam.nbaTeamId)}
                        alt=""
                        className="w-7 h-7 object-contain"
                        onError={(e) => {
                            e.currentTarget.style.display = "none";
                        }}
                    />
                )}
                <span className="text-[white] text-xs font-semibold uppercase tracking-wide">
                    {opponentTeam?.abbreviation ?? opponentTeam?.teamName ?? ""}
                </span>
            </div>

            <div className="flex gap-5 flex-1 justify-end">
                <div className="text-center min-w-[50px]">
                    <p className="text-white text-sm font-semibold">
                        {game.minutesPlayed ?? game.min ?? game.minutes ?? "—"}
                    </p>
                    <p className="text-[#ffffff] text-[10px] uppercase">MIN</p>
                </div>

                <div className="text-center min-w-[50px]">
                    <p
                        className={`text-sm font-semibold ${!Number.isNaN(numericValue) ? (hit ? "text-green-500" : "text-red-500") : "text-white"}`}
                    >
                        {value}
                    </p>
                    <p className="text-[#ffffff] text-[10px] uppercase">
                        {statType}
                    </p>
                </div>
            </div>
        </div>
    );
}

function buildDistributionChartData(prediction) {
    const proj = prediction.projectedValue;
    const std = prediction.stdDev || 1;
    const line = prediction.line;
    const min = Math.max(0, Math.floor(proj - 3 * std));
    const max = Math.ceil(proj + 3 * std);
    const step = Math.max(1, Math.round((max - min) / 20));
    const labels = [];
    const values = [];
    const colors = [];

    for (let x = min; x <= max; x += step) {
        labels.push(x.toString());
        const z = (x - proj) / std;
        const pdf = Math.exp(-0.5 * z * z) / (std * Math.sqrt(2 * Math.PI));
        values.push(parseFloat((pdf * 100).toFixed(2)));
        colors.push(x >= line ? "#47e897" : "#e86347");
    }

    return {
        labels,
        datasets: [
            {
                label: "Probability Density",
                data: values,
                backgroundColor: colors,
                borderRadius: 4,
                maxBarThickness: 24,
            },
        ],
    };
}

const distributionChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
        legend: { display: false },
        tooltip: {
            callbacks: {
                label: (ctx) => `${ctx.parsed.y.toFixed(2)}%`,
            },
        },
    },
    scales: {
        x: {
            grid: { color: "rgba(26,37,64,0.45)" },
            ticks: { color: "#667594", font: { size: 10 } },
            title: {
                display: true,
                text: "Stat Value",
                color: "#667594",
                font: { size: 11 },
            },
        },
        y: {
            beginAtZero: true,
            grid: { color: "rgba(26,37,64,0.45)" },
            ticks: { display: false },
        },
    },
};

function PropPlayerSearch({ player, onSelect, onClear }) {
    const [q, setQ] = useState("");
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [open, setOpen] = useState(false);
    const ref = useRef(null);
    const debounceRef = useRef(null);

    useEffect(() => {
        const handler = (e) => {
            if (ref.current && !ref.current.contains(e.target)) setOpen(false);
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    const search = (val) => {
        setQ(val);
        if (debounceRef.current) clearTimeout(debounceRef.current);
        if (val.trim().length < 2) {
            setResults([]);
            setOpen(false);
            return;
        }

        debounceRef.current = setTimeout(async () => {
            setLoading(true);
            try {
                const res = await fetch(API.playerSearch(val.trim()));
                const data = await res.json();
                setResults(normalizePlayerSearchResponse(data));
                setOpen(true);
            } catch {
                setResults([]);
            } finally {
                setLoading(false);
            }
        }, 300);
    };

    const pick = (selectedPlayer) => {
        onSelect(selectedPlayer);
        setQ("");
        setResults([]);
        setOpen(false);
    };

    return (
        <div ref={ref} className="search-wrapper">
            <label className="search-label search-label--player">Player</label>
            <div className="search-field-wrap">
                {player ? (
                    <div className="search-selected">
                        <div className="search-avatar">
                            {!player.nbaPlayerId && (
                                <span className="search-avatar__initials">
                                    {player.firstName?.[0]}
                                    {player.lastName?.[0]}
                                </span>
                            )}
                            {player.nbaPlayerId && (
                                <img
                                    src={playerHeadshot(player.nbaPlayerId)}
                                    alt=""
                                    className="search-avatar__img"
                                    onError={(e) => {
                                        e.currentTarget.style.display = "none";
                                    }}
                                />
                            )}
                        </div>
                        <div className="search-info">
                            <p className="search-info__name">
                                {player.firstName} {player.lastName}
                            </p>
                            <p className="search-info__meta">
                                {player.team?.abbreviation ??
                                    player.team?.teamName ??
                                    "Team unavailable"}{" "}
                                · {player.position ?? "Position unavailable"}
                            </p>
                        </div>
                        <button type="button" onClick={onClear} className="search-clear-btn">
                            X
                        </button>
                    </div>
                ) : (
                    <div
                        className={`search-input-bar ${open ? "search-input-bar--open" : ""}`}
                    >
                        {loading ? (
                            <div className="search-spinner" />
                        ) : (
                            <svg
                                className="search-icon"
                                width="16"
                                height="16"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2.5"
                            >
                                <circle cx="11" cy="11" r="8" />
                                <line x1="21" y1="21" x2="16.65" y2="16.65" />
                            </svg>
                        )}
                        <input
                            value={q}
                            onChange={(e) => search(e.target.value)}
                            placeholder="Search player name..."
                        />
                    </div>
                )}

                {open && results.length > 0 && (
                    <div className="search-dropdown">
                        {results.map((result) => (
                            <button
                                type="button"
                                key={result.externalApiId ?? result.playerId}
                                onClick={() => pick(result)}
                                className="search-dropdown__item"
                            >
                                <div className="search-avatar search-avatar--sm">
                                    {!result.nbaPlayerId && (
                                        <span className="search-avatar__initials">
                                            {result.firstName?.[0]}
                                            {result.lastName?.[0]}
                                        </span>
                                    )}
                                    {result.nbaPlayerId && (
                                        <img
                                            src={playerHeadshot(result.nbaPlayerId)}
                                            alt=""
                                            className="search-avatar__img"
                                            onError={(e) => {
                                                e.currentTarget.style.display = "none";
                                            }}
                                        />
                                    )}
                                </div>
                                <div className="search-info">
                                    <p className="search-dropdown__item-name">
                                        {result.firstName} {result.lastName}
                                    </p>
                                    <p className="search-dropdown__item-meta">
                                        {result.team?.abbreviation ?? "Team unavailable"} ·{" "}
                                        {result.position ?? "Position unavailable"}
                                    </p>
                                </div>
                            </button>
                        ))}
                    </div>
                )}

                {open && results.length === 0 && !loading && q.length >= 2 && (
                    <div className="search-dropdown">
                        <p className="search-dropdown__empty">No players found</p>
                    </div>
                )}
            </div>
        </div>
    );
}

function TeamSearch({ teams, team, onSelect, onClear, disabledAbbr }) {
    const [q, setQ] = useState("");
    const [open, setOpen] = useState(false);
    const ref = useRef(null);

    useEffect(() => {
        const handler = (e) => {
            if (ref.current && !ref.current.contains(e.target)) setOpen(false);
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    const filtered = teams.filter((candidate) => {
        const search = q.toLowerCase();
        const isDisabled =
            disabledAbbr &&
            (candidate.abbreviation ?? "").toLowerCase() ===
                disabledAbbr.toLowerCase();

        if (isDisabled) return false;

        return (
            (candidate.full_name ?? candidate.teamName ?? "")
                .toLowerCase()
                .includes(search) ||
            (candidate.abbreviation ?? "").toLowerCase().includes(search) ||
            (candidate.city ?? "").toLowerCase().includes(search)
        );
    });

    const pick = (selectedTeam) => {
        onSelect(selectedTeam);
        setQ("");
        setOpen(false);
    };

    return (
        <div ref={ref} className="search-wrapper">
            <label className="search-label search-label--team">Opponent Team</label>
            <div className="search-field-wrap">
                {team ? (
                    <div className="search-selected">
                        <img
                            src={`https://cdn.nba.com/logos/nba/${team.nbaTeamId}/primary/L/logo.svg`}
                            alt={team.abbreviation}
                            className="search-team-logo"
                            onError={(e) => {
                                e.currentTarget.style.display = "none";
                            }}
                        />
                        <div className="search-info">
                            <p className="search-info__name">
                                {team.full_name ?? team.teamName}
                            </p>
                            <p className="search-info__meta">
                                {team.city} · {team.abbreviation}
                            </p>
                        </div>
                        <button type="button" onClick={onClear} className="search-clear-btn">
                            X
                        </button>
                    </div>
                ) : (
                    <div
                        className={`search-input-bar search-input-bar--clickable ${open ? "search-input-bar--open-team" : ""}`}
                        onClick={() => setOpen((current) => !current)}
                    >
                        <svg
                            className="search-icon"
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2.5"
                        >
                            <circle cx="11" cy="11" r="8" />
                            <line x1="21" y1="21" x2="16.65" y2="16.65" />
                        </svg>
                        <input
                            value={q}
                            onChange={(e) => {
                                setQ(e.target.value);
                                setOpen(true);
                            }}
                            placeholder="Search team..."
                            onClick={(e) => {
                                e.stopPropagation();
                                setOpen(true);
                            }}
                        />
                        <svg
                            className={`search-chevron ${open ? "search-chevron--open" : ""}`}
                            width="14"
                            height="14"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2.5"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                d="M19 9l-7 7-7-7"
                            />
                        </svg>
                    </div>
                )}

                {open && !team && (
                    <div className="search-dropdown search-dropdown--scroll fade-in">
                        {filtered.length === 0 ? (
                            <p className="search-dropdown__empty">No teams found</p>
                        ) : (
                            filtered.map((candidate) => (
                                <button
                                    type="button"
                                    key={candidate.id ?? candidate.teamId}
                                    onClick={() => pick(candidate)}
                                    className="search-dropdown__item"
                                >
                                    <img
                                        src={`https://cdn.nba.com/logos/nba/${candidate.nbaTeamId}/primary/L/logo.svg`}
                                        alt={candidate.abbreviation}
                                        className="search-team-logo search-team-logo--sm"
                                        onError={(e) => {
                                            e.currentTarget.style.display = "none";
                                        }}
                                    />
                                    <div className="search-info">
                                        <p className="search-dropdown__item-name">
                                            {candidate.full_name ?? candidate.teamName}
                                        </p>
                                        <p className="search-dropdown__item-meta">
                                            {candidate.city} · {candidate.abbreviation}
                                        </p>
                                    </div>
                                </button>
                            ))
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

export default function PlayersPredictions() {
    const navigate = useNavigate();
    const [activePage, setActivePage] = useState("PREDICTIONS");
    const [teams, setTeams] = useState([]);
    const [player, setPlayer] = useState(null);
    const [opponentTeam, setOpponentTeam] = useState(null);
    const [statType, setStatType] = useState("pts");
    const [line, setLine] = useState(DEFAULT_LINES.pts);
    const [prediction, setPrediction] = useState(null);
    const [recentGames, setRecentGames] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetch(API.teams)
            .then((response) => response.json())
            .then((data) => setTeams(normalizeTeamsResponse(data)))
            .catch(() => {});
    }, []);

    useEffect(() => {
        setLine(DEFAULT_LINES[statType] ?? "0.5");
        setPrediction(null);
        setRecentGames([]);
        setError(null);
    }, [statType]);

    const playerApiId = player?.externalApiId ?? player?.playerId;
    const opponentTeamApiId =
        opponentTeam?.externalApiId ?? opponentTeam?.teamId ?? null;
    const canPredict = Boolean(playerApiId && opponentTeamApiId && line);

    const resetResults = () => {
        setPrediction(null);
        setRecentGames([]);
        setError(null);
    };

    const handlePredict = async () => {
        if (!canPredict) return;

        setLoading(true);
        setError(null);
        setPrediction(null);
        setRecentGames([]);

        try {
            const preloadStatsRes = await fetch(API.playerStats(playerApiId));
            if (!preloadStatsRes.ok) {
                const preloadError = await preloadStatsRes.text();
                throw new Error(
                    preloadError || `Failed to load player stats (${preloadStatsRes.status})`
                );
            }

            const predRes = await fetch(
                API.predict(playerApiId, opponentTeamApiId, statType, line)
            );

            if (!predRes.ok) {
                const errText = await predRes.text();
                throw new Error(errText || `Server error ${predRes.status}`);
            }

            const predData = await predRes.json();
            setPrediction(predData);

            try {
                const statsRes = await fetch(API.playerStats(playerApiId));
                if (statsRes.ok) {
                    const statsData = await statsRes.json();
                    const games = Array.isArray(statsData)
                        ? statsData
                        : statsData?.recentGames ?? statsData?.games ?? [];
                    const allGames = Array.isArray(games) ? games : [];
                    setRecentGames(allGames.slice(0, 5));
                }
            } catch {
                // Non-critical secondary request.
            }
        } catch (err) {
            setError(err.message ?? "Prediction failed");
        } finally {
            setLoading(false);
        }
    };

    const overPct = prediction ? (prediction.overProbability * 100).toFixed(1) : "0";
    const isOver = prediction?.overProbability > 0.5;
    const verdictPct = prediction
        ? (
              Math.max(prediction.overProbability, prediction.underProbability) * 100
          ).toFixed(1)
        : "0.0";
    const distributionChartData =
        prediction ? buildDistributionChartData(prediction) : null;

    return (
        <div className="prediction-page-shell">
            <NavBar
                activePage={activePage}
                setActivePage={setActivePage}
                teams={teams}
                onTeamClick={(team) =>
                    navigate(`/team/${team.teamId}/players`, { state: { team } })
                }
            />

            <main className="matchups-page prediction-page">
                <div className="matchups-bg-texture" />
                <div className="matchups-content">
                    <header className="matchups-header">
                        <p className="matchups-header__eyebrow">Matchup Predictions</p>
                    </header>

                    <section className="predict-inputs">
                        <PropPlayerSearch
                            player={player}
                            onSelect={(selectedPlayer) => {
                                setPlayer(selectedPlayer);
                                if (
                                    opponentTeam &&
                                    selectedPlayer?.team?.abbreviation?.toLowerCase() ===
                                        opponentTeam.abbreviation?.toLowerCase()
                                ) {
                                    setOpponentTeam(null);
                                }
                                resetResults();
                            }}
                            onClear={() => {
                                setPlayer(null);
                                resetResults();
                            }}
                        />

                        <TeamSearch
                            teams={teams}
                            team={opponentTeam}
                            onSelect={(selectedTeam) => {
                                setOpponentTeam(selectedTeam);
                                resetResults();
                            }}
                            onClear={() => {
                                setOpponentTeam(null);
                                resetResults();
                            }}
                            disabledAbbr={player?.team?.abbreviation}
                        />

                        <div className="predict-field">
                            <label className="predict-field__label">Stat Type</label>
                            <select
                                value={statType}
                                onChange={(e) => setStatType(e.target.value)}
                                className="predict-field__select"
                            >
                                {STAT_TYPES.map((stat) => (
                                    <option key={stat.value} value={stat.value}>
                                        {stat.label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="predict-field">
                            <label className="predict-field__label">Line</label>
                            <input
                                type="number"
                                step="0.5"
                                value={line}
                                onChange={(e) => {
                                    setLine(e.target.value);
                                    resetResults();
                                }}
                                placeholder="22.5"
                                className="predict-field__input"
                            />
                        </div>
                    </section>

                    <div className="controls-bar">
                        <div className="controls-spacer" />
                        <button
                            type="button"
                            onClick={handlePredict}
                            disabled={!canPredict || loading}
                            className={`controls-analyze-btn ${canPredict && !loading ? "controls-analyze-btn--ready" : "controls-analyze-btn--disabled"}`}
                        >
                            {loading ? (
                                <span className="controls-analyze-spinner">
                                    <span className="controls-analyze-spinner__ring" />
                                    Analyzing
                                </span>
                            ) : (
                                "Prediction"
                            )}
                        </button>
                    </div>

                    <section className="results-panel">
                        {!prediction && !loading && !error && (
                            <div className="results-empty">
                                <p className="results-empty__title">
                                    {!canPredict
                                        ? "Select a player, opponent, and line"
                                        : "Press Generate Prediction to run"}
                                </p>
                            </div>
                        )}

                        {loading && (
                            <div className="results-loading">
                                <div className="results-loading__spinner-wrap">
                                    <div className="results-loading__pulse" />
                                    <div className="results-loading__ring" />
                                </div>
                                <p className="results-loading__text">
                                    Running prediction model...
                                </p>
                            </div>
                        )}

                        {error && (
                            <div className="results-error">
                                <span className="results-error__icon">!</span>
                                <p>
                                    {error}. Make sure the player stats have been loaded
                                    and DvP data has been refreshed.
                                </p>
                            </div>
                        )}

                        {prediction && (
                            <div className="predict-results">
                                <div className="predict-summary">
                                    <div className="predict-summary__main">
                                        <p className="predict-summary__eyebrow">Projection</p>
                                        <div className="predict-summary__value-row">
                                            <span className="predict-summary__value">
                                                {prediction.projectedValue.toFixed(1)}
                                            </span>
                                            <span className="predict-summary__stat">
                                                {STAT_TYPES.find(
                                                    (stat) =>
                                                        stat.value === prediction.statType
                                                )?.label ?? prediction.statType}
                                            </span>
                                        </div>
                                    </div>

                                    <div
                                        className={`predict-summary__verdict ${isOver ? "predict-summary__verdict--over" : "predict-summary__verdict--under"}`}
                                    >
                                        <span className="predict-summary__verdict-label">
                                            {isOver ? "Over" : "Under"}
                                        </span>
                                        <span className="predict-summary__verdict-prob">
                                            {verdictPct}%
                                        </span>
                                    </div>
                                </div>

                                <div className="predict-badges">
                                    <div className="predict-badge">
                                        <span className="predict-badge__label">
                                            Est. Minutes
                                        </span>
                                        <span className="predict-badge__value">
                                            {prediction.projectedMinutes.toFixed(1)}
                                        </span>
                                    </div>
                                    <div className="predict-badge">
                                        <span className="predict-badge__label">Usage %</span>
                                        <span className="predict-badge__value">
                                            {(prediction.usgPct * 100).toFixed(1)}%
                                        </span>
                                    </div>
                                    <div className="predict-badge">
                                        <span className="predict-badge__label">True Shooting %</span>
                                        <span className="predict-badge__value">
                                            {(prediction.tsPct * 100).toFixed(1)}%
                                        </span>
                                    </div>
                                    <div className="predict-badge">
                                        <span className="predict-badge__label">
                                            Games Used
                                        </span>
                                        <span className="predict-badge__value">
                                            {prediction.gamesUsed}
                                        </span>
                                    </div>
                                    {prediction.lowConfidence && (
                                        <div className="predict-badge">
                                            <span className="predict-badge__label">
                                                Confidence
                                            </span>
                                            <span className="predict-badge__value">
                                                Low
                                            </span>
                                        </div>
                                    )}
                                </div>

                                <div className="predict-prob-section">
                                    <div className="predict-prob-labels">
                                        <span className="predict-prob-labels__over">
                                            Over {prediction.line}
                                        </span>
                                        <span className="predict-prob-labels__under">
                                            Under {prediction.line}
                                        </span>
                                    </div>
                                    <div className="predict-prob-track">
                                        <div
                                            className={`predict-prob-fill ${prediction.overProbability > 0.5 ? "predict-prob-fill--over" : "predict-prob-fill--under"}`}
                                            style={{ width: `${overPct}%` }}
                                        >
                                            {overPct}%
                                        </div>
                                    </div>
                                </div>

                                {distributionChartData && (
                                    <div className="predict-chart">
                                        <div className="predict-chart__header">
                                            <p className="predict-chart__title">
                                                Projected Distribution
                                            </p>
                                        </div>
                                        <div className="predict-chart__canvas">
                                            <Bar
                                                data={distributionChartData}
                                                options={distributionChartOptions}
                                            />
                                        </div>
                                    </div>
                                )}

                                <div className="predict-breakdown">
                                    <p className="predict-section__eyebrow">
                                        PROJECTION BREAKDOWN
                                    </p>

                                    <div className="predict-breakdown__row">
                                        <span className="predict-breakdown__label">
                                            Per Min Rate (EWMA)
                                        </span>
                                        <span className="predict-breakdown__value">
                                            {prediction.ewmaPerMinute.toFixed(3)}
                                        </span>
                                    </div>
                                    <div className="predict-breakdown__row">
                                        <span className="predict-breakdown__label">
                                            Proj. Minutes
                                        </span>
                                        <span className="predict-breakdown__value">
                                            {prediction.projectedMinutes.toFixed(1)}
                                        </span>
                                    </div>
                                    <div className="predict-breakdown__row">
                                        <span className="predict-breakdown__label">
                                            Pace Adjustment
                                        </span>
                                        <span
                                            className={`predict-breakdown__value ${prediction.paceAdjustment >= 1 ? "predict-breakdown__value--positive" : "predict-breakdown__value--negative"}`}
                                        >
                                            x{prediction.paceAdjustment.toFixed(3)}
                                        </span>
                                    </div>
                                    <div className="predict-breakdown__row">
                                        <span className="predict-breakdown__label">
                                            Opponent Adjustment
                                        </span>
                                        <span
                                            className={`predict-breakdown__value ${prediction.opponentAdjustment >= 1 ? "predict-breakdown__value--positive" : "predict-breakdown__value--negative"}`}
                                        >
                                            x{prediction.opponentAdjustment.toFixed(3)}
                                        </span>
                                    </div>
                                    <div className="predict-breakdown__row">
                                        <span className="predict-breakdown__label">
                                            Rest Adjustment
                                        </span>
                                        <span
                                            className={`predict-breakdown__value ${prediction.restAdjustment >= 1 ? "predict-breakdown__value--positive" : "predict-breakdown__value--negative"}`}
                                        >
                                            x{prediction.restAdjustment.toFixed(3)}
                                        </span>
                                    </div>
                                    <div className="predict-breakdown__row">
                                        <span className="predict-breakdown__label">
                                            Std Deviation
                                        </span>
                                        <span className="predict-breakdown__value">
                                            {prediction.stdDev.toFixed(2)}
                                        </span>
                                    </div>

                                    <div className="predict-breakdown__total">
                                        <span className="predict-breakdown__total-label">
                                            PROJECTED
                                        </span>
                                        <span className="predict-breakdown__total-value">
                                            {prediction.projectedValue.toFixed(1)}
                                        </span>
                                    </div>
                                </div>

                                {recentGames.length > 0 && (
                                    <div className="predict-games">
                                        <p className="predict-section__eyebrow">
                                            RECENT GAMES
                                        </p>
                                        <div className="px-10 pt-6 pb-2 flex items-center gap-10 border-b border-[#1a2540] mt-6! ">
                                            <span className="text-[#ffffff] text-[10px] font-semibold uppercase tracking-wider w-8 text-right mb-3!">
                                                #
                                            </span>
                                            <span className="text-[#ffffff] text-[12px] font-semibold uppercase tracking-wider w-32 mb-3!">
                                                Date
                                            </span>
                                            <span className="text-[#ffffff] text-[12px] font-semibold uppercase tracking-wider w-24 mb-3!">
                                                Matchup
                                            </span>
                                            <div className="flex gap-5 flex-1 justify-end">
                                                <span className="text-[#ffffff] text-[12px] font-semibold uppercase tracking-wider min-w-[50px] text-center mb-3!">
                                                    MIN PLAYED
                                                </span>
                                                <span className="text-[#ffffff] text-[12px] font-semibold uppercase tracking-wider min-w-[50px] text-center mb-3!">
                                                    {statType}
                                                </span>
                                            </div>
                                        </div>
                                        <div className="p-10 flex flex-col gap-5">
                                            {recentGames.map((game, index) => (
                                                <PredictionRecentGameRow
                                                    key={game.statisticId ?? index}
                                                    game={game}
                                                    statType={statType}
                                                    line={line}
                                                    index={index}
                                                />
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}
                    </section>
                </div>
            </main>
        </div>
    );
}