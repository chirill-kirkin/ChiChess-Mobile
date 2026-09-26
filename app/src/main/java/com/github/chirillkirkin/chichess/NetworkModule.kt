package com.github.chirillkirkin.chichess

import android.content.Context
import com.github.chirillkirkin.chichess.core.data.network.createAuthenticatedChiChessHttpClient
import com.github.chirillkirkin.chichess.core.data.network.createChiChessHttpClient
import com.github.chirillkirkin.chichess.core.data.session.DataStoreGuestSessionStorage
import com.github.chirillkirkin.chichess.core.data.session.GuestSessionStorage
import com.github.chirillkirkin.chichess.core.data.session.RemoteGuestSessionRepository
import com.github.chirillkirkin.chichess.core.domain.session.GuestSessionRepository
import com.github.chirillkirkin.chichess.feature.game.online.data.KtorOnlineGameChannel
import com.github.chirillkirkin.chichess.feature.game.online.data.RemoteOnlineGameRepository
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameChannel
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BootstrapHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  @Provides
  @Singleton
  @BootstrapHttpClient
  fun provideBootstrapHttpClient(): HttpClient = createChiChessHttpClient(BuildConfig.SERVER_URL, BuildConfig.DEBUG)

  @Provides
  @Singleton
  fun provideGuestSessionStorage(
    @ApplicationContext context: Context,
  ): GuestSessionStorage = DataStoreGuestSessionStorage(context)

  @Provides
  @Singleton
  fun provideGuestSessionRepository(
    @BootstrapHttpClient httpClient: HttpClient,
    sessionStorage: GuestSessionStorage,
  ): GuestSessionRepository = RemoteGuestSessionRepository(httpClient, sessionStorage)

  @Provides
  @Singleton
  @AuthenticatedHttpClient
  fun provideAuthenticatedHttpClient(
    guestSessions: GuestSessionRepository,
  ): HttpClient = createAuthenticatedChiChessHttpClient(BuildConfig.SERVER_URL, BuildConfig.DEBUG) {
    guestSessions.currentSession().token
  }

  @Provides
  @Singleton
  fun provideOnlineGameRepository(
    @AuthenticatedHttpClient httpClient: HttpClient,
  ): OnlineGameRepository = RemoteOnlineGameRepository(httpClient)

  @Provides
  @Singleton
  fun provideOnlineGameChannel(
    @AuthenticatedHttpClient httpClient: HttpClient,
  ): OnlineGameChannel = KtorOnlineGameChannel(httpClient, BuildConfig.SERVER_URL)
}
