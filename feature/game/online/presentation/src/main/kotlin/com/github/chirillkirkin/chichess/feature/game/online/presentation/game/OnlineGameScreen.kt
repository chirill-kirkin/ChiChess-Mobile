package com.github.chirillkirkin.chichess.feature.game.online.presentation.game

import android.content.ClipData
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.BoardState
import com.github.chirillkirkin.chichess.feature.game.board.BoardWithSidePanel
import com.github.chirillkirkin.chichess.feature.game.board.ChessBoard
import com.github.chirillkirkin.chichess.feature.game.clock.ChessClockFace
import com.github.chirillkirkin.chichess.feature.game.clock.rememberClockRemaining
import com.github.chirillkirkin.chichess.feature.game.domain.ChessClock
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.initialChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.opponent
import com.github.chirillkirkin.chichess.feature.game.online.domain.CommandRejection
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameResult
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameStatus
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineTerminationReason
import com.github.chirillkirkin.chichess.feature.game.online.presentation.R
import kotlinx.coroutines.launch

private val ControlsSpace = 128.dp
private val ClockRowHeight = 56.dp
private const val SidePanelControlsWeight = 1f

@Composable
fun OnlineGameRoot(
  gameId: String,
  onGameFail: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OnlineGameViewModel = hiltViewModel<OnlineGameViewModel, OnlineGameViewModel.Factory>(
    creationCallback = { factory -> factory.create(gameId) },
  ),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentOnGameFail by rememberUpdatedState(onGameFail)

  LaunchedEffect(viewModel, lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.commands.collect { command ->
        if (command is OnlineGameCommand.ExitOnError) currentOnGameFail()
      }
    }
  }

  OnlineGameScreen(state = state, onMessage = viewModel::send, modifier = modifier)
}

@Composable
fun OnlineGameScreen(state: OnlineGameState, onMessage: (OnlineGameMessage) -> Unit, modifier: Modifier = Modifier) {
  val board = state.board
  if (board == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(text = connectionText(state.connection), style = ChiChessTheme.typography.gameResult)
    }
    return
  }

  val snackbarHostState = remember { SnackbarHostState() }
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()
  val clipLabel = stringResource(R.string.online_invite_code_hint)
  val copiedMessage = stringResource(R.string.online_invite_code_copied)
  val onCopyInviteCode: (String) -> Unit = { code ->
    scope.launch {
      clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipLabel, code)))
      // Android 13+ shows its own copy confirmation, so only add a snackbar on older versions.
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // Replace the visible confirmation so repeated taps refresh one snackbar instead of stacking.
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(copiedMessage)
      }
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      if (maxWidth > maxHeight) {
        OnlineGameLandscapeContent(
          board = board,
          state = state,
          onMessage = onMessage,
          onCopyInviteCode = onCopyInviteCode,
        )
      } else {
        OnlineGamePortraitContent(
          board = board,
          state = state,
          onMessage = onMessage,
          onCopyInviteCode = onCopyInviteCode,
          maxWidth = maxWidth,
          maxHeight = maxHeight,
        )
      }
    }

    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
  }
}

@Composable
private fun OnlineGameLandscapeContent(
  board: BoardState,
  state: OnlineGameState,
  onMessage: (OnlineGameMessage) -> Unit,
  onCopyInviteCode: (String) -> Unit,
) {
  val clock = state.clock
  val yourColor = state.yourColor ?: PieceColor.WHITE

  BoardWithSidePanel(
    board = { boardModifier ->
      OnlineChessBoard(board = board, state = state, onMessage = onMessage, modifier = boardModifier)
    },
    panel = { panelModifier ->
      Column(modifier = panelModifier) {
        if (clock != null) {
          ClockRow(
            clock = clock,
            color = yourColor.opponent,
            modifier = Modifier.fillMaxWidth().height(ClockRowHeight),
            clockAlignment = Alignment.CenterStart,
          )
        }

        Box(
          modifier =
            Modifier
              .weight(SidePanelControlsWeight)
              .fillMaxWidth()
              .padding(horizontal = ChiChessTheme.spacing.small),
          contentAlignment = Alignment.CenterStart,
        ) {
          OnlineGameControls(
            state = state,
            onMessage = onMessage,
            onCopyInviteCode = onCopyInviteCode,
            horizontalAlignment = Alignment.Start,
          )
        }

        if (clock != null) {
          ClockRow(
            clock = clock,
            color = yourColor,
            modifier = Modifier.fillMaxWidth().height(ClockRowHeight),
            clockAlignment = Alignment.CenterStart,
          )
        }
      }
    },
  )
}

