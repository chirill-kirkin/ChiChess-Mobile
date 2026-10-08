package com.github.chirillkirkin.chichess.feature.game.online.domain

interface OnlineGameRepository {
  suspend fun createGame(): CreatedGame

  suspend fun joinGame(inviteCode: String): JoinGameResult

  suspend fun snapshot(gameId: String): SnapshotResult

  suspend fun history(): List<OnlineGameSnapshot>
}

sealed interface JoinGameResult {
  data class Joined(val gameId: String) : JoinGameResult

  data object NotFound : JoinGameResult

  data object OwnGame : JoinGameResult

  data object AlreadyJoined : JoinGameResult
}

sealed interface SnapshotResult {
  data class Success(val snapshot: OnlineGameSnapshot) : SnapshotResult

  data object NotFound : SnapshotResult

  data object NotParticipant : SnapshotResult
}
