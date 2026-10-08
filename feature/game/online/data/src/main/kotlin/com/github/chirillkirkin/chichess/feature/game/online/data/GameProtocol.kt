package com.github.chirillkirkin.chichess.feature.game.online.data

import com.github.chirillkirkin.chichess.feature.game.online.domain.CommandRejection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal const val GAME_PROTOCOL_VERSION = 1

private const val EVENT_TYPE_KEY = "type"
private const val SEALED_SUBCLASSES_ELEMENT_INDEX = 1

internal val gameProtocolJson = Json {
  ignoreUnknownKeys = true
  classDiscriminator = EVENT_TYPE_KEY
}

@Serializable
internal sealed interface GameCommand {
  val protocolVersion: Int
  val commandId: String
}

@Serializable
@SerialName("REQUEST_SYNC")
internal data class RequestSync(override val protocolVersion: Int, override val commandId: String) : GameCommand

@Serializable
@SerialName("MAKE_MOVE")
internal data class MakeMove(
  override val protocolVersion: Int,
  override val commandId: String,
  val expectedRevision: Long,
  val uci: String,
) : GameCommand

@Serializable
@SerialName("RESIGN")
internal data class Resign(
  override val protocolVersion: Int,
  override val commandId: String,
  val expectedRevision: Long,
) : GameCommand

@Serializable
@SerialName("OFFER_DRAW")
internal data class OfferDraw(override val protocolVersion: Int, override val commandId: String) : GameCommand

@Serializable
@SerialName("ACCEPT_DRAW")
internal data class AcceptDraw(override val protocolVersion: Int, override val commandId: String) : GameCommand

@Serializable
@SerialName("DECLINE_DRAW")
internal data class DeclineDraw(override val protocolVersion: Int, override val commandId: String) : GameCommand

@Serializable
@SerialName("CLAIM_DRAW")
internal data class ClaimDraw(
  override val protocolVersion: Int,
  override val commandId: String,
  val expectedRevision: Long,
) : GameCommand

@Serializable
internal sealed interface GameEvent

@Serializable
@SerialName("SNAPSHOT")
internal data class SnapshotEvent(val snapshot: GameSnapshotResponse, val opponentConnected: Boolean) : GameEvent

@Serializable
@SerialName("PLAYER_JOINED")
internal data class PlayerJoinedEvent(val color: ApiPieceColor) : GameEvent

@Serializable
@SerialName("PLAYER_LEFT")
internal data class PlayerLeftEvent(val color: ApiPieceColor) : GameEvent

@Serializable
@SerialName("MOVE_APPLIED")
internal data class MoveAppliedEvent(
  val revision: Long,
  val fen: String,
  val status: ApiGameStatus,
  val lastMove: String,
  val result: ApiGameResult? = null,
  val terminationReason: ApiTerminationReason? = null,
) : GameEvent

@Serializable
@SerialName("GAME_FINISHED")
internal data class GameFinishedEvent(
  val revision: Long,
  val status: ApiGameStatus,
  val result: ApiGameResult,
  val terminationReason: ApiTerminationReason,
) : GameEvent

@Serializable
@SerialName("DRAW_OFFERED")
internal data class DrawOfferedEvent(val by: ApiPieceColor) : GameEvent

@Serializable
@SerialName("DRAW_DECLINED")
internal data object DrawDeclinedEvent : GameEvent

@Serializable
@SerialName("COMMAND_REJECTED")
internal data class CommandRejectedEvent(val commandId: String? = null, val code: String) : GameEvent

internal fun commandRejectionOf(code: String): CommandRejection =
  CommandRejection.entries.firstOrNull { it.name == code } ?: CommandRejection.UNKNOWN

internal sealed interface DecodedGameEvent {
  data class Known(val event: GameEvent) : DecodedGameEvent

  data class Unknown(val type: String) : DecodedGameEvent

  data class Malformed(val type: String?, val error: SerializationException) : DecodedGameEvent
}

private val knownGameEventTypes: Set<String> =
  GameEvent
    .serializer()
    .descriptor
    .getElementDescriptor(SEALED_SUBCLASSES_ELEMENT_INDEX)
    .elementNames
    .toSet()

internal fun decodeGameEvent(text: String): DecodedGameEvent {
  val element =
    try {
      gameProtocolJson.parseToJsonElement(text)
    } catch (e: SerializationException) {
      return DecodedGameEvent.Malformed(type = null, error = e)
    }
  val type = ((element as? JsonObject)?.get(EVENT_TYPE_KEY) as? JsonPrimitive)?.contentOrNull
  if (type != null && type !in knownGameEventTypes) return DecodedGameEvent.Unknown(type)
  return try {
    DecodedGameEvent.Known(gameProtocolJson.decodeFromJsonElement(GameEvent.serializer(), element))
  } catch (e: SerializationException) {
    DecodedGameEvent.Malformed(type, e)
  }
}
