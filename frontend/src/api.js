const DEFAULT_API_BASE = "http://localhost:8080";

export const API_BASE = import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE;
export const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === "true";

const join = (path) => `${API_BASE}${path}`;

export const API = {
  test: join("/test"),
  teams: join("/teams"),
  playerSearch: (q) => join(`/api/players/search?q=${encodeURIComponent(q)}`),
  playerInjuries: (id) => join(`/bdl/players/${id}/injuries`),
  // upcomingGames: join("/bdl/games/upcoming?days=2"),
  upcomingGames: join("/bdl/games/completed?days=13"), // This is for the Demo Presentation

  completedGames: join("/bdl/games/completed?days=2"),
  playerStats: (id, limit) => join(`/stats/player/external/${id}?limit=${limit}`),
  playerStatsApi: (apiId) => join(`/stats/player/external/${apiId}`),
  recentAnalyze: (params) => join(`/stats/recent/analyze?${new URLSearchParams(params)}`),
  explainRecent: (params) => join(`/api/ai/explain/matchup?${new URLSearchParams(params)}`),
  leaderboard: (statType, season) => join(`/leaderboard/${statType}?season=${season}`),
  matchup: (params) => join(`/api/matchup/analyze?${new URLSearchParams(params)}`),
  explainMatchup: (params) => join(`/api/ai/explain/matchup?${new URLSearchParams(params)}`),
  compare: (id1, id2) => join(`/api/comparison/compare?playerOneApiId=${id1}&playerTwoApiId=${id2}`),
  explainComparison: (id1, id2) => join(`/api/ai/explain/comparison?playerOneApiId=${id1}&playerTwoApiId=${id2}`),
  predict: (playerApiId, opponentTeamApiId, statType, line) =>
    join(`/api/props/predict?playerApiId=${playerApiId}&opponentTeamApiId=${opponentTeamApiId}&statType=${statType}&line=${line}`),
  explainPrediction: (playerApiId, opponentTeamApiId, statType, line) =>
    join(`/api/ai/explain/prop?playerApiId=${playerApiId}&opponentTeamApiId=${opponentTeamApiId}&statType=${statType}&line=${line}`),
};

