package com.github.chirillkirkin.chichess.feature.game.online.data

import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl
import com.github.chirillkirkin.chichess.feature.game.online.domain.ClockReading
import com.github.chirillkirkin.chichess.feature.game.online.domain.CommandRejection
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameStatus
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val COMMAND_ID = "cmd-1"
private const val EXPECTED_REVISION = 7L
private const val MOVE_UCI = "e2e4"
private const val FEN = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"

class GameProtocolTest {
  @Test
  fun `MakeMove serializes with its type discriminator and fields`() {
    val command = MakeMove(GAME_PROTOCOL_VERSION, COMMAND_ID, EXPECTED_REVISION, MOVE_UCI)

    val encoded = gameProtocolJson.encodeToString(GameCommand.serializer(), command).jsonObject()

    assertEquals("MAKE_MOVE", encoded["type"]?.jsonPrimitive?.content)
    assertEquals(MOVE_UCI, encoded["uci"]?.jsonPrimitive?.content)
    assertEquals(EXPECTED_REVISION, encoded["expectedRevision"]?.jsonPrimitive?.long)
    assertEquals(GAME_PROTOCOL_VERSION.toString(), encoded["protocolVersion"]?.jsonPrimitive?.content)
  }

  @Test
  fun `draw commands serialize with their type discriminators`() {
    assertEquals("OFFER_DRAW", typeOf(OfferDraw(GAME_PROTOCOL_VERSION, COMMAND_ID)))
    assertEquals("ACCEPT_DRAW", typeOf(AcceptDraw(GAME_PROTOCOL_VERSION, COMMAND_ID)))
    assertEquals("DECLINE_DRAW", typeOf(DeclineDraw(GAME_PROTOCOL_VERSION, COMMAND_ID)))
    assertEquals("CLAIM_DRAW", typeOf(ClaimDraw(GAME_PROTOCOL_VERSION, COMMAND_ID, EXPECTED_REVISION)))
  }

  @Test
  fun `SNAPSHOT event maps last move and pending draw offer`() {
    val json =
      """{"type":"SNAPSHOT","snapshot":{"gameId":"g","inviteCode":"INV0000000","yourColor":"WHITE","status":"IN_PROGRESS","revision":$EXPECTED_REVISION,"fen":"$FEN","lastMove":"$MOVE_UCI","pendingDrawOfferBy":"BLACK"},"opponentConnected":false}"""

    val event = assertIs<SnapshotEvent>(decode(json))
    val snapshot = event.snapshot.toSnapshot()

    assertEquals(ChessMove(Square(ChessFile.E, ChessRank.TWO), Square(ChessFile.E, ChessRank.FOUR)), snapshot.lastMove)
    assertEquals(PieceColor.BLACK, snapshot.pendingDrawOfferBy)
    assertEquals(OnlineGameStatus.IN_PROGRESS, snapshot.status)
    assertEquals(false, event.opponentConnected)
  }

  @Test
  fun `MOVE_APPLIED event carries the last move`() {
    val json = """{"type":"MOVE_APPLIED","revision":8,"fen":"$FEN","status":"IN_PROGRESS","lastMove":"$MOVE_UCI"}"""

    val event = assertIs<MoveAppliedEvent>(decode(json))

    assertEquals(8L, event.revision)
    assertEquals(MOVE_UCI, event.lastMove)
    assertEquals(ApiGameStatus.IN_PROGRESS, event.status)
    assertNull(event.result)
  }

  @Test
  fun `GAME_FINISHED event carries the result and reason`() {
    val json =
      """
      {"type":"GAME_FINISHED","revision":9,"status":"FINISHED","result":"DRAW","terminationReason":"AGREEMENT"}
      """.trimIndent()

    val event = assertIs<GameFinishedEvent>(decode(json))

    assertEquals(ApiGameResult.DRAW, event.result)
    assertEquals(ApiTerminationReason.AGREEMENT, event.terminationReason)
  }

  @Test
  fun `SNAPSHOT event maps the time control and clock`() {
    val json =
      """{"type":"SNAPSHOT","snapshot":{"gameId":"g","inviteCode":"INV0000000","yourColor":"WHITE","status":"IN_PROGRESS","revision":$EXPECTED_REVISION,"fen":"$FEN","timeControl":{"initialSeconds":300,"incrementSeconds":3},"clock":{"whiteRemainingMs":295000,"blackRemainingMs":300000,"runningColor":"BLACK"}},"opponentConnected":true}"""

    val snapshot = assertIs<SnapshotEvent>(decode(json)).snapshot.toSnapshot()

    assertEquals(TimeControl(initial = 5.minutes, increment = 3.seconds), snapshot.timeControl)
    assertEquals(ClockReading(295.seconds, 300.seconds, runningSide = PieceColor.BLACK), snapshot.clock)
  }

