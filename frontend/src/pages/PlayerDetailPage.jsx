import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../Playerdetailpage.css";
import NavBar from "../components/Navbar.jsx";
import { Bar, Line } from "react-chartjs-2";
import {
    Chart as ChartJS,
    BarElement,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Tooltip,
    Legend,
} from "chart.js";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Tooltip, Legend);

const RECENT_GAME_LIMITS = [5, 10, 15];

const API = {
    playerStats: (id, limit) =>
        `http://localhost:8080/stats/player/external/${id}?limit=${limit}`,
    recentAnalyze: (params) =>
        `http://localhost:8080/stats/recent/analyze?${new URLSearchParams(params)}`,
    teams: "http://localhost:8080/teams",
};

function headshot(id) {
    return id ? `https://cdn.nba.com/headshots/nba/latest/1040x760/${id}.png` : null;
}

function logo(nbaTeamId) {
    return `https://cdn.nba.com/logos/nba/${nbaTeamId}/primary/L/logo.svg`;
}

function buildRecentGamesChart(rawStats, statLine) {
    const stats = [...rawStats].reverse();

    return {
        labels: stats.map((stat) => {
            const gameDate = stat?.game?.gameDate;
            return gameDate
                ? new Date(gameDate + "T00:00:00").toLocaleDateString("en-US", { month: "short", day: "numeric" })
                : "—";
        }),
        datasets: [
            {
                label: "PTS",
                data: stats.map((stat) => stat.pointsScored ?? 0),
                borderColor: "#4f7cff",
                backgroundColor: "rgba(79,124,255,0.18)",
                tension: 0.3,
                borderWidth: 2,
                pointRadius: 3,
                pointHoverRadius: 4,
            },
            {
                label: "REB",
                data: stats.map((stat) => stat.totalRebounds ?? 0),
                borderColor: "#47e897",
                backgroundColor: "rgba(71,232,151,0.16)",
                tension: 0.3,
                borderWidth: 2,
                pointRadius: 3,
                pointHoverRadius: 4,
            },
            {
                label: "AST",
                data: stats.map((stat) => stat.assists ?? 0),
                borderColor: "#e8c547",
                backgroundColor: "rgba(232,197,71,0.16)",
                tension: 0.3,
                borderWidth: 2,
                pointRadius: 3,
                pointHoverRadius: 4,
            },
            {
                label: "Stat Line",
                type: "line",
                data: stats.map(() => statLine),
                borderColor: "#ff6b6b",
                backgroundColor: "rgba(255,107,107,0.12)",
                borderWidth: 2,
                borderDash: [6, 6],
                pointRadius: 0,
                pointHoverRadius: 0,
                tension: 0,
                fill: false,
                order: 0,
            },
        ],
    };
}

const recentGamesChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
        legend: {
            labels: {
                color: "#cfd7e6",
                boxWidth: 10,
                boxHeight: 10,
            },
        },
        tooltip: {
            mode: "index",
            intersect: false,
        },
    },
    interaction: {
        mode: "index",
        intersect: false,
    },
    scales: {
        x: {
            grid: {
                color: "rgba(26,37,64,0.45)",
            },
            ticks: {
                color: "#ffffff",
            },
        },
        y: {
            beginAtZero: true,
            grid: {
                color: "rgba(26,37,64,0.45)",
            },
            ticks: {
                color: "#ffffff",
                precision: 0,
            },
        },
    },
};

