package com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby

import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl

enum class LobbyError { NOT_FOUND, OWN_GAME, ALREADY_JOINED, GENERIC }

data class OnlineLobbyState(
  val inviteCode: String = "",
  // A null time control creates a game without a clock.
  val timeControl: TimeControl? = TimeControl.Default,
  val busy: Boolean = false,
  val error: LobbyError? = null,
)

sealed interface OnlineLobbyMessage {
  data class InviteCodeChanged(val value: String) : OnlineLobbyMessage

  data class TimeControlChanged(val timeControl: TimeControl?) : OnlineLobbyMessage

  data object CreateGame : OnlineLobbyMessage

  data object JoinGame : OnlineLobbyMessage

  data class GameReady(val gameId: String) : OnlineLobbyMessage

  data class Failed(val error: LobbyError) : OnlineLobbyMessage
}

sealed interface OnlineLobbyCommand {
  data class CreateGame(val timeControl: TimeControl?) : OnlineLobbyCommand

  data class JoinGame(val inviteCode: String) : OnlineLobbyCommand

  data class OpenGame(val gameId: String) : OnlineLobbyCommand
}
