package com.github.chirillkirkin.chichess.feature.game.online.data

import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.parseUci
import com.github.chirillkirkin.chichess.feature.game.online.domain.CreatedGame
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameResult
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameSnapshot
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameStatus
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineTerminationReason
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateGameResponse(
  val gameId: String,
  val inviteCode: String,
)

@Serializable
internal data class JoinGameRequest(
  val inviteCode: String,
)

@Serializable
internal data class JoinGameResponse(
  val gameId: String,
)

@Serializable
internal data class GameSnapshotResponse(
  val gameId: String,
  val inviteCode: String,
  val yourColor: ApiPieceColor,
  val status: ApiGameStatus,
  val revision: Long,
  val fen: String,
  val lastMove: String? = null,
  val pendingDrawOfferBy: ApiPieceColor? = null,
  val result: ApiGameResult? = null,
  val terminationReason: ApiTerminationReason? = null,
)

@Serializable
internal data class ApiErrorResponse(
  val code: String,
)

@Serializable
internal enum class ApiPieceColor { WHITE, BLACK }

@Serializable
internal enum class ApiGameStatus { WAITING_FOR_OPPONENT, IN_PROGRESS, FINISHED }

@Serializable
internal enum class ApiGameResult { WHITE_WON, BLACK_WON, DRAW }

@Serializable
internal enum class ApiTerminationReason {
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

internal fun CreateGameResponse.toCreatedGame(): CreatedGame =
  CreatedGame(gameId = gameId, inviteCode = inviteCode)

internal fun GameSnapshotResponse.toSnapshot(): OnlineGameSnapshot =
  OnlineGameSnapshot(
    gameId = gameId,
    inviteCode = inviteCode,
    yourColor = yourColor.toPieceColor(),
    status = status.toStatus(),
    revision = revision,
    fen = Fen(fen),
    lastMove = lastMove?.let(::parseUci),
    pendingDrawOfferBy = pendingDrawOfferBy?.toPieceColor(),
    result = result?.toResult(),
    terminationReason = terminationReason?.toTerminationReason(),
  )

internal fun ApiPieceColor.toPieceColor(): PieceColor =
  when (this) {
    ApiPieceColor.WHITE -> PieceColor.WHITE
    ApiPieceColor.BLACK -> PieceColor.BLACK
  }

internal fun ApiGameStatus.toStatus(): OnlineGameStatus =
  when (this) {
    ApiGameStatus.WAITING_FOR_OPPONENT -> OnlineGameStatus.WAITING_FOR_OPPONENT
    ApiGameStatus.IN_PROGRESS -> OnlineGameStatus.IN_PROGRESS
    ApiGameStatus.FINISHED -> OnlineGameStatus.FINISHED
  }

internal fun ApiGameResult.toResult(): OnlineGameResult =
  when (this) {
    ApiGameResult.WHITE_WON -> OnlineGameResult.WHITE_WON
    ApiGameResult.BLACK_WON -> OnlineGameResult.BLACK_WON
    ApiGameResult.DRAW -> OnlineGameResult.DRAW
  }

internal fun ApiTerminationReason.toTerminationReason(): OnlineTerminationReason =
  when (this) {
    ApiTerminationReason.CHECKMATE -> OnlineTerminationReason.CHECKMATE
    ApiTerminationReason.STALEMATE -> OnlineTerminationReason.STALEMATE
    ApiTerminationReason.RESIGNATION -> OnlineTerminationReason.RESIGNATION
    ApiTerminationReason.AGREEMENT -> OnlineTerminationReason.AGREEMENT
    ApiTerminationReason.INSUFFICIENT_MATERIAL -> OnlineTerminationReason.INSUFFICIENT_MATERIAL
    ApiTerminationReason.FIFTY_MOVE_RULE -> OnlineTerminationReason.FIFTY_MOVE_RULE
    ApiTerminationReason.SEVENTY_FIVE_MOVE_RULE -> OnlineTerminationReason.SEVENTY_FIVE_MOVE_RULE
    ApiTerminationReason.THREEFOLD_REPETITION -> OnlineTerminationReason.THREEFOLD_REPETITION
    ApiTerminationReason.FIVEFOLD_REPETITION -> OnlineTerminationReason.FIVEFOLD_REPETITION
  }