// ── Raw Game Row ──────────────────────────────────────────────────────────────
function RawGameRow({ stat, index }) {
    const game = stat.game;
    if (!game) return null;

    const date = game.gameDate
        ? new Date(game.gameDate + "T00:00:00").toLocaleDateString("en-US", { month: "short", day: "numeric" })
        : "—";

    const playerTeamId = stat.team?.teamId;
    const isHome = game.homeTeam?.teamId === playerTeamId;
    const opp = isHome ? game.awayTeam : game.homeTeam;
    const pScore = isHome ? game.homeTeamScore : game.awayTeamScore;
    const oScore = isHome ? game.awayTeamScore : game.homeTeamScore;
    const won = pScore > oScore;

    return (
        <div className="flex items-center gap-10 px-10 py-8 bg-[#0a0e1c] border border-[#1a2540] hover:border-[#253660] transition-colors">
            <span className="text-[white] text-xs font-mono w-5 text-right">{index + 1}</span>
            <span className="text-[white] text-sm font-mono w-20">{date}</span>

            <div className="flex items-center gap-2 w-24 mb-4!">
                {opp?.nbaTeamId && (
                    <img src={logo(opp.nbaTeamId)} alt="" className="w-5 h-5 object-contain"
                         onError={(e) => { e.target.style.display = "none"; }} />
                )}
                <span className="text-[white] text-xss font-large">
          {isHome ? "vs" : "vs"} {opp?.abbreviation ?? "—"}
        </span>
            </div>

            <span className={`text-xs font-semibold font-mono w-16 ${won ? "text-green-500" : "text-red-500"}`}>
        {pScore}-{oScore} {won ? "W" : "L"}
      </span>

            <div className="flex gap-5 flex-1 justify-end">
                {[
                    { v: stat.pointsScored, l: "PTS" },
                    { v: stat.totalRebounds, l: "REB" },
                    { v: stat.assists, l: "AST" },
                    { v: stat.steals, l: "STL" },
                    { v: stat.blocks, l: "BLK" },
                    { v: stat.minutesPlayed, l: "MIN" },
                ].map((s) => (
                    <div key={s.l} className="text-center min-w-[32px]">
                        <p className="text-white text-sm font-semibold">{s.v}</p>
                        <p className="text-[#ffffff] text-[10px] uppercase">{s.l}</p>
                    </div>
                ))}
            </div>
        </div>
    );
}