@Composable
private fun OnlineGamePortraitContent(
  board: BoardState,
  state: OnlineGameState,
  onMessage: (OnlineGameMessage) -> Unit,
  onCopyInviteCode: (String) -> Unit,
  maxWidth: Dp,
  maxHeight: Dp,
) {
  val clock = state.clock
  val yourColor = state.yourColor ?: PieceColor.WHITE
  val gap = ChiChessTheme.spacing.small

  // Reserve matching space above and below so the board stays centered with the controls right
  // beneath it; the board is the largest square that fits what is left.
  val clockRowsSpace = if (clock != null) (ClockRowHeight + gap) * 2 else 0.dp
  val boardSize = minOf(maxWidth, (maxHeight - ControlsSpace * 2 - clockRowsSpace).coerceAtLeast(0.dp))

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Spacer(modifier = Modifier.height(ControlsSpace))

    if (clock != null) {
      ClockRow(
        clock = clock,
        color = yourColor.opponent,
        modifier = Modifier.padding(bottom = gap).size(width = boardSize, height = ClockRowHeight),
      )
    }

    OnlineChessBoard(
      board = board,
      state = state,
      onMessage = onMessage,
      modifier = Modifier.size(boardSize),
    )

    if (clock != null) {
      ClockRow(
        clock = clock,
        color = yourColor,
        modifier = Modifier.padding(top = gap).size(width = boardSize, height = ClockRowHeight),
      )
    }

    Box(
      modifier = Modifier.size(width = boardSize, height = ControlsSpace),
      contentAlignment = Alignment.TopCenter,
    ) {
      OnlineGameControls(state = state, onMessage = onMessage, onCopyInviteCode = onCopyInviteCode)
    }
  }
}

@Composable
private fun OnlineChessBoard(
  board: BoardState,
  state: OnlineGameState,
  onMessage: (OnlineGameMessage) -> Unit,
  modifier: Modifier = Modifier,
) {
  ChessBoard(
    state = board,
    onSquareClick = { onMessage(OnlineGameMessage.Board(BoardMessage.SquareClick(it))) },
    onPieceDragStart = { onMessage(OnlineGameMessage.Board(BoardMessage.PieceDragStart(it))) },
    onPieceDrop = { from, to -> onMessage(OnlineGameMessage.Board(BoardMessage.PieceDrop(from, to))) },
    perspective = state.yourColor ?: PieceColor.WHITE,
    promotionSquare = state.pendingPromotion?.to,
    onPromotionSelect = { onMessage(OnlineGameMessage.PromotionSelected(it)) },
    onPromotionDismiss = { onMessage(OnlineGameMessage.PromotionDismissed) },
    modifier = modifier,
  )
}

@Composable
private fun ClockRow(
  clock: ChessClock,
  color: PieceColor,
  modifier: Modifier = Modifier,
  clockAlignment: Alignment = Alignment.CenterEnd,
) {
  val remaining by rememberClockRemaining(clock, color)

  Box(modifier = modifier.padding(horizontal = ChiChessTheme.spacing.small), contentAlignment = clockAlignment) {
    ChessClockFace(remaining = remaining, isRunning = clock.isRunning && clock.activeSide == color)
  }
}

