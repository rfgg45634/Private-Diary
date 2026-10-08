package com.example.mydiary.util

import com.example.mydiary.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    private val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val tagFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    private val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())
    private val monthLabelFormat = SimpleDateFormat("M月", Locale.getDefault())
    private val weekdayFormat = SimpleDateFormat("E", Locale.getDefault())

    fun formatFull(timestamp: Long): String = fullFormat.format(Date(timestamp))

    fun formatTag(timestamp: Long): String = tagFormat.format(Date(timestamp))

    /** 日记卡片左侧大号的「日」数字，如 28 */
    fun formatDayNumber(timestamp: Long): String = dayNumberFormat.format(Date(timestamp))

    private val monthDayFormat = SimpleDateFormat("M月d日", Locale.getDefault())

    /** 知识卡片底部的小字日期，如 9月28日 */
    fun formatMonthDay(timestamp: Long): String = monthDayFormat.format(Date(timestamp))

    /** 日记卡片日期块下方的「月 + 星期」小字，如 9月 周一 */
    fun formatMonthWeekday(timestamp: Long): String =
        monthLabelFormat.format(Date(timestamp)) + " " + weekdayFormat.format(Date(timestamp))

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

    /** 累计记录了多少个不同的日子（同一天写多条只算一天） */
    fun countDistinctDays(createdTimes: List<Long>): Int {
        return createdTimes.map { startOfDay(it) }.toHashSet().size
    }

    /** 按每月几号返回对应的装饰图标资源（1~31 各一个） */
    fun dayIconRes(timestamp: Long): Int {
        val day = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
            .get(java.util.Calendar.DAY_OF_MONTH)
        return when (day) {
            1 -> R.drawable.day_01
            2 -> R.drawable.day_02
            3 -> R.drawable.day_03
            4 -> R.drawable.day_04
            5 -> R.drawable.day_05
            6 -> R.drawable.day_06
            7 -> R.drawable.day_07
            8 -> R.drawable.day_08
            9 -> R.drawable.day_09
            10 -> R.drawable.day_10
            11 -> R.drawable.day_11
            12 -> R.drawable.day_12
            13 -> R.drawable.day_13
            14 -> R.drawable.day_14
            15 -> R.drawable.day_15
            16 -> R.drawable.day_16
            17 -> R.drawable.day_17
            18 -> R.drawable.day_18
            19 -> R.drawable.day_19
            20 -> R.drawable.day_20
            21 -> R.drawable.day_21
            22 -> R.drawable.day_22
            23 -> R.drawable.day_23
            24 -> R.drawable.day_24
            25 -> R.drawable.day_25
            26 -> R.drawable.day_26
            27 -> R.drawable.day_27
            28 -> R.drawable.day_28
            29 -> R.drawable.day_29
            30 -> R.drawable.day_30
            else -> R.drawable.day_31
        }
    }
}
