package com.github.chirillkirkin.chichess.feature.game.online.presentation.game

import androidx.lifecycle.SavedStateHandle
import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameChannel
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameEvent
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameSession
import com.github.chirillkirkin.mvu.CommandExecutor
import com.github.chirillkirkin.mvu.Subscription
import com.github.chirillkirkin.mvu.savedstate.SavedStateMVU
import com.github.chirillkirkin.mvu.savedstate.mvuStore
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineGameConnection(
  private val channel: OnlineGameChannel,
  private val gameId: String,
) {
  // A distinct instance per request, so reconnecting with the same delay still restarts the socket.
  private class ConnectRequest(val startDelay: Duration)

  private val requests = MutableStateFlow<ConnectRequest?>(null)
  private var session: OnlineGameSession? = null

  val events: Flow<OnlineGameEvent> =
    requests.flatMapLatest { request -> request?.let { socketEvents(it.startDelay) } ?: emptyFlow() }

  suspend fun send(command: OnlineGameCommand) {
    val session = session
    when (command) {
      is OnlineGameCommand.Connect -> requests.value = ConnectRequest(command.delay)
      OnlineGameCommand.Disconnect -> requests.value = null
      is OnlineGameCommand.RequestSync -> session?.requestSync(command.commandId)
      is OnlineGameCommand.SendMove -> session?.makeMove(command.commandId, command.expectedRevision, command.move)
      is OnlineGameCommand.SendResign -> session?.resign(command.commandId, command.expectedRevision)
      is OnlineGameCommand.SendOfferDraw -> session?.offerDraw(command.commandId)
      is OnlineGameCommand.SendAcceptDraw -> session?.acceptDraw(command.commandId)
      is OnlineGameCommand.SendDeclineDraw -> session?.declineDraw(command.commandId)
      is OnlineGameCommand.SendClaimDraw -> session?.claimDraw(command.commandId, command.expectedRevision)
      OnlineGameCommand.ExitOnError -> Unit
    }
  }

  private fun socketEvents(startDelay: Duration): Flow<OnlineGameEvent> =
    flow {
      delay(startDelay)
      val opened =
        try {
          channel.connect(gameId)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          emit(OnlineGameEvent.Closed(permanent = false))
          return@flow
        }
      session = opened
      try {
        emitAll(opened.events)
      } finally {
        session = null
        withContext(NonCancellable) { opened.close() }
      }
    }
}

internal fun onlineGameCommandExecutor(
  connection: OnlineGameConnection,
): CommandExecutor<OnlineGameCommand, OnlineGameMessage> =
  { command -> flow { connection.send(command) } } // side effect only; results arrive as events

private const val ONLINE_GAME_STATE_KEY = "online_game"

fun SavedStateHandle.onlineGameStore(
  gameId: String,
  connection: OnlineGameConnection,
  gameEngine: ChessGameEngine,
  appInForeground: Flow<Boolean>,
  newCommandId: () -> String = { UUID.randomUUID().toString() },
): SavedStateMVU<OnlineGameMessage, OnlineGameState, OnlineGameCommand> =
  mvuStore(
    initialState = OnlineGameState(gameId = gameId),
    update = onlineGameUpdate(gameEngine, newCommandId),
    commandExecutor = onlineGameCommandExecutor(connection),
    saveState = OnlineGameState::toSavedOnlineGame,
    restoreState = { saved, initial -> initial.restoredFrom(saved, gameEngine) },
    stateKey = ONLINE_GAME_STATE_KEY,
    subscriptions = listOf(socketEvents(connection), appVisibility(appInForeground)),
  )

private fun socketEvents(connection: OnlineGameConnection): Subscription<OnlineGameState, OnlineGameMessage> =
  { connection.events.map { OnlineGameMessage.Event(it) } }

private fun appVisibility(appInForeground: Flow<Boolean>): Subscription<OnlineGameState, OnlineGameMessage> =
  {
    appInForeground.distinctUntilChanged().map { inForeground ->
      if (inForeground) OnlineGameMessage.AppForegrounded else OnlineGameMessage.AppBackgrounded
    }
  }
