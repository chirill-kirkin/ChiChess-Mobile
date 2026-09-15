package com.github.chirillkirkin.chichess.feature.game.domain

interface ChessGameEngine {
  fun legalMoves(position: ChessPosition): Set<ChessMove>

  fun applyMove(
    position: ChessPosition,
    move: ChessMove,
  ): MoveApplicationResult
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