// ── Main ──────────────────────────────────────────────────────────────────────
export default function PlayerDetailPage() {
    const { state } = useLocation();
    const navigate = useNavigate();
    const player = state?.player;
    const resolvedExternalApiId = player?.externalApiId ?? player?.playerId ?? null;

    const [activePage, setActivePage] = useState("PLAYERS");
    const [teams, setTeams] = useState([]);
    const [rawStats, setRawStats] = useState([]);
    const [rawLoading, setRawLoading] = useState(false);
    const [analysisData, setAnalysisData] = useState(null);
    const [analysisLoading, setAnalysisLoading] = useState(false);
    const [rawStatsCache, setRawStatsCache] = useState({});
    const [analysisCache, setAnalysisCache] = useState({});
    const [statLine, setStatLine] = useState(15.5);
    const [recentGamesLimit, setRecentGamesLimit] = useState(5);
    const [chartType, setChartType] = useState("line");
    const [imgErr, setImgErr] = useState(false);

    useEffect(() => {
        fetch(API.teams)
            .then((r) => r.json())
            .then((data) => {
                const allTeams = Array.isArray(data?.data || data) ? (data.data || data) : [];
                setTeams(allTeams);
            })
            .catch(() => setTeams([]));
    }, []);

    useEffect(() => {
        setRawStatsCache({});
        setAnalysisCache({});
        setRawStats([]);
        setAnalysisData(null);
    }, [resolvedExternalApiId]);

    useEffect(() => {
        if (!resolvedExternalApiId) return;
        const cacheKey = `${resolvedExternalApiId}-${recentGamesLimit}`;

        if (cacheKey in rawStatsCache) {
            setRawStats(rawStatsCache[cacheKey]);
            setRawLoading(false);
            return;
        }

        setRawLoading(true);
        fetch(API.playerStats(resolvedExternalApiId, recentGamesLimit))
            .then((r) => r.json())
            .then((d) => {
                const nextStats = Array.isArray(d) ? d : [];
                setRawStats(nextStats);
                setRawStatsCache((prev) => ({ ...prev, [cacheKey]: nextStats }));
            })
            .catch(() => {
                setRawStats([]);
                setRawStatsCache((prev) => ({ ...prev, [cacheKey]: [] }));
            })
            .finally(() => setRawLoading(false));
    }, [resolvedExternalApiId, recentGamesLimit, rawStatsCache]);

    useEffect(() => {
        if (!resolvedExternalApiId) return;
        const cacheKey = `${resolvedExternalApiId}-${recentGamesLimit}-${statLine}`;

        if (cacheKey in analysisCache) {
            setAnalysisData(analysisCache[cacheKey]);
            setAnalysisLoading(false);
            return;
        }

        setAnalysisLoading(true);
        fetch(API.recentAnalyze({
            playerApiId: resolvedExternalApiId,
            statLine,
            limit: recentGamesLimit,
            statType: "pts",
        }))
            .then((r) => r.json())
            .then((data) => {
                setAnalysisData(data);
                setAnalysisCache((prev) => ({ ...prev, [cacheKey]: data }));
            })
            .catch(() => {
                setAnalysisData(null);
                setAnalysisCache((prev) => ({ ...prev, [cacheKey]: null }));
            })
            .finally(() => setAnalysisLoading(false));
    }, [resolvedExternalApiId, statLine, recentGamesLimit, analysisCache]);

    const recentGamesChartData = buildRecentGamesChart(rawStats, statLine);

    if (!player) {
        return (
            <div className="min-h-screen bg-[#060810] flex items-center justify-center ">
                <div className="text-center">
                    <p className="text-[#3a4a6a] text-lg mb-4">No player data found.</p>
                    <button onClick={() => navigate(-1)}
                            className="px-6 py-2 rounded-lg bg-[#4f7cff] text-white text-sm font-semibold">
                        Go Back
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-[#060810] text-white">
            <NavBar
                activePage={activePage}
                setActivePage={setActivePage}
                teams={teams}
                onTeamClick={(team) => navigate(`/team/${team.teamId}/players`, { state: { team } })}
            />
            <div className="player-detail-layout px-6 py-6 flex flex-col gap-5">

                {/* Back */}
                <button onClick={() => navigate(-1)}
                className="bg-transparent! flex items-center gap-1.5 text-[white] hover:text-[#4f7cff] text-sm transition-colors w-fit mt-10!">
                    <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7"/>
                    </svg>
                    Back
                </button>

                {/* ── Hero ── */}
                <div className="relative overflow-hidden border border-[#1a2540] bg-[#0a0e1c] min-h-[280px] flex items-end rounded-[20px]">
                    {/* Headshot */}
                    <div className="absolute right-0 bottom-0 w-[340px] h-full">
                        {!imgErr && player.nbaPlayerId ? (
                            <img src={headshot(player.nbaPlayerId)} alt=""
                                 className="w-full h-full object-contain object-bottom"
                                 onError={() => setImgErr(true)} />
                        ) : (
                            <div
                                className="h-60 w-60 aspect-square flex-shrink-0 flex items-center justify-center text-6xl font-bold text-white bg-gradient-to-br from-[#5d84ff] to-[#4d73ea] rounded-full">
                                {player.firstName?.[0]}{player.lastName?.[0]}
                            </div>
                        )}
                    </div>

                    {/* Gradient */}
                    {/*<div className="absolute inset-0 bg-gradient-to-r from-[#0a0e1c] via-[#0a0e1c] to-transparent z-[1]" />*/}
                    {/*<div className="absolute inset-0 bg-gradient-to-t from-[#0a0e1c] to-transparent z-[1]" />*/}

                    {/* Info */}
                    <div className="relative z-[2] p-8">
                        <div className="flex items-center gap-2 mb-3">
                            {player.team?.nbaTeamId && (
                                <img src={logo(player.team.nbaTeamId)} alt="" className="w-40 h-40 object-contain !ml-3"
                                     onError={(e) => { e.target.style.display = "none"; }} />
                            )}
                            <span className="text-[#4f7cff] text-xsl font-semibold uppercase tracking-wider  ">
                {player.team?.teamName ?? "—"}
              </span>
                        </div>
                        <h1 className="text-4xl font-bold text-white !mb-1 ml-3!">
                            {player.firstName} <span>{player.lastName}</span>
                        </h1>
                        <p className="text-[white] text-xl !mb-5 ml-4!">
                            {player.position ?? "—"} · #{player.jerseyNumber ?? "—"}
                        </p>
                    </div>
                </div>

                {/* ── Bio ── */}
                <div className="border border-[#1a2540] bg-[#0a0e1c] p-6 rounded-[20px] overflow-hidden">
                    <p className="text-[white] text-xs font-semibold uppercase tracking-widest !mb-4 !mt-3 !ml-2">Player Info</p>
                    <div className="grid grid-cols-4 gap-4 !ml-4 mb-4!">
                        {[
                            { l: "Position", v: player.position ?? "—" },
                            { l: "Height", v: player.height ?? "—" },
                            { l: "Weight", v: player.weight ? `${player.weight} lbs` : "—" },
                            { l: "Jersey", v: `#${player.jerseyNumber ?? "—"}` },
                            { l: "Team", v: player.team?.teamName ?? "—" },
                            { l: "Conference", v: player.team?.conference ?? "—" },
                            { l: "Division", v: player.team?.division ?? "—" },
                            { l: "Status", v: player.isActive ? "Active" : "Not Active" },
                        ].map((item) => (
                            <div key={item.l} className="py-2 border-b border-[#111825]">
                                <p className="text-[white] text-[10px] font-semibold uppercase tracking-wider mb-1!">{item.l}</p>
                                <p className={`text-base font-bold ${item.color ?? "text-white mb-1! "}`}>{item.v}</p>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="border border-[#1a2540] bg-[#0a0e1c] px-10 py-8 rounded-[20px] overflow-hidden">
                    <div className="flex items-start justify-between gap-10 !mb-6 !mt-5 flex-wrap">
                        <div className="flex flex-col gap-3">
                            <p className="text-[white] text-xs font-semibold uppercase tracking-widest ml-4!">Recent Games Chart</p>
                            <div className="flex items-center gap-2 flex-wrap">
                                <p className="text-[white] text-xs font-semibold uppercase tracking-widest ml-4!">Number of Games: </p>
                                {RECENT_GAME_LIMITS.map((limit) => (
                                    <button
                                        key={limit}
                                        type="button"
                                        onClick={() => setRecentGamesLimit(limit)}
                                        className="px-3 py-1.5 text-xs font-semibold border border-[#1a2540] transition-colors rounded-md"
                                        style={{
                                            background: recentGamesLimit === limit ? "#4f7cff" : "transparent",
                                            color: recentGamesLimit === limit ? "#fff" : "#fff",
                                            borderColor: recentGamesLimit === limit ? "#4f7cff" : "#1a2540",
                                        }}
                                    >
                                        {limit}
                                    </button>
                                ))}
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            <div className="flex items-center gap-2">
                                <span className="text-[white] text-[12px] font-bold uppercase tracking-wider">Stat Line</span>
                                <input
                                    type="number"
                                    step="0.5"
                                    value={statLine}
                                    onChange={(e) => setStatLine(parseFloat(e.target.value) || " ")}
                                    className="player-detail-line-input rounded-full "
                                />
                            </div>
                            <button
                                onClick={() => setChartType("line")}
                                className="px-3 py-1.5 text-xs font-semibold border border-[white]! transition-colors"
                                style={{
                                    background: chartType === "line" ? "#4f7cff" : "transparent",
                                    color: chartType === "line" ? "#fff" : "#fff",
                                }}
                            >
                                Line
                            </button>
                            <button
                                onClick={() => setChartType("bar")}
                                className="px-3 py-1.5 text-xs font-semibold border border-[white]! transition-colors  mr-4!"
                                style={{
                                    background: chartType === "bar" ? "#4f7cff" : "transparent",
                                    color: chartType === "bar" ? "#fff" : "#fff",
                                }}
                            >
                                Bar
                            </button>
                        </div>
                    </div>
                    {analysisLoading ? (
                        <div className="flex items-center justify-center py-10 mt-2 border-b border-[#111825] mb-6">
                            <div className="w-6 h-6 border-2 border-[#4f7cff] border-t-transparent rounded-full animate-spin" />
                        </div>
                    ) : analysisData && (
                        <div className="grid grid-cols-4 gap-4 mt-3 mb-6 pb-6 border-b border-[#111825]">
                            <div className="py-2">
                                <p className="text-[#ffffff] text-[10px] uppercase tracking-wider mb-1! ml-2!">Avg PTS</p>
                                <p className="text-base font-semibold text-white mb-1! ml-2!">{analysisData.average ?? "—"}</p>
                            </div>
                            <div className="py-2">
                                <p className="text-[#ffffff] text-[10px] uppercase tracking-wider mb-1! ml-2!">Hit Rate</p>
                                <p className="text-base font-semibold text-white mr-5!">
                                    {typeof analysisData.hitRate === "number" ? `${Math.round(analysisData.hitRate * 100)}%` : "—"}
                                </p>
                            </div>
                            <div className="py-2">
                                <p className="text-[#ffffff] text-[10px] uppercase tracking-wider mb-1">Hit Count</p>
                                <p className="text-base font-semibold text-white">
                                    {analysisData.hitCount ?? "—"}/{analysisData.totalGames ?? "—"}
                                </p>
                            </div>
                            <div className="py-2">
                                <p className="text-[#ffffff] text-[10px] uppercase tracking-wider mb-1">Std Dev</p>
                                <p className="text-base font-semibold text-white">{analysisData.standardDeviation ?? "—"}</p>
                            </div>
                        </div>
                    )}
                    <div style={{ height: 320  }} className="mb-4! mt-4!">
                        {rawLoading ? (
                            <div className="flex items-center justify-center h-full">
                                <div className="w-6 h-6 border-2 border-[#4f7cff] border-t-transparent rounded-full animate-spin" />
                            </div>
                        ) : rawStats.length === 0 ? (
                            <p className="text-[#2a3a5a] text-sm text-center py-10">No chart data available</p>
                        ) : (
                            chartType === "bar"
                                ? <Bar data={recentGamesChartData} options={recentGamesChartOptions} />
                                : <Line data={recentGamesChartData} options={recentGamesChartOptions} />
                        )}
                    </div>
                </div>

                {/* ── Recent Games ── */}
                <div className="border border-[#1a2540] bg-[#0a0e1c] rounded-[20px] overflow-hidden">
                    <div className="px-10 py-8 border-b border-[#111825]">
                        <p className="text-[white] text-xs font-semibold uppercase tracking-widest mt-5! mb-4! ml-2!">Recent Games</p>
                    </div>
                    <div className="p-10 flex flex-col gap-5">
                        {rawLoading ? (
                            <div className="flex items-center justify-center px-10 py-10">
                                <div className="w-6 h-6 border-2 border-[#4f7cff] border-t-transparent rounded-full animate-spin" />
                            </div>
                        ) : rawStats.length === 0 ? (
                            <p className="text-[#2a3a5a] text-sm text-center py-10">No recent game data</p>
                        ) : (
                            rawStats.map((s, i) => <RawGameRow key={s.statisticId ?? i} stat={s} index={i} />)
                        )}
                    </div>
                </div>

            </div>
        </div>
    );
}
