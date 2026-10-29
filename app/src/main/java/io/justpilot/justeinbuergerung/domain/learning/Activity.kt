package io.justpilot.justeinbuergerung.domain.learning

import java.time.LocalDate

/** Answers of the last seven days and the streak of days with a finished Daily 10. */
data class Activity(
    /** Answers per day, oldest first, ending today; days without answers have 0. */
    val days: List<Day>,
    /** Consecutive days with a finished Daily 10, up to today or, while today is still open, yesterday. */
    val streak: Int,
    val dailyDoneToday: Boolean,
) {
    data class Day(val date: LocalDate, val answers: Int)

    companion object {
        const val DAYS = 7

        fun of(answersPerDay: Map<LocalDate, Int>, dailyDays: Set<LocalDate>, today: LocalDate): Activity {
            val doneToday = today in dailyDays
            // An open today does not break the streak yet; it only counts once done.
            var day = if (doneToday) today else today.minusDays(1)
            var streak = 0
            while (day in dailyDays) {
                streak++
                day = day.minusDays(1)
            }
            return Activity(
                days = (DAYS - 1 downTo 0).map { today.minusDays(it.toLong()) }.map { Day(it, answersPerDay[it] ?: 0) },
                streak = streak,
                dailyDoneToday = doneToday,
            )
        }
    }
}
