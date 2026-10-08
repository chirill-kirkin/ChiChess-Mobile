# ChiChess-Mobile

> 🚧 Work in progress

**ChiChess** (Chi[rkin]Chess) — a simple Android chess app.

## Features

- **Offline play** — a local chess game against another player.
- **Online multiplayer** — a lobby for connecting to a game via invite code and a game screen.

## Architecture

The project follows an MVU (so called Model-View-Update, inspired by [The Elm Architecture](https://guide.elm-lang.org/architecture/)) pattern, combined with clean architecture and a breakdown into modules (multi-module architecture).

## Tech stack

- **Language** — Kotlin
- **UI** — Jetpack Compose, Material 3, Navigation 3
- **DI** — Hilt
- **Networking** — Ktor, WebSockets
- **Async / Serialization** — Kotlin Coroutines, Kotlinx Serialization
- **Chess rules & validation library** — [chesslib](https://github.com/bhlangonijr/chesslib)
- **Lint** — Detekt, Ktlint

## Planned features
- **Offline game against computer**
- **Clock**
- **Accounts**
- **Chat**
- **Games history**