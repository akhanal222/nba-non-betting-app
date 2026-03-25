import { useLocation, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";

const API_BASE = "http://localhost:8080";

export default function GameLineupPage() {
    const { state } = useLocation();
    const navigate  = useNavigate();
    const game      = state?.game;

    const [gameInfo,  setGameInfo]  = useState(null);
    const [stats,     setStats]     = useState([]);
    const [lineups,   setLineups]   = useState([]);
    const [loading,   setLoading]   = useState(true);
    const [error,     setError]     = useState(null);

    useEffect(() => {
        if (!game?.id) return;

        // Fire all 3 requests in parallel
        Promise.all([
            fetch(`${API_BASE}/bdl/games/${game.id}`)
                .then(r => r.json()).catch(() => null),

            fetch(`${API_BASE}/bdl/games/${game.id}/stats`)
                .then(r => r.json()).catch(() => ({ data: [] })),

            fetch(`${API_BASE}/bdl/games/${game.id}/lineups`)
                .then(r => r.json()).catch(() => ({ data: [] })),
        ]).then(([info, statsData, lineupData]) => {
            setGameInfo(info);
            setStats(Array.isArray(statsData) ? statsData : statsData?.data || []);
            setLineups(Array.isArray(lineupData) ? lineupData : lineupData?.data || []);
            setLoading(false);
        }).catch(() => {
            setError("Failed to load game data.");
            setLoading(false);
        });
    }, [game?.id]);

    if (!game) {
        return (
            <div style={s.centered}>
                <p style={{ color: "#555" }}>No game data found.</p>
                <button onClick={() => navigate("/")} style={s.backBtn}>← Back</button>
            </div>
        );
    }

    // Use gameInfo if loaded, fall back to the game object passed via state
    const g           = gameInfo ?? game;
    const homeAbbr    = g.home_team?.abbreviation    ?? game.home_team?.abbreviation    ?? "";
    const visitorAbbr = g.visitor_team?.abbreviation ?? game.visitor_team?.abbreviation ?? "";
    const homeName    = g.home_team?.full_name        ?? game.home_team?.full_name        ?? homeAbbr;
    const visitorName = g.visitor_team?.full_name     ?? game.visitor_team?.full_name     ?? visitorAbbr;
    const homeScore   = g.home_team_score;
    const visitorScore= g.visitor_team_score;
    const isFinished  = g.status === "Final" || (typeof g.status === "string" && g.status.toLowerCase().includes("final"));

    // Build a map: playerId → stats row for quick lookup
    const statsMap = {};
    stats.forEach(st => {
        if (st.player?.id) statsMap[st.player.id] = st;
    });

    // Split lineups by team
    const homeLineup    = lineups.filter(l => l.team?.abbreviation === homeAbbr);
    const visitorLineup = lineups.filter(l => l.team?.abbreviation === visitorAbbr);

    // If no lineups, build from stats
    const homeStats    = stats.filter(st => st.team?.abbreviation === homeAbbr);
    const visitorStats = stats.filter(st => st.team?.abbreviation === visitorAbbr);

    const showLineups = lineups.length > 0;

    return (
        <div style={{ minHeight: "100vh", background: "#0a0c14", fontFamily: "system-ui, -apple-system, sans-serif", color: "#fff" }}>

            {/* ── Nav ── */}
            <nav style={s.nav}>
                <button onClick={() => navigate(-1)} style={s.backBtn}
                        onMouseEnter={e => e.currentTarget.style.color = "#4f7cff"}
                        onMouseLeave={e => e.currentTarget.style.color = "#666"}
                >
                    <svg width="15" height="15" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7"/>
                    </svg>
                    Back
                </button>
            </nav>

            {/* ── Score Hero ── */}
            <div style={s.hero}>
                <div style={{ ...s.glow, left: "8%",  background: "rgba(79,124,255,0.12)" }} />
                <div style={{ ...s.glow, right: "8%", background: "rgba(79,124,255,0.07)" }} />

                <div style={s.heroInner}>

                    {/* Home team */}
                    <TeamBlock abbr={homeAbbr} name={homeName} score={homeScore} align="right"
                               winner={isFinished && homeScore != null && visitorScore != null && homeScore > visitorScore} />

                    {/* Center */}
                    <div style={s.centerBlock}>
                        {isFinished ? (
                            <div style={s.finalBadge}>FINAL</div>
                        ) : (
                            <div style={s.vsText}>VS</div>
                        )}
                    </div>

                    {/* Visitor team */}
                    <TeamBlock abbr={visitorAbbr} name={visitorName} score={visitorScore} align="left"
                               winner={isFinished && homeScore != null && visitorScore != null && visitorScore > homeScore} />
                </div>
            </div>

            {/* ── Content ── */}
            <div style={{ maxWidth: 1020, margin: "0 auto", padding: "36px 24px 80px" }}>
                {loading ? (
                    <Spinner />
                ) : error ? (
                    <p style={{ color: "#555", textAlign: "center", marginTop: 40 }}>{error}</p>
                ) : (
                    <div style={s.columns}>
                        {showLineups ? (
                            <>
                                <LineupWithStats
                                    teamName={homeName} abbr={homeAbbr}
                                    players={homeLineup} statsMap={statsMap}
                                />
                                <div style={s.divider} />
                                <LineupWithStats
                                    teamName={visitorName} abbr={visitorAbbr}
                                    players={visitorLineup} statsMap={statsMap}
                                    flip
                                />
                            </>
                        ) : (
                            <>
                                <StatsOnlyCol teamName={homeName}    abbr={homeAbbr}    stats={homeStats}    />
                                <div style={s.divider} />
                                <StatsOnlyCol teamName={visitorName} abbr={visitorAbbr} stats={visitorStats} flip />
                            </>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

// ── Team hero block (with optional score) ─────────────────────────────────────
function TeamBlock({ abbr, name, score, align, winner }) {
    return (
        <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: align === "right" ? "flex-end" : "flex-start", gap: 8 }}>
            <img src={logoUrl(abbr)} alt={abbr} onError={e => { e.target.style.display = "none"; }}
                 style={{ width: 68, height: 68, objectFit: "contain" }} />
            <div style={{ textAlign: align }}>
                <div style={{ fontSize: "1.4rem", fontWeight: 900, color: winner ? "#4f7cff" : "#fff" }}>{abbr}</div>
                <div style={{ fontSize: "0.7rem", color: "#555", marginTop: 2 }}>{name}</div>
            </div>
            {score != null && (
                <div style={{
                    fontSize: "2.8rem", fontWeight: 900, lineHeight: 1,
                    color: winner ? "#4f7cff" : "#aaa",
                    textShadow: winner ? "0 0 30px rgba(79,124,255,0.4)" : "none",
                }}>
                    {score}
                </div>
            )}
        </div>
    );
}

// ── Lineup column with stats merged in ───────────────────────────────────────
function LineupWithStats({ teamName, abbr, players, statsMap, flip }) {
    const starters = players.filter(p => p.starter);
    const bench    = players.filter(p => !p.starter);

    return (
        <div style={{ flex: 1, minWidth: 0 }}>
            <ColHeader abbr={abbr} teamName={teamName} flip={flip} />
            {players.length === 0 ? (
                <p style={{ color: "#333", fontSize: "0.8rem", padding: "0 12px" }}>No lineup data.</p>
            ) : (
                <>
                    {starters.length > 0 && <>
                        <GroupLabel flip={flip}>Starters</GroupLabel>
                        {starters.map((e, i) => (
                            <PlayerStatRow key={i} entry={e} stat={statsMap[e.player?.id]} flip={flip} />
                        ))}
                    </>}
                    {bench.length > 0 && <>
                        <GroupLabel flip={flip}>Bench</GroupLabel>
                        {bench.map((e, i) => (
                            <PlayerStatRow key={i} entry={e} stat={statsMap[e.player?.id]} flip={flip} />
                        ))}
                    </>}
                </>
            )}
        </div>
    );
}

// ── Stats-only column (when no lineup data, use stats players) ────────────────
function StatsOnlyCol({ teamName, abbr, stats, flip }) {
    return (
        <div style={{ flex: 1, minWidth: 0 }}>
            <ColHeader abbr={abbr} teamName={teamName} flip={flip} />
            {stats.length === 0 ? (
                <p style={{ color: "#bdbdbd", fontSize: "0.8rem", padding: "0 12px" }}>No stats available.</p>
            ) : (
                <>
                    <GroupLabel flip={flip}>Players</GroupLabel>
                    {stats
                        .sort((a, b) => (b.pts ?? 0) - (a.pts ?? 0))
                        .map((st, i) => (
                            <PlayerStatRow key={i} entry={{ player: { ...st.player, first_name: st.player?.first_name, last_name: st.player?.last_name, id: st.player?.id }, position: st.position, starter: null }} stat={st} flip={flip} />
                        ))}
                </>
            )}
        </div>
    );
}

// ── Column header ─────────────────────────────────────────────────────────────
function ColHeader({ abbr, teamName, flip }) {
    return (
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 16, flexDirection: flip ? "row-reverse" : "row" }}>
            <img src={logoUrl(abbr)} alt={abbr} onError={e => { e.target.style.display = "none"; }}
                 style={{ width: 30, height: 30, objectFit: "contain" }} />
            <div style={{ textAlign: flip ? "right" : "left" }}>
                <div style={{ fontSize: "0.58rem", color: "#4f7cff", fontWeight: 700, letterSpacing: "0.14em", textTransform: "uppercase" }}>Lineup & Stats</div>
                <div style={{ fontSize: "0.88rem", fontWeight: 800 }}>{teamName}</div>
            </div>
        </div>
    );
}

// ── Player row with stats ─────────────────────────────────────────────────────
function PlayerStatRow({ entry, stat, flip }) {
    const [hovered, setHovered] = useState(false);
    const p = entry?.player;
    if (!p) return null;

    const img = p.nbaPlayerId ? `https://cdn.nba.com/headshots/nba/latest/1040x760/${p.id}.png` : null;

    const statCols = [
        { label: "PTS", value: stat?.pts },
        { label: "REB", value: stat?.reb },
        { label: "AST", value: stat?.ast },
        { label: "MIN", value: stat?.min ? String(stat.min).split(":")[0] : null },
    ];

    const hasStats = stat && (stat.pts != null || stat.reb != null || stat.ast != null);

    return (
        <div
            // onMouseEnter={() => setHovered(true)}
            // onMouseLeave={() => setHovered(false)}
            style={{
                padding: "10px 12px", borderRadius: 10, marginBottom: 3,
                background: hovered ? "#111620" : "transparent",
                border: `1px solid ${hovered ? "#4f7cff30" : "transparent"}`,
                transition: "all 0.15s",
            }}
        >
            {/* Top row: photo + name */}
            <div style={{ display: "flex", alignItems: "center", gap: 10, flexDirection: flip ? "row-reverse" : "row" }}>
                {/* Headshot */}
                <div style={{
                    width: 36, height: 36, borderRadius: "50%", overflow: "hidden", flexShrink: 0,
                    background: "#111620", border: `1.5px solid ${hovered ? "#4f7cff40" : "#1e2333"}`,
                    transition: "border-color 0.15s",
                }}>
                    {img ? (
                        <img src={img} alt={`${p.first_name} ${p.last_name}`}
                             style={{ width: "100%", height: "100%", objectFit: "cover", objectPosition: "top" }}
                             onError={e => { e.target.style.display = "none"; }}
                        />
                    ) : (
                        <div style={{ width: "100%", height: "100%", display: "flex", alignItems: "center", justifyContent: "center" }}>
                            <svg width="15" height="15" fill="#333" viewBox="0 0 24 24">
                                <path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z"/>
                            </svg>
                        </div>
                    )}
                </div>

                {/* Name + position */}
                <div style={{ flex: 1, minWidth: 0, textAlign: flip ? "right" : "left" }}>
                    <div style={{ fontSize: "0.82rem", fontWeight: 700, color: hovered ? "#fff" : "#ccc", transition: "color 0.15s", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                        {p.first_name} <span style={{ color: hovered ? "#4f7cff" : "#fff" }}>{p.last_name}</span>
                    </div>
                    <div style={{ display: "flex", alignItems: "center", gap: 4, marginTop: 1, flexDirection: flip ? "row-reverse" : "row" }}>
                        {entry.position && (
                            <span style={{ fontSize: "0.62rem", color: "#555" }}>{entry.position}</span>
                        )}
                        {entry.starter === true && (
                            <span style={{ fontSize: "0.56rem", fontWeight: 700, color: "#4f7cff", background: "#4f7cff15", border: "1px solid #4f7cff25", borderRadius: 3, padding: "1px 5px", letterSpacing: "0.07em", textTransform: "uppercase" }}>
                S
              </span>
                        )}
                    </div>
                </div>
            </div>

            {/* Stats row — only shown if stats exist */}
            {hasStats && (
                <div style={{
                    display: "grid", gridTemplateColumns: "repeat(4, 1fr)",
                    gap: 4, marginTop: 8,
                    flexDirection: flip ? "row-reverse" : "row",
                }}>
                    {statCols.map(({ label, value }) => (
                        <div key={label} style={{
                            background: "#0a0c14", borderRadius: 6, padding: "5px 4px",
                            textAlign: "center", border: "1px solid #1a1f30",
                        }}>
                            <div style={{ fontSize: "0.78rem", fontWeight: 900, color: value > 0 ? "#fff" : "#333" }}>
                                {value ?? "—"}
                            </div>
                            <div style={{ fontSize: "0.56rem", color: "white", letterSpacing: "0.08em", marginTop: 1 }}>
                                {label}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* DNP / no stats fallback */}
            {!hasStats && stat !== undefined && (
                <div style={{ fontSize: "0.62rem", color: "#333", marginTop: 5, paddingLeft: flip ? 0 : 46, paddingRight: flip ? 46 : 0, textAlign: flip ? "right" : "left" }}>
                    DNP
                </div>
            )}
        </div>
    );
}

// ── Reusables ─────────────────────────────────────────────────────────────────
function GroupLabel({ children, flip }) {
    return (
        <div style={{ display: "flex", alignItems: "center", gap: 7, margin: "12px 0 6px", padding: "0 12px", flexDirection: flip ? "row-reverse" : "row" }}>
            <div style={{ width: 3, height: 10, background: "#4f7cff", borderRadius: 2 }} />
            <span style={{ fontSize: "0.58rem", fontWeight: 700, letterSpacing: "0.14em", textTransform: "uppercase", color: "#4f7cff" }}>
        {children}
      </span>
        </div>
    );
}

function Spinner() {
    return (
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 12, padding: "60px 0" }}>
            <div style={{ width: 30, height: 30, borderRadius: "50%", border: "3px solid #1e2333", borderTopColor: "#4f7cff", animation: "spin 0.8s linear infinite" }} />
            <span style={{ color: "#555", fontSize: "0.72rem", letterSpacing: "0.1em", textTransform: "uppercase" }}>Loading game data...</span>
            <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
    );
}

// ── Utils ─────────────────────────────────────────────────────────────────────
function logoUrl(abbr) {
    if (!abbr) return "";
    const ov = { UTA: 1610612762, NOP: 1610612740 };
    if (ov[abbr.toUpperCase()]) return `https://cdn.nba.com/logos/nba/${ov[abbr.toUpperCase()]}/primary/L/logo.svg`;
    return `https://a.espncdn.com/i/teamlogos/nba/500/${abbr.toLowerCase()}.png`;
}


// ── Styles ────────────────────────────────────────────────────────────────────
const s = {
    centered: { minHeight: "100vh", background: "#0a0c14", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 16 },
    nav: {
        position: "sticky", top: 0, zIndex: 100,
        background: "rgba(10,12,20,0.95)", backdropFilter: "blur(14px)",
        borderBottom: "1px solid #1e2333",
        padding: "0 28px", height: 54,
        display: "flex", alignItems: "center", justifyContent: "space-between",
    },
    backBtn: {
        display: "flex", alignItems: "center", gap: 7,
        background: "none", border: "none", cursor: "pointer",
        color: "#666", fontSize: "0.78rem", fontWeight: 700,
        letterSpacing: "0.1em", textTransform: "uppercase", transition: "color 0.2s",
    },
    hero: {
        position: "relative", overflow: "hidden",
        background: "#0d0f1c", borderBottom: "1px solid #1e2333",
        padding: "44px 28px 40px",
    },
    glow: { position: "absolute", width: 260, height: 260, borderRadius: "50%", filter: "blur(70px)", top: "50%", transform: "translateY(-50%)", pointerEvents: "none" },
    heroInner: { position: "relative", maxWidth: 680, margin: "0 auto", display: "flex", alignItems: "center", gap: 16 },
    centerBlock: { display: "flex", flexDirection: "column", alignItems: "center", gap: 6, flexShrink: 0, minWidth: 80 },
    finalBadge: { fontSize: "0.68rem", fontWeight: 900, letterSpacing: "0.18em", color: "#4f7cff", background: "#4f7cff18", border: "1px solid #4f7cff40", borderRadius: 5, padding: "4px 10px" },
    vsText: { fontSize: "1.5rem", fontWeight: 900, color: "#4f7cff", letterSpacing: "0.1em" },
    statusPill: { background: "#111620", border: "1px solid #1e2333", borderRadius: 5, padding: "3px 10px", fontSize: "0.64rem", color: "#ccc", fontWeight: 600 },
    columns: { display: "grid", gridTemplateColumns: "1fr 1px 1fr", gap: 24 },
    divider: { background: "#1e2333", width: 1 },
};