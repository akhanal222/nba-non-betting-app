import { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import NavBar from "../components/Navbar.jsx";
import "../matchup.css";

// ── Config ────────────────────────────────────────────────────────────────────
const API = {
    teams: "http://localhost:8080/teams",
    playerSearch: (q) =>
        `http://localhost:8080/api/players/search?q=${encodeURIComponent(q)}`,
    matchup: (params) =>
        `http://localhost:8080/api/matchup/analyze?${new URLSearchParams(params)}`,
};

const STAT_TYPES = [
    { key: "pts", label: "PTS", color: "#ffffff", glow: "rgba(232,197,71,0.15)" },
    { key: "reb", label: "REB", color: "#47e897", glow: "rgba(71,232,151,0.15)" },
    { key: "ast", label: "AST", color: "#479de8", glow: "rgba(71,157,232,0.15)" },
    { key: "blk", label: "BLK", color: "#e86347", glow: "rgba(232,99,71,0.15)" },
    { key: "stl", label: "STL", color: "#47e897", glow: "rgba(232,99,71,0.15)" },
    { key: "turnover", label: "TOVR", color: "#47e897", glow: "rgba(232,99,71,0.15)" },
    { key: "fg3m", label: "3PT", color: "#47e897", glow: "rgba(232,99,71,0.15)" },



];

const DEFAULT_STAT_LINES = {
    pts: 15.5,
    reb: 5.5,
    ast: 4.5,
    blk: 0.5,
    stl: 0.5,
    turnover: 2.5,
    fg3m: 1.5,
};

function playerHeadshot(id) {
    return id
        ? `https://cdn.nba.com/headshots/nba/latest/1040x760/${id}.png`
        : null;
}

// ── Player Search ─────────────────────────────────────────────────────────────
function PlayerSearch({ player, onSelect, onClear }) {
    const [q, setQ] = useState("");
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [open, setOpen] = useState(false);
    const ref = useRef(null);

    useEffect(() => {
        const handler = (e) => {
            if (ref.current && !ref.current.contains(e.target)) setOpen(false);
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    const search = async (val) => {
        setQ(val);
        if (val.trim().length < 2) { setResults([]); setOpen(false); return; }
        setLoading(true);
        try {
            const res = await fetch(API.playerSearch(val.trim()));
            const data = await res.json();
            setResults(Array.isArray(data) ? data.slice(0, 6) : []);
            setOpen(true);
        } catch { setResults([]); }
        finally { setLoading(false); }
    };

    const pick = (p) => { onSelect(p); setQ(""); setResults([]); setOpen(false); };

    return (
        <div ref={ref} className="search-wrapper">
            <label className="search-label search-label--player">Player</label>
            <div className="search-field-wrap">
                {player ? (
                    <div className="search-selected">
                        <div className="search-avatar">
              <span className="search-avatar__initials">
                {player.firstName?.[0]}{player.lastName?.[0]}
              </span>
                            {player.nbaPlayerId && (
                                <img src={playerHeadshot(player.nbaPlayerId)} alt=""
                                     className="search-avatar__img"
                                     onError={(e) => { e.target.style.display = "none"; }} />
                            )}
                        </div>
                        <div className="search-info">
                            <p className="search-info__name">{player.firstName} {player.lastName}</p>
                            <p className="search-info__meta">
                                {player.team?.abbreviation ?? "—"} · {player.position ?? "—"}
                            </p>
                        </div>
                        <button onClick={onClear} className="search-clear-btn">✕</button>
                    </div>
                ) : (
                    <div className={`search-input-bar ${open ? "search-input-bar--open" : ""}`}>
                        {loading
                            ? <div className="search-spinner" />
                            : <svg className="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
                            </svg>
                        }
                        <input value={q} onChange={(e) => search(e.target.value)}
                               placeholder="Search player name..." />
                    </div>
                )}

                {open && results.length > 0 && (
                    <div className="search-dropdown fade-in">
                        {results.map((p) => (
                            <button key={p.playerId} onClick={() => pick(p)} className="search-dropdown__item">
                                <div className="search-avatar search-avatar--sm">
                  <span className="search-avatar__initials">
                    {p.firstName?.[0]}{p.lastName?.[0]}
                  </span>
                                    {p.nbaPlayerId && (
                                        <img src={playerHeadshot(p.nbaPlayerId)} alt=""
                                             className="search-avatar__img"
                                             onError={(e) => { e.target.style.display = "none"; }} />
                                    )}
                                </div>
                                <div className="search-info">
                                    <p className="search-dropdown__item-name">{p.firstName} {p.lastName}</p>
                                    <p className="search-dropdown__item-meta">
                                        {p.team?.abbreviation ?? "—"} · {p.position ?? "—"}
                                    </p>
                                </div>
                            </button>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}

// ── Team Search ───────────────────────────────────────────────────────────────
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

    const filtered = teams.filter((t) => {
        const s = q.toLowerCase();
        const isDisabled = disabledAbbr && (t.abbreviation ?? "").toLowerCase() === disabledAbbr.toLowerCase();
        if (isDisabled) return false;
        return (
            (t.full_name ?? t.teamName ?? "").toLowerCase().includes(s) ||
            (t.abbreviation ?? "").toLowerCase().includes(s) ||
            (t.city ?? "").toLowerCase().includes(s)
        );
    });

    const pick = (t) => { onSelect(t); setQ(""); setOpen(false); };

    return (
        <div ref={ref} className="search-wrapper">
            <label className="search-label search-label--team">Opponent Team </label>
            <div className="search-field-wrap">
                {team ? (
                    <div className="search-selected">
                        <img src={`https://cdn.nba.com/logos/nba/${team.nbaTeamId}/primary/L/logo.svg`}
                             alt={team.abbreviation} className="search-team-logo"
                             onError={(e) => { e.target.style.display = "none"; }} />
                        <div className="search-info">
                            <p className="search-info__name">{team.full_name ?? team.teamName}</p>
                            <p className="search-info__meta">{team.city} · {team.abbreviation}</p>
                        </div>
                        <button onClick={onClear} className="search-clear-btn">✕</button>
                    </div>
                ) : (
                    <div className={`search-input-bar search-input-bar--clickable ${open ? "search-input-bar--open-team" : ""}`}
                         onClick={() => setOpen(v => !v)}>
                        <svg className="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
                        </svg>
                        <input value={q}
                               onChange={(e) => { setQ(e.target.value); setOpen(true); }}
                               placeholder="Search team..."
                               onClick={(e) => { e.stopPropagation(); setOpen(true); }} />
                        <svg className={`search-chevron ${open ? "search-chevron--open" : ""}`}
                             width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7"/>
                        </svg>
                    </div>
                )}

                {open && !team && (
                    <div className="search-dropdown search-dropdown--scroll fade-in">
                        {filtered.length === 0
                            ? <p className="search-dropdown__empty">No teams found</p>
                            : filtered.map((t) => (
                                <button key={t.id ?? t.teamId} onClick={() => pick(t)} className="search-dropdown__item">
                                    <img src={`https://cdn.nba.com/logos/nba/${t.nbaTeamId}/primary/L/logo.svg`}
                                         alt={t.abbreviation} className="search-team-logo search-team-logo--sm"
                                         onError={(e) => { e.target.style.display = "none"; }} />
                                    <div className="search-info">
                                        <p className="search-dropdown__item-name">{t.full_name ?? t.teamName}</p>
                                        <p className="search-dropdown__item-meta">{t.city} · {t.abbreviation}</p>
                                    </div>
                                </button>
                            ))
                        }
                    </div>
                )}
            </div>
        </div>
    );
}

// ── Result Row ────────────────────────────────────────────────────────────────
function ResultRow({ result, statType, statLine, index, total }) {
    const stat = STAT_TYPES.find((s) => s.key === statType);
    const statColor = stat?.color ?? "#e8c547";
    const hit = result.hitLine;
    const date = result.date
        ? new Date(result.date + "T00:00:00").toLocaleDateString("en-US", {
            month: "short", day: "numeric", year: "numeric",
        })
        : "—";
    const pct = total > 0 ? (result.statValue ?? 0) / (statLine * 2) : 0;

    return (
        <div className={`result-row ${hit ? "result-row--hit" : "result-row--miss"}`}>
      <span className="result-row__index">
        {String(index + 1).padStart(2, "0")}
      </span>
            <p className="result-row__date">{date}</p>
            <div className="result-row__bar-track">
                <div className="result-row__bar-fill"
                     style={{
                         width: `${Math.min(pct * 100, 100)}%`,
                         background: hit
                             ? `linear-gradient(90deg, ${statColor}60, ${statColor})`
                             : `linear-gradient(90deg, #e8634760, #e86347)`,
                     }} />
            </div>
            <div className="result-row__stat">
        <span className="result-row__stat-value" style={{ color: hit ? statColor : "#e86347" }}>
          {result.statValue ?? "—"}
        </span>
                <span className="result-row__stat-label">{statType}</span>
            </div>
            <div className="result-row__line">
                <p className="result-row__line-label">line</p>
                <p className="result-row__line-value">{statLine}</p>
            </div>
            <div className={`result-row__indicator ${hit ? "result-row__indicator--hit" : "result-row__indicator--miss"}`} />
        </div>
    );
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function MatchupsDashboard() {
    const [teams, setTeams] = useState([]);
    const [pageLoading, setPageLoading] = useState(true);
    const [activePage, setActivePage] = useState("MATCHUPS");
    const navigate = useNavigate();

    const [playerA, setPlayerA] = useState(null);
    const [opponentTeam, setOpponentTeam] = useState(null);
    const [statType, setStatType] = useState("pts");
    const [statLine, setStatLine] = useState(DEFAULT_STAT_LINES.pts);
    const [limit, setLimit] = useState(5);
    const [includePlayoffs, setIncludePlayoffs] = useState(false);
    const [results, setResults] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetch(API.teams)
            .then(r => r.json())
            .then(data => {
                const filteredTeams = (Array.isArray(data.data || data) ? (data.data || data) : [])
                    .filter((team) => {
                        const id = Number(team?.externalApiId ?? team?.teamId);
                        return Number.isInteger(id) && id >= 1 && id <= 30;
                    })
                    .sort((a, b) => Number(a?.externalApiId ?? a?.teamId) - Number(b?.externalApiId ?? b?.teamId));

                setTeams(filteredTeams);
            })
            .catch(() => {})
            .finally(() => setPageLoading(false));
    }, []);

    const resetResults = () => { setResults(null); setError(null); };

    useEffect(() => {
        setStatLine(DEFAULT_STAT_LINES[statType] ?? 0);
        resetResults();
    }, [statType]);

    const handleSelectPlayer = (p) => {
        setPlayerA(p);
        if (opponentTeam && p?.team?.abbreviation?.toLowerCase() === opponentTeam.abbreviation?.toLowerCase())
            setOpponentTeam(null);
        resetResults();
    };

    const canAnalyze = playerA && opponentTeam;

    const analyze = async () => {
        if (!canAnalyze) return;
        setLoading(true); setError(null); setResults(null);
        try {
            const res = await fetch(API.matchup({
                playerApiId: playerA.externalApiId,
                opponentApiId: opponentTeam.externalApiId,
                statLine,
                limit, includePlayoffs, statType,
            }));
            if (!res.ok) throw new Error(`Server error ${res.status}`);
            setResults(await res.json());
        } catch (err) {
            setError(err.message ?? "Failed to analyze matchup");
        } finally { setLoading(false); }
    };

    const selectedStat = STAT_TYPES.find((s) => s.key === statType);
    const games = results?.games ?? (Array.isArray(results) ? results : []);
    const hitCount = games.filter((g) => g.hitLine).length;
    const hitRate = games.length > 0 ? ((hitCount / games.length) * 100).toFixed(0) : 0;

    return (
        <div className="matchups-page">
            <div className="matchups-bg-texture" />

            <NavBar
                activePage={activePage}
                setActivePage={setActivePage}
                teams={teams}
                onTeamClick={(team) => navigate(`/team/${team.teamId}/players`, { state: { team } })}
            />

            {pageLoading ? (
                <div className="matchups-page-loader">
                    <div className="results-loading">
                        <div className="results-loading__spinner-wrap">
                            <div className="results-loading__ring" />
                        </div>
                        <p className="results-loading__text">Loading matchup page...</p>
                    </div>
                </div>
            ) : (
            <div className="matchups-content">
                {/* ── Header ── */}
                <header className="matchups-header">
                    <p className="matchups-header__eyebrow">Player Analytics</p>
                    <h1 className="matchups-header__title">
                        Matchup <em>Analysis</em>
                    </h1>
                    <p className="matchups-header__subtitle">
                        Compare player performance against specific opponents
                    </p>
                </header>

                {/* ── Search Row ── */}
                <div className="matchups-search-row">
                    <PlayerSearch
                        player={playerA}
                        onSelect={handleSelectPlayer}
                        onClear={() => { setPlayerA(null); resetResults(); }}
                    />
                    <TeamSearch
                        teams={teams}
                        team={opponentTeam}
                        onSelect={(t) => { setOpponentTeam(t); resetResults(); }}
                        onClear={() => { setOpponentTeam(null); resetResults(); }}
                        disabledAbbr={playerA?.team?.abbreviation}
                    />
                </div>

                {/* ── Controls Bar ── */}
                <div className="controls-bar">
                    <div className="controls-stat-pills">
                        {STAT_TYPES.map((s) => (
                            <button key={s.key}
                                    onClick={() => setStatType(s.key)}
                                    className="controls-stat-pill"
                                    style={statType === s.key
                                        ? { color: s.color, background: s.glow, borderColor: `${s.color}50` }
                                        : undefined}>
                                {s.label}
                            </button>
                        ))}
                    </div>

                    <div className="controls-divider" />

                    <div className="controls-games-group">
                        <span className="controls-games-label">Games</span>
                        <div className="controls-games-btns">
                            {[5, 10, 15, 20].map((n) => (
                                <button key={n}
                                        onClick={() => { setLimit(n); resetResults(); }}
                                        className={`controls-game-btn ${limit === n ? "controls-game-btn--active" : ""}`}>
                                    {n}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="controls-divider" />

                    <button onClick={() => { setIncludePlayoffs(p => !p); resetResults(); }}
                            className={`controls-playoff-btn ${includePlayoffs ? "controls-playoff-btn--active" : ""}`}>
                        {includePlayoffs ? "✓ " : ""}Playoffs
                    </button>

                    <div className="controls-divider" />

                    <div className="controls-line-group">
                        <span className="controls-line-label">Line</span>
                        <input
                            type="number"
                            step="0.5"
                            value={statLine}
                            onChange={(e) => {
                                setStatLine(parseFloat(e.target.value) || "");
                                resetResults();
                            }}
                            className="controls-line-input"
                        />
                    </div>

                    <div className="controls-spacer" />

                    <button onClick={analyze} disabled={!canAnalyze || loading}
                            className={`controls-analyze-btn ${canAnalyze && !loading ? "controls-analyze-btn--ready" : "controls-analyze-btn--disabled"}`}>
                        {loading ? (
                            <span className="controls-analyze-spinner">
                <span className="controls-analyze-spinner__ring" />
                Analyzing
              </span>
                        ) : "Analyze →"}
                    </button>
                </div>

                {/* ── Results Panel ── */}
                <div className="results-panel">
                    {!results && !loading && !error && (
                        <div className="results-empty">
                            <div className="results-empty__icon">
                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1e2535" strokeWidth="1.5">
                                    <path strokeLinecap="round" strokeLinejoin="round"
                                          d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zm6.75-4.5c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zm6.75-4.5c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z"/>
                                </svg>
                            </div>
                            <p className="results-empty__title">
                                {!canAnalyze ? "Select a player and opponent" : "Press Analyze to run"}
                            </p>
                            <p className="results-empty__subtitle">Historical performance data</p>
                        </div>
                    )}

                    {loading && (
                        <div className="results-loading">
                            <div className="results-loading__spinner-wrap">
                                <div className="results-loading__pulse" />
                                <div className="results-loading__ring" />
                            </div>
                            <p className="results-loading__text">Analyzing matchup...</p>
                        </div>
                    )}

                    {error && (
                        <div className="results-error">
                            <span className="results-error__icon">⚠</span>
                            <p>{error}</p>
                        </div>
                    )}

                    {results && (
                        <>
                            <div className="results-header">
                                <div className="results-header__left">
                  <span className="results-header__stat-badge"
                        style={{ color: selectedStat.color, background: selectedStat.glow, border: `1px solid ${selectedStat.color}25` }}>
                    {selectedStat.label}
                  </span>
                                    <div>
                                        <p className="results-header__matchup-title">
                                            {results.playerName ?? `${playerA.firstName} ${playerA.lastName}`}
                                            <span className="results-header__vs">vs</span>
                                            <span className="results-header__opponent">
                        {results.opponentTeamName ?? opponentTeam.full_name ?? opponentTeam.teamName}
                      </span>
                                        </p>
                                        <p className="results-header__meta">
                                            {results.totalGames ?? games.length} games · Avg {results.average ?? "—"} {selectedStat.label} · Line {results.statLine ?? statLine}
                                        </p>
                                    </div>
                                </div>

                                {games.length > 0 && (
                                    <div className="results-hitrate">
                    <span className={`results-hitrate__value ${
                        hitRate >= 60 ? "results-hitrate__value--high"
                            : hitRate >= 40 ? "results-hitrate__value--mid"
                                : "results-hitrate__value--low"
                    }`}>
                      {hitRate}%
                    </span>
                                        <span className="results-hitrate__label">Hit Rate</span>
                                    </div>
                                )}
                            </div>

                            <div className="results-games">
                                {games.length === 0
                                    ? <p className="results-games__empty">No historical matchup data found.</p>
                                    : games.map((game, i) => (
                                        <ResultRow key={i} result={game} statType={statType}
                                                   statLine={results.statLine ?? statLine}
                                                   index={i} total={games.length} />
                                    ))
                                }
                            </div>
                        </>
                    )}
                </div>
            </div>
            )}
        </div>
    );
}