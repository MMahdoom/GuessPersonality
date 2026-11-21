# Guess the Personality (Web — Zero Sign-In, GitHub Pages Ready)

A mobile- & desktop-browser party game featuring **100 Indian personalities** (70 Tamil + 30 Pan-Indian), Iron Thumbs–style **QR room codes**, **zero login/sign-in**, **60-minute inactivity auto-clear**, **host personality elimination from the pile**, **Forehead Display Mode**, and **3-round / 15-question live scoring**.

## Quick Start (Local)

```bash
cd web
npm install
npm run dev
```

Without a `.env` file, the app automatically runs in **Local Multi-Tab Mode** (`BroadcastChannel` + `localStorage`) so you can open two browser tabs and test host + player sync immediately.

## Free Multi-Phone Real-Time Sync (No Sign-In Required)

1. Go to [Firebase Console](https://console.firebase.google.com/) and create a free Spark-plan project.
2. Enable **Realtime Database** (not Auth — no Google Sign-In is needed!).
3. Paste `web/database.rules.json` into **Realtime Database → Rules** and click **Publish**.
   - Access is strictly locked to individual `/rooms/$roomCode` paths (root `/rooms` listing is blocked).
   - Every room records `lastActiveAt` and `expiresAt = lastActiveAt + 60 min`, and automatically clears itself after **60 minutes of inactivity**.
4. Copy `web/.env.example` to `web/.env` and fill in your Firebase web config values (`VITE_FIREBASE_*`).

## Deploy to GitHub Pages

1. Push this repository to GitHub (from AI Studio's export/GitHub menu or `git push`).
2. In your GitHub repo, go to **Settings → Pages → Build and deployment → Source: GitHub Actions**.
3. *(Optional, for multi-phone sync)* Add your `VITE_FIREBASE_*` keys under **Settings → Secrets and variables → Actions**.
4. Push to `main` (or trigger `.github/workflows/deploy-web.yml` manually under **Actions**). Your static site will go live at `https://<your-username>.github.io/<repo-name>/`!
