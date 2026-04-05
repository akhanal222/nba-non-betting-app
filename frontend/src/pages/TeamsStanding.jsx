import { useState, useMemo, useEffect, useCallback } from "react";
import NavBar from "../components/Navbar.jsx";
import { useNavigate } from "react-router-dom";

const API_BASE = "http://localhost:8080";
const teamsUrl = "http://localhost:8080/teams";

const RankBadge = ({ rank }) => {
    const colors = rank === 1 ? { bg: "#F6B100", text: "#1a1a2e" }
        : rank === 2 ? { bg: "#A0AEC0", text: "#1a1a2e" }
            : rank === 3 ? { bg: "#CD7F32", text: "#1a1a2e" }
                : rank > 25 ? { bg: "transparent", text: "#718096" }
                : { bg: "transparent", text: "#718096" };
    return (
        <div style={{
            width: 28, height: 28, borderRadius: "50%",
            display: "flex", alignItems: "center", justifyContent: "center",
            background: colors.bg, color: colors.text,
            fontSize: 12, fontWeight: 700,
            border: rank > 3 ? "1px solid rgba(255,255,255,0.08)" : "none",
            fontFamily: "'JetBrains Mono', monospace",
        }}>
            {rank}
        </div>
    );
};

const DiffBadge = ({ diff }) => {
    const col = diff > 0 ? "#48BB78" : diff < 0 ? "#FC8181" : "#718096";
    return (
        <span style={{
            color: col, fontVariantNumeric: "tabular-nums",
            fontSize: 13, fontWeight: 600,
            fontFamily: "'JetBrains Mono', monospace",
        }}>
            {diff > 0 ? "+" : ""}{diff}
        </span>
    );
};

const Spinner = () => (
    <div style={{ display: "flex", justifyContent: "center", padding: 60 }}>
        <div style={{
            width: 36, height: 36, border: "3px solid rgba(255,255,255,0.1)",
            borderTopColor: "#F6B100", borderRadius: "50%",
            animation: "spin 0.8s linear infinite",
        }} />
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
);

const Toast = ({ message, type, onDismiss }) => {
    useEffect(() => {
        const t = setTimeout(onDismiss, 4000);
        return () => clearTimeout(t);
    }, [onDismiss]);
    const bg = type === "success" ? "rgba(72,187,120,0.15)" : type === "error" ? "rgba(252,129,129,0.15)" : "rgba(246,177,0,0.15)";
    const border = type === "success" ? "#48BB78" : type === "error" ? "#FC8181" : "#4f7cff";
    const icon = type === "success" ? "\u2713" : type === "error" ? "\u2715" : "\u27F3";
    return (
        <div style={{
            position: "fixed", top: 20, right: 20, zIndex: 1000,
            background: bg, border: `1px solid ${border}33`,
            borderRadius: 10, padding: "12px 20px",
            display: "flex", alignItems: "center", gap: 10,
            animation: "slideIn 0.3s ease-out",
            color: "#e6edf3", fontSize: 13, fontFamily: "'DM Sans', sans-serif",
        }}>
            <style>{`@keyframes slideIn { from { opacity:0; transform:translateY(-10px); } to { opacity:1; transform:translateY(0); } }`}</style>
            <span style={{ color: border, fontWeight: 700, fontSize: 16 }}>{icon}</span>
            {message}
        </div>
    );
};

