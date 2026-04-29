// Base URL defaults to the local Spring Boot server.
const DEFAULT_API_BASE = "http://localhost:8080";

// API_BASE is what the frontend will actually call.
// Priority: VITE_API_BASE_URL (if set) else it will tirgger  DEFAULT_API_BASE.
export const API_BASE = import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE;

// Demo flag to let the UI switch behavior for presentations (controlled via VITE_DEMO_MODE="true").
export const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === "true";

// Helper to build full URLs for backend endpoints.
const join = (path) => `${API_BASE}${path}`;

export const API = {
  // Health-check endpoint to confirm backend is reachable.
  test: join("/test"),

  // Returns the list of NBA teams from our database.
  teams: join("/teams"),

  // Player search (our backend + DB search) by name.
  // Example: /api/players/search?q=lebron
  playerSearch: (q) => join(`/api/players/search?q=${encodeURIComponent(q)}`),

  // External API passthrough: latest injury info for a specific player (by external API id).
  playerInjuries: (id) => join(`/bdl/players/${id}/injuries`),

  // Upcoming games.
  // NOTE: For demo/presentation, we temporarily point this to a completed-games window so the UI always has data.
  upcomingGames: join("/bdl/games/upcoming?days=2"),
  // upcomingGames: join("/bdl/games/completed?days=29"), // Demo placeholder: shows recent completed games as "upcoming".

  // Recently completed games (by time window).
  completedGames: join("/bdl/games/completed?days=2"),

  // Recent game stats for a player by external API id with an explicit limit.
  playerStats: (id, limit) => join(`/stats/player/external/${id}?limit=${limit}`),

  // All available stats for a player by external API id.
  playerStatsApi: (apiId) => join(`/stats/player/external/${apiId}`),

  // "Recent form" analysis endpoint.
  recentAnalyze: (params) => join(`/stats/recent/analyze?${new URLSearchParams(params)}`),

  // AI explanation for a recent/matchup style analysis.
  explainRecent: (params) => join(`/api/ai/explain/matchup?${new URLSearchParams(params)}`),

  // Leaderboard by stat type and season.
  leaderboard: (statType, season) => join(`/leaderboard/${statType}?season=${season}`),

  // Head-to-head matchup analysis endpoint.
  matchup: (params) => join(`/api/matchup/analyze?${new URLSearchParams(params)}`),

  // AI explanation for head-to-head matchup analysis.
  explainMatchup: (params) => join(`/api/ai/explain/matchup?${new URLSearchParams(params)}`),

  // Player vs Player comparison.
  compare: (id1, id2) => join(`/api/comparison/compare?playerOneApiId=${id1}&playerTwoApiId=${id2}`),

  // AI explanation for a player-vs-player comparison.
  explainComparison: (id1, id2) => join(`/api/ai/explain/comparison?playerOneApiId=${id1}&playerTwoApiId=${id2}`),

  // Prop prediction endpoint.
  predict: (playerApiId, opponentTeamApiId, statType, line) =>
    join(`/api/props/predict?playerApiId=${playerApiId}&opponentTeamApiId=${opponentTeamApiId}&statType=${statType}&line=${line}`),

  // AI explanation for a prop prediction.
  explainPrediction: (playerApiId, opponentTeamApiId, statType, line) =>
    join(`/api/ai/explain/prop?playerApiId=${playerApiId}&opponentTeamApiId=${opponentTeamApiId}&statType=${statType}&line=${line}`),
};

