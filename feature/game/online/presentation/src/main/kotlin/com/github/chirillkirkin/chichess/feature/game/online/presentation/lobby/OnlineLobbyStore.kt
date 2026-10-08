package com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby

import com.github.chirillkirkin.chichess.feature.game.online.domain.JoinGameResult
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameRepository
import com.github.chirillkirkin.mvu.CommandExecutor
import com.github.chirillkirkin.mvu.MVUStore
import com.github.chirillkirkin.mvu.Update
import com.github.chirillkirkin.mvu.update
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal val onlineLobbyUpdate: Update<OnlineLobbyMessage, OnlineLobbyState, OnlineLobbyCommand> =
  update { message, state ->
    when (message) {
      is OnlineLobbyMessage.InviteCodeChanged -> state(state.copy(inviteCode = message.value, error = null))

      OnlineLobbyMessage.CreateGame ->
        if (!state.busy) {
          state(state.copy(busy = true, error = null))
          command(OnlineLobbyCommand.CreateGame)
        }

      OnlineLobbyMessage.JoinGame ->
        if (!state.busy && state.inviteCode.isNotBlank()) {
          state(state.copy(busy = true, error = null))
          command(OnlineLobbyCommand.JoinGame(state.inviteCode))
        }

      is OnlineLobbyMessage.GameReady -> {
        state(state.copy(busy = false))
        command(OnlineLobbyCommand.OpenGame(message.gameId))
      }

      is OnlineLobbyMessage.Failed -> state(state.copy(busy = false, error = message.error))
    }
  }

internal fun JoinGameResult.toLobbyMessage(): OnlineLobbyMessage = when (this) {
  is JoinGameResult.Joined -> OnlineLobbyMessage.GameReady(gameId)
  JoinGameResult.NotFound -> OnlineLobbyMessage.Failed(LobbyError.NOT_FOUND)
  JoinGameResult.OwnGame -> OnlineLobbyMessage.Failed(LobbyError.OWN_GAME)
  JoinGameResult.AlreadyJoined -> OnlineLobbyMessage.Failed(LobbyError.ALREADY_JOINED)
}

internal fun onlineLobbyCommandExecutor(
  repository: OnlineGameRepository,
): CommandExecutor<OnlineLobbyCommand, OnlineLobbyMessage> = { command ->
  when (command) {
    OnlineLobbyCommand.CreateGame ->
      flow { emit(lobbyResult { OnlineLobbyMessage.GameReady(repository.createGame().gameId) }) }

    is OnlineLobbyCommand.JoinGame ->
      flow { emit(lobbyResult { repository.joinGame(command.inviteCode).toLobbyMessage() }) }

    is OnlineLobbyCommand.OpenGame -> emptyFlow() // navigation only; the UI observes it
  }
}

private inline fun lobbyResult(block: () -> OnlineLobbyMessage): OnlineLobbyMessage = try {
  block()
} catch (cancellation: CancellationException) {
  throw cancellation
} catch (_: Exception) {
  OnlineLobbyMessage.Failed(LobbyError.GENERIC)
}

@ViewModelScoped
class OnlineLobbyStore @Inject constructor(repository: OnlineGameRepository) :
  MVUStore<OnlineLobbyMessage, OnlineLobbyState, OnlineLobbyCommand>(
    initialState = OnlineLobbyState(),
    update = onlineLobbyUpdate,
    commandExecutor = onlineLobbyCommandExecutor(repository),
  )
