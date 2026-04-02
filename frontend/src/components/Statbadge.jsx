// Display a stat value with a label
export default function StatBadge({ value, label }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
      <span style={{ color: "var(--accent-soft)", fontWeight: 700, fontSize: "1.3rem" }}>
        {value ?? "—"}
      </span>
      <span style={{ color: "var(--text-muted)", fontSize: "0.72rem", letterSpacing: "0.1em" }}>
        {label}
      </span>
    </div>
  );
}