package com.github.chirillkirkin.chichess.feature.game.domain

enum class PieceColor {
  WHITE,
  BLACK,
}

@JvmInline
value class Fen(val value: String)

enum class PieceType {
  KING,
  QUEEN,
  ROOK,
  BISHOP,
  KNIGHT,
  PAWN,
}

enum class PromotionPiece {
  QUEEN,
  ROOK,
  BISHOP,
  KNIGHT,
}

enum class ChessFile(val notation: Char) {
  A('a'),
  B('b'),
  C('c'),
  D('d'),
  E('e'),
  F('f'),
  G('g'),
  H('h'),
}

enum class ChessRank(val notation: Int) {
  ONE(1),
  TWO(2),
  THREE(3),
  FOUR(4),
  FIVE(5),
  SIX(6),
  SEVEN(7),
  EIGHT(8),
}

enum class SquareColor {
  LIGHT,
  DARK,
}

data class ChessPiece(val color: PieceColor, val type: PieceType)

data class Square(val file: ChessFile, val rank: ChessRank) {
  val color: SquareColor
    get() =
      if ((file.ordinal + rank.ordinal).isEven) {
        SquareColor.DARK
      } else {
        SquareColor.LIGHT
      }
}

data class ChessMove(val from: Square, val to: Square, val promotion: PromotionPiece? = null)

@ConsistentCopyVisibility
data class ChessPosition private constructor(
  val fen: Fen,
  private val cells: List<ChessPiece?>,
  val sideToMove: PieceColor,
  val historyStartFen: Fen,
  val moveHistory: List<ChessMove>,
) {
  operator fun get(square: Square): ChessPiece? = cells[square.positionIndex]

  companion object {
    private val squareCount = ChessFile.entries.size * ChessRank.entries.size

    fun fromSnapshot(
      fen: Fen,
      pieces: Map<Square, ChessPiece>,
      sideToMove: PieceColor,
      historyStartFen: Fen = fen,
      moveHistory: List<ChessMove> = emptyList(),
    ): ChessPosition {
      val cells = MutableList<ChessPiece?>(squareCount) { null }
      pieces.forEach { (square, piece) -> cells[square.positionIndex] = piece }
      return ChessPosition(
        fen = fen,
        cells = cells,
        sideToMove = sideToMove,
        historyStartFen = historyStartFen,
        moveHistory = moveHistory.toList(),
      )
    }
  }
}

fun initialChessPosition(): ChessPosition {
  val backRank =
    listOf(
      PieceType.ROOK,
      PieceType.KNIGHT,
      PieceType.BISHOP,
      PieceType.QUEEN,
      PieceType.KING,
      PieceType.BISHOP,
      PieceType.KNIGHT,
      PieceType.ROOK,
    )

  val pieces =
    buildMap {
      ChessFile.entries.zip(backRank).forEach { (file, pieceType) ->
        put(Square(file, ChessRank.ONE), ChessPiece(PieceColor.WHITE, pieceType))
        put(Square(file, ChessRank.TWO), ChessPiece(PieceColor.WHITE, PieceType.PAWN))
        put(Square(file, ChessRank.SEVEN), ChessPiece(PieceColor.BLACK, PieceType.PAWN))
        put(Square(file, ChessRank.EIGHT), ChessPiece(PieceColor.BLACK, pieceType))
      }
    }

  return ChessPosition.fromSnapshot(
    fen = Fen(InitialPositionFenValue),
    pieces = pieces,
    sideToMove = PieceColor.WHITE,
  )
}

private const val InitialPositionFenValue =
  "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

private val Square.positionIndex: Int
  get() = rank.ordinal * ChessFile.entries.size + file.ordinal

private val Int.isEven: Boolean
  get() = this % 2 == 0
