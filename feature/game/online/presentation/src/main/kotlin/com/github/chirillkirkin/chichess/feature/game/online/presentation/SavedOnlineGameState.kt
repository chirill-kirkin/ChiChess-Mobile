package com.github.chirillkirkin.chichess.feature.game.online.presentation

import android.os.Parcelable
import com.github.chirillkirkin.chichess.feature.game.board.BoardState
import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameResult
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameStatus
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineTerminationReason
import kotlinx.parcelize.Parcelize

/**
 * The last confirmed game as a cache for instant render after process death; it holds no optimistic
 * move and is overwritten by the authoritative snapshot once the socket reconnects.
 */
@Parcelize
data class SavedOnlineGameState(
  val fen: String?,
  val yourColor: PieceColor?,
  val status: OnlineGameStatus,
  val revision: Long,
  val result: OnlineGameResult?,
  val terminationReason: OnlineTerminationReason?,
  val pendingDrawOfferBy: PieceColor?,
) : Parcelable

internal fun OnlineGameState.toSavedOnlineGame(): SavedOnlineGameState =
  SavedOnlineGameState(
    fen = confirmedPosition?.fen?.value,
    yourColor = yourColor,
    status = status,
    revision = revision,
    result = result,
    terminationReason = terminationReason,
    pendingDrawOfferBy = pendingDrawOfferBy,
  )

internal fun OnlineGameState.restoredFrom(
  saved: SavedOnlineGameState,
  engine: ChessGameEngine,
): OnlineGameState {
  val fen = saved.fen ?: return this
  val position = engine.positionFromFen(Fen(fen))
  return copy(
    yourColor = saved.yourColor,
    confirmedPosition = position,
    board = BoardState(position = position, checkedKingSquare = engine.checkedKingSquare(position)),
    revision = saved.revision,
    status = saved.status,
    result = saved.result,
    terminationReason = saved.terminationReason,
    pendingDrawOfferBy = saved.pendingDrawOfferBy,
  )
}
