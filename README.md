# Aiko-Games

**Android companion for playing **Shogi (将棋)**, **Go (囲碁)** and **Koi-Koi (こいこい)** with Aiko — locally deployed, privacy-first.**

Connects to your Aiko-chan server over Tailscale:

| Game | API | Strong AI (optional) | Fallback |
|------|-----|----------------------|----------|
| Shogi | `/api/games/shogi` | YaneuraOu (USI) | Random legal |
| Go | `/api/games/go` | KataGo (GTP) | Random legal |
| Koi-Koi | `/api/games/koikoi` | Aiko heuristic (built-in, always ready) | Random legal |

**Author:** [OppaAI](https://github.com/OppaAI) · Beautiful British Columbia, Canada

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Status](https://img.shields.io/badge/Status-experimental-orange.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6-4285F4?logo=android&logoColor=white)

---

## Features

- **Lobby** — Shogi (将棋) vs Aiko · Go (囲碁) vs Aiko · Koi-Koi (こいこい) vs Aiko · separate rules pages · Preferences
- **Preferences** — difficulty (default medium), side, Go board size 9/13/19, Koi-Koi months 3/6/12
- **Server URL** — baked in at build time from Aiko-chan's `AIKO_PUBLIC_BASE_URL` (`util/sync_app_server_url.sh` → `BuildConfig`); no in-app editor
- **Shogi** — SFEN board, hand drops, promote dialog, YaneuraOu when online
- **Go** — intersection grid, pass/resign, captures, KataGo when online
- **Koi-Koi** — bundled hanafuda art (Louie Mantia, CC BY-SA 4.0 via Wikimedia Commons), yaku + koi-koi/stop calls, Aiko's built-in brain (no engine binary needed)
- **Engines on the server** — the phone never runs search; Jetson Orin Nano can use casual AI only

### Jetson Orin Nano & engines

| Engine | Use | Note |
|--------|-----|------|
| *(none)* | Default | Pure rules + random legal moves — fine for play |
| **YaneuraOu** | Shogi | Lightweight enough for Nano if you install it |
| **KataGo** | Go | Strong but heavy (GPU/model). Optional; skip on Nano if tight |
| **GNU Go** | Go (future) | Lighter than KataGo; backend can add later without app changes |

---

## Quick Start

1. Merge Aiko-chan **Go API** (PR #157) and ensure Shogi package is mounted.
2. Clone this repo, open in Android Studio, sync Gradle.
3. From Aiko-chan run `util/sync_app_server_url.sh` to bake in the Tailscale URL, then build.
4. **Shogi (将棋) vs Aiko**, **Go (囲碁) vs Aiko** or **Koi-Koi (こいこい) vs Aiko**.

---

## Related

- [Aiko-chan](https://github.com/OppaAI/Aiko-chan) — backend (`interface/android_app/shogi`, `interface/android_app/go`)
- [Aiko-Lingo](https://github.com/OppaAI/Aiko-Lingo) — Japanese learning app

## License

Apache License 2.0 — see [LICENSE](LICENSE).
