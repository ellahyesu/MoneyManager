# StockManager Dashboard

A full-stack dashboard that surfaces key investment signals at a glance: global index prices, crypto, the Fear & Greed index, and watchlist-driven buy/sell stickers. This is a starter architecture with mocked data and a clear path to integrate real market data feeds.

## Goals
- Show US indices plus Korea, India, crypto prices and the Fear & Greed index on the home screen.
- Show technical indicators and buy/sell signals for user-defined watchlist assets.
- Keep the system extensible for future features.

## Tech Stack
- Backend: Java 11, Spring Boot 2.7.x
- Frontend: Vue 3 (Vite), JavaScript
- Container: Docker + docker-compose

## Repository Structure
- `backend` Spring Boot API service
- `frontend` Vue 3 dashboard
- `docker-compose.yml` multi-service setup

## API Endpoints (Backend)
- `GET /api/market/overview` market prices + Fear & Greed index
- `GET /api/watchlist` watchlist data and signals
- `POST /api/watchlist` add a symbol (JSON: `{ "symbol": "AMD", "name": "Advanced Micro Devices" }`)
- `DELETE /api/watchlist/{symbol}` remove a symbol

## Running Locally (Dev)

### Backend
Requirements: Java 11 and Maven 3.9+

```bash
cd backend
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`.

### Frontend
Requirements: Node.js 20+

Set Finnhub API key for the Vite frontend:

```bash
set VITE_FINNHUB_API_KEY=your_key_here
```

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173` and proxies `/api` to the backend.

## Running with Docker

```bash
docker compose up --build
```

- Frontend: `http://localhost:8081`
- Backend: `http://localhost:8080`

## Notes on Data
Market data is loaded from Finnhub directly in the frontend. Technical indicators and the Fear & Greed index are derived locally from price changes (free-tier friendly).
- `backend/src/main/java/com/stockmanager/api/service/MarketDataService.java`
- `backend/src/main/java/com/stockmanager/api/service/WatchlistService.java`

When integrating a real data provider, you can inject API keys via environment variables and wire the HTTP client in these services.

## Suggested Next Steps
- Add persistent storage for watchlist items (PostgreSQL or Redis).
- Add scheduled jobs for fresh market data.
- Expand indicators (Bollinger Bands, ATR, volume/flow metrics).
- Add auth for personalized watchlists.

