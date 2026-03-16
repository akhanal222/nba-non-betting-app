import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import GameLineupPage from './pages/GameLineupPage.jsx'
import TeamPlayersPage from './pages/TeamPlayersPage.jsx'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <BrowserRouter>
            <Routes>
                <Route path="/"element={<App />} />
                <Route path="/game/:gameId" element={<GameLineupPage />} />
                <Route path="/team/:teamId/players" element={<TeamPlayersPage />} />
            </Routes>
        </BrowserRouter>
    </StrictMode>,
)