package com.github.chirillkirkin.chichess.feature.game.offline.presentation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import com.github.chirillkirkin.chichess.feature.game.board.BoardFacing
import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.BoardWithSidePanel
import com.github.chirillkirkin.chichess.feature.game.board.ChessBoard
import com.github.chirillkirkin.chichess.feature.game.clock.ChessClockFace
import com.github.chirillkirkin.chichess.feature.game.clock.rememberClockRemaining
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.GameStatus
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineBoardLayout

private val GameResultSpace = 64.dp
private val PlayerRowHeight = 56.dp
private const val UpsideDownRotationDegrees = 180f
private const val UprightRotationDegrees = 0f
private val ClockControlButtonSize = 64.dp
private val ClockControlIconSize = 36.dp
private const val SidePanelCenterWeight = 1f

@Composable
fun OfflineGameRoot(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OfflineGameViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val activity = LocalActivity.current

  // Leaving the screen, or the app going to the background, pauses the clock; a rotation does not.
  LifecycleStartEffect(viewModel, activity) {
    onStopOrDispose {
      if (activity?.isChangingConfigurations != true) viewModel.send(OfflineGameMessage.PauseClock)
    }
  }

  OfflineGameScreen(
    state = state,
    onMessage = viewModel::send,
    onBack = onBack,
    modifier = modifier,
  )
}

@Composable
fun OfflineGameScreen(
  state: OfflineGameState,
  onMessage: (OfflineGameMessage) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isSettingsVisible by rememberSaveable { mutableStateOf(false) }
  var isNewGameVisible by rememberSaveable { mutableStateOf(false) }

  BoxWithConstraints(modifier = modifier) {
    // In landscape the clock control moves from the bottom bar into the panel beside the board.
    val isLandscape = maxWidth > maxHeight

    Scaffold(
      topBar = {
        OfflineGameTopBar(
          onBack = onBack,
          onSettingsClick = { isSettingsVisible = true },
        )
      },
      bottomBar = {
        if (!isLandscape && !state.isLoading && state.clock != null) {
          ClockControlBar(state = state, onMessage = onMessage)
        }
      },
    ) { innerPadding ->
      if (!state.isLoading) {
        if (isLandscape) {
          OfflineGameLandscapeContent(
            state = state,
            onMessage = onMessage,
            modifier = Modifier.padding(innerPadding),
          )
        } else {
          OfflineGameContent(
            state = state,
            onMessage = onMessage,
            modifier = Modifier.padding(innerPadding),
          )
        }
      }
    }
  }

  if (isSettingsVisible) {
    OfflineGameSettingsSheet(
      boardLayout = state.boardLayout,
      onBoardLayoutSelect = { layout -> onMessage(OfflineGameMessage.BoardLayoutSelected(layout)) },
      onNewGameClick = {
        isSettingsVisible = false
        isNewGameVisible = true
      },
      onDismiss = { isSettingsVisible = false },
    )
  }

  if (isNewGameVisible) {
    NewGameSheet(
      initialTimeControl = state.clock?.timeControl,
      onPlayClick = { timeControl ->
        isNewGameVisible = false
        onMessage(OfflineGameMessage.NewGame(timeControl))
      },
      onDismiss = { isNewGameVisible = false },
    )
  }
}

@Composable
private fun ClockControlBar(state: OfflineGameState, onMessage: (OfflineGameMessage) -> Unit) {
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(vertical = ChiChessTheme.spacing.small),
    contentAlignment = Alignment.Center,
  ) {
    ClockControlButton(state = state, onMessage = onMessage)
  }
}

@Composable
private fun ClockControlButton(state: OfflineGameState, onMessage: (OfflineGameMessage) -> Unit) {
  val isPaused = state.isClockPaused
  FilledTonalIconButton(
    onClick = { onMessage(if (isPaused) OfflineGameMessage.ResumeClock else OfflineGameMessage.PauseClock) },
    modifier = Modifier.size(ClockControlButtonSize),
    enabled = state.gameStatus == GameStatus.Ongoing && state.clock?.activeSide != null,
  ) {
    if (isPaused) {
      Icon(
        imageVector = Icons.Filled.PlayArrow,
        contentDescription = stringResource(R.string.resume_clock),
        modifier = Modifier.size(ClockControlIconSize),
      )
    } else {
      Icon(
        painter = painterResource(R.drawable.ic_pause),
        contentDescription = stringResource(R.string.pause_clock),
        modifier = Modifier.size(ClockControlIconSize),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineGameTopBar(onBack: () -> Unit, onSettingsClick: () -> Unit) {
  TopAppBar(
    title = { Text(stringResource(R.string.over_the_board)) },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.navigate_back),
        )
      }
    },
    actions = {
      IconButton(onClick = onSettingsClick) {
        Icon(
          imageVector = Icons.Filled.Settings,
          contentDescription = stringResource(R.string.game_settings),
        )
      }
    },
  )
}