@Composable
private fun OnlineGameControls(
  state: OnlineGameState,
  onMessage: (OnlineGameMessage) -> Unit,
  onCopyInviteCode: (String) -> Unit,
  horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
  val resultText = state.result?.let { resultRes(it, state.terminationReason) }?.let { stringResource(it) }

  Column(
    horizontalAlignment = horizontalAlignment,
    verticalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.medium),
  ) {
    when {
      resultText != null -> Text(text = resultText, style = ChiChessTheme.typography.gameResult)

      state.connection == ConnectionStatus.CLOSED ->
        Text(text = stringResource(R.string.online_disconnected))

      state.connection == ConnectionStatus.CONNECTING ->
        Text(text = stringResource(R.string.online_connecting))

      state.status == OnlineGameStatus.WAITING_FOR_OPPONENT -> {
        Text(text = stringResource(R.string.online_waiting_for_opponent))
        state.inviteCode?.let { code ->
          Text(
            text = stringResource(R.string.online_invite_code, code),
            modifier = Modifier
              .clip(RoundedCornerShape(percent = 50))
              .clickable { onCopyInviteCode(code) }
              .padding(horizontal = ChiChessTheme.spacing.medium, vertical = ChiChessTheme.spacing.small),
          )
        }
      }

      else -> PlayingControls(state = state, onMessage = onMessage)
    }
  }
}

@Composable
private fun ColumnScope.PlayingControls(state: OnlineGameState, onMessage: (OnlineGameMessage) -> Unit) {
  val opponentOfferedDraw = state.pendingDrawOfferBy != null && state.pendingDrawOfferBy != state.yourColor

  if (!state.opponentConnected) {
    Text(text = stringResource(R.string.online_opponent_disconnected))
  }

  if (opponentOfferedDraw) {
    Text(text = stringResource(R.string.online_opponent_offered_draw))
    Row(horizontalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.medium)) {
      Button(onClick = { onMessage(OnlineGameMessage.AcceptDraw) }) {
        Text(text = stringResource(R.string.online_accept_draw))
      }
      Button(onClick = { onMessage(OnlineGameMessage.DeclineDraw) }) {
        Text(text = stringResource(R.string.online_decline_draw))
      }
    }
  } else {
    Row(horizontalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.medium)) {
      Button(onClick = { onMessage(OnlineGameMessage.Resign) }) {
        Text(text = stringResource(R.string.online_resign))
      }
      if (state.pendingDrawOfferBy == null) {
        Button(
          onClick = { onMessage(OnlineGameMessage.OfferDraw) },
          // A draw cannot be offered before the first move has been played.
          enabled = state.lastMove != null,
        ) {
          Text(text = stringResource(R.string.online_offer_draw))
        }
      }
    }
    if (state.pendingDrawOfferBy == state.yourColor) {
      Text(text = stringResource(R.string.online_draw_offer_sent))
    }
  }

  state.moveError?.let { Text(text = stringResource(errorRes(it))) }
}

@Composable
@ReadOnlyComposable
private fun connectionText(connection: ConnectionStatus): String = stringResource(
  if (connection == ConnectionStatus.CLOSED) R.string.online_disconnected else R.string.online_connecting,
)

