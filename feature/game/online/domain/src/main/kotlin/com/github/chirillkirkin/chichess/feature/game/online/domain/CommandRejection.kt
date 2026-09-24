package com.github.chirillkirkin.chichess.feature.game.online.domain

/** Machine-readable reason the server refused a command; the UI maps it to a localized message. */
enum class CommandRejection {
  UNSUPPORTED_PROTOCOL_VERSION,
  MALFORMED_COMMAND,
  NOT_YOUR_TURN,
  ILLEGAL_MOVE,
  GAME_NOT_READY,
  GAME_FINISHED,
  REVISION_CONFLICT,
  NO_DRAW_OFFER,
  DRAW_ALREADY_OFFERED,
  DRAW_NOT_CLAIMABLE,
  UNKNOWN,
}
