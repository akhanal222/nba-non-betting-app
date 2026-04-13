## NBA (Non-Betting App)

Live website: https://nba-non-betting-app.vercel.app/

---

## Deployment

### Render backend
- Create a new **Web Service** from this repo
- Choose **Docker** as the environment
- Use the repository root as the service root
- Set these environment variables:
  - `DB_Password`
  - `BDL_API_KEY`
  - `GEMINI_API_KEY`
- The backend listens on `PORT`, which is provided by Render automatically

### Frontend
- Deploy `frontend/` separately on Vercel or Netlify
- Set `VITE_API_BASE_URL` to your Render backend URL

---

## Architecture Overview

#### Application flow:

##### Frontend (React + Vite)  
→ REST Controllers (Spring Boot)  
→ Service Layer  
→ JPA Repositories  
→ Neon PostgreSQL Database

#### External Data Flow:

##### Balldontlie API  
→ Backend  
→ Database

## Setup Instructions

### Environment Variables
Create environment variables through the Run button, Edit config:

**Backend**
- `DB_Password` = Put Neon database password
- `BDL_API_KEY` = Put Balldontlie API key
- `GEMINI_API_KEY` = Put Gemini API key

---
### Run Backend and Frontend:
#### 1. Backend :
   ```python
    http://localhost:8080/
   ```
#### 2. Frontend :
 ```python
  cd frontend/
   ```
 ```python
  npm run dev
   ```
   ```python
    http://localhost:5173/
   ```

## Week 1: Architecture & Foundation

### Goal: 
Establish backend, database, and overall project structure.

---

## Week 1 Progress

### Backend Setup

| Task | Status |
|------|--------|
| Spring Boot project initialized | Completed |
| Maven configured | Completed |
| Neon PostgreSQL database created | Completed |
| Backend connected to Neon | Completed |
| JPA entities implemented | Completed |
| Repository layer created | Completed |
| Balldontlie API configured | Completed |
| Basic controller structure created | Completed |
| Lombok integrated | Completed |
| Frontend setup(React + Vite) | Completed|

---

### Database Schema

The following core tables were created:

| Table Name |
|------------|
| players |
| teams |
| games |
| player_game_statistics |
| user / authentication related tables |

Database Schema link (Diagram) :
   ```python
    https://drive.google.com/file/d/1coaCfPUfVlGbc75deqzRQlqomIR0r3jX/view?usp=sharing
   ```
---

## Week 2: Data Ingestion from External api

### Goal:  
System can fetch and store real NBA data

---

## Week 2 Progress

### Get Data in Database
| Task                                                                | Status    |
|---------------------------------------------------------------------| --------- |
| Player search (DB-first strategy) implemented                       | Completed |
| Player upsert logic (prevent duplicates)                            | Completed |
| Team upsert logic                                                   | Completed |
| PlayerStatsService implemented                                      | Completed |
| Game upsert with home and away teams                                | Completed |
| PlayerGameStatistic upsert logic                                    | Completed |
| Limit validation (5 / 10 / 15 games)                                | Completed |
| Safe date parsing implemented                                       | Completed |
| nba_player_lookup table integrated<br/>(For getting photo of the player) | Completed |
| NBA headshot rendering in frontend                                  | Completed |
|Frontend page added with search and all team lookup                                | Completed |
---

## Week 3: Prop Analysis & Matchup Intelligence

### Goal:
Add matchup-based prop analysis and recent-form analysis on top of stored NBA data.

---

## Week 3 Progress

| Task | Status |
|------|--------|
| Head-to-head matchup analysis endpoint added | Completed |
| Recent stats prop analysis endpoint added | Completed |
| Stat type support expanded across common prop categories | Completed |
| Playoff filtering added to matchup analysis | Completed |
| Standard deviation and hit-rate calculations added | Completed |
| Frontend prediction dashboard updated with stat selector and line input | Completed |

---

## Week 4: Dashboard, Team Players & Game Views

### Goal:
Add team rosters, upcoming and completed games, leaderboard views, and automatic player image lookup.

---

## Week 4 Progress

| Task | Status |
|------|--------|
| Get team players endpoint added | Completed |
| Upcoming games endpoint added | Completed |
| Completed games endpoint added | Completed |
| Top performers leaderboard endpoint added | Completed |
| NBA image lookup for players added | Completed |
| Dashboard updated with games, teams, and leaderboard sections | Completed |
| Recent results now open game-level player stats | Completed |

---

## Week 5: Player Comparisons, Trends & Team Rankings

### Goal:
Expand the UI with player-vs-team comparisons, stat filters, trends, and team ranking views.

---

## Week 5 Progress

| Task | Status |
|------|--------|
| Player-vs-team comparison UI implemented | Completed |
| Head-to-head toggle added | Completed |
| Stat filters added for top players (points, assists, rebounds, 3PT, steals, turnovers) | Completed |
| Team stats and rankings table added | Completed |
| Season player stats for 2026 added | Completed |
| Chart.js trend lines for single-player stats | Completed |
| Comparison bar charts | Completed |
| AI integration research for future use | Completed |
| Player career stats | Completed |
| Favorite players / teams (login feature) | Completed |
| Injury report | Completed |

---

## Week 6: Player Matchups, Predictions & Team Comparison

### Goal:
Improve matchup analysis, prediction tools, and team comparison views.

---

## Week 6 Progress

| Task | Status |
|------|--------|
| Player vs player UI added in the matchup dashboard | Completed |
| Player vs team / player vs player toggle added | Completed |
| Graphs added to matchup views | Completed |
| UI improved for matchup and comparison views | Completed |
| Top player loading performance improved | Completed |
| Win percentage shown in the standings table | Completed |
| Detail button fixed across all pages | Completed |
| Prediction page with advanced stats | Completed |
| Matchup predictor for team vs team comparisons | Completed |
| Comparison bar charts | Completed |
| AI integration for predictions | Completed |

---

## Week 7: Prop Prediction Engine & AI Explanations

### Goal:
Add prop prediction and AI-generated explanations across the main analysis pages.

---

## Week 7 Progress

| Task | Status |
|------|--------|
| Prop prediction engine added | Completed |
| AI explanation layer added | Completed |
| AI explanations for player detail, matchup, player vs team, player vs player, and prediction pages | Completed |
| Scheduler setup for nightly DvP refresh | Completed |
| Advanced stats caching and EWMA support added | Completed |
| Team defense vs position data generation added | Completed |

---

## Week 8: Frontend AI Explanations & Top 20 Context

### Goal:
Make the analysis pages easier to understand with AI explanations and better season context on the Top 20 Players page.

---

## Week 8 Progress

| Task | Status |
|------|--------|
| Total games played this season added to the Top 20 Players page | Completed |
| AI explanation layer planned for the frontend analysis pages | Completed |
| Player detail page AI explanation | Completed |
| Matchup page AI explanation | Completed |
| Player vs team AI explanation | Completed |
| Player vs player AI explanation | Completed |
| Prediction page AI explanation | Completed |

---
