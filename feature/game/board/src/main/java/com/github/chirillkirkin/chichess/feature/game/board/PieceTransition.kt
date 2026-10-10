package com.github.chirillkirkin.chichess.feature.game.board

import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPiece
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.PieceType
import com.github.chirillkirkin.chichess.feature.game.domain.PromotionPiece
import com.github.chirillkirkin.chichess.feature.game.domain.Square

private val boardSquares = ChessRank.entries.flatMap { rank -> ChessFile.entries.map { file -> Square(file, rank) } }

private val promotionPieceTypes = PromotionPiece.entries.map { it.toPieceType() }.toSet()

internal data class PieceTransition(
  /** Origin square of every piece that moved, keyed by its destination square. */
  val movements: Map<Square, Square>,
  val vanishedPieces: Map<Square, ChessPiece>,
)

internal fun pieceTransition(previous: ChessPosition, current: ChessPosition): PieceTransition {
  val departures = boardSquares.filter { previous[it] != null && previous[it] != current[it] }.toMutableList()
  val arrivals = boardSquares.filter { current[it] != null && current[it] != previous[it] }
  val movements = mutableMapOf<Square, Square>()

  fun match(arrival: Square, origin: ChessPiece) {
    val departure = departures.filter { previous[it] == origin }.minByOrNull { it.distanceTo(arrival) } ?: return
    movements[arrival] = departure
    departures -= departure
  }

  arrivals.forEach { arrival -> current[arrival]?.let { match(arrival, it) } }
  // Promoted pieces are matched only after exact matches so that they cannot take a pawn that merely moved.
  arrivals
    .filter { it !in movements }
    .forEach { arrival -> current[arrival]?.promotedPawn()?.let { match(arrival, it) } }

  return PieceTransition(
    movements = movements,
    vanishedPieces = departures.mapNotNull { square -> previous[square]?.let { square to it } }.toMap(),
  )
}

private fun ChessPiece.promotedPawn(): ChessPiece? = takeIf { type in promotionPieceTypes }?.copy(type = PieceType.PAWN)

private fun Square.distanceTo(other: Square): Int {
  val fileDistance = file.ordinal - other.file.ordinal
  val rankDistance = rank.ordinal - other.rank.ordinal
  return fileDistance * fileDistance + rankDistance * rankDistance
}
