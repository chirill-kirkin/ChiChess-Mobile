package com.github.chirillkirkin.chichess.feature.game.engine

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
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
import javax.inject.Inject
import com.github.bhlangonijr.chesslib.File as ChesslibFile
import com.github.bhlangonijr.chesslib.PieceType as ChesslibPieceType
import com.github.bhlangonijr.chesslib.Rank as ChesslibRank
import com.github.bhlangonijr.chesslib.Square as ChesslibSquare

private const val FiftyMoveHalfMoveCount = 100
private const val SeventyFiveMoveHalfMoveCount = 150
private const val AutomaticRepetitionCount = 5

class ChesslibGameEngine @Inject constructor() : ChessGameEngine {
  override fun positionFromFen(fen: Fen): ChessPosition {
    val board = Board().apply { loadFromFen(fen.value) }
    return ChessPosition.fromSnapshot(
      fen = fen,
      pieces = board.readPieces(),
      sideToMove = board.sideToMove.toDomainPieceColor(),
    )
  }

  override fun legalMoves(position: ChessPosition): Set<ChessMove> = position
    .toChesslibBoard()
    .legalMoves()
    .mapTo(linkedSetOf(), Move::toDomainMove)

  override fun checkedKingSquare(position: ChessPosition): Square? {
    val board = position.toChesslibBoard()
    return if (board.isKingAttacked) {
      board.getKingSquare(board.sideToMove).toDomainSquare()
    } else {
      null
    }
  }

  override fun gameStatus(position: ChessPosition): GameStatus {
    val board = position.toChesslibBoardWithHistory()
    return when {
      board.isMated -> GameStatus.Checkmate(winner = board.sideToMove.flip().toDomainPieceColor())
      board.isStaleMate -> GameStatus.Draw(DrawReason.STALEMATE)
      board.isInsufficientMaterial -> GameStatus.Draw(DrawReason.INSUFFICIENT_MATERIAL)
      board.isRepetition(AutomaticRepetitionCount) -> GameStatus.Draw(DrawReason.FIVEFOLD_REPETITION)
      board.halfMoveCounter >= SeventyFiveMoveHalfMoveCount -> GameStatus.Draw(DrawReason.SEVENTY_FIVE_MOVE_RULE)
      else -> GameStatus.Ongoing
    }
  }

  override fun claimableDrawReason(position: ChessPosition): DrawReason? {
    val board = position.toChesslibBoardWithHistory()
    return when {
      board.isRepetition -> DrawReason.THREEFOLD_REPETITION
      board.halfMoveCounter >= FiftyMoveHalfMoveCount -> DrawReason.FIFTY_MOVE_RULE
      else -> null
    }
  }

  override fun applyMove(position: ChessPosition, move: ChessMove): MoveApplicationResult {
    val board = position.toChesslibBoard()
    val candidateMoves =
      board
        .legalMoves()
        .filter { candidate ->
          candidate.from == move.from.toChesslibSquare() && candidate.to == move.to.toChesslibSquare()
        }

    val candidate =
      when (val promotion = move.promotion) {
        null -> {
          if (candidateMoves.any { it.promotion != Piece.NONE }) {
            return MoveApplicationResult.Rejected(MoveRejectionReason.PROMOTION_REQUIRED)
          }
          candidateMoves.singleOrNull()
        }

        else ->
          candidateMoves.singleOrNull { candidateMove ->
            candidateMove.promotion == promotion.toChesslibPiece(position.sideToMove)
          }
      }
        ?: return MoveApplicationResult.Rejected(MoveRejectionReason.ILLEGAL_MOVE)

    if (!board.doMove(candidate, true)) {
      return MoveApplicationResult.Rejected(MoveRejectionReason.ILLEGAL_MOVE)
    }

    return MoveApplicationResult.Applied(board.toDomainPosition(position, candidate.toDomainMove()))
  }
}

private fun ChessPosition.toChesslibBoard(): Board {
  val fenValue = fen.value
  return Board().apply {
    loadFromFen(fenValue)
  }
}

private fun ChessPosition.toChesslibBoardWithHistory(): Board = Board().apply {
  loadFromFen(historyStartFen.value)
  moveHistory.forEach { move ->
    val chesslibMove =
      move.promotion?.let { promotion ->
        Move(
          move.from.toChesslibSquare(),
          move.to.toChesslibSquare(),
          promotion.toChesslibPiece(sideToMove.toDomainPieceColor()),
        )
      } ?: Move(move.from.toChesslibSquare(), move.to.toChesslibSquare())
    check(doMove(chesslibMove, true)) { "Cannot replay game history" }
  }
}

private fun Board.readPieces(): Map<Square, ChessPiece> = buildMap {
  ChessRank.entries.forEach { rank ->
    ChessFile.entries.forEach { file ->
      val square = Square(file = file, rank = rank)
      val piece = getPiece(square.toChesslibSquare())
      if (piece != Piece.NONE) {
        put(square, piece.toDomainPiece())
      }
    }
  }
}

private fun Board.toDomainPosition(previousPosition: ChessPosition, appliedMove: ChessMove): ChessPosition =
  ChessPosition.fromSnapshot(
    fen = Fen(fen),
    pieces = readPieces(),
    sideToMove = sideToMove.toDomainPieceColor(),
    historyStartFen = previousPosition.historyStartFen,
    moveHistory = previousPosition.moveHistory + appliedMove,
  )

