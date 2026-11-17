package com.github.chirillkirkin.chichess.feature.game.online.domain

import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl
import kotlin.time.Duration

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
  TIMEOUT,
  TIMEOUT_VS_INSUFFICIENT_MATERIAL,
}

/** Remaining time of both sides at the moment the server sent it; [runningSide] is null while no clock runs. */
data class ClockReading(val whiteRemaining: Duration, val blackRemaining: Duration, val runningSide: PieceColor?)

data class CreatedGame(val gameId: String, val inviteCode: String)

data class OnlineGameSnapshot(
  val gameId: String,
  val inviteCode: String,
  val yourColor: PieceColor,
  val status: OnlineGameStatus,
  val revision: Long,
  val fen: Fen,
  val lastMove: ChessMove? = null,
  val pendingDrawOfferBy: PieceColor? = null,
  val result: OnlineGameResult? = null,
  val terminationReason: OnlineTerminationReason? = null,
  val timeControl: TimeControl? = null,
  val clock: ClockReading? = null,
)
