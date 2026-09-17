package com.github.chirillkirkin.chichess.feature.game.engine

import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPiece
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.GameStatus
import com.github.chirillkirkin.chichess.feature.game.domain.MoveApplicationResult
import com.github.chirillkirkin.chichess.feature.game.domain.MoveRejectionReason
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.PieceType
import com.github.chirillkirkin.chichess.feature.game.domain.PromotionPiece
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.domain.initialChessPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChesslibGameEngineTest {
  private val engine = ChesslibGameEngine()

  @Test
  fun `initial legal moves are mapped to domain coordinates`() {
    val legalMoves = engine.legalMoves(initialChessPosition())

    assertEquals(InitialLegalMoveCount, legalMoves.size)
    assertTrue(
      ChessMove(
        from = Square(ChessFile.E, ChessRank.TWO),
        to = Square(ChessFile.E, ChessRank.FOUR),
      ) in legalMoves,
    )
    assertTrue(
      ChessMove(
        from = Square(ChessFile.G, ChessRank.ONE),
        to = Square(ChessFile.F, ChessRank.THREE),
      ) in legalMoves,
    )
  }

  @Test
  fun `legal move returns an updated domain snapshot`() {
    val initialPosition = initialChessPosition()
    val origin = Square(ChessFile.E, ChessRank.TWO)
    val destination = Square(ChessFile.E, ChessRank.FOUR)
    val movingPiece = initialPosition[origin]

    val result = engine.applyMove(initialPosition, ChessMove(origin, destination))

    val updatedPosition = assertIs<MoveApplicationResult.Applied>(result).position
    assertNull(updatedPosition[origin])
    assertEquals(movingPiece, updatedPosition[destination])
    assertEquals(PieceColor.BLACK, updatedPosition.sideToMove)
    assertNotEquals(initialPosition.fen, updatedPosition.fen)
  }

  @Test
  fun `move absent from chesslib legal moves is rejected`() {
    val illegalMove =
      ChessMove(
        from = Square(ChessFile.E, ChessRank.TWO),
        to = Square(ChessFile.E, ChessRank.FIVE),
      )

    assertEquals(
      MoveApplicationResult.Rejected(MoveRejectionReason.ILLEGAL_MOVE),
      engine.applyMove(initialChessPosition(), illegalMove),
    )
  }

  @Test
  fun `promotion choice is required and mapped to the resulting piece`() {
    val origin = Square(ChessFile.A, ChessRank.SEVEN)
    val destination = Square(ChessFile.A, ChessRank.EIGHT)
    val position = promotionPosition(origin)

    assertEquals(
      MoveApplicationResult.Rejected(MoveRejectionReason.PROMOTION_REQUIRED),
      engine.applyMove(position, ChessMove(origin, destination)),
    )

    val result =
      engine.applyMove(
        position,
        ChessMove(origin, destination, promotion = PromotionPiece.QUEEN),
      )
    val updatedPosition = assertIs<MoveApplicationResult.Applied>(result).position

    assertEquals(
      ChessPiece(color = PieceColor.WHITE, type = PieceType.QUEEN),
      updatedPosition[destination],
    )
  }

  @Test
  fun `initial position remains ongoing`() {
    assertEquals(GameStatus.Ongoing, engine.gameStatus(initialChessPosition()))
  }

  @Test
  fun `checked king square is mapped in check and checkmate`() {
    val checkedPosition =
      ChessPosition.fromSnapshot(
        fen = Fen(CheckFenValue),
        pieces =
          mapOf(
            Square(ChessFile.E, ChessRank.EIGHT) to ChessPiece(PieceColor.BLACK, PieceType.KING),
            Square(ChessFile.E, ChessRank.TWO) to ChessPiece(PieceColor.WHITE, PieceType.QUEEN),
            Square(ChessFile.E, ChessRank.ONE) to ChessPiece(PieceColor.WHITE, PieceType.KING),
          ),
        sideToMove = PieceColor.BLACK,
      )
    val matedPosition =
      queenEndgamePosition(
        fen = CheckmateFenValue,
        queenSquare = Square(ChessFile.G, ChessRank.SEVEN),
        whiteKingSquare = Square(ChessFile.F, ChessRank.SIX),
      )

    assertNull(engine.checkedKingSquare(initialChessPosition()))
    assertEquals(GameStatus.Ongoing, engine.gameStatus(checkedPosition))
    assertEquals(Square(ChessFile.E, ChessRank.EIGHT), engine.checkedKingSquare(checkedPosition))
    assertEquals(Square(ChessFile.H, ChessRank.EIGHT), engine.checkedKingSquare(matedPosition))
  }

  @Test
  fun `checkmate identifies the winning side`() {
    val position =
      queenEndgamePosition(
        fen = CheckmateFenValue,
        queenSquare = Square(ChessFile.G, ChessRank.SEVEN),
        whiteKingSquare = Square(ChessFile.F, ChessRank.SIX),
      )

    assertEquals(GameStatus.Checkmate(PieceColor.WHITE), engine.gameStatus(position))
  }

  @Test
  fun `stalemate is distinguished from checkmate`() {
    val position =
      queenEndgamePosition(
        fen = StalemateFenValue,
        queenSquare = Square(ChessFile.F, ChessRank.SEVEN),
        whiteKingSquare = Square(ChessFile.G, ChessRank.SIX),
      )

    assertEquals(GameStatus.Draw(DrawReason.STALEMATE), engine.gameStatus(position))
  }

  @Test
  fun `repetition can be claimed on third occurrence and ends automatically on fifth`() {
    val whiteKnightHome = Square(ChessFile.G, ChessRank.ONE)
    val whiteKnightAway = Square(ChessFile.F, ChessRank.THREE)
    val blackKnightHome = Square(ChessFile.G, ChessRank.EIGHT)
    val blackKnightAway = Square(ChessFile.F, ChessRank.SIX)
    val cycle =
      listOf(
        ChessMove(whiteKnightHome, whiteKnightAway),
        ChessMove(blackKnightHome, blackKnightAway),
        ChessMove(whiteKnightAway, whiteKnightHome),
        ChessMove(blackKnightAway, blackKnightHome),
      )
    val afterOneCycle = cycle.fold(initialChessPosition()) { position, move ->
      assertIs<MoveApplicationResult.Applied>(engine.applyMove(position, move)).position
    }
    val afterTwoCycles = cycle.fold(afterOneCycle) { position, move ->
      assertIs<MoveApplicationResult.Applied>(engine.applyMove(position, move)).position
    }
    val afterFourCycles = (cycle + cycle).fold(afterTwoCycles) { position, move ->
      assertIs<MoveApplicationResult.Applied>(engine.applyMove(position, move)).position
    }

    assertEquals(GameStatus.Ongoing, engine.gameStatus(afterOneCycle))
    assertNull(engine.claimableDrawReason(afterOneCycle))
    assertEquals(GameStatus.Ongoing, engine.gameStatus(afterTwoCycles))
    assertEquals(DrawReason.THREEFOLD_REPETITION, engine.claimableDrawReason(afterTwoCycles))
    assertEquals(GameStatus.Draw(DrawReason.FIVEFOLD_REPETITION), engine.gameStatus(afterFourCycles))
  }

  @Test
  fun `fifty move rule can be claimed and seventy five moves ends the game`() {
    val rookSquare = Square(ChessFile.H, ChessRank.ONE)
    val beforeClaim = rookEndgamePosition(BeforeFiftyMoveDrawFenValue)
    val beforeAutomaticDraw = rookEndgamePosition(BeforeSeventyFiveMoveDrawFenValue)
    val claimablePosition =
      assertIs<MoveApplicationResult.Applied>(
        engine.applyMove(beforeClaim, ChessMove(rookSquare, Square(ChessFile.H, ChessRank.TWO))),
      ).position
    val automaticDrawPosition =
      assertIs<MoveApplicationResult.Applied>(
        engine.applyMove(beforeAutomaticDraw, ChessMove(rookSquare, Square(ChessFile.H, ChessRank.TWO))),
      ).position

    assertNull(engine.claimableDrawReason(beforeClaim))
    assertEquals(GameStatus.Ongoing, engine.gameStatus(claimablePosition))
    assertEquals(DrawReason.FIFTY_MOVE_RULE, engine.claimableDrawReason(claimablePosition))
    assertEquals(GameStatus.Draw(DrawReason.SEVENTY_FIVE_MOVE_RULE), engine.gameStatus(automaticDrawPosition))
  }

  @Test
  fun `kings alone are a draw by insufficient material`() {
    val position =
      ChessPosition.fromSnapshot(
        fen = Fen(KingsOnlyFenValue),
        pieces =
          mapOf(
            Square(ChessFile.E, ChessRank.EIGHT) to ChessPiece(PieceColor.BLACK, PieceType.KING),
            Square(ChessFile.E, ChessRank.ONE) to ChessPiece(PieceColor.WHITE, PieceType.KING),
          ),
        sideToMove = PieceColor.WHITE,
      )

    assertEquals(GameStatus.Draw(DrawReason.INSUFFICIENT_MATERIAL), engine.gameStatus(position))
  }

  private fun queenEndgamePosition(
    fen: String,
    queenSquare: Square,
    whiteKingSquare: Square,
  ): ChessPosition =
    ChessPosition.fromSnapshot(
      fen = Fen(fen),
      pieces =
        mapOf(
          queenSquare to ChessPiece(PieceColor.WHITE, PieceType.QUEEN),
          whiteKingSquare to ChessPiece(PieceColor.WHITE, PieceType.KING),
          Square(ChessFile.H, ChessRank.EIGHT) to ChessPiece(PieceColor.BLACK, PieceType.KING),
        ),
      sideToMove = PieceColor.BLACK,
    )

  private fun promotionPosition(pawnSquare: Square): ChessPosition =
    ChessPosition.fromSnapshot(
      fen = Fen(PromotionPositionFenValue),
      pieces =
        mapOf(
          pawnSquare to ChessPiece(PieceColor.WHITE, PieceType.PAWN),
          Square(ChessFile.H, ChessRank.EIGHT) to
            ChessPiece(PieceColor.BLACK, PieceType.KING),
          Square(ChessFile.H, ChessRank.ONE) to
            ChessPiece(PieceColor.WHITE, PieceType.KING),
        ),
      sideToMove = PieceColor.WHITE,
    )

  private fun rookEndgamePosition(fen: String): ChessPosition =
    ChessPosition.fromSnapshot(
      fen = Fen(fen),
      pieces =
        mapOf(
          Square(ChessFile.E, ChessRank.EIGHT) to ChessPiece(PieceColor.BLACK, PieceType.KING),
          Square(ChessFile.E, ChessRank.ONE) to ChessPiece(PieceColor.WHITE, PieceType.KING),
          Square(ChessFile.H, ChessRank.ONE) to ChessPiece(PieceColor.WHITE, PieceType.ROOK),
        ),
      sideToMove = PieceColor.WHITE,
    )

  private companion object {
    const val InitialLegalMoveCount = 20
    const val PromotionPositionFenValue = "7k/P7/8/8/8/8/8/7K w - - 0 1"
    const val CheckFenValue = "4k3/8/8/8/8/8/4Q3/4K3 b - - 0 1"
    const val CheckmateFenValue = "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1"
    const val StalemateFenValue = "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"
    const val BeforeFiftyMoveDrawFenValue = "4k3/8/8/8/8/8/8/4K2R w - - 99 51"
    const val BeforeSeventyFiveMoveDrawFenValue = "4k3/8/8/8/8/8/8/4K2R w - - 149 76"
    const val KingsOnlyFenValue = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"
  }
}
