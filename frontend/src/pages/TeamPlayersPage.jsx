import { useLocation, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import PlayerCard from "../components/Playercard.jsx";
import NavBar from "../components/Navbar.jsx";

const API_BASE = "http://localhost:8080";
const API = {
    teams: "http://localhost:8080/teams",
};

export default function TeamPlayersPage() {
    const { state } = useLocation();
    const navigate = useNavigate();
    const team = state?.team;

    const [teamPlayers, setTeamPlayers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activePage, setActivePage] = useState(null);
    const [teams, setTeams] = useState([])

    useEffect(() => {
        fetch(API.teams)
            .then(r => r.json())
            .then(data => setTeams(data.data || data))
            .catch(() => {});
    }, []);

    useEffect(() => {
        if (!team?.teamId) return;

        setLoading(true);
        fetch(`${API_BASE}/bdl/teams/${team.externalApiId}/players`)
            .then(r => r.json())
            .then(data => {
                setTeamPlayers(Array.isArray(data.data) ? data.data : data);
                setLoading(false);
            })
            .catch(err => {
                console.error("Failed to fetch team players:", err);
                setError("Failed to load team players");
                setLoading(false);
            });
    }, [team?.teamId]);

    if (!team) {
        return (

            <div className="flex flex-col items-center justify-center min-h-screen text-center">
                <p className="text-[#555]">No team data found.</p>
                <button
                    onClick={() => navigate("/")}
                    className="mt-4 bg-transparent border-none text-[#666] cursor-pointer text-[0.9rem] font-semibold tracking-[0.08em] flex items-center gap-2 transition-colors hover:text-[#4f7cff] font-[inherit]"
                >
                    ← Back
                </button>
            </div>
        );
    }

    return (

        <div className="min-h-screen bg-[#0a0c14] font-sans text-white">
            <NavBar
                activePage={activePage}
                setActivePage={setActivePage}
                teams={teams}
                onTeamClick={(team) => navigate(`/team/${team.teamId}/players`, { state: { team } })}
            />

            {/* ── Nav ── */}
            <nav className="flex items-center h-14 border-b border-[#111620] pl-6 !mt-5">
                <button
                    onClick={() => navigate(-1)}
                    className="!bg-transparent border-none text-[#666] cursor-pointer text-[0.9rem] font-semibold tracking-[0.08em] flex items-center gap-2 transition-colors duration-200 hover:text-[#4f7cff] font-[inherit]">
                    <svg width="15" height="15" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7"/>
                    </svg>
                    Back
                </button>
            </nav>

            {/* ── Team Header ── */}
            <div className="relative px-5 pt-[60px] pb-10 text-center border-b border-[#111620] overflow-hidden">
                {/* Glow blobs */}
                <div className="absolute top-0 left-[8%] w-[400px] h-[400px] rounded-full pointer-events-none blur-[80px] bg-[rgba(79,124,255,0.12)]" />
                <div className="absolute top-0 right-[8%] w-[400px] h-[400px] rounded-full pointer-events-none blur-[80px] bg-[rgba(79,124,255,0.07)]" />

                <div className="relative z-10">
                    <div className="flex flex-col items-center gap-4">
                        <img
                            src={`https://cdn.nba.com/logos/nba/${team.nbaTeamId}/primary/L/logo.svg`}
                            alt={`${team.teamName} logo`}
                            className="w-[120px] h-[120px]"
                            onError={(e) => { e.target.style.display = "none"; }}
                        />
                        <div className="text-center">
                            <h1 className="text-[2.4rem] font-black text-white mb-2">
                                {team.teamName}
                            </h1>
                            <p className="text-base text-[#888]">
                                {team.city} • {team.conference} • {team.division}
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* ── Players Section ── */}
            <main className="pt-16 pb-10">
                <div className="flex flex-col items-center">
                    <h2 className="text-center text-2xl font-bold tracking-wide text-white !mt-10 !mb-16">
                        Team Players ({teamPlayers.length})
                    </h2>

                    {loading ? (
                        <p className="text-[#555] text-center text-[1.1rem]">Loading team players...</p>
                    ) : error ? (
                        <p className="text-[#f87171] text-center text-[1.1rem]">{error}</p>
                    ) : teamPlayers.length > 0 ? (
                            <div className="grid grid-cols-4 gap-7 w-fit ">
                            {teamPlayers.map((player) => (
                                <div key={player.id} className="w-[265px] ">
                                <PlayerCard
                                    player={{
                                        playerId: player.id,
                                        externalApiId: player.externalApiId ?? player.id,
                                        firstName: player.firstName,
                                        lastName: player.lastName,
                                        position: player.position,
                                        height: player.height,
                                        weight: player.weight,
                                        jerseyNumber: player.jerseyNumber,
                                        nbaPlayerId: player.nbaPlayerId,
                                        isActive: true,
                                        team: {
                                            teamId: team.teamId,
                                            teamName: team.teamName,
                                            abbreviation: team.abbreviation,
                                            city: team.city,
                                            conference: team.conference,
                                            division: team.division,
                                            nbaTeamId:team.nbaTeamId,
                                        },
                                    }}
                                    selected={false}
                                    onAnalyze={(p) => {
                                        // Trigger background search and wait for completion before navigating
                                        const searchQuery = `${p.firstName} ${p.lastName}`.trim();
                                        fetch(`${API_BASE}/api/players/search?q=${encodeURIComponent(searchQuery)}`)
                                            .then(() => {
                                                // Search completed, now navigate to detail page
                                                navigate(`/players/${p.playerId}`, { state: { player: p } });
                                            })
                                            .catch(() => {
                                                // If search fails, navigate anyway (backend bootstrap will handle it)
                                                navigate(`/players/${p.playerId}`, { state: { player: p } });
                                            });
                                    }}
                                />
                                </div>
                            ))}
                        </div>
                    ) : (
                        <p className="text-[#555] text-center text-[1.1rem]">No players found for this team.</p>
                    )}

                </div>
            </main>
        </div>
    );
}