import StatBadge from "./StatBadge";

// This is to generate initials if player photo is unavailable
function getInitials(firstName, lastName) {
  return `${firstName?.[0] ?? ""}${lastName?.[0] ?? ""}`.toUpperCase();
}

export default function PlayerCard({ player, onAnalyze, selected }) {
  const initials = getInitials(player.firstName, player.lastName);

  return (
    <div style={{
      background: selected ? "#1a1f2e" : "#131720",
      border: `1.5px solid ${selected ? "#4f7cff" : "#1e2333"}`,
      borderRadius: 14,
      padding: 28,
      display: "flex",
      flexDirection: "column",
      gap: 10,
      cursor: "pointer",
      boxShadow: selected ? "0 0 18px #4f7cff44" : "none",
      transition: "border 0.2s, box-shadow 0.2s",
    }}>

      {/* Player avatar, name and team info */}
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>

        {/* Photo of the player  */}
        {player.nbaPlayerId ? (
          <img
            src={`https://cdn.nba.com/headshots/nba/latest/1040x760/${player.nbaPlayerId}.png`}
            alt={`${player.firstName} ${player.lastName}`}
            style={{ width: 100, height: 100, borderRadius: "50%", objectFit: "cover", border: "2px solid #4f7cff", flexShrink: 0 }}
            onError={(e) => { e.target.style.display = "none"; e.target.nextSibling.style.display = "flex"; }}
          />
        ) : null}

        {/* Initials fallback */}
        <div style={{
          width: 90, height: 90, borderRadius: "50%",
          background: "#2a3be0",
          display: player.nbaPlayerId ? "none" : "flex",
          alignItems: "center", justifyContent: "center",
          fontWeight: 700, fontSize: "1rem", color: "#fff",
          flexShrink: 0,
        }}>
          {initials}
        </div>

        {/* Player name and basic team info */}
        <div>
          <div style={{ color: "#fff", fontWeight: 700, fontSize: "1.15rem" }}>
            {player.firstName} {player.lastName}
          </div>
          <div style={{ color: "#555", fontSize: "0.75rem" }}>
            {player.team?.abbreviation ?? player.team?.teamName ?? "—"} • {player.position ?? "—"}
          </div>
        </div>
      </div>

      {/* Stats of the player when searched */}
      <div style={{
          display: "flex",
          justifyContent: "center",
          gap: 18,
          marginTop: 10,
      }}>
        <StatBadge value={player.position} label="Pos" />
        <StatBadge value={player.team.abbreviation} label="Team" />
        <StatBadge value={player.jerseyNumber} label="#" />
      </div>

       {/* Button to open detailed analysis for the player */}
      <button
        onClick={() => onAnalyze(player)}
        style={{
          background:selected? "#2a3be0": "#1a1f2e",
          border:selected ? "none": "1px solid #2a2f44",
          color:selected ? "#fff": "#888",
          borderRadius:8,
          padding:"12px 0",
          fontSize:"1rem",
          letterSpacing:"0.08em",
          fontWeight:600,
          cursor:"pointer",
          width:"100%",
          transition:"background 0.2s, color 0.2s",
          fontFamily:"inherit",
        }}
        onMouseEnter={e => { if (!selected) { e.currentTarget.style.background = "#22294a"; e.currentTarget.style.color = "#fff"; }}}
        onMouseLeave={e => { if (!selected) { e.currentTarget.style.background = "#1a1f2e"; e.currentTarget.style.color = "#888"; }}}
      >
       Detail
      </button>
    </div>
  );
}