@Composable
private fun OfflineGameContent(
  state: OfflineGameState,
  onMessage: (OfflineGameMessage) -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    val resultSpace = GameResultSpace.coerceAtMost(maxHeight)
    val rowGap = ChiChessTheme.spacing.small
    // Both player rows, plus a gap between each of the four stacked items.
    val boardSize = minOf(maxWidth, (maxHeight - resultSpace - PlayerRowHeight * 2 - rowGap * 3).coerceAtLeast(0.dp))
    val facing = state.boardFacing()

    Column(
      verticalArrangement = Arrangement.spacedBy(rowGap),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      PlayerRow(
        state = state,
        color = PieceColor.BLACK,
        facing = facing,
        onMessage = onMessage,
        modifier = Modifier.width(boardSize),
      )

      OfflineChessBoard(state = state, facing = facing, onMessage = onMessage, modifier = Modifier.size(boardSize))

      PlayerRow(
        state = state,
        color = PieceColor.WHITE,
        facing = facing,
        onMessage = onMessage,
        modifier = Modifier.width(boardSize),
      )

      Box(
        modifier = Modifier.size(width = boardSize, height = resultSpace),
        contentAlignment = Alignment.TopCenter,
      ) {
        GameResult(status = state.gameStatus, modifier = Modifier.padding(top = ChiChessTheme.spacing.small))
      }
    }
  }
}

// The board takes the full height; the player rows, the result, and the clock control stand beside it.
@Composable
private fun OfflineGameLandscapeContent(
  state: OfflineGameState,
  onMessage: (OfflineGameMessage) -> Unit,
  modifier: Modifier = Modifier,
) {
  val facing = state.boardFacing()

  BoardWithSidePanel(
    board = { boardModifier ->
      OfflineChessBoard(state = state, facing = facing, onMessage = onMessage, modifier = boardModifier)
    },
    panel = { panelModifier ->
      Column(modifier = panelModifier) {
        PlayerRow(
          state = state,
          color = PieceColor.BLACK,
          facing = facing,
          onMessage = onMessage,
          modifier = Modifier.fillMaxWidth(),
          isBesideBoard = true,
        )

        Column(
          modifier = Modifier.weight(SidePanelCenterWeight).padding(horizontal = ChiChessTheme.spacing.small),
          verticalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.small, Alignment.CenterVertically),
        ) {
          GameResult(status = state.gameStatus, textAlign = TextAlign.Start)

          if (state.clock != null) {
            ClockControlButton(state = state, onMessage = onMessage)
          }
        }

        PlayerRow(
          state = state,
          color = PieceColor.WHITE,
          facing = facing,
          onMessage = onMessage,
          modifier = Modifier.fillMaxWidth(),
          isBesideBoard = true,
        )
      }
    },
    modifier = modifier,
  )
}

@Composable
private fun OfflineChessBoard(
  state: OfflineGameState,
  facing: BoardFacing,
  onMessage: (OfflineGameMessage) -> Unit,
  modifier: Modifier = Modifier,
) {
  ChessBoard(
    state = state.board,
    onSquareClick = { square ->
      onMessage(OfflineGameMessage.Board(BoardMessage.SquareClick(square)))
    },
    onPieceDragStart = { square ->
      onMessage(OfflineGameMessage.Board(BoardMessage.PieceDragStart(square)))
    },
    onPieceDrop = { from, to ->
      onMessage(OfflineGameMessage.Board(BoardMessage.PieceDrop(from, to)))
    },
    modifier = modifier,
    facing = facing,
    promotionSquare = state.pendingPromotion?.to,
    onPromotionSelect = { piece -> onMessage(OfflineGameMessage.PromotionSelected(piece)) },
    onPromotionDismiss = { onMessage(OfflineGameMessage.PromotionDismissed) },
  )
}

@Composable
private fun GameResult(status: GameStatus, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center) {
  val resultText = gameResultText(status) ?: return
  Text(
    text = resultText,
    modifier = modifier,
    style = ChiChessTheme.typography.gameResult,
    textAlign = textAlign,
  )
}

