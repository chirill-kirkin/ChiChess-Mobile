package com.github.chirillkirkin.chichess.feature.game.domain

// A flag loses unless the opponent cannot checkmate at all, which is then a draw (FIDE Laws, 6.9).
fun ChessPosition.timeoutStatus(flagged: PieceColor): GameStatus {
  val opponent = flagged.opponent
  return if (hasInsufficientMatingMaterial(opponent)) {
    GameStatus.Draw(DrawReason.TIMEOUT_VS_INSUFFICIENT_MATERIAL)
  } else {
    GameStatus.Timeout(winner = opponent)
  }
}

// The opponent's pieces count too: they can block their own king's escape squares (the e4ec rules lichess uses).
fun ChessPosition.hasInsufficientMatingMaterial(color: PieceColor): Boolean {
  val own = squaresByType(color)
  val opponent = squaresByType(color.opponent)
  return when (own.keys) {
    emptySet<PieceType>() -> true

    // A king with only queens can always capture a lone knight or escape its check.
    setOf(PieceType.KNIGHT) -> own.getValue(PieceType.KNIGHT).size == 1 && opponent.keys.all { it == PieceType.QUEEN }

    // A rook or a queen can always block a bishop's check; a knight, a pawn, or a bishop on the other color cannot.
    setOf(PieceType.BISHOP) ->
      opponent.keys.none { it == PieceType.KNIGHT || it == PieceType.PAWN } &&
        (own.getValue(PieceType.BISHOP) + opponent[PieceType.BISHOP].orEmpty()).map(Square::color).distinct().size == 1

    else -> false
  }
}

private fun ChessPosition.squaresByType(color: PieceColor): Map<PieceType, List<Square>> = Square.entries
  .filter { this[it]?.color == color }
  .groupBy { checkNotNull(this[it]).type }
  .minus(PieceType.KING)
