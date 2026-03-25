import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../Playerdetailpage.css";
import NavBar from "../components/Navbar.jsx";

const API = {
    playerStats: (id, limit) =>
        `http://localhost:8080/stats/player/external/${id}?limit=${limit}`,
    teams: "http://localhost:8080/teams",
};

function headshot(id) {
    return id ? `https://cdn.nba.com/headshots/nba/latest/1040x760/${id}.png` : null;
}

function logo(nbaTeamId) {
    return `https://cdn.nba.com/logos/nba/${nbaTeamId}/primary/L/logo.svg`;
}

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
            <span className="text-[#667594] text-xs font-mono w-5 text-right">{index + 1}</span>
            <span className="text-[#667594] text-sm font-mono w-20">{date}</span>

            <div className="flex items-center gap-2 w-24">
                {opp?.nbaTeamId && (
                    <img src={logo(opp.nbaTeamId)} alt="" className="w-5 h-5 object-contain"
                         onError={(e) => { e.target.style.display = "none"; }} />
                )}
                <span className="text-[#667594] text-xss font-large">
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
                        <p className="text-[#8bb5b4] text-[10px] uppercase">{s.l}</p>
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

    const [activePage, setActivePage] = useState("PLAYERS");
    const [teams, setTeams] = useState([]);
    const [rawStats, setRawStats] = useState([]);
    const [rawLoading, setRawLoading] = useState(false);
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
        if (!player) return;
        setRawLoading(true);
        fetch(API.playerStats(player.externalApiId, 5))
            .then((r) => r.json())
            .then((d) => setRawStats(Array.isArray(d) ? d : []))
            .catch(() => setRawStats([]))
            .finally(() => setRawLoading(false));
    }, [player]);

    if (!player) {
        return (
            <div className="min-h-screen bg-[#060810] flex items-center justify-center">
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
                        className="flex items-center gap-1.5 text-[white] hover:text-[#4f7cff] text-sm transition-colors w-fit">
                    <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7"/>
                    </svg>
                    Back
                </button>

                {/* ── Hero ── */}
                <div className="relative overflow-hidden border border-[#1a2540] bg-[#0a0e1c] min-h-[280px] flex items-end">
                    {/* Headshot */}
                    <div className="absolute right-0 bottom-0 w-[340px] h-full">
                        {!imgErr && player.nbaPlayerId ? (
                            <img src={headshot(player.nbaPlayerId)} alt=""
                                 className="w-full h-full object-contain object-bottom"
                                 onError={() => setImgErr(true)} />
                        ) : (
                            <div className="w-full h-full flex items-center justify-center text-6xl font-bold text-[#1a2540]">
                                {player.firstName?.[0]}{player.lastName?.[0]}
                            </div>
                        )}
                    </div>

                    {/* Gradient */}
                    <div className="absolute inset-0 bg-gradient-to-r from-[#0a0e1c] via-[#0a0e1cee] to-transparent z-[1]" />
                    <div className="absolute inset-0 bg-gradient-to-t from-[#0a0e1c] to-transparent z-[1]" />

                    {/* Info */}
                    <div className="relative z-[2] p-8">
                        <div className="flex items-center gap-2 mb-3">
                            {player.team?.nbaTeamId && (
                                <img src={logo(player.team.nbaTeamId)} alt="" className="w-6 h-6 object-contain"
                                     onError={(e) => { e.target.style.display = "none"; }} />
                            )}
                            <span className="text-[#4f7cff] text-xs font-semibold uppercase tracking-wider">
                {player.team?.teamName ?? "—"}
              </span>
                        </div>
                        <h1 className="text-4xl font-bold text-white mb-1">
                            {player.firstName} <span>{player.lastName}</span>
                        </h1>
                        <p className="text-[#8bb5b4] text-sm">
                            {player.position ?? "—"} · #{player.jerseyNumber ?? "—"}
                        </p>
                    </div>
                </div>

                {/* ── Bio ── */}
                <div className="border border-[#1a2540] bg-[#0a0e1c] p-6">
                    <p className="text-[white] text-xs font-semibold uppercase tracking-widest mb-4">Player Info</p>
                    <div className="grid grid-cols-4 gap-4">
                        {[
                            { l: "Position", v: player.position ?? "—" },
                            { l: "Height", v: player.height ?? "—" },
                            { l: "Weight", v: player.weight ? `${player.weight} lbs` : "—" },
                            { l: "Jersey", v: `#${player.jerseyNumber ?? "—"}` },
                            { l: "Team", v: player.team?.teamName ?? "—" },
                            { l: "Conference", v: player.team?.conference ?? "—" },
                            { l: "Division", v: player.team?.division ?? "—" },
                            { l: "Status", v: player.isActive ? "N/A" : "N/A" },
                        ].map((item) => (
                            <div key={item.l} className="py-2 border-b border-[#111825]">
                                <p className="text-[#8bb5b4] text-[10px] uppercase tracking-wider mb-1">{item.l}</p>
                                <p className={`text-base font-semibold ${item.color ?? "text-white"}`}>{item.v}</p>
                            </div>
                        ))}
                    </div>
                </div>

                {/* ── Recent Games ── */}
                <div className="border border-[#1a2540] bg-[#0a0e1c] overflow-hidden">
                    <div className="px-10 py-8 border-b border-[#111825]">
                        <p className="text-[white] text-xs font-semibold uppercase tracking-widest">Recent Games</p>
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