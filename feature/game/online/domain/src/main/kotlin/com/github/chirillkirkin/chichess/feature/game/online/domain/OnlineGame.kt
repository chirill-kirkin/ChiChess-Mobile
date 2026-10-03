package com.github.chirillkirkin.chichess.feature.game.online.domain

import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor

enum class OnlineGameStatus {
  WAITING_FOR_OPPONENT,
  IN_PROGRESS,
  FINISHED,
}

enum class OnlineGameResult {
  WHITE_WON,
  BLACK_WON,
  DRAW,
}

enum class OnlineTerminationReason {
  CHECKMATE,
  STALEMATE,
  RESIGNATION,
  AGREEMENT,
  INSUFFICIENT_MATERIAL,
  FIFTY_MOVE_RULE,
  SEVENTY_FIVE_MOVE_RULE,
  THREEFOLD_REPETITION,
  FIVEFOLD_REPETITION,
}

data class CreatedGame(
  val gameId: String,
  val inviteCode: String,
)

data class OnlineGameSnapshot(
  val gameId: String,
  val inviteCode: String,
  val yourColor: PieceColor,
  val status: OnlineGameStatus,
  val opponentConnected: Boolean,
  val revision: Long,
  val fen: Fen,
  val lastMove: ChessMove? = null,
  val pendingDrawOfferBy: PieceColor? = null,
  val result: OnlineGameResult? = null,
  val terminationReason: OnlineTerminationReason? = null,
)
