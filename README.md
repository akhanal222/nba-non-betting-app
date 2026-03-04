<h1 align="center">NBA (Non-Betting App)</h1>

## Week 2: Data Ingestion from External api

### Goal:  
System can fetch and store real NBA data

---
<h1 align="center">Week 2 Progress</h1>

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
## Week 1: Architecture & Foundation

### Goal: 
Establish backend, database, and overall project structure.

---

<h1 align="center">Week 1 Progress</h1>

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

<h1 align="center">Setup Instructions</h1>

### Environment Variables
Create environment variables through the Run button, Edit config:

**Backend**
- `DB_Password` = Put Neon database password
- `BDL_API_KEY` = Put Balldontlie API key

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

---



