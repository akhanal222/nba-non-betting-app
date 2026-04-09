import StatBadge from "./Statbadge.jsx";

// This is to generate initials if player photo is unavailable
function getInitials(firstName, lastName) {
  return `${firstName?.[0] ?? ""}${lastName?.[0] ?? ""}`.toUpperCase();
}

export default function PlayerCard({ player, onAnalyze, selected }) {
  const initials = getInitials(player.firstName, player.lastName);

  return (
    <div style={{
      background: selected ? "var(--bg-surface)" : "var(--bg-surface-2)",
      border: `1.5px solid ${selected ? "var(--accent)" : "var(--border-default)"}`,
      borderRadius: 14,
      padding: 28,
      display: "flex",
      flexDirection: "column",
      gap: 10,
      cursor: "pointer",
      boxShadow: selected ? "0 0 24px rgba(93, 132, 255, 0.28), inset 0 0 0 1px rgba(93, 132, 255, 0.15)" : "none",
      transition: "border 0.2s, box-shadow 0.2s",
    }}>

      {/* Player avatar, name and team info */}
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>

        {/* Photo of the player  */}
        {player.nbaPlayerId ? (
          <img
            src={`https://cdn.nba.com/headshots/nba/latest/1040x760/${player.nbaPlayerId}.png`}
            alt={`${player.firstName} ${player.lastName}`}
            style={{ width: 100, height: 100, borderRadius: "50%", objectFit: "cover", border: "2px solid var(--accent-soft)", flexShrink: 0 }}
            onError={(e) => { e.target.style.display = "none"; e.target.nextSibling.style.display = "flex"; }}
          />
        ) : null}

        {/* Initials fallback */}
        <div style={{
          width: 90, height: 90, borderRadius: "50%",
          background: "var(--accent)",
          display: player.nbaPlayerId ? "none" : "flex",
          alignItems: "center",
          justifyContent: "center",
          fontWeight: 700,
          fontSize: "1rem",
          color: "var(--bg-page)",
          flexShrink: 0,
        }}>
          {initials}
        </div>

        {/* Player name and basic team info */}
        <div>
          <div style={{ color: "var(--text-primary)", fontWeight: 700, fontSize: "1.15rem" }}>
            {player.firstName} {player.lastName}
          </div>
          <div style={{ color: "var(--text-secondary)", fontSize: "0.75rem" }}>
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
          background: selected ? "var(--accent)" : "var(--bg-surface-2)",
          border: selected ? "none" : `1px solid var(--border-default)`,
          color: selected ? "var(--bg-page)" : "var(--text-primary)",
          borderRadius:8,
          padding:"12px 0",
          fontSize:"1rem",
          letterSpacing:"0.08em",
          fontWeight:600,
          cursor:"pointer",
          width:"100%",
          transition: "all 0.2s",
          fontFamily:"inherit",
        }}
        onMouseEnter={e => {
          if (!selected) {
            e.currentTarget.style.background = "var(--bg-surface)";
            e.currentTarget.style.borderColor = "var(--accent)";
            e.currentTarget.style.color = "var(--text-primary)";
            e.currentTarget.style.boxShadow = "0 6px 16px rgba(93, 132, 255, 0.15)";
          }
        }}
        onMouseLeave={e => {
          if (!selected) {
            e.currentTarget.style.background = "var(--bg-surface-2)";
            e.currentTarget.style.borderColor = "var(--border-default)";
            e.currentTarget.style.color = "var(--text-primary)";
            e.currentTarget.style.boxShadow = "none";
          }
        }}
      >
       Detail
      </button>
    </div>
  );
}