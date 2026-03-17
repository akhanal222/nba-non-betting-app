import { useState } from "react";
import { useNavigate } from "react-router-dom";
import NavBar from "../components/Navbar.jsx";

export default function PlayerProfile() {
    const [activePage, setActivePage] = useState("PLAYERS");
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
                <p>This is the new page opened from the navbar.</p>
            </div>
        </div>
    );
}