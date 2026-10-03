package com.github.chirillkirkin.chichess.feature.game.online.data

import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.parseUci
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameEvent

internal fun GameEvent.toOnlineEvent(): OnlineGameEvent =
  when (this) {
    is SnapshotEvent -> OnlineGameEvent.Snapshot(snapshot.toSnapshot())
    is PlayerJoinedEvent -> OnlineGameEvent.PlayerJoined(color.toPieceColor())
    is PlayerLeftEvent -> OnlineGameEvent.PlayerLeft(color.toPieceColor())
    is MoveAppliedEvent ->
      OnlineGameEvent.MoveApplied(
        revision = revision,
        fen = Fen(fen),
        status = status.toStatus(),
        lastMove = parseUci(lastMove),
        result = result?.toResult(),
        terminationReason = terminationReason?.toTerminationReason(),
      )
    is GameFinishedEvent ->
      OnlineGameEvent.GameFinished(
        revision = revision,
        result = result.toResult(),
        terminationReason = terminationReason.toTerminationReason(),
      )
    is DrawOfferedEvent -> OnlineGameEvent.DrawOffered(by.toPieceColor())
    DrawDeclinedEvent -> OnlineGameEvent.DrawDeclined
    is CommandRejectedEvent -> OnlineGameEvent.CommandRejected(commandId, commandRejectionOf(code))
  }
