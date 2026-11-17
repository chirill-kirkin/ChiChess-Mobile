package com.github.chirillkirkin.chichess

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
  @Provides
  fun provideTimeSource(): TimeSource.WithComparableMarks = TimeSource.Monotonic
}
