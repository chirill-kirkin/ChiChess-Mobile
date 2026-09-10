package com.github.chirillkirkin.mvu

import kotlin.test.Test
import kotlin.test.assertEquals

internal class UpdateTest {
  @Test
  fun `update DSL transforms state and preserves command order`() {
    val update: Update<Message, State, Command> = update { message, currentState ->
      when (message) {
        Message.Increment -> {
          state { copy(value = value + 1) }
          commands(Command.First, Command.Second)
        }

        Message.Replace -> state(currentState.copy(value = 42))
      }
    }

    val result = update(Message.Increment, State(value = 1))
    val replacement = update(Message.Replace, State(value = 1))

    assertEquals(State(value = 2), result.state)
    assertEquals(listOf(Command.First, Command.Second), result.commands)
    assertEquals(State(value = 42), replacement.state)
    assertEquals(emptyList(), replacement.commands)
  }

  @Test
  fun `update result extensions build expected results`() {
    val state = State(value = 1)

    val stateOnly: UpdateResult<State, Command> = state.only()
    val withOneCommand: UpdateResult<State, Command> = state andCommand Command.First
    val withSeveralCommands = state.andCommands(Command.First, Command.Second)

    val expectedStateOnly: UpdateResult<State, Command> = UpdateResult(state, emptyList())
    val expectedOneCommand: UpdateResult<State, Command> =
      UpdateResult(state, listOf(Command.First))

    assertEquals(expectedStateOnly, stateOnly)
    assertEquals(expectedOneCommand, withOneCommand)
    assertEquals(
      UpdateResult(state, listOf(Command.First, Command.Second)),
      withSeveralCommands,
    )
  }

  private data class State(
    val value: Int,
  )

  private sealed interface Message {
    data object Increment : Message
    data object Replace : Message
  }

  private sealed interface Command {
    data object First : Command
    data object Second : Command
  }
}
