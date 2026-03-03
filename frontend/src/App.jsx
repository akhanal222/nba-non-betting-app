import { useEffect, useState } from "react";
import "./App.css";

export default function App() {
  const [count, setCount] = useState(0);
  const [msg, setMsg] = useState("Loading...");
  const [teams, setTeams] = useState([]);
  const [loading, setLoading] = useState(false);
  const [q, setQ] = useState("");
  const [players, setPlayers] = useState([]);
  const [loadingPlayers, setLoadingPlayers] = useState(false);

  // Test backend connection on page load
  useEffect(() => {
    fetch("http://localhost:8080/test") // Checking backend is running or not
      .then((r) => r.text())
      .then(setMsg)
      .catch(() => setMsg("Backend not reachable"));
  }, []);

  const loadTeams = () => {
    setLoading(true);

    // test that the external api is connceted to show up in the front page
    fetch("http://localhost:8080/bdl/teams") // Get all the teams in the nba this is for testing
      .then((res) => res.json())
      .then((data) => {
        setTeams(data.data || data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  };
    const searchPlayers = () => {
        const query = q.trim();
        if (query.length < 2) return;

        setLoadingPlayers(true);

        fetch(`http://localhost:8080/api/players/search?q=${encodeURIComponent(query)}`) // Search player endpoint
            .then((res) => res.json())
            .then((data) => {
                setPlayers(Array.isArray(data) ? data : []);
                setLoadingPlayers(false);
            })
            .catch((err) => {
                console.error(err);
                setLoadingPlayers(false);
            });
    };

  return (
    <div style={{ padding: 20 }}>
      <h1>NBA App</h1>

      <p>Backend says: {msg}</p>

      <div className="card">
        <button onClick={() => setCount((c) => c + 1)}>
          count is {count}
        </button>
      </div>

      <hr />

      <h2>NBA Teams</h2>

        <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search player (ex: lebron james)"
        />

        <button onClick={searchPlayers} disabled={loadingPlayers}>
            {loadingPlayers ? "Searching..." : "Search Player"}
        </button>

        {players.map((player) => (
            <div key={player.playerId}>
                {player.nbaPlayerId && (
                    <img
                        src={`https://cdn.nba.com/headshots/nba/latest/1040x760/${player.nbaPlayerId}.png`}
                        alt={`${player.firstName} ${player.lastName}`}
                        style={{ width: "120px", height: "auto" }}
                    />
                )}
                <p>Name: {player.firstName} {player.lastName}</p>
                <p>Jersey Number: {player.jerseyNumber ?? "—"}</p>
                <p>Height: {player.height ?? "—"}</p>
                <p>Weight: {player.weight ?? "—"}</p>
                <p>Position: {player.position || "—"}</p>

                <p>Team Name: {player.team?.teamName ?? "—"}</p>
                <p>{player.team?.abbreviation ?? ""}</p>
                <p>{player.team?.city ?? ""}</p>
                <p>{player.team?.conference ?? ""}</p>
                <p>{player.team?.division ?? ""}</p>
            </div>
        ))}

      <button onClick={loadTeams}>Load Teams</button>'
      {loading && <p>Loading...</p>}

      {teams.map((team) => (
        <div key={team.id}>
          <p>{team.full_name}</p>
        </div>

      ))}
    </div>
  );
}