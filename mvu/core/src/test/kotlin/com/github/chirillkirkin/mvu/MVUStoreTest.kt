package com.github.chirillkirkin.mvu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class MVUStoreTest {

  @Test
  fun `messages are processed sequentially`() = runTest {
    val store = createStore()
    store.launchIn(backgroundScope)

    store.send(Message.Add(1))
    store.send(Message.Add(2))
    store.send(Message.Add(3))
    runCurrent()

    assertEquals(State(total = 6), store.state.value)
  }

  @Test
  fun `commands are published and executor messages return to update`() = runTest {
    val observedCommands = mutableListOf<Command>()
    val store = createStore(
      commandExecutor = { command -> flowOf(Message.Completed(command.id)) },
    )

    backgroundScope.launch {
      store.commands.collect(observedCommands::add)
    }
    store.launchIn(backgroundScope)

    store.send(Message.StartWork(listOf(1, 2)))
    runCurrent()

    val expectedCommands: List<Command> = listOf(Command.Work(1), Command.Work(2))
    assertEquals(expectedCommands, observedCommands)
    assertEquals(2, store.state.value.workCompletedCount)
  }

  @Test
  fun `initial commands are published and executed`() = runTest {
    val observedCommands = mutableListOf<Command>()
    val store = createStore(
      initialCommands = listOf(Command.Work(7)),
      commandExecutor = { command -> flowOf(Message.Completed(command.id)) },
    )

    backgroundScope.launch {
      store.commands.collect(observedCommands::add)
    }
    store.launchIn(backgroundScope)
    runCurrent()

    val expectedCommands: List<Command> = listOf(Command.Work(7))
    assertEquals(expectedCommands, observedCommands)
    assertEquals(1, store.state.value.workCompletedCount)
  }

  @Test
  fun `command execution respects concurrency limit`() = runTest {
    var activeExecutions = 0
    var maximumActiveExecutions = 0
    val store = createStore(
      initialCommands = listOf(Command.Work(1), Command.Work(2), Command.Work(3)),
      concurrency = 2,
      commandExecutor = { command ->
        flow {
          activeExecutions += 1
          maximumActiveExecutions = maxOf(maximumActiveExecutions, activeExecutions)
          try {
            delay(1_000)
            emit(Message.Completed(command.id))
          } finally {
            activeExecutions -= 1
          }
        }
      },
    )

    store.launchIn(backgroundScope)
    runCurrent()

    assertEquals(2, maximumActiveExecutions)
    assertEquals(2, activeExecutions)

    advanceTimeBy(1_000)
    runCurrent()

    assertEquals(2, maximumActiveExecutions)
    advanceTimeBy(1_000)
    runCurrent()
    assertEquals(3, store.state.value.workCompletedCount)
  }

  @Test
  fun `cancelling owner scope stops message processing`() = runTest {
    val ownerJob = SupervisorJob()
    val ownerScope = CoroutineScope(ownerJob + StandardTestDispatcher(testScheduler))
    val store = createStore()
    store.launchIn(ownerScope)

    store.send(Message.Add(1))
    runCurrent()
    assertEquals(1, store.state.value.total)

    ownerScope.cancel()
    store.send(Message.Add(1))
    runCurrent()

    assertEquals(1, store.state.value.total)
  }

  private fun createStore(
    initialCommands: List<Command> = emptyList(),
    commandExecutor: CommandExecutor<Command, Message> = { emptyFlow() },
    concurrency: Int = 16,
  ): MVUStore<Message, State, Command> = MVUStore(
    initialState = State(),
    update = { message, state ->
      when (message) {
        is Message.Add -> state.copy(total = state.total + message.value).only()
        is Message.StartWork -> state.andCommands(message.ids.map(Command::Work))
        is Message.Completed -> state.copy(workCompletedCount = state.workCompletedCount + 1).only()
      }
    },
    commandExecutor = commandExecutor,
    initialCommands = initialCommands,
    concurrency = concurrency,
  )

  private data class State(
    val total: Int = 0,
    val workCompletedCount: Int = 0,
  )

  private sealed interface Message {
    data class Add(val value: Int) : Message
    data class StartWork(val ids: List<Int>) : Message
    data class Completed(val id: Int) : Message
  }

  private sealed interface Command {
    val id: Int

    data class Work(override val id: Int) : Command
  }
}
