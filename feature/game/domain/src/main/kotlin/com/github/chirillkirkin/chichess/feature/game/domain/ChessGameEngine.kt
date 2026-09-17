package com.github.chirillkirkin.chichess.feature.game.domain

interface ChessGameEngine {
  fun legalMoves(position: ChessPosition): Set<ChessMove>

  fun checkedKingSquare(position: ChessPosition): Square?

  fun gameStatus(position: ChessPosition): GameStatus

  fun claimableDrawReason(position: ChessPosition): DrawReason?

  fun applyMove(
    position: ChessPosition,
    move: ChessMove,
  ): MoveApplicationResult
}

sealed interface GameStatus {
  data object Ongoing : GameStatus

  data class Checkmate(
    val winner: PieceColor,
  ) : GameStatus

  data class Draw(
    val reason: DrawReason,
  ) : GameStatus
}

enum class DrawReason {
  STALEMATE,
  THREEFOLD_REPETITION,
  FIFTY_MOVE_RULE,
  FIVEFOLD_REPETITION,
  SEVENTY_FIVE_MOVE_RULE,
  INSUFFICIENT_MATERIAL,
}

enum class MoveRejectionReason {
  ILLEGAL_MOVE,
  PROMOTION_REQUIRED,
}

sealed interface MoveApplicationResult {
  data class Applied(
    val position: ChessPosition,
  ) : MoveApplicationResult

  data class Rejected(
    val reason: MoveRejectionReason,
  ) : MoveApplicationResult
}
