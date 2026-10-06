# ChiChess-Mobile Project Guide

## Project

- Product name: `ChiChess`.
- Android application ID and namespace root: `com.github.chirillkirkin.chichess`.
- Declare dependency and plugin versions in `gradle/libs.versions.toml`; do not hardcode them in module build files.

## Static analysis

- Detekt, with the ktlint wrapper and Compose rules, is configured in `config/detekt/detekt.yml` and is not part of `check`.
- Run `./gradlew detektMain detektTest` before every commit and fix all findings before committing. Do not run it at any other time unless the user explicitly asks.

## Module structure

```text
:app
:mvu:core
:mvu:android-savedstate
:core:domain
:core:data
:core:data:network
:core:data:database
:core:designsystem
:feature:home:domain
:feature:home:data
:feature:home:presentation
:feature:game:domain
:feature:game:engine
:feature:game:board
:feature:game:offline
```

The active game dependency graph is:

```text
:app
  -> :core:designsystem
  -> :feature:home:presentation
  -> :feature:game:domain
  -> :feature:game:engine
  -> :feature:game:offline

:feature:game:offline
  -> :feature:game:board
  -> :feature:game:domain
  -> :core:designsystem

:feature:game:board
  -> :feature:game:domain
  -> :core:designsystem

:feature:game:engine
  -> :feature:game:domain
```

- `:app` is the composition root. It owns application setup, app-level resources, dependency-injection wiring, and Navigation 3 wiring.
- `:mvu:core` is the platform-independent MVU runtime. `:mvu:android-savedstate` adds optional Android saved-state support.
- `:core:domain` contains domain concepts shared by unrelated features.
- `:core:data` contains shared data-layer integration. `:core:data:network` and `:core:data:database` contain reusable network and database infrastructure.
- `:core:designsystem` owns the application theme, shared colors, typography, spacing, reusable UI components, and shared drawable resources.
- The `:feature:home:*` modules own their corresponding Home layers.
- `:feature:game:domain` contains shared chess models and engine contracts and must remain independent of Android, Compose, presentation, and data modules.
- `:feature:game:engine` adapts chesslib to the project's domain models. Chesslib types must not cross this module boundary.
- `:feature:game:board` contains the reusable stateless Compose board, board UI state, coordinates, rendering, and square selection.
- `:feature:game:offline` contains the offline screen, MVU/ViewModel orchestration, and local-game behavior.
- Offline depends on the shared game domain and board, not the chesslib implementation; `:app` binds the engine contract to the implementation. The offline unit tests may depend on `:feature:game:engine` directly.
- Unrelated feature modules do not depend on each other. Move code to a core module only when multiple features genuinely share it.

## Domain and UI rules

- Domain code owns semantic chess concepts. UI code maps them to design-system colors, dimensions, text styles, drawables, and Android resources.
- Never expose Android or Compose types from a domain module.
- Use typed files, ranks, and squares instead of raw character or integer coordinates.
- `ChessPosition` exposes type-safe access through `Square`; its storage representation remains private.
- Keep board rendering stateless: it receives `BoardState` and reports interactions through explicit callbacks.
- Shared visual values belong to `:core:designsystem`; feature modules must reuse them instead of defining copies.
- Access application-specific typography through `ChiChessTheme.typography`. Its defaults may delegate to Material typography, but feature code does not access Material typography directly for application-specific styles.
- Keep chess piece vector drawables in `:core:designsystem` and name them `ic_piece_{white|black}_{piece}`.
- Keep user-facing text in string resources owned by the module that displays it. Provide Russian (`values-ru`) translations for new strings while retaining English defaults in `values`; keep the ChiChess app name in `:app`.

## MVU and dependency injection

- Hilt is the Android dependency-injection framework.
- Define a feature-specific Store with an `@Inject` constructor and scope it with `@ViewModelScoped`. Inherit a regular Store from `MVUStore`; delegate a saved-state Store to `SavedStateMVU`.
- Inject the feature Store into its ViewModel under the name `store` and delegate the ViewModel's `MVU` or `SavedStateMVU` contract to it. The ViewModel does not construct or manually forward the runtime.
- Keep a small feature's State, Message, Command, Update, Store, and ViewModel together by default. Split them only when size or distinct responsibilities justify it.

## Navigation

- Use Navigation 3 with typed, serializable `NavKey` routes.
- `:app` owns cross-feature routes and back-stack mutation.
- Feature UI reports navigation intent through commands and callbacks rather than manipulating the app back stack.
- Scope Hilt ViewModels to Navigation 3 entries with the ViewModel-store entry decorator.
