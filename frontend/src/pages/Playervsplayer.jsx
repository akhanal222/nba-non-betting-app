import { useEffect, useState, useRef } from "react";
import "../matchup.css";
import "../pvp.css";

const API = {
    playerSearch: (q) =>
        `http://localhost:8080/api/players/search?q=${encodeURIComponent(q)}`,
    compare: (id1, id2) =>
        `http://localhost:8080/api/comparison/compare?playerOneApiId=${id1}&playerTwoApiId=${id2}`,
};

function playerHeadshot(id) {
    return id
        ? `https://cdn.nba.com/headshots/nba/latest/1040x760/${id}.png`
        : null;
}

//  Player Search
function CompPlayerSearch({ player, onSelect, onClear, label, excludeId }) {
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
        if (val.trim().length < 2) { setResults([]); setOpen(false); return; }
        debounceRef.current = setTimeout(async () => {
            setLoading(true);
            try {
                const res = await fetch(API.playerSearch(val.trim()));
                const data = await res.json();
                const list = Array.isArray(data) ? data : [];
                setResults(list.filter((p) => (p.externalApiId ?? p.playerId) !== excludeId).slice(0, 6));
                setOpen(true);
            } catch { setResults([]); }
            finally { setLoading(false); }
        }, 300);
    };

    const pick = (p) => { onSelect(p); setQ(""); setResults([]); setOpen(false); };

    return (
        <div ref={ref} className="search-wrapper">
            <label className="search-label search-label--player">{label}</label>
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
                                {player.team?.abbreviation ?? player.team?.teamName ?? "—"} · {player.position ?? "—"}
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
                    <div className="search-dropdown">
                        {results.map((p) => (
                            <button key={p.externalApiId ?? p.playerId} onClick={() => pick(p)} className="search-dropdown__item">
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

                {open && results.length === 0 && !loading && q.length >= 2 && (
                    <div className="search-dropdown">
                        <p className="search-dropdown__empty">No players found</p>
                    </div>
                )}
            </div>
        </div>
    );
}

// Stat Compare Row
function StatCompareRow({ label, valA, valB, format = "number", higherIsBetter = true }) {
    const a = valA ?? 0;
    const b = valB ?? 0;
    const max = Math.max(a, b, 0.01);
    const pctA = (a / max) * 100;
    const pctB = (b / max) * 100;

    const aLeads = higherIsBetter ? a > b : a < b;
    const bLeads = higherIsBetter ? b > a : b < a;

    const fmt = (v) => {
        if (v == null) return "—";
        if (format === "pct") return `${(v * 100).toFixed(1)}%`;
        return Number.isInteger(v) ? v : v.toFixed(1);
    };

    const hitColor = "#47e897";
    const missColor = "#e86347";
    const tieColor = "#667594";

    return (
        <div className="pvp-stat-row">
            <span className="pvp-stat-row__value pvp-stat-row__value--left"
                  style={{ color: aLeads ? hitColor : bLeads ? missColor : tieColor }}>
                {fmt(a)}
            </span>
            <div className="pvp-stat-row__bar-track">
                <div className="pvp-stat-row__bar-fill pvp-stat-row__bar-fill--left"
                     style={{
                         width: `${pctA}%`,
                         background: aLeads
                             ? hitColor
                             : bLeads
                                 ? missColor
                                 : tieColor,
                     }} />
            </div>
            <span className="pvp-stat-row__label">{label}</span>
            <div className="pvp-stat-row__bar-track">
                <div className="pvp-stat-row__bar-fill pvp-stat-row__bar-fill--right"
                     style={{
                         width: `${pctB}%`,
                         background: bLeads
                             ? hitColor
                             : aLeads
                                 ? missColor
                                 : tieColor,
                     }} />
            </div>
            <span className="pvp-stat-row__value pvp-stat-row__value--right"
                  style={{ color: bLeads ? hitColor : aLeads ? missColor : tieColor }}>
                {fmt(b)}
            </span>
        </div>
    );
}

// Profile Card
function ProfileCard({ profile, accentColor }) {
    const [imgErr, setImgErr] = useState(false);
    if (!profile) return null;

    return (
        <div className="pvp-profile">
            <div className="pvp-profile__avatar" style={{ borderColor: accentColor }}>
                {!imgErr && profile.imageUrl ? (
                    <img src={profile.imageUrl} alt=""
                         className="pvp-profile__avatar-img"
                         onError={() => setImgErr(true)} />
                ) : (
                    <span className="pvp-profile__avatar-initials">
                        {profile.firstName?.[0]}{profile.lastName?.[0]}
                    </span>
                )}
            </div>
            <h3 className="pvp-profile__name">{profile.firstName} {profile.lastName}</h3>
            <p className="pvp-profile__meta">{profile.position} · {profile.team ?? "—"}</p>
            <div className="pvp-profile__details">
                <div className="pvp-profile__detail">
                    <span className="pvp-profile__detail-label">HT</span>
                    <span className="pvp-profile__detail-value">{profile.height ?? "—"}</span>
                </div>
                <div className="pvp-profile__detail">
                    <span className="pvp-profile__detail-label">WT</span>
                    <span className="pvp-profile__detail-value">{profile.weight ? `${profile.weight}` : "—"}</span>
                </div>
                <div className="pvp-profile__detail">
                    <span className="pvp-profile__detail-label">DRAFT</span>
                    <span className="pvp-profile__detail-value">
                        {profile.draftYear
                            ? `'${String(profile.draftYear).slice(2)} R${profile.draftRound}/#${profile.draftNumber}`
                            : "—"}
                    </span>
                </div>
            </div>
            {profile.college && (
                <p className="pvp-profile__college">{profile.college}</p>
            )}
        </div>
    );
}

// Stat rows config
const STAT_ROWS = [
    { label: "PTS", key: "pts" },
    { label: "REB", key: "reb" },
    { label: "AST", key: "ast" },
    { label: "STL", key: "stl" },
    { label: "BLK", key: "blk" },
    { label: "TOV", key: "turnover", higherIsBetter: false },
    { label: "FG%", key: "fgPct", format: "pct" },
    { label: "3P%", key: "fg3Pct", format: "pct" },
    { label: "FT%", key: "ftPct", format: "pct" },
    { label: "3PM", key: "fg3m" },
    { label: "MPG", key: "minutesPerGame" },
    { label: "GP", key: "gamesPlayed" },
];

// Main Export
export default function PlayerVsPlayer() {
    const [playerA, setPlayerA] = useState(null);
    const [playerB, setPlayerB] = useState(null);
    const [compData, setCompData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [statView, setStatView] = useState("season");

    const idA = playerA?.externalApiId ?? playerA?.playerId;
    const idB = playerB?.externalApiId ?? playerB?.playerId;
    const canCompare = idA && idB;

    const compare = async () => {
        if (!canCompare) return;
        setLoading(true); setError(null); setCompData(null);
        try {
            const res = await fetch(API.compare(idA, idB));
            if (!res.ok) throw new Error(`Server error ${res.status}`);
            setCompData(await res.json());
        } catch (err) {
            setError(err.message ?? "Comparison failed");
            setCompData(null);
        } finally { setLoading(false); }
    };

    const resetResults = () => { setCompData(null); setError(null); };

    const pOne = compData?.playerOne;
    const pTwo = compData?.playerTwo;
    const statsA = statView === "season" ? pOne?.seasonAverages : pOne?.careerAverages;
    const statsB = statView === "season" ? pTwo?.seasonAverages : pTwo?.careerAverages;

    return (
        <>
            {/* ── Header ── */}
            <header className="matchups-header">
                <p className="matchups-header__eyebrow">Player Analytics</p>
                <h1 className="matchups-header__title">
                    Player <em>Comparison</em>
                </h1>
                <p className="matchups-header__subtitle">
                    Compare stats head-to-head between two players
                </p>
            </header>

            {/* ── Search Row ── */}
            <div className="matchups-search-row">
                <CompPlayerSearch
                    label="Player One"
                    player={playerA}
                    onSelect={(p) => { setPlayerA(p); resetResults(); }}
                    onClear={() => { setPlayerA(null); resetResults(); }}
                    excludeId={idB}
                />
                <CompPlayerSearch
                    label="Player Two"
                    player={playerB}
                    onSelect={(p) => { setPlayerB(p); resetResults(); }}
                    onClear={() => { setPlayerB(null); resetResults(); }}
                    excludeId={idA}
                />
            </div>

            {/* ── Controls Bar ── */}
            <div className="controls-bar">
                <div className="controls-stat-pills">
                    {["season", "career"].map((v) => (
                        <button key={v}
                                onClick={() => setStatView(v)}
                                className="controls-stat-pill"
                                style={statView === v
                                    ? { color: "#47e897", background: "rgba(71,232,151,0.15)", borderColor: "rgba(71,232,151,0.3)" }
                                    : undefined}>
                            {v === "season" ? "Season Avg" : "Career Avg"}
                        </button>
                    ))}
                </div>

                <div className="controls-spacer" />

                <button onClick={compare} disabled={!canCompare || loading}
                        className={`controls-analyze-btn ${canCompare && !loading ? "controls-analyze-btn--ready" : "controls-analyze-btn--disabled"}`}>
                    {loading ? (
                        <span className="controls-analyze-spinner">
                            <span className="controls-analyze-spinner__ring" />
                            Comparing
                        </span>
                    ) : "Compare →"}
                </button>
            </div>

            {/* ── Results Panel ── */}
            <div className="results-panel">
                {/* Empty */}
                {!compData && !loading && !error && (
                    <div className="results-empty">
                        <div >
                        </div>
                        <p className="results-empty__title">
                            {!canCompare ? "Select two players to compare" : "Press Compare to run"}
                        </p>
                        <p className="results-empty__subtitle">Season & career averages</p>
                    </div>
                )}

                {/* Loading */}
                {loading && (
                    <div className="results-loading">
                        <p className="results-loading__text">Comparing players...</p>
                    </div>
                )}

                {/* Error */}
                {error && (
                    <div className="results-error">
                        <span className="results-error__icon">⚠</span>
                        <p>{error} — Make sure both players have been searched first.</p>
                    </div>
                )}

                {/* Results */}
                {compData && pOne && pTwo && (
                    <>
                        {/* ── Profile Cards ── */}
                        <div className="pvp-profiles-row">
                            <ProfileCard profile={pOne} accentColor="#4f7cff" />
                            <div className="pvp-vs-divider">
                                <span className="pvp-vs-text">VS</span>
                            </div>
                            <ProfileCard profile={pTwo} accentColor="#47e897" />
                        </div>

                        {/* ── Stat Header ── */}
                        <div className="results-header">
                            <div className="results-header__left">
                                <span className="results-header__stat-badge"
                                      style={{ color: "white", background: "rgba(73,164,193,0.15)", border: "1px solid rgba(71,232,151,0.25)" }}>
                                    {statView === "season" ? "SEASON" : "CAREER"}
                                </span>
                                <div>
                                    <p className="results-header__matchup-title">
                                        {pOne.firstName} {pOne.lastName}
                                        <span className="results-header__vs">vs</span>
                                        <span className="results-header__opponent">
                                            {pTwo.firstName} {pTwo.lastName}
                                        </span>
                                    </p>
                                    <p className="results-header__meta">
                                        <span
                                            style={{
                                                display: "block",
                                                width: "100%",
                                                textAlign: "center",
                                                fontSize: "14px",
                                                fontWeight: 600,
                                                color: "#ffffff",
                                            }}
                                        >
                                            {statsA?.gamesPlayed != null && statsB?.gamesPlayed != null
                                                ? `${statsA.gamesPlayed} GP vs ${statsB.gamesPlayed} GP`
                                                : ""}
                                        </span>
                                    </p>
                                </div>
                            </div>
                        </div>

                        {/* ── Stat Bars ── */}
                        <div className="results-games">
                            {statsA && statsB ? (
                                STAT_ROWS.map((row) => (
                                    <StatCompareRow
                                        key={row.key}
                                        label={row.label}
                                        valA={statsA[row.key]}
                                        valB={statsB[row.key]}
                                        format={row.format ?? "number"}
                                        higherIsBetter={row.higherIsBetter ?? true}
                                    />
                                ))
                            ) : (
                                <p className="results-games__empty">
                                    {statView === "season"
                                        ? "Season averages not available for one or both players."
                                        : "Career averages not available for one or both players."}
                                </p>
                            )}
                        </div>
                    </>
                )}
            </div>
        </>
    );
}
