package io.justpilot.justeinbuergerung.ui.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

/** Today's date in the clock's zone, emitting again right after every midnight. */
fun currentDay(clock: Clock): Flow<LocalDate> = flow {
    while (true) {
        val now = clock.instant()
        val today = now.atZone(clock.zone).toLocalDate()
        emit(today)
        val midnight = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
        delay(Duration.between(now, midnight).toMillis() + 1)
    }
}.distinctUntilChanged()
