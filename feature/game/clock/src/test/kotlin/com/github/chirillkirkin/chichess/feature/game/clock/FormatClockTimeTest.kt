package com.github.chirillkirkin.chichess.feature.game.clock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FormatClockTimeTest {
  @Test
  fun `minutes and seconds are shown below an hour`() {
    assertEquals("5:07", formatClockTime(5.minutes + 7.seconds + 900.milliseconds))
  }

  @Test
  fun `hours are shown from an hour`() {
    assertEquals("1:30:05", formatClockTime(1.hours + 30.minutes + 5.seconds))
  }

  @Test
  fun `tenths are shown below ten seconds`() {
    assertEquals("10:00", formatClockTime(10.minutes))
    assertEquals("0:10", formatClockTime(10.seconds))
    assertEquals("9.9", formatClockTime(9.seconds + 990.milliseconds))
    assertEquals("0.0", formatClockTime(Duration.ZERO))
  }
}
