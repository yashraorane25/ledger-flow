# Ledger-Flow Operations Console (Frontend)

A modern, responsive operations console built with **React**, **TypeScript**, and **Vite** for the **Ledger-Flow** event-driven payment settlement and double-entry ledger system.

It communicates with both Spring Boot backend services (`payment-service` on port `8080` and `ledger-service` on port `8081`) via a Vite reverse proxy.

---

## Features

### 1. Payment Terminal & Async Status Polling
* **Natural Currency Input**: Enter payments in standard USD format (e.g. `$50.00`), automatically converted to minor units (`5000` cents) before sending to prevent floating-point calculation errors.
* **Idempotency Key Generator**: Automatically generates unique keys per transaction or allows custom keys, with a 🎲 Random button.
* **Simulation Presets**:
  * **$50.00 (Normal)**: Tests standard asynchronous approval (`PENDING` $\rightarrow$ `AUTHORIZED`).
  * **$15,000.00 (> Limit)**: Exceeds the simulator's threshold, testing the declined path (`PENDING` $\rightarrow$ `FAILED`).
* **Live Status Tracking**: Automatically polls `GET /payments/{id}` every second until Kafka finishes the authorization round trip.

### 2. Double-Entry Ledger & Redis Balances
* **Account Cards Grid**: Displays the seeded double-entry ledger accounts:
  * `CASH` (Asset)
  * `MERCHANT_PAYABLE` (Liability)
  * `PSP_RECEIVABLE` (Asset)
  * `FEE_REVENUE` (Revenue)
* **Real-Time Account Balances**: Formatted currency display with thousands separators.
* **Cache Telemetry**: Visual badges indicating whether the balance was served in sub-milliseconds from the **Redis cache** (`⚡ Redis Cache (Hit)`) or recalculated from PostgreSQL entries (`🐘 Postgres (Cache Miss)`).
* **Auto & Manual Refresh**: Balances auto-refresh when payments complete, with an on-demand **🔄 Refresh Balances** button.

### 3. Reconciliation & Audit Center
* **On-Demand Audit Trigger**: Click **▶ Run Audit Now** to trigger `POST /reconciliation/run` on `ledger-service` immediately instead of waiting for the 60-second background scheduler.
* **Mismatches Table**: Displays open reconciliation reports (`MISSING_JOURNAL` or `AMOUNT_MISMATCH`), deduplicated to highlight the latest audit state per payment.

---

## Tech Stack

* **Framework**: React 19
* **Language**: TypeScript
* **Build Tool & Dev Server**: Vite
* **Styling**: Standard CSS (Flexbox & CSS Grid)
* **Networking**: Native Browser `fetch` API

---

## Architecture & Proxy Configuration

To prevent browser Cross-Origin Resource Sharing (CORS) blocks, Vite is configured as a reverse proxy in `vite.config.ts`:

| Frontend Route | Target Service | Backend Destination |
|---|---|---|
| `/api/payments/*` | `payment-service` | `http://localhost:8080/payments/*` |
| `/api/ledger/*` | `ledger-service` | `http://localhost:8081/*` |

---

## Getting Started

### Prerequisites

* **Node.js**: `v20.19+` or `v22.12+`
* **Docker & Backends Running**: Ensure Kafka, PostgreSQL, Redis, `payment-service`, and `ledger-service` are running.

### Installation

1. Navigate to the frontend directory:
   ```bash
   cd ledger-frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```

4. Open your browser at:
   ```
   http://localhost:5173
   ```

---

## Project Structure

```
ledger-frontend/
├── src/
│   ├── components/
│   │   ├── PaymentManager.tsx        # Payment form, presets, and live status table
│   │   ├── LedgerManager.tsx         # Account balances & Redis cache badges
│   │   └── ReconciliationManager.tsx # Reconciliation audit trigger & mismatch table
│   ├── api.ts                        # API client functions for payment & ledger endpoints
│   ├── types.ts                      # TypeScript interfaces & types
│   ├── App.tsx                       # Main layout coordinating the modules
│   ├── App.css                       # Application styles
│   └── main.tsx                      # React root entry point
├── vite.config.ts                    # Vite dev server proxy configuration
├── package.json
└── README.md
```

---

## Build for Production

To create an optimized production build:

```bash
npm run build
```

The output files will be generated in the `dist/` directory, ready to be served by any static web server or reverse proxy (e.g., Nginx, Caddy, or Spring Boot static resources).