@StringRes
private fun resultRes(result: OnlineGameResult, reason: OnlineTerminationReason?): Int? = when (result) {
  OnlineGameResult.WHITE_WON ->
    when (reason) {
      OnlineTerminationReason.CHECKMATE -> R.string.online_white_wins_by_checkmate

      OnlineTerminationReason.RESIGNATION -> R.string.online_white_wins_by_resignation

      OnlineTerminationReason.TIMEOUT -> R.string.online_white_wins_on_time

      OnlineTerminationReason.STALEMATE,
      OnlineTerminationReason.AGREEMENT,
      OnlineTerminationReason.INSUFFICIENT_MATERIAL,
      OnlineTerminationReason.FIFTY_MOVE_RULE,
      OnlineTerminationReason.SEVENTY_FIVE_MOVE_RULE,
      OnlineTerminationReason.THREEFOLD_REPETITION,
      OnlineTerminationReason.FIVEFOLD_REPETITION,
      OnlineTerminationReason.TIMEOUT_VS_INSUFFICIENT_MATERIAL,
      null,
      -> null
    }

  OnlineGameResult.BLACK_WON ->
    when (reason) {
      OnlineTerminationReason.CHECKMATE -> R.string.online_black_wins_by_checkmate

      OnlineTerminationReason.RESIGNATION -> R.string.online_black_wins_by_resignation

      OnlineTerminationReason.TIMEOUT -> R.string.online_black_wins_on_time

      OnlineTerminationReason.STALEMATE,
      OnlineTerminationReason.AGREEMENT,
      OnlineTerminationReason.INSUFFICIENT_MATERIAL,
      OnlineTerminationReason.FIFTY_MOVE_RULE,
      OnlineTerminationReason.SEVENTY_FIVE_MOVE_RULE,
      OnlineTerminationReason.THREEFOLD_REPETITION,
      OnlineTerminationReason.FIVEFOLD_REPETITION,
      OnlineTerminationReason.TIMEOUT_VS_INSUFFICIENT_MATERIAL,
      null,
      -> null
    }

  OnlineGameResult.DRAW ->
    when (reason) {
      OnlineTerminationReason.STALEMATE -> R.string.online_draw_by_stalemate

      OnlineTerminationReason.AGREEMENT -> R.string.online_draw_by_agreement

      OnlineTerminationReason.INSUFFICIENT_MATERIAL -> R.string.online_draw_by_insufficient_material

      OnlineTerminationReason.THREEFOLD_REPETITION -> R.string.online_draw_by_threefold_repetition

      OnlineTerminationReason.FIVEFOLD_REPETITION -> R.string.online_draw_by_fivefold_repetition

      OnlineTerminationReason.FIFTY_MOVE_RULE -> R.string.online_draw_by_fifty_move_rule

      OnlineTerminationReason.SEVENTY_FIVE_MOVE_RULE -> R.string.online_draw_by_seventy_five_move_rule

      OnlineTerminationReason.TIMEOUT_VS_INSUFFICIENT_MATERIAL ->
        R.string.online_draw_by_timeout_vs_insufficient_material

      OnlineTerminationReason.CHECKMATE,
      OnlineTerminationReason.RESIGNATION,
      OnlineTerminationReason.TIMEOUT,
      null,
      -> null
    }
}

@StringRes
private fun errorRes(reason: CommandRejection): Int = when (reason) {
  CommandRejection.ILLEGAL_MOVE -> R.string.online_error_illegal_move

  CommandRejection.NOT_YOUR_TURN -> R.string.online_error_not_your_turn

  CommandRejection.GAME_NOT_READY -> R.string.online_error_game_not_ready

  CommandRejection.GAME_FINISHED -> R.string.online_error_game_finished

  CommandRejection.DRAW_NOT_CLAIMABLE -> R.string.online_error_draw_not_claimable

  CommandRejection.UNSUPPORTED_PROTOCOL_VERSION,
  CommandRejection.MALFORMED_COMMAND,
  CommandRejection.REVISION_CONFLICT,
  CommandRejection.NO_DRAW_OFFER,
  CommandRejection.DRAW_ALREADY_OFFERED,
  CommandRejection.UNKNOWN,
  -> R.string.online_error_generic
}

@Preview(name = "Phone", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun OnlineGameScreenPreview() {
  ChiChessTheme(dynamicColor = false) {
    OnlineGameScreen(
      state = OnlineGameState(
        gameId = "preview",
        connection = ConnectionStatus.CONNECTED,
        yourColor = PieceColor.WHITE,
        board = BoardState(position = initialChessPosition()),
        status = OnlineGameStatus.IN_PROGRESS,
      ),
      onMessage = {},
    )
  }
}
