@file:OptIn(FlowPreview::class)

package com.github.chirillkirkin.mvu.savedstate

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.github.chirillkirkin.mvu.CommandExecutor
import com.github.chirillkirkin.mvu.MVU
import com.github.chirillkirkin.mvu.MVUStore
import com.github.chirillkirkin.mvu.Subscription
import com.github.chirillkirkin.mvu.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** An [MVU] runtime that reports whether its initial state was restored. */
public interface SavedStateMVU<Message, State, Command> : MVU<Message, State, Command> {
  /** Whether the runtime restored state from its [SavedStateHandle]. */
  public val isStateRestored: Boolean
}

/**
 * A [SavedStateMVU] decorator that restores state from and saves state to a [SavedStateHandle].
 */
public class SavedStateMVUStore<Message, State, Command> internal constructor(
  private val delegate: MVU<Message, State, Command>,
  private val savedStateHandle: SavedStateHandle,
  private val stateKey: String,
  private val saveState: (State) -> Any,
  override val isStateRestored: Boolean,
) : SavedStateMVU<Message, State, Command> {
  override val state: StateFlow<State> = delegate.state
  override val commands: Flow<Command> = delegate.commands

  override fun send(message: Message) {
    delegate.send(message)
  }

  override fun launchIn(scope: CoroutineScope) {
    delegate.state
      .onEach { state -> savedStateHandle[stateKey] = saveState(state) }
      .launchIn(scope)

    delegate.launchIn(scope)
  }
}

/**
 * Creates an MVU store that persists its entire [Parcelable] state.
 *
 * [initialCommands] receives whether state was restored, allowing initialization work to be
 * skipped after system-initiated process recreation. Keep the state small and use the projection
 * overload for state containing data that does not need to survive process death.
 */
public fun <Message, State : Parcelable, Command> SavedStateHandle.mvuStore(
  initialState: State,
  update: Update<Message, State, Command>,
  commandExecutor: CommandExecutor<Command, Message>,
  stateKey: String,
  initialCommands: (isStateRestored: Boolean) -> List<Command> = { emptyList() },
  subscriptions: List<Subscription<State, Message>> = emptyList(),
  concurrency: Int = DEFAULT_CONCURRENCY,
  onCommandException: (command: Command, throwable: Throwable) -> Unit = { _, _ -> },
): SavedStateMVUStore<Message, State, Command> = createMVUStore(
  initialState = initialState,
  update = update,
  commandExecutor = commandExecutor,
  saveState = { state -> state },
  restoreState = { savedState, _ -> savedState },
  stateKey = stateKey,
  initialCommands = initialCommands,
  subscriptions = subscriptions,
  concurrency = concurrency,
  onCommandException = onCommandException,
)

/**
 * Creates an MVU store that persists a projection of its state.
 *
 * [SavedState] must be a type supported by [SavedStateHandle]. [restoreState] merges the saved
 * projection with [initialState], so non-persisted fields retain their fresh initial values.
 */
public fun <Message, State : Any, SavedState : Any, Command> SavedStateHandle.mvuStore(
  initialState: State,
  update: Update<Message, State, Command>,
  commandExecutor: CommandExecutor<Command, Message>,
  saveState: (State) -> SavedState,
  restoreState: (savedState: SavedState, initialState: State) -> State,
  stateKey: String,
  initialCommands: (isStateRestored: Boolean) -> List<Command> = { emptyList() },
  subscriptions: List<Subscription<State, Message>> = emptyList(),
  concurrency: Int = DEFAULT_CONCURRENCY,
  onCommandException: (command: Command, throwable: Throwable) -> Unit = { _, _ -> },
): SavedStateMVUStore<Message, State, Command> = createMVUStore(
  initialState = initialState,
  update = update,
  commandExecutor = commandExecutor,
  saveState = saveState,
  restoreState = restoreState,
  stateKey = stateKey,
  initialCommands = initialCommands,
  subscriptions = subscriptions,
  concurrency = concurrency,
  onCommandException = onCommandException,
)

private fun <Message, State : Any, SavedState : Any, Command> SavedStateHandle.createMVUStore(
  initialState: State,
  update: Update<Message, State, Command>,
  commandExecutor: CommandExecutor<Command, Message>,
  saveState: (State) -> SavedState,
  restoreState: (savedState: SavedState, initialState: State) -> State,
  stateKey: String,
  initialCommands: (isStateRestored: Boolean) -> List<Command>,
  subscriptions: List<Subscription<State, Message>>,
  concurrency: Int,
  onCommandException: (command: Command, throwable: Throwable) -> Unit,
): SavedStateMVUStore<Message, State, Command> {
  val savedState = get<SavedState>(stateKey)
  val isStateRestored = savedState != null
  val state = savedState?.let { restoreState(it, initialState) } ?: initialState
  val store = MVUStore(
    initialState = state,
    update = update,
    commandExecutor = commandExecutor,
    initialCommands = initialCommands(isStateRestored),
    subscriptions = subscriptions,
    concurrency = concurrency,
    onCommandException = onCommandException,
  )

  return SavedStateMVUStore(
    delegate = store,
    savedStateHandle = this,
    stateKey = stateKey,
    saveState = saveState,
    isStateRestored = isStateRestored,
  )
}
