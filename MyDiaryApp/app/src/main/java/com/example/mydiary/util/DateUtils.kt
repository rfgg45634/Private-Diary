package com.example.mydiary.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    private val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val tagFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    fun formatFull(timestamp: Long): String = fullFormat.format(Date(timestamp))

    fun formatTag(timestamp: Long): String = tagFormat.format(Date(timestamp))

    /** 卡片显示的标题：用户未定义标题时，用创建时间代替 */
    fun displayTitle(title: String?, createdTime: Long): String {
        return if (!title.isNullOrBlank()) title else formatFull(createdTime)
    }

    /** 计算并格式化回收站剩余保留时间 */
    fun formatRemainingTime(deletedTime: Long): String {
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
        val elapsed = System.currentTimeMillis() - deletedTime
        val remaining = sevenDaysInMillis - elapsed
        if (remaining <= 0) return "即将清除"
        val days = (remaining / (24 * 60 * 60 * 1000L)).toInt()
        if (days > 0) return "${days}天后清除"
        val hours = (remaining / (60 * 60 * 1000L)).toInt()
        if (hours > 0) return "${hours}小时后清除"
        return "即将清除"
    }

    /** 把时间戳对齐到当天零点，用于比较"是不是同一天" */
    private fun startOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * 计算连续记录天数：从今天（如果今天还没写，从昨天）往前数，
     * 只要每天都至少有一条记录就继续累加，中间断了一天就停止。
     */
    fun calculateStreak(createdTimes: List<Long>): Int {
        if (createdTimes.isEmpty()) return 0
        val dayBuckets = createdTimes.map { startOfDay(it) }.toHashSet()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        var cursor = startOfDay(System.currentTimeMillis())
        if (!dayBuckets.contains(cursor)) {
            cursor -= oneDayMillis
        }
        var streak = 0
        while (dayBuckets.contains(cursor)) {
            streak++
            cursor -= oneDayMillis
        }
        return streak
    }
}
