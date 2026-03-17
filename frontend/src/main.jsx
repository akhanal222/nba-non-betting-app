import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import GameLineupPage from './pages/GameLineupPage.jsx'
import TeamPlayersPage from './pages/TeamPlayersPage.jsx'
import PlayerProfile from './pages/PlayerProfile.jsx'
import MatchupsDashboard from "./pages/MatchupsDashboard.jsx";
import PlayersPredictions from "./pages/PlayersPredictions.jsx"

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <BrowserRouter>
            <Routes>
                <Route path="/"element={<App />} />
                <Route path="/game/:gameId" element={<GameLineupPage />} />
                <Route path="/team/:teamId/players" element={<TeamPlayersPage />} />\
                <Route path="/players" element={<PlayerProfile />} />
                <Route path="/matchups" element={<MatchupsDashboard/>} />
                <Route path= "/predictions" element={<PlayersPredictions/>} />
            </Routes>
        </BrowserRouter>
    </StrictMode>,
)