package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom")
}

object DateUtils {
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val displayDateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    fun formatDate(millis: Long): String = displayDateFormat.format(Date(millis))
    fun formatTime(millis: Long): String = displayTimeFormat.format(Date(millis))
    fun formatDateTime(millis: Long): String = displayDateTimeFormat.format(Date(millis))
    fun formatShortDate(millis: Long): String = shortDateFormat.format(Date(millis))

    fun isToday(millis: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(millis: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isThisWeek(millis: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
    }

    fun isThisMonth(millis: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    fun matchesFilter(millis: Long, filter: DateFilter, customStart: Long? = null, customEnd: Long? = null): Boolean {
        return when (filter) {
            DateFilter.ALL -> true
            DateFilter.TODAY -> isToday(millis)
            DateFilter.YESTERDAY -> isYesterday(millis)
            DateFilter.THIS_WEEK -> isThisWeek(millis)
            DateFilter.THIS_MONTH -> isThisMonth(millis)
            DateFilter.CUSTOM -> {
                val start = customStart ?: 0L
                val end = customEnd ?: Long.MAX_VALUE
                millis in start..end
            }
        }
    }

    fun getTomorrowMillis(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 18)
        cal.set(Calendar.MINUTE, 0)
        return cal.timeInMillis
    }

    fun getTodayEveningMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 18)
        cal.set(Calendar.MINUTE, 0)
        return cal.timeInMillis
    }
}