// The row next to each player's side of the board: their clock and, on their turn, the draw claim.
@Composable
private fun PlayerRow(
  state: OfflineGameState,
  color: PieceColor,
  facing: BoardFacing,
  onMessage: (OfflineGameMessage) -> Unit,
  modifier: Modifier = Modifier,
  isBesideBoard: Boolean = false,
) {
  val clock = state.clock
  val canClaimDraw =
    state.gameStatus == GameStatus.Ongoing &&
      state.claimableDrawReason != null &&
      state.board.position.sideToMove == color
  val isUpsideDown = facing.isUpsideDownFor(color)
  // The rotation mirrors the row, so an upside-down clock goes to the row's end to stay on the board's side.
  val isClockAtRowStart = isBesideBoard && !isUpsideDown
  val clockAlignment = if (isClockAtRowStart) Alignment.CenterStart else Alignment.CenterEnd
  val claimDrawAlignment = if (isClockAtRowStart) Alignment.CenterEnd else Alignment.CenterStart

  Box(
    modifier =
      modifier
        .height(PlayerRowHeight)
        .rotate(if (isUpsideDown) UpsideDownRotationDegrees else UprightRotationDegrees)
        .padding(horizontal = ChiChessTheme.spacing.small),
  ) {
    if (canClaimDraw) {
      Button(
        onClick = { onMessage(OfflineGameMessage.ClaimDraw) },
        modifier = Modifier.align(if (clock != null) claimDrawAlignment else Alignment.Center),
      ) {
        Text(stringResource(R.string.claim_draw))
      }
    }

    if (clock != null) {
      val remaining by rememberClockRemaining(clock, color)
      ChessClockFace(
        remaining = remaining,
        isRunning = clock.isRunning && clock.activeSide == color,
        modifier = Modifier.align(clockAlignment),
      )
    }
  }
}

// Each row is read by the player its pieces face.
private fun BoardFacing.isUpsideDownFor(color: PieceColor): Boolean = when (this) {
  BoardFacing.BOTTOM -> false
  BoardFacing.TOP -> true
  BoardFacing.FACE_TO_FACE -> color == PieceColor.BLACK
}

private fun OfflineGameState.boardFacing(): BoardFacing = when (boardLayout) {
  OfflineBoardLayout.STANDARD -> BoardFacing.BOTTOM

  OfflineBoardLayout.BLACK_UPSIDE_DOWN -> BoardFacing.FACE_TO_FACE

  OfflineBoardLayout.FLIP_AFTER_MOVE ->
    when (board.position.sideToMove) {
      PieceColor.WHITE -> BoardFacing.BOTTOM
      PieceColor.BLACK -> BoardFacing.TOP
    }
}

@Composable
@ReadOnlyComposable
private fun gameResultText(status: GameStatus): String? = when (status) {
  GameStatus.Ongoing -> null

  is GameStatus.Checkmate ->
    stringResource(
      when (status.winner) {
        PieceColor.WHITE -> R.string.white_wins_by_checkmate
        PieceColor.BLACK -> R.string.black_wins_by_checkmate
      },
    )

  is GameStatus.Draw ->
    stringResource(
      when (status.reason) {
        DrawReason.STALEMATE -> R.string.draw_by_stalemate
        DrawReason.THREEFOLD_REPETITION -> R.string.draw_by_threefold_repetition
        DrawReason.FIFTY_MOVE_RULE -> R.string.draw_by_fifty_move_rule
        DrawReason.FIVEFOLD_REPETITION -> R.string.draw_by_fivefold_repetition
        DrawReason.SEVENTY_FIVE_MOVE_RULE -> R.string.draw_by_seventy_five_move_rule
        DrawReason.INSUFFICIENT_MATERIAL -> R.string.draw_by_insufficient_material
        DrawReason.TIMEOUT_VS_INSUFFICIENT_MATERIAL -> R.string.draw_by_timeout_vs_insufficient_material
      },
    )

  is GameStatus.Timeout ->
    stringResource(
      when (status.winner) {
        PieceColor.WHITE -> R.string.white_wins_on_time
        PieceColor.BLACK -> R.string.black_wins_on_time
      },
    )
}

@Preview(name = "Phone", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "Phone landscape", widthDp = 800, heightDp = 360, showBackground = true)
@Preview(name = "Tablet landscape", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun OfflineGameScreenPreview() {
  ChiChessTheme(dynamicColor = false) {
    OfflineGameScreen(
      state = OfflineGameState(),
      onMessage = {},
      onBack = {},
    )
  }
}
