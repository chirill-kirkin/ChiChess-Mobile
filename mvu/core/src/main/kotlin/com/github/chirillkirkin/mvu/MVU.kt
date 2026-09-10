package com.github.chirillkirkin.mvu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Produces a new state and commands from a message and the current state. */
public typealias Update<Message, State, Command> =
  (message: Message, state: State) -> UpdateResult<State, Command>

/** Executes a command and emits the messages produced by that execution. */
public typealias CommandExecutor<Command, Message> = (command: Command) -> Flow<Message>

/** Produces messages for as long as its state-driven subscription remains active. */
public typealias Subscription<State, Message> = (state: StateFlow<State>) -> Flow<Message>

/**
 * A running Model-View-Update feature.
 *
 * [Message] values are reduced into a new [State] and zero or more [Command] values.
 */
public interface MVU<Message, State, Command> {
  /** The current state and all subsequent state changes. */
  public val state: StateFlow<State>

  /** Commands emitted by updates for the UI to observe. Can be used for tests. */
  public val commands: Flow<Command>

  /** Sends a message to the update loop. */
  public fun send(message: Message)

  /** Starts this MVU feature in [scope]. */
  public fun launchIn(scope: CoroutineScope)
}
