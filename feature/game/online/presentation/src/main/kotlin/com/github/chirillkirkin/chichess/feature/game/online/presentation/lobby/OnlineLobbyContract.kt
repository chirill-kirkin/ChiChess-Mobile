package com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby

enum class LobbyError { NOT_FOUND, OWN_GAME, ALREADY_JOINED, GENERIC }

data class OnlineLobbyState(
  val inviteCode: String = "",
  val busy: Boolean = false,
  val error: LobbyError? = null,
)

sealed interface OnlineLobbyMessage {
  data class InviteCodeChanged(val value: String) : OnlineLobbyMessage

  data object CreateGame : OnlineLobbyMessage

  data object JoinGame : OnlineLobbyMessage

  data class GameReady(val gameId: String) : OnlineLobbyMessage

  data class Failed(val error: LobbyError) : OnlineLobbyMessage
}

sealed interface OnlineLobbyCommand {
  data object CreateGame : OnlineLobbyCommand

  data class JoinGame(val inviteCode: String) : OnlineLobbyCommand

  data class OpenGame(val gameId: String) : OnlineLobbyCommand
}
