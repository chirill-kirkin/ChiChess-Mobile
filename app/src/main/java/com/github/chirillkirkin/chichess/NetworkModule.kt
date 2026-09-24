package com.github.chirillkirkin.chichess

import android.content.Context
import com.github.chirillkirkin.chichess.core.data.network.createChiChessHttpClient
import com.github.chirillkirkin.chichess.core.data.session.DataStoreGuestSessionStorage
import com.github.chirillkirkin.chichess.core.data.session.GuestSessionStorage
import com.github.chirillkirkin.chichess.core.data.session.RemoteGuestSessionRepository
import com.github.chirillkirkin.chichess.core.domain.session.GuestSessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  @Provides
  @Singleton
  fun provideHttpClient(): HttpClient = createChiChessHttpClient(BuildConfig.SERVER_URL)

  @Provides
  @Singleton
  fun provideGuestSessionStorage(
    @ApplicationContext context: Context,
  ): GuestSessionStorage = DataStoreGuestSessionStorage(context)

  @Provides
  @Singleton
  fun provideGuestSessionRepository(
    httpClient: HttpClient,
    sessionStorage: GuestSessionStorage,
  ): GuestSessionRepository = RemoteGuestSessionRepository(httpClient, sessionStorage)
}