private fun Move.toDomainMove(): ChessMove = ChessMove(
  from = from.toDomainSquare(),
  to = to.toDomainSquare(),
  promotion = promotion.toDomainPromotionPiece(),
)

private fun Square.toChesslibSquare(): ChesslibSquare =
  ChesslibSquare.encode(rank.toChesslibRank(), file.toChesslibFile())

private fun ChesslibSquare.toDomainSquare(): Square = Square(
  file = file.toDomainFile(),
  rank = rank.toDomainRank(),
)

private fun Piece.toDomainPiece(): ChessPiece = ChessPiece(
  color = pieceSide.toDomainPieceColor(),
  type = pieceType.toDomainPieceType(),
)

private fun PromotionPiece.toChesslibPiece(color: PieceColor): Piece = Piece.make(
  color.toChesslibSide(),
  when (this) {
    PromotionPiece.QUEEN -> ChesslibPieceType.QUEEN
    PromotionPiece.ROOK -> ChesslibPieceType.ROOK
    PromotionPiece.BISHOP -> ChesslibPieceType.BISHOP
    PromotionPiece.KNIGHT -> ChesslibPieceType.KNIGHT
  },
)

private fun Piece.toDomainPromotionPiece(): PromotionPiece? = when (pieceType) {
  ChesslibPieceType.QUEEN -> PromotionPiece.QUEEN
  ChesslibPieceType.ROOK -> PromotionPiece.ROOK
  ChesslibPieceType.BISHOP -> PromotionPiece.BISHOP
  ChesslibPieceType.KNIGHT -> PromotionPiece.KNIGHT
  else -> null
}

private fun PieceColor.toChesslibSide(): Side = when (this) {
  PieceColor.WHITE -> Side.WHITE
  PieceColor.BLACK -> Side.BLACK
}

private fun Side.toDomainPieceColor(): PieceColor = when (this) {
  Side.WHITE -> PieceColor.WHITE
  Side.BLACK -> PieceColor.BLACK
}

private fun ChesslibPieceType.toDomainPieceType(): PieceType = when (this) {
  ChesslibPieceType.KING -> PieceType.KING
  ChesslibPieceType.QUEEN -> PieceType.QUEEN
  ChesslibPieceType.ROOK -> PieceType.ROOK
  ChesslibPieceType.BISHOP -> PieceType.BISHOP
  ChesslibPieceType.KNIGHT -> PieceType.KNIGHT
  ChesslibPieceType.PAWN -> PieceType.PAWN
  ChesslibPieceType.NONE -> error("A missing piece has no domain piece type")
}

private fun ChessFile.toChesslibFile(): ChesslibFile = when (this) {
  ChessFile.A -> ChesslibFile.FILE_A
  ChessFile.B -> ChesslibFile.FILE_B
  ChessFile.C -> ChesslibFile.FILE_C
  ChessFile.D -> ChesslibFile.FILE_D
  ChessFile.E -> ChesslibFile.FILE_E
  ChessFile.F -> ChesslibFile.FILE_F
  ChessFile.G -> ChesslibFile.FILE_G
  ChessFile.H -> ChesslibFile.FILE_H
}

private fun ChesslibFile.toDomainFile(): ChessFile = when (this) {
  ChesslibFile.FILE_A -> ChessFile.A
  ChesslibFile.FILE_B -> ChessFile.B
  ChesslibFile.FILE_C -> ChessFile.C
  ChesslibFile.FILE_D -> ChessFile.D
  ChesslibFile.FILE_E -> ChessFile.E
  ChesslibFile.FILE_F -> ChessFile.F
  ChesslibFile.FILE_G -> ChessFile.G
  ChesslibFile.FILE_H -> ChessFile.H
  ChesslibFile.NONE -> error("A missing file has no domain file")
}

private fun ChessRank.toChesslibRank(): ChesslibRank = when (this) {
  ChessRank.ONE -> ChesslibRank.RANK_1
  ChessRank.TWO -> ChesslibRank.RANK_2
  ChessRank.THREE -> ChesslibRank.RANK_3
  ChessRank.FOUR -> ChesslibRank.RANK_4
  ChessRank.FIVE -> ChesslibRank.RANK_5
  ChessRank.SIX -> ChesslibRank.RANK_6
  ChessRank.SEVEN -> ChesslibRank.RANK_7
  ChessRank.EIGHT -> ChesslibRank.RANK_8
}

private fun ChesslibRank.toDomainRank(): ChessRank = when (this) {
  ChesslibRank.RANK_1 -> ChessRank.ONE
  ChesslibRank.RANK_2 -> ChessRank.TWO
  ChesslibRank.RANK_3 -> ChessRank.THREE
  ChesslibRank.RANK_4 -> ChessRank.FOUR
  ChesslibRank.RANK_5 -> ChessRank.FIVE
  ChesslibRank.RANK_6 -> ChessRank.SIX
  ChesslibRank.RANK_7 -> ChessRank.SEVEN
  ChesslibRank.RANK_8 -> ChessRank.EIGHT
  ChesslibRank.NONE -> error("A missing rank has no domain rank")
}
