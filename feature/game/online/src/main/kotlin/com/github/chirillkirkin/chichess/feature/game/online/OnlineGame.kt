package com.github.chirillkirkin.chichess.feature.game.online

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
  val revision: Long,
  val fen: Fen,
  val result: OnlineGameResult? = null,
  val terminationReason: OnlineTerminationReason? = null,
)
