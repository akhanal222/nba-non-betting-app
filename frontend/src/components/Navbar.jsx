import { useState } from "react";
import NbaLogo from "../assets/logo.png";

// Main navigation buttons shown in the top bar
const NAV_ITEMS = ["DASHBOARD", "PLAYERS", "MATCHUPS", "PREDICTIONS"];

 function NavBar({ activePage, setActivePage, teams }) {
    // Controls whether the teams list panel is visible
  const [showTeams, setShowTeams] = useState(false);

  return (
    <>
{/*       Top navigation bar (logo + nav items + teams toggle */}
      <nav style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        height: 56,
        borderBottom: "1px solid #111620",
        gap: 40,
      }}>
{/*    Logo of the app */}
          <div style={{ position: "absolute", left: 24, marginTop:30, display: "flex", alignItems: "center" }}>
            <img
              src={NbaLogo}
              alt="NBA Logo"
              style={{ height: 100, width: "auto" }}
            />
          </div>

        {NAV_ITEMS.map((item) => (
          <button key={item} className={`nav-btn${item === "DASHBOARD" ? " active" : ""}`}>
            {item}
          </button>
        ))}

{/*                Toggle button to show/hide the teams panel on the top right */}
        <button
          className="nav-btn"
          onClick={() => setShowTeams((prev) => !prev)}
          style={{
            background: showTeams ? "#2a3be0" : "transparent",
            border: "1.5px solid #2a2f44",
            color: showTeams ? "#fff" : "#888",
            borderRadius: 8,
            padding: "5px 14px",
            fontSize: "0.8rem",
            fontWeight: 700,
            letterSpacing: "0.08em",
            cursor: "pointer",
            transition: "all 0.2s",
          }}

       // Hover effect only when teams panel is closed
          onMouseEnter={e => { if (!showTeams) { e.currentTarget.style.borderColor = "#4f7cff"; e.currentTarget.style.color = "#fff"; }}}
          onMouseLeave={e => { if (!showTeams) { e.currentTarget.style.borderColor = "#2a2f44"; e.currentTarget.style.color = "#888"; }}}
        >
          {showTeams ? "Hide Teams" : "All Teams"}
        </button>
      </nav>
        {/* Home / Analysis Tabs */}
        <div style={{ padding: "14px 0", display: "flex", gap: 10, marginTop:30, borderBottom: "1px solid #111620" }}>
            {["Home", "Analysis"].map((tab) => (
                <button
                    key={tab}
                    className={`tab-btn ${tab === activePage ? "active" : "inactive"}`}
                    onClick={() => setActivePage(tab)}
                >
                    {tab}
                </button>
            ))}
        </div>

      {/* Teams Panel */}
      {showTeams && (
        <div style={{
          background: "#0d1018",
          borderBottom: "1px solid #111620",
          padding: "24px 32px",
        }}>
          <h3 style={{ color: "#fff", fontSize: "1rem", fontWeight: 700, marginBottom: 16, letterSpacing: "0.05em", textAlign: "center" }}>
            NBA Teams
          </h3>
          <div style={{
              display: "grid",
              gridTemplateColumns: "repeat(5, 1fr)",
              gap: 16,
              maxWidth: 1000,
              margin: "0 auto"
          }}>
            {teams.map((team) => (
              <div key={team.id} style={{
                background: "#131720",
                border: "1.5px solid #1e2333",
                borderRadius: 10,
                padding: "12px 16px",
                display: "flex",
                flexDirection: "column",
                gap: 4,
              }}>
                  <div key={team.teamId} style= {{display: "flex", justifyContent: "center", marginBottom: 8}}>
                      <img
                          src={`https://cdn.nba.com/logos/nba/${team.nbaTeamId}/primary/L/logo.svg`}
                          alt={`${team.teamName} logo`}
                          style={{ width: 100, height: 100 }}
                      />
                  </div>
                <span style={{ color: "#4f7cff", fontWeight: 700, fontSize: "0.95rem" }}>
                  {team.abbreviation}
                </span>
                <span style={{ color: "#fff", fontWeight: 600, fontSize: "0.85rem" }}>
                  {team.full_name}
                </span>
                <span style={{ color: "ghostwhite ", fontSize: "0.75rem" }}>
                  {team.city} • {team.conference ?? ""}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </>
  );
}
export default NavBar;