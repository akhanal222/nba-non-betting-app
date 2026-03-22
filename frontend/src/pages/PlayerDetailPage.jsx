import { useLocation, useNavigate } from "react-router-dom";
import AnalyzePanel from "../components/Analyzepanel.jsx";

export default function PlayerDetailPage() {
    const { state } = useLocation();
    const navigate = useNavigate();
    const player = state?.player;

    if (!player) {
        return (
            <div style={{ minHeight: "100vh", background: "#0a0c14", color: "#fff", padding: 40 }}>
                <p>No player data found.</p>
                <button onClick={() => navigate("/players")}>Back</button>
            </div>
        );
    }

    return (
        <div style={{ minHeight: "100vh" }}>
            <button
                onClick={() => navigate(-1)}
                onMouseEnter={e => e.currentTarget.style.color = "#4f7cff"}
                onMouseLeave={e => e.currentTarget.style.color = "#666"}
            >
                <svg width="15" height="15" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7"/>
                </svg>
                Back
            </button>

            <AnalyzePanel player={player} />
        </div>
    );
}