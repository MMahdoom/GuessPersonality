# Guess the Personality (Web — Zero Sign-In, GitHub Pages Ready)

A mobile- & desktop-browser party game featuring **62 curated Indian personalities** (45 Tamil + 17 Pan-Indian) with live portrait photos, Iron Thumbs–style **QR room codes + built-in QR scanner**, **zero sign-in**, **60-minute inactivity auto-clear**, **per-game random number shuffling**, **host personality elimination from the pile**, **Forehead Display + Landscape Full-Screen Mode**, **compact Info rules tab**, and **3-round / 15-question live scoring**.

## Quick Start (Local)

```bash
cd web
npm install
npm run dev
```

Run automated tests:

```bash
cd web
node --test tests/*.test.mjs
```

Without a `.env` file, the app automatically runs in **Single-Device Mode** (`BroadcastChannel` + `localStorage`) so you can open two browser tabs and test host + player sync immediately.

## Free Multi-Phone Real-Time Sync (No Sign-In Required)

1. Go to [Firebase Console](https://console.firebase.google.com/) and create a free Spark-plan project.
2. Enable **Realtime Database** (no Auth / Google Sign-In is needed).
3. Paste `web/database.rules.json` into **Realtime Database → Rules** and click **Publish**.
   - Access is strictly locked to individual `/rooms/$roomCode` paths (root `/rooms` listing is blocked).
   - Every room tracks `lastActiveAt` and `expiresAt = lastActiveAt + 60 min`, and automatically deletes itself after **60 minutes of inactivity**.
4. Copy `web/.env.example` to `web/.env` and set `VITE_FIREBASE_DATABASE_URL` (that single value is all that's required for unauthenticated RTDB sync).

## Deploy to GitHub Pages

1. Push this repository to a **Public** GitHub repository (or deploy `web/` from a private repo on Vercel / Netlify / Cloudflare Pages).
2. In GitHub, go to **Settings → Pages → Build and deployment → Source: GitHub Actions**.
3. Add `VITE_FIREBASE_DATABASE_URL` under **Settings → Secrets and variables → Actions** (Secrets or Variables tab).
4. Push to `main` or run **Actions → Deploy Guess the Personality Web to GitHub Pages → Run workflow**.
