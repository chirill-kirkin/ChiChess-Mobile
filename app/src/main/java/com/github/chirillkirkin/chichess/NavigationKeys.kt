package com.github.chirillkirkin.chichess

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object OfflineGameRoute : NavKey

@Serializable
data object OnlineLobbyRoute : NavKey

@Serializable
data class OnlineGameRoute(val gameId: String) : NavKey
