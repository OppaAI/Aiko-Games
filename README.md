# Aiko-Shogi

**An Android companion app for playing Shogi with Aiko—locally deployed, privacy-first.**

> Aiko-Shogi is a native Kotlin/Jetpack Compose client that connects to your local Aiko-chan server over Tailscale. Play 将棋 against Aiko: you move first (先手), she replies via the `/api/games/shogi` API—optionally powered by **YaneuraOu** for strong moves, with a casual fallback when the engine is offline.

**Author:** [OppaAI](https://github.com/OppaAI) · Beautiful British Columbia, Canada

[![Repo](https://img.shields.io/badge/Repo-OppaAI%2FAiko--Shogi-967BB6?logo=github&logoColor=white)](https://github.com/OppaAI/Aiko-Shogi)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Status](https://img.shields.io/badge/Status-experimental-orange.svg)

**Frontend:**
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6-4285F4?logo=android&logoColor=white)
![Android](https://img.shields.io/badge/Android-SDK%2034+-3DDC84?logo=android&logoColor=white)

**Backend:**
![Python](https://img.shields.io/badge/Python-3.12-blue?logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-async-009688?logo=fastapi&logoColor=white)
![python-shogi](https://img.shields.io/badge/python--shogi-rules-003B57)
![YaneuraOu](https://img.shields.io/badge/YaneuraOu-optional%20USI-967BB6)

---

## Features

### Current (Kotlin/Jetpack Compose + Aiko-chan `/api/games/shogi`)

- **Play vs Aiko** – You are 先手 (Black, first move); Aiko is 後手
- **9×9 board** – Renders SFEN from the server; Japanese piece glyphs (歩香桂銀金角飛玉 + promoted)
- **Tap to move** – Select a piece, then a destination; drops from the hand when the server lists `X*sq` legal moves
- **Legal-move hints** – Highlights from `GET /legal-moves` (USI)
- **Engine badge** – Shows whether Aiko is using **YaneuraOu** or the casual random fallback
- **Aiko comments** – `ai_comment` from the server after each reply
- **Resign** – End the game and return to the lobby
- **Server 🔗** – In-app Tailscale hostname / Tailnet IP (same idea as Aiko-Lingo); persists across restarts

### Planned

- Hand-piece tray UI for drops (API already supports `B*5e`-style USI)
- Move history / SFEN copy
- Optional Aiko voice line after each move (reuse lingo TTS)
- Puzzles / daily tsume later

---

## Quick Start

### Prerequisites
- Android Studio 2024.1+
- Android SDK 34+ (target API level)
- Kotlin 2.0+
- Jetpack Compose
- Aiko-chan with **[PR #156](https://github.com/OppaAI/Aiko-chan/pull/156)** (`/api/games/shogi`) merged and running
- `pip install python-shogi` on the server; optional `YANEURAOU_PATH` for strong AI

### Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/OppaAI/Aiko-Shogi.git
   cd Aiko-Shogi
   ```

2. **Open in Android Studio:**
   - `File` → `Open` → select the `Aiko-Shogi` folder
   - Let Gradle sync (use the Gradle wrapper if present, or let Studio generate one)

3. **Configure server connection:**
   - Open the app → **Server 🔗** → enter your Aiko-chan Tailscale URL  
     e.g. `https://aiko.ide-chroma.ts.net/` or `http://100.x.y.z:8787/`
   - Session cookie auth must work the same way as Aiko-Lingo (log in via the web UI on that host if needed)

4. **Run:**
   - Select an emulator or device → **Run**

---

## Architecture

### Client (this app)
```
MainActivity
├── Lobby (start vs Aiko, engine status, server URL)
├── ShogiScreen + ShogiViewModel
│   ├── Board from SFEN (9×9)
│   ├── Selection + legal USI highlights
│   └── Status / Aiko comment / Resign
├── data/ServerConfig          — persisted base URL
├── data/remote/ShogiApi       — Retrofit → /api/games/shogi/*
└── data/model + SfenBoard     — SFEN parse + piece glyphs
```

### Server (Aiko-chan)
```
/api/games/shogi
├── POST /start
├── POST /move          { "move": "7g7f" }
├── GET  /state
├── GET  /legal-moves
├── GET  /engine        — yaneuraou available?
└── POST /resign
```

Rules: **python-shogi**. Strong moves: optional **YaneuraOu** (USI). See Aiko-chan `interface/webui/lingo/games_shogi.py` and `yaneuraou.py`.

### Move notation (USI)

| Example | Meaning |
|---------|---------|
| `7g7f` | Move from 7g to 7f |
| `8h2b+` | Move and promote |
| `B*5e` | Drop Bishop on 5e |

---

## Related

- [Aiko-chan](https://github.com/OppaAI/Aiko-chan) — backend
- [Aiko-Lingo](https://github.com/OppaAI/Aiko-Lingo) — Japanese learning Android app (same stack)
- [YaneuraOu](https://github.com/yaneurao/YaneuraOu) — optional Shogi engine

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).
