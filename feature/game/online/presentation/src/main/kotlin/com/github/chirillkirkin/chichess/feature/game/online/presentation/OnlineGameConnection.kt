package com.github.chirillkirkin.chichess.feature.game.online.presentation

import androidx.lifecycle.SavedStateHandle
import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameChannel
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameEvent
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameSession
import com.github.chirillkirkin.mvu.CommandExecutor
import com.github.chirillkirkin.mvu.savedstate.SavedStateMVU
import com.github.chirillkirkin.mvu.savedstate.mvuStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Holds the live [OnlineGameSession] so incoming events and outgoing commands share one socket. */
class OnlineGameConnection(
  private val channel: OnlineGameChannel,
) {
  private var session: OnlineGameSession? = null

  fun events(gameId: String): Flow<OnlineGameEvent> =
    flow {
      val opened = channel.connect(gameId)
      session = opened
      emitAll(opened.events)
    }

  suspend fun send(command: OnlineGameCommand) {
    val session = session ?: return
    when (command) {
      OnlineGameCommand.Connect -> Unit
      is OnlineGameCommand.SendMove -> session.makeMove(command.commandId, command.expectedRevision, command.move)
      is OnlineGameCommand.SendResign -> session.resign(command.commandId, command.expectedRevision)
      is OnlineGameCommand.SendOfferDraw -> session.offerDraw(command.commandId)
      is OnlineGameCommand.SendAcceptDraw -> session.acceptDraw(command.commandId)
      is OnlineGameCommand.SendDeclineDraw -> session.declineDraw(command.commandId)
      is OnlineGameCommand.SendClaimDraw -> session.claimDraw(command.commandId, command.expectedRevision)
    }
  }
}

internal fun onlineGameCommandExecutor(
  connection: OnlineGameConnection,
  gameId: String,
): CommandExecutor<OnlineGameCommand, OnlineGameMessage> =
  { command ->
    when (command) {
      OnlineGameCommand.Connect -> connection.events(gameId).map { OnlineGameMessage.Event(it) }
      else -> flow { connection.send(command) } // side effect only; results arrive as events
    }
  }

private const val ONLINE_GAME_STATE_KEY = "online_game"

fun SavedStateHandle.onlineGameStore(
  gameId: String,
  connection: OnlineGameConnection,
  gameEngine: ChessGameEngine,
  newCommandId: () -> String = { UUID.randomUUID().toString() },
): SavedStateMVU<OnlineGameMessage, OnlineGameState, OnlineGameCommand> =
  mvuStore(
    initialState = OnlineGameState(gameId = gameId),
    update = onlineGameUpdate(gameEngine, newCommandId),
    commandExecutor = onlineGameCommandExecutor(connection, gameId),
    saveState = OnlineGameState::toSavedOnlineGame,
    restoreState = { saved, initial -> initial.restoredFrom(saved, gameEngine) },
    stateKey = ONLINE_GAME_STATE_KEY,
    initialCommands = { listOf(OnlineGameCommand.Connect) },
  )
