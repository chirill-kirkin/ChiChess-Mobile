package com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby

import com.github.chirillkirkin.chichess.feature.game.online.domain.JoinGameResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val INVITE_CODE = "CODE123456"
private const val GAME_ID = "game-1"

class OnlineLobbyUpdateTest {
  private val update = onlineLobbyUpdate

  @Test
  fun `create requests a game and marks busy`() {
    val result = update(OnlineLobbyMessage.CreateGame, OnlineLobbyState())

    assertTrue(result.state.busy)
    assertTrue(OnlineLobbyCommand.CreateGame in result.commands)
  }

  @Test
  fun `create is ignored while busy`() {
    val result = update(OnlineLobbyMessage.CreateGame, OnlineLobbyState(busy = true))

    assertTrue(result.commands.isEmpty())
  }

  @Test
  fun `join requires a non-blank code`() {
    val blank = update(OnlineLobbyMessage.JoinGame, OnlineLobbyState(inviteCode = "  "))
    assertTrue(blank.commands.isEmpty())

    val result = update(OnlineLobbyMessage.JoinGame, OnlineLobbyState(inviteCode = INVITE_CODE))
    assertTrue(result.state.busy)
    assertTrue(OnlineLobbyCommand.JoinGame(INVITE_CODE) in result.commands)
  }

  @Test
  fun `game ready opens the game`() {
    val result = update(OnlineLobbyMessage.GameReady(GAME_ID), OnlineLobbyState(busy = true))

    assertEquals(false, result.state.busy)
    assertTrue(OnlineLobbyCommand.OpenGame(GAME_ID) in result.commands)
  }

  @Test
  fun `failure surfaces the error and clears busy`() {
    val result = update(OnlineLobbyMessage.Failed(LobbyError.NOT_FOUND), OnlineLobbyState(busy = true))

    assertEquals(false, result.state.busy)
    assertEquals(LobbyError.NOT_FOUND, result.state.error)
  }

  @Test
  fun `editing the code clears the previous error`() {
    val result = update(OnlineLobbyMessage.InviteCodeChanged("X"), OnlineLobbyState(error = LobbyError.NOT_FOUND))

    assertEquals("X", result.state.inviteCode)
    assertNull(result.state.error)
  }

  @Test
  fun `join results map to lobby messages`() {
    assertEquals(OnlineLobbyMessage.GameReady(GAME_ID), JoinGameResult.Joined(GAME_ID).toLobbyMessage())
    assertEquals(OnlineLobbyMessage.Failed(LobbyError.NOT_FOUND), JoinGameResult.NotFound.toLobbyMessage())
    assertEquals(OnlineLobbyMessage.Failed(LobbyError.OWN_GAME), JoinGameResult.OwnGame.toLobbyMessage())
    assertEquals(OnlineLobbyMessage.Failed(LobbyError.ALREADY_JOINED), JoinGameResult.AlreadyJoined.toLobbyMessage())
  }
}
