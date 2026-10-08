package com.github.chirillkirkin.chichess.feature.game.domain

fun ChessMove.toUci(): String = buildString {
  append(from.toUci())
  append(to.toUci())
  promotion?.let { append(it.uciSymbol) }
}

fun parseUci(uci: String): ChessMove? {
  if (uci.length !in UCI_MIN_LENGTH..UCI_MAX_LENGTH) return null
  val from = squareFromUci(uci[0], uci[1]) ?: return null
  val to = squareFromUci(uci[2], uci[3]) ?: return null
  val promotion =
    if (uci.length == UCI_MAX_LENGTH) promotionFromUci(uci[4]) ?: return null else null
  return ChessMove(from = from, to = to, promotion = promotion)
}

private const val UCI_MIN_LENGTH = 4
private const val UCI_MAX_LENGTH = 5

private fun Square.toUci(): String = "${file.notation}${rank.notation}"

private fun squareFromUci(fileChar: Char, rankChar: Char): Square? {
  val file = ChessFile.entries.firstOrNull { it.notation == fileChar } ?: return null
  val rankDigit = rankChar.digitToIntOrNull() ?: return null
  val rank = ChessRank.entries.firstOrNull { it.notation == rankDigit } ?: return null
  return Square(file = file, rank = rank)
}

private val PromotionPiece.uciSymbol: Char
  get() =
    when (this) {
      PromotionPiece.QUEEN -> 'q'
      PromotionPiece.ROOK -> 'r'
      PromotionPiece.BISHOP -> 'b'
      PromotionPiece.KNIGHT -> 'n'
    }

private fun promotionFromUci(symbol: Char): PromotionPiece? = when (symbol) {
  'q' -> PromotionPiece.QUEEN
  'r' -> PromotionPiece.ROOK
  'b' -> PromotionPiece.BISHOP
  'n' -> PromotionPiece.KNIGHT
  else -> null
}
