package com.github.chirillkirkin.mvu.savedstate

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.github.chirillkirkin.mvu.CommandExecutor
import com.github.chirillkirkin.mvu.Update
import com.github.chirillkirkin.mvu.only
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class SavedStateMVUStoreTest {
  @Test
  fun `full state is restored and reported`() {
    val restoredState = ParcelableState(count = RESTORED_COUNT)
    val savedStateHandle = SavedStateHandle(mapOf(STATE_KEY to restoredState))
    var initialCommandsReceivedRestoredState: Boolean? = null

    val store = savedStateHandle.mvuStore(
      initialState = ParcelableState(count = FRESH_COUNT),
      update = parcelableUpdate,
      commandExecutor = parcelableCommandExecutor,
      stateKey = STATE_KEY,
      initialCommands = { isStateRestored ->
        initialCommandsReceivedRestoredState = isStateRestored
        emptyList()
      },
    )

    assertEquals(restoredState, store.state.value)
    assertTrue(store.isStateRestored)
    assertEquals(true, initialCommandsReceivedRestoredState)
  }

  @Test
  fun `initial state is used when saved state is absent`() {
    val initialState = ParcelableState(count = INITIAL_COUNT)
    var initialCommandsReceivedRestoredState: Boolean? = null

    val store = SavedStateHandle().mvuStore(
      initialState = initialState,
      update = parcelableUpdate,
      commandExecutor = parcelableCommandExecutor,
      stateKey = STATE_KEY,
      initialCommands = { isStateRestored ->
        initialCommandsReceivedRestoredState = isStateRestored
        emptyList()
      },
    )

    assertEquals(initialState, store.state.value)
    assertFalse(store.isStateRestored)
    assertEquals(false, initialCommandsReceivedRestoredState)
  }

  @Test
  fun `initial and updated full state are saved`() = runTest {
    val savedStateHandle = SavedStateHandle()
    val store = savedStateHandle.mvuStore(
      initialState = ParcelableState(count = INITIAL_COUNT),
      update = parcelableUpdate,
      commandExecutor = parcelableCommandExecutor,
      stateKey = STATE_KEY,
    )

    store.launchIn(backgroundScope)
    runCurrent()
    assertEquals(ParcelableState(count = INITIAL_COUNT), savedStateHandle[STATE_KEY])

    store.send(Message.Increment)
    runCurrent()

    assertEquals(ParcelableState(count = UPDATED_COUNT), store.state.value)
    assertEquals(ParcelableState(count = UPDATED_COUNT), savedStateHandle[STATE_KEY])
  }

  @Test
  fun `state projection is restored and saved`() = runTest {
    val savedStateHandle = SavedStateHandle(mapOf(STATE_KEY to RESTORED_COUNT))
    val update: Update<Message, ProjectedState, Command> = { message, state ->
      when (message) {
        Message.Increment ->
          state
            .copy(
              count = state.count + INCREMENT,
              transientValue = UPDATED_TRANSIENT_VALUE,
            ).only()
      }
    }
    val commandExecutor: CommandExecutor<Command, Message> = { emptyFlow() }
    val store = savedStateHandle.mvuStore(
      initialState = ProjectedState(
        count = FRESH_COUNT,
        transientValue = FRESH_TRANSIENT_VALUE,
      ),
      update = update,
      commandExecutor = commandExecutor,
      saveState = ProjectedState::count,
      restoreState = { savedCount, initialState -> initialState.copy(count = savedCount) },
      stateKey = STATE_KEY,
    )

    assertEquals(
      ProjectedState(count = RESTORED_COUNT, transientValue = FRESH_TRANSIENT_VALUE),
      store.state.value,
    )
    assertTrue(store.isStateRestored)

    store.launchIn(backgroundScope)
    store.send(Message.Increment)
    runCurrent()

    assertEquals(
      ProjectedState(count = RESTORED_COUNT + INCREMENT, transientValue = UPDATED_TRANSIENT_VALUE),
      store.state.value,
    )
    assertEquals(RESTORED_COUNT + INCREMENT, savedStateHandle[STATE_KEY])
  }

  @Parcelize
  private data class ParcelableState(val count: Int) : Parcelable

  private data class ProjectedState(val count: Int, val transientValue: String)

  private sealed interface Message {
    data object Increment : Message
  }

  private sealed interface Command

  private companion object {
    const val STATE_KEY = "test_state"
    const val FRESH_COUNT = 0
    const val INITIAL_COUNT = 2
    const val RESTORED_COUNT = 7
    const val INCREMENT = 1
    const val UPDATED_COUNT = INITIAL_COUNT + INCREMENT
    const val FRESH_TRANSIENT_VALUE = "fresh"
    const val UPDATED_TRANSIENT_VALUE = "changed"

    val parcelableUpdate: Update<Message, ParcelableState, Command> = { message, state ->
      when (message) {
        Message.Increment -> state.copy(count = state.count + INCREMENT).only()
      }
    }

    val parcelableCommandExecutor: CommandExecutor<Command, Message> = { emptyFlow() }
  }
}