  @Test
  fun `SNAPSHOT event of a game without a clock has no time control`() {
    val json =
      """{"type":"SNAPSHOT","snapshot":{"gameId":"g","inviteCode":"INV0000000","yourColor":"WHITE","status":"IN_PROGRESS","revision":$EXPECTED_REVISION,"fen":"$FEN","timeControl":null,"clock":null},"opponentConnected":true}"""

    val snapshot = assertIs<SnapshotEvent>(decode(json)).snapshot.toSnapshot()

    assertNull(snapshot.timeControl)
    assertNull(snapshot.clock)
  }

  @Test
  fun `MOVE_APPLIED event carries the clock`() {
    val json =
      """{"type":"MOVE_APPLIED","revision":8,"fen":"$FEN","status":"IN_PROGRESS","lastMove":"$MOVE_UCI","clock":{"whiteRemainingMs":61000,"blackRemainingMs":59500,"runningColor":"BLACK"}}"""

    val event = assertIs<MoveAppliedEvent>(decode(json))

    assertEquals(
      ClockDto(whiteRemainingMs = 61_000, blackRemainingMs = 59_500, runningColor = ApiPieceColor.BLACK),
      event.clock,
    )
  }

  @Test
  fun `GAME_FINISHED event decodes a timeout with a stopped clock`() {
    val json =
      """{"type":"GAME_FINISHED","revision":9,"status":"FINISHED","result":"BLACK_WON","terminationReason":"TIMEOUT","clock":{"whiteRemainingMs":0,"blackRemainingMs":12000,"runningColor":null}}"""

    val event = assertIs<GameFinishedEvent>(decode(json))

    assertEquals(ApiTerminationReason.TIMEOUT, event.terminationReason)
    assertEquals(ClockDto(whiteRemainingMs = 0, blackRemainingMs = 12_000, runningColor = null), event.clock)
  }

  @Test
  fun `draw offer and decline events decode`() {
    assertEquals(ApiPieceColor.WHITE, assertIs<DrawOfferedEvent>(decode("""{"type":"DRAW_OFFERED","by":"WHITE"}""")).by)
    assertIs<DrawDeclinedEvent>(decode("""{"type":"DRAW_DECLINED"}"""))
  }

  @Test
  fun `player left event decodes its color`() {
    assertEquals(
      ApiPieceColor.BLACK,
      assertIs<PlayerLeftEvent>(decode("""{"type":"PLAYER_LEFT","color":"BLACK"}""")).color,
    )
  }

  @Test
  fun `an unknown event type is reported as unknown`() {
    assertEquals(
      DecodedGameEvent.Unknown("SOMETHING_NEW"),
      decodeGameEvent("""{"type":"SOMETHING_NEW","color":"BLACK"}"""),
    )
  }

  @Test
  fun `an unknown field in a known event is ignored`() {
    val decoded = assertIs<DecodedGameEvent.Known>(
      decodeGameEvent("""{"type":"PLAYER_LEFT","color":"BLACK","extra":1}"""),
    )

    assertEquals(PlayerLeftEvent(ApiPieceColor.BLACK), decoded.event)
  }

  @Test
  fun `a known event missing a required field is malformed`() {
    val json =
      """{"type":"SNAPSHOT","snapshot":{"gameId":"g","inviteCode":"INV0000000","yourColor":"WHITE","status":"IN_PROGRESS","revision":$EXPECTED_REVISION,"fen":"$FEN"}}"""

    val decoded = assertIs<DecodedGameEvent.Malformed>(decodeGameEvent(json))

    assertEquals("SNAPSHOT", decoded.type)
    assertIs<MissingFieldException>(decoded.error)
  }

  @Test
  fun `text that is not json is malformed`() {
    assertNull(assertIs<DecodedGameEvent.Malformed>(decodeGameEvent("not json")).type)
  }

  @Test
  fun `COMMAND_REJECTED decodes and maps a known code`() {
    val event = assertIs<CommandRejectedEvent>(
      decode("""{"type":"COMMAND_REJECTED","commandId":"$COMMAND_ID","code":"ILLEGAL_MOVE"}"""),
    )

    assertEquals(COMMAND_ID, event.commandId)
    assertEquals(CommandRejection.ILLEGAL_MOVE, commandRejectionOf(event.code))
  }

  @Test
  fun `commandRejectionOf falls back to UNKNOWN for an unrecognized code`() {
    assertEquals(CommandRejection.UNKNOWN, commandRejectionOf("SOMETHING_NEW"))
  }

  private fun typeOf(command: GameCommand): String? = gameProtocolJson
    .encodeToString(GameCommand.serializer(), command)
    .jsonObject()["type"]
    ?.jsonPrimitive
    ?.content

  private fun decode(json: String): GameEvent = gameProtocolJson.decodeFromString(GameEvent.serializer(), json)

  private fun String.jsonObject() = gameProtocolJson.parseToJsonElement(this).jsonObject
}
