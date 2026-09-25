package com.github.chirillkirkin.chichess.feature.game.online.data

import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.online.domain.CommandRejection
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameEvent
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameResult
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameStatus
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineTerminationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private const val COMMAND_ID = "cmd-1"
private const val FEN = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"

class GameEventMapperTest {
  @Test
  fun `maps move applied with parsed last move`() {
    val event = MoveAppliedEvent(revision = 8, fen = FEN, status = ApiGameStatus.IN_PROGRESS, lastMove = "e2e4").toOnlineEvent()

    val moveApplied = assertIs<OnlineGameEvent.MoveApplied>(event)
    assertEquals(8L, moveApplied.revision)
    assertEquals(ChessMove(Square(ChessFile.E, ChessRank.TWO), Square(ChessFile.E, ChessRank.FOUR)), moveApplied.lastMove)
    assertEquals(OnlineGameStatus.IN_PROGRESS, moveApplied.status)
    assertNull(moveApplied.result)
  }

  @Test
  fun `maps game finished with result and reason`() {
    val event = GameFinishedEvent(
      revision = 9,
      status = ApiGameStatus.FINISHED,
      result = ApiGameResult.DRAW,
      terminationReason = ApiTerminationReason.AGREEMENT,
    ).toOnlineEvent()

    val finished = assertIs<OnlineGameEvent.GameFinished>(event)
    assertEquals(OnlineGameResult.DRAW, finished.result)
    assertEquals(OnlineTerminationReason.AGREEMENT, finished.terminationReason)
  }

  @Test
  fun `maps draw offered and declined`() {
    assertEquals(PieceColor.WHITE, assertIs<OnlineGameEvent.DrawOffered>(DrawOfferedEvent(ApiPieceColor.WHITE).toOnlineEvent()).by)
    assertIs<OnlineGameEvent.DrawDeclined>(DrawDeclinedEvent.toOnlineEvent())
  }

  @Test
  fun `maps command rejected to a typed reason`() {
    val event = CommandRejectedEvent(commandId = COMMAND_ID, code = "NOT_YOUR_TURN").toOnlineEvent()

    val rejected = assertIs<OnlineGameEvent.CommandRejected>(event)
    assertEquals(COMMAND_ID, rejected.commandId)
    assertEquals(CommandRejection.NOT_YOUR_TURN, rejected.reason)
  }

  @Test
  fun `maps snapshot event`() {
    val response = GameSnapshotResponse(
      gameId = "g",
      inviteCode = "INV0000000",
      yourColor = ApiPieceColor.BLACK,
      status = ApiGameStatus.IN_PROGRESS,
      revision = 7,
      fen = FEN,
    )

    val snapshot = assertIs<OnlineGameEvent.Snapshot>(SnapshotEvent(response).toOnlineEvent()).snapshot
    assertEquals(PieceColor.BLACK, snapshot.yourColor)
  }
}