export default function NBAStandings() {
    const navigate = useNavigate();
    const [activePage, setActivePage] = useState("STANDINGS");
    const [teams, setTeams] = useState([]);
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(null);
    const [error, setError] = useState(null);
    const [toast, setToast] = useState(null);
    const [conf, setConf] = useState("All");
    const [hoveredRow, setHoveredRow] = useState(null);

    const fetchStandings = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await fetch(`${API_BASE}/standings/2025`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const json = await res.json();
            setData(Array.isArray(json) ? json : []);
        } catch (e) {
            setError(e.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { fetchStandings(); }, [fetchStandings]);

    useEffect(() => {
        fetch(teamsUrl)
            .then(r => r.json())
            .then(data => setTeams(data.data || data))
            .catch(() => {});
    }, []);

    const importSeason = async () => {
        setActionLoading("import");
        try {
            const res = await fetch(`${API_BASE}/bdl/games/import-season?season=2025`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            setToast({ message: "Season games imported successfully", type: "success" });
        } catch (e) {
            setToast({ message: `Import failed: ${e.message}`, type: "error" });
        } finally {
            setActionLoading(null);
        }
    };

    const refreshStandings = async () => {
        setActionLoading("refresh");
        try {
            const res = await fetch(`${API_BASE}/standings/refresh/2025`, { method: "POST" });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            setToast({ message: "Standings refreshed", type: "success" });
            await fetchStandings();
        } catch (e) {
            setToast({ message: `Refresh failed: ${e.message}`, type: "error" });
        } finally {
            setActionLoading(null);
        }
    };


    const filtered = useMemo(() => {
        let d = [...data];
        if (conf !== "All") {
            // Try both full names and abbreviations
            const confMap = { "East": ["Eastern", "East"], "West": ["Western", "West"] };
            const possibleValues = confMap[conf] || [];
            d = d.filter(t => possibleValues.includes(t.team.conference));
        }
        return d;
    }, [data, conf]);

    const leaderWins = filtered.length > 0 ? Math.max(...filtered.map(t => t.wins)) : 0;

    // Dynamically calculate which teams should be highlighted red
    const getTopRowStyle = (rank, totalTeams) => {
        if (rank === 1) return { background: "rgba(246, 177, 0, 0.06)", borderLeft: "3px solid #F6B100" };
        if (rank === 2) return { background: "rgba(160, 174, 192, 0.05)", borderLeft: "3px solid #A0AEC0" };
        if (rank === 3) return { background: "rgba(205, 127, 50, 0.05)", borderLeft: "3px solid #CD7F32" };
        // Last 8 teams in full league (30 teams), or last 4 teams in filtered views (15 teams)
        const lastTeamsCount = totalTeams === 30 ? 10 : 5;
        if (rank > totalTeams - lastTeamsCount) return { background: "rgba(205, 127, 50, 0.05)", borderLeft: "3px solid red" };
        return { background: "transparent", borderLeft: "3px solid transparent" };
    };

    const columns = [
        { key: "rank", label: "#", w: 50 },
        { key: "team", label: "TEAM", w: null },
        { key: "gamesPlayed", label: "GP", w: 55 },
        { key: "wins", label: "W", w: 50 },
        { key: "winPercentage", label: "W%", w: 50 },
        { key: "losses", label: "L", w: 50 },
        { key: "pointDifference", label: "DIFF", w: 70 },
        { key: "gb", label: "GB", w: 55 },
    ];


    const ActionBtn = ({ onClick, loading: isLoading, label, icon }) => (
        <button onClick={onClick} disabled={!!actionLoading} style={{
            padding: "8px 16px", borderRadius: 8, border: "1px solid rgba(255,255,255,0.1)",
            fontSize: 12, fontWeight: 600, cursor: actionLoading ? "not-allowed" : "pointer",
            fontFamily: "'DM Sans', sans-serif", letterSpacing: "0.02em",
            background: "rgba(255,255,255,0.04)", color: "#8b949e",
            transition: "all 0.2s", display: "flex", alignItems: "center", gap: 6,
            opacity: actionLoading && !isLoading ? 0.4 : 1,
        }}>
            {isLoading ? (
                <div style={{
                    width: 14, height: 14, border: "2px solid rgba(255,255,255,0.1)",
                    borderTopColor: "#4f7cff", borderRadius: "50%",
                    animation: "spin 0.8s linear infinite",
                }} />
            ) : (
                <span style={{ fontSize: 14 }}>{icon}</span>
            )}
            {label}
        </button>
    );

    return (
        <div style={{
            minHeight: "100vh",
            background: "#0b1020",
            color: "#e6edf3",
            fontFamily: "'DM Sans', 'Segoe UI', sans-serif",
        }}>
            <NavBar
                activePage={activePage}
                setActivePage={setActivePage}
                teams={teams}
                onTeamClick={(team) => navigate(`/team/${team.teamId}/players`, { state: { team } })}
            />
            <div style={{ padding: "32px 20px" }}>
                <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=JetBrains+Mono:wght@400;600;700&family=Outfit:wght@700;800;900&display=swap" rel="stylesheet" />
                <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>

                {toast && <Toast message={toast.message} type={toast.type} onDismiss={() => setToast(null)} />}

                {/* Header */}
                <div style={{ maxWidth: 1100, margin: "0 auto 28px" }}>
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 16, marginBottom: 4 }}>
                        <div style={{ display: "flex", alignItems: "baseline", gap: 12 }}>
                            <h1 style={{
                                fontFamily: "'Outfit', sans-serif", fontWeight: 100, fontSize: 38
                            }}>
                                Teams Standings
                            </h1>
                        </div>
                        <div style={{ display: "flex", gap: 8 }}>
                            <ActionBtn onClick={importSeason} loading={actionLoading === "import"} label="Import Season"  />
                            <ActionBtn onClick={refreshStandings} loading={actionLoading === "refresh"} label="Refresh Standings"  />
                        </div>
                    </div>
                    <div/>

                    {/* Conference Filter */}
                    <div style={{ display: "flex", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
                        {["All", "East", "West"].map(c => (
                            <button key={c} onClick={() => setConf(c)} style={{
                                padding: "8px 20px", borderRadius: 8, border: "none",
                                fontSize: 13, fontWeight: 600, cursor: "pointer",
                                fontFamily: "'DM Sans', sans-serif", letterSpacing: "0.04em",
                                background: conf === c ? "#3d5ce8" : "rgba(255,255,255,0.05)",
                                color: conf === c ? "#fff" : "#8b949e",
                                transition: "all 0.2s",
                            }}>
                                {c === "All" ? "ALL TEAMS" : c.toUpperCase() + "ERN"}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Table */}
                <div style={{
                    maxWidth: 1100, margin: "0 auto",
                    background: "#212536",
                    border: "1px solid rgba(255,255,255,0.06)",
                    borderRadius: 16, overflow: "hidden",
                }}>
                    {/* Table Header */}
                    <div style={{
                        display: "flex", alignItems: "center", padding: "14px 24px",
                        borderBottom: "1px solid rgba(255,255,255,0.06)",
                        background: "rgba(255,255,255,0.02)",
                    }}>
                        {columns.map(col => (
                            <div key={col.key} style={{
                                width: col.w || undefined, flex: col.w ? undefined : 1,
                                fontSize: 11, fontWeight: 700, letterSpacing: "0.08em",
                                color: "white",
                                fontFamily: "'JetBrains Mono', monospace",
                                textAlign: col.key === "team" ? "left" : "center",
                                userSelect: "none",
                            }}>
                                {col.label}
                            </div>
                        ))}
                    </div>

                    {/* Content */}
                    {loading ? <Spinner /> : error ? (
                        <div style={{ padding: "48px 24px", textAlign: "center" }}>
                            <div style={{ fontSize: 40, marginBottom: 12 }}>{"\u26A0"}</div>
                            <div style={{ color: "#FC8181", fontSize: 15, fontWeight: 600, marginBottom: 6 }}>
                                Failed to load standings
                            </div>
                            <div style={{ color: "#555c68", fontSize: 13, marginBottom: 16 }}>{error}</div>
                            <div style={{ color: "#555c68", fontSize: 12, lineHeight: 1.6 }}><span style={{ color: "#8b949e", fontFamily: "'JetBrains Mono', monospace" }}>{API_BASE}</span>
                                <br />
                            </div>
                            <button onClick={fetchStandings} style={{
                                marginTop: 16, padding: "8px 20px", borderRadius: 8, border: "none",
                                background: "rgba(246,177,0,0.15)", color: "#4f7cff",
                                fontSize: 13, fontWeight: 600, cursor: "pointer",
                                fontFamily: "'DM Sans', sans-serif",
                            }}>
                                Try Again
                            </button>
                        </div>
                    ) : filtered.length === 0 ? (
                        <div style={{ padding: "48px 24px", textAlign: "center", color: "#555c68", fontSize: 14 }}>
                            No teams found
                        </div>
                    ) : (
                        filtered.map((row, i) => {
                            const rank = i + 1;
                            const gb = i === 0 ? "\u2014"
                                : (((filtered[0].wins - row.wins) + (row.losses - filtered[0].losses)) / 2).toFixed(1);
                            const isHovered = hoveredRow === i;
                            const topStyle = getTopRowStyle(rank, filtered.length);

                            return (
                                <div key={row.standingId}
                                     onMouseEnter={() => setHoveredRow(i)}
                                     onMouseLeave={() => setHoveredRow(null)}
                                     style={{
                                         display: "flex", alignItems: "center",
                                         padding: "12px 24px",
                                         borderBottom: i < filtered.length - 1 ? "1px solid rgba(255,255,255,0.03)" : "none",
                                         background: isHovered ? "rgba(255,255,255,0.04)" : topStyle.background,
                                         borderLeft: topStyle.borderLeft,
                                         transition: "background 0.15s", cursor: "default",
                                     }}
                                >
                                    <div style={{ width: 50, display: "flex", justifyContent: "center" }}>
                                        <RankBadge rank={rank} />
                                    </div>

                                    <div style={{ flex: 1, display: "flex", alignItems: "center", gap: 12 }}>
                                        <div style={{
                                            width: 36, height: 36, borderRadius: 10,
                                            background: "rgb(210,211,220)",
                                            border: "1px solid rgba(255,255,255,0.08)",
                                            display: "flex", alignItems: "center", justifyContent: "center",
                                            overflow: "hidden",
                                        }}>
                                            {row.team.nbaTeamId ? (
                                                <img
                                                    src={`https://cdn.nba.com/logos/nba/${row.team.nbaTeamId}/primary/L/logo.svg`}
                                                    alt={row.team.abbreviation}
                                                    style={{ width: "100%", height: "100%", objectFit: "contain" }}
                                                />
                                            ) : (
                                                <span style={{
                                                    fontFamily: "'JetBrains Mono', monospace",
                                                    fontSize: 11, fontWeight: 700, color: "#8b949e",
                                                }}>
                                                    {row.team.abbreviation}
                                                </span>
                                            )}
                                        </div>
                                        <div>
                                            <div style={{ fontWeight: 600, fontSize: 14, lineHeight: 1.3 }}>
                                                {row.team.city}{" "}
                                                <span style={{ color: "#e6edf3" }}>
                                                    {row.team.teamName.replace(row.team.city, "").trim()}
                                                </span>
                                            </div>
                                            <div style={{ fontSize: 11, color: "#555c68", fontFamily: "'JetBrains Mono', monospace" }}>
                                                {row.team.division}
                                            </div>
                                        </div>
                                    </div>

                                    <div style={{ width: 55, textAlign: "center", fontSize: 13, color: "#8b949e", fontVariantNumeric: "tabular-nums" }}>
                                        {row.gamesPlayed}
                                    </div>

                                    <div style={{
                                        width: 50, textAlign: "center", fontSize: 14,
                                        fontWeight: row.wins === leaderWins ? 700 : 500,
                                        color: row.wins === leaderWins ? "#48BB78" : "#e6edf3",
                                        fontVariantNumeric: "tabular-nums",
                                    }}>
                                        {row.wins}
                                    </div>
                                    <div style={{ width: 50, textAlign: "center", fontSize: 14, color: "#8b949e", fontVariantNumeric: "tabular-nums" }}>
                                        {(row.winPercentage * 100).toFixed(1) + "%"}
                                    </div>

                                    <div style={{ width: 50, textAlign: "center", fontSize: 14, color: "#8b949e", fontVariantNumeric: "tabular-nums" }}>
                                        {row.losses}
                                    </div>


                                    <div style={{ width: 70, textAlign: "center" }}>
                                        <DiffBadge diff={row.pointDifference} />
                                    </div>

                                    <div style={{
                                        width: 55, textAlign: "center", fontSize: 13,
                                        fontFamily: "'JetBrains Mono', monospace",
                                        color: gb === "\u2014" ? "#F6B100" : "#718096",
                                        fontWeight: gb === "\u2014" ? 700 : 400,
                                    }}>
                                        {gb}
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            </div>
        </div>
    );
}
