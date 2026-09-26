package com.github.chirillkirkin.chichess.feature.game.online.presentation.game

import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.engine.ChesslibGameEngine
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameEvent
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameSnapshot
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameStatus
import kotlin.test.Test
import kotlin.test.assertEquals

private const val GAME_ID = "game-1"
private const val INVITE_CODE = "INV0000000"
private const val REVISION = 4L
private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

class SavedOnlineGameStateTest {
  private val engine = ChesslibGameEngine()

  @Test
  fun `saves the confirmed game and restores a rendered board`() {
    val update = onlineGameUpdate(engine) { "cmd" }
    val connected = update(snapshotMessage(), OnlineGameState(GAME_ID)).state

    val restored = OnlineGameState(GAME_ID).restoredFrom(connected.toSavedOnlineGame(), engine)

    assertEquals(PieceColor.WHITE, restored.yourColor)
    assertEquals(REVISION, restored.revision)
    assertEquals(OnlineGameStatus.IN_PROGRESS, restored.status)
    assertEquals(connected.board?.position, restored.board?.position)
  }

  @Test
  fun `restore is a no-op before the first snapshot`() {
    val initial = OnlineGameState(GAME_ID)

    assertEquals(initial, initial.restoredFrom(initial.toSavedOnlineGame(), engine))
  }

  private fun snapshotMessage() =
    OnlineGameMessage.Event(
      OnlineGameEvent.Snapshot(
        OnlineGameSnapshot(
          gameId = GAME_ID,
          inviteCode = INVITE_CODE,
          yourColor = PieceColor.WHITE,
          status = OnlineGameStatus.IN_PROGRESS,
          revision = REVISION,
          fen = Fen(START_FEN),
        ),
      ),
    )
}
