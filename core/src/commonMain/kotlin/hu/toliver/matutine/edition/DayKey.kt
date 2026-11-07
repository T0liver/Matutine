package hu.toliver.matutine.edition

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

fun todayLocalDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Clock.System.todayIn(timeZone)

fun LocalDate.toDayKey(): String = toString()

fun LocalDate.toDateLine(): String {
    val weekday = dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercase)
    val month = month.name.lowercase().replaceFirstChar(Char::uppercase)
    return "$weekday, $month $dayOfMonth, $year"
}
