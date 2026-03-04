// Component that displays basic information about the selected player
function AnalyzePanel({ player }) {

    // List of player attributes to display in the panel
  const fields = [
    ["Position", player.position],
    ["Team", player.team?.teamName],
    ["City", player.team?.city],
    ["Conference", player.team?.conference],
    ["Division", player.team?.division],
    ["Jersey #", player.jerseyNumber],
    ["Height", player.height],
    ["Weight", player.weight],
  ];

  return (
    <div style={{
      marginTop: 36,
      background: "#111620",
      border: "1.5px solid #1e2333",
      borderRadius: 14,
      padding: "28px 32px",
      textAlign: "left",
    }}>
      <h2 style={{ fontSize: "1.3rem", fontWeight: 800, color: "#fff", marginBottom: 18 }}>
        {player.firstName} {player.lastName}
      </h2>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))", gap: 14 }}>
        {fields.map(([label, val]) => (
           <div key={label} style={{ background: "#0a0c14", borderRadius: 8, padding: "12px 16px" }}>
            <div style={{ color: "#444", fontSize: "0.7rem", letterSpacing: "0.1em", marginBottom: 4 }}>
              {label}
            </div>
            <div style={{ color: "#ccc", fontWeight: 600, fontSize: "0.95rem" }}>
              {val ?? "—"}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
export default AnalyzePanel;