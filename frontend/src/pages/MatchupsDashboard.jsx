import {useEffect, useState} from "react";
import { useNavigate } from "react-router-dom";
import NavBar from "../components/Navbar.jsx";

const API = {
    teams: "http://localhost:8080/teams",
};

export default function MatchupsDashboard() {
    const [activePage, setActivePage] = useState("MATCHUPS");
    const [teams, setTeams] = useState([])
    const navigate = useNavigate();

    useEffect(() => {
        fetch(API.teams)
            .then(r => r.json())
            .then(data => setTeams(data.data || data))
            .catch(() => {});
    }, []);

    return (
        <div style={{ minHeight: "100vh", background: "#0a0c14", color: "#fff" }}>
            <NavBar
                activePage={activePage}
                setActivePage={setActivePage}
                teams={teams}
                onTeamClick={(team) => navigate(`/team/${team.teamId}/players`, { state: { team } })}
            />

            <div style={{ padding: "40px" }}>
                <h1>Players Page</h1>
                <p>Player A VS Player B </p>
            </div>
        </div>
    );
}