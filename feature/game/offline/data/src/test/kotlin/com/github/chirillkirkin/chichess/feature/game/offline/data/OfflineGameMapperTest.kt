package com.github.chirillkirkin.chichess.feature.game.offline.data

import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameEntity
import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameMoveEntity
import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameWithMoves
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.PromotionPiece
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineBoardLayout
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class OfflineGameMapperTest {
  @Test
  fun `maps moves in half-move order`() {
    val snapshot = gameWithMoves(
      OfflineGameMoveEntity(halfMoveIndex = 1, uci = "e7e5"),
      OfflineGameMoveEntity(halfMoveIndex = 0, uci = "e2e4"),
    ).toSnapshot()

    assertEquals(
      listOf(
        ChessMove(Square(ChessFile.E, ChessRank.TWO), Square(ChessFile.E, ChessRank.FOUR)),
        ChessMove(Square(ChessFile.E, ChessRank.SEVEN), Square(ChessFile.E, ChessRank.FIVE)),
      ),
      snapshot.moves,
    )
  }

  @Test
  fun `maps promotion moves`() {
    val snapshot = gameWithMoves(OfflineGameMoveEntity(halfMoveIndex = 0, uci = "a7a8n")).toSnapshot()

    assertEquals(
      ChessMove(
        from = Square(ChessFile.A, ChessRank.SEVEN),
        to = Square(ChessFile.A, ChessRank.EIGHT),
        promotion = PromotionPiece.KNIGHT,
      ),
      snapshot.moves.single(),
    )
  }

  @Test
  fun `maps draw reason and board layout`() {
    val snapshot = OfflineGameWithMoves(
      game = OfflineGameEntity(
        drawReason = DrawReason.THREEFOLD_REPETITION.name,
        boardLayout = OfflineBoardLayout.FLIP_AFTER_MOVE.name,
      ),
      moves = emptyList(),
    ).toSnapshot()

    assertEquals(DrawReason.THREEFOLD_REPETITION, snapshot.drawReason)
    assertEquals(OfflineBoardLayout.FLIP_AFTER_MOVE, snapshot.boardLayout)
  }

  @Test
  fun `maps a missing draw reason to null`() {
    assertNull(gameWithMoves().toSnapshot().drawReason)
  }

  @Test
  fun `maps the stored clock and flag`() {
    val snapshot = OfflineGameWithMoves(
      game = OfflineGameEntity(
        drawReason = null,
        boardLayout = OfflineBoardLayout.STANDARD.name,
        initialMs = 5.minutes.inWholeMilliseconds,
        incrementMs = 3.seconds.inWholeMilliseconds,
        whiteRemainingMs = 4.minutes.inWholeMilliseconds,
        blackRemainingMs = 3.minutes.inWholeMilliseconds,
        flaggedSide = PieceColor.BLACK.name,
      ),
      moves = emptyList(),
    ).toSnapshot()

    assertEquals(
      OfflineClock(TimeControl(initial = 5.minutes, increment = 3.seconds), 4.minutes, 3.minutes),
      snapshot.clock,
    )
    assertEquals(PieceColor.BLACK, snapshot.flaggedSide)
  }

  @Test
  fun `maps a game without a time control to no clock`() {
    val snapshot = gameWithMoves().toSnapshot()

    assertNull(snapshot.clock)
    assertNull(snapshot.flaggedSide)
  }

  @Test
  fun `fails on a corrupted stored move`() {
    assertFailsWith<IllegalStateException> {
      gameWithMoves(OfflineGameMoveEntity(halfMoveIndex = 0, uci = "z9z9")).toSnapshot()
    }
  }
}

private fun gameWithMoves(vararg moves: OfflineGameMoveEntity): OfflineGameWithMoves = OfflineGameWithMoves(
  game = OfflineGameEntity(drawReason = null, boardLayout = OfflineBoardLayout.STANDARD.name),
  moves = moves.toList(),
)
