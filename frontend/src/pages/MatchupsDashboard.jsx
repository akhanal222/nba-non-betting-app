import { useState } from "react";
import { useNavigate } from "react-router-dom";
import NavBar from "../components/Navbar.jsx";

export default function MatchupsDashboard() {
    const [activePage, setActivePage] = useState("MATCHUPS");
    const navigate = useNavigate();

    return (
        <div style={{ minHeight: "100vh", background: "#0a0c14", color: "#fff" }}>
            <NavBar
                activePage={activePage}
                setActivePage={setActivePage}
                teams={[]}
                onTeamClick={(team) => navigate(`/team/${team.teamId}/players`, { state: { team } })}
            />

            <div style={{ padding: "40px" }}>
                <h1>Players Page</h1>
                <p>Player A VS Player B </p>
            </div>
        </div>
    );
}