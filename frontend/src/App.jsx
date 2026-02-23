import { useEffect, useState } from "react";
import "./App.css";

export default function App() {
  const [count, setCount] = useState(0);
  const [msg, setMsg] = useState("Loading...");
  const [teams, setTeams] = useState([]);
  const [loading, setLoading] = useState(false);

  // Test backend connection on page load
  useEffect(() => {
    fetch("http://localhost:8080/test")
      .then((r) => r.text())
      .then(setMsg)
      .catch(() => setMsg("Backend not reachable"));
  }, []);

  const loadTeams = () => {
    setLoading(true);

    fetch("http://localhost:8080/bdl/teams")
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

      <button onClick={loadTeams}>Load Teams</button>

      {loading && <p>Loading...</p>}

      {teams.map((team) => (
        <div key={team.id}>
          <p>{team.full_name}</p>
        </div>
      ))}
    </div>
  );
}