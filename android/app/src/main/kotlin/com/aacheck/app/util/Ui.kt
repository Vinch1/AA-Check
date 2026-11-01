package com.aacheck.app.util

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 金额格式化：分 -> ¥xx.xx */
fun centsToYuan(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    return "$sign¥${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
}

fun netCentsText(cents: Long): String =
    if (cents > 0) "应收 ${centsToYuan(cents)}" else if (cents < 0) "应付 ${centsToYuan(-cents)}" else "已平账"

private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}
private val dayFmt = SimpleDateFormat("M月d日 EEE", Locale.CHINA)
private val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)

fun parseIso(iso: String): Date? = runCatching { isoParser.parse(iso.take(19)) }.getOrNull()

fun formatDay(iso: String): String = parseIso(iso)?.let { dayFmt.format(it) } ?: ""
fun formatTime(iso: String): String = parseIso(iso)?.let { timeFmt.format(it) } ?: ""
fun dayKey(iso: String): String = parseIso(iso)?.let { dayFmt.format(it) } ?: iso.take(10)

enum class Category(
    val key: String,
    val label: String,
    val emoji: String,
    val color: Color,
    val darkColor: Color,
) {
    FOOD("FOOD", "餐饮", "🍜", Color(0xFFB5482E), Color(0xFFE8926B)),
    TRANSPORT("TRANSPORT", "交通", "🚗", Color(0xFF8A6D3B), Color(0xFFD9B36A)),
    TICKET("TICKET", "门票", "🎫", Color(0xFFA8651F), Color(0xFFE59A4E)),
    HOTEL("HOTEL", "住宿", "🏨", Color(0xFF7A6A4F), Color(0xFFCBB890)),
    SHOPPING("SHOPPING", "购物", "🛍️", Color(0xFFC08A3E), Color(0xFFE6B86A)),
    ENTERTAINMENT("ENTERTAINMENT", "娱乐", "🎮", Color(0xFF9A4F6B), Color(0xFFE08AA6)),
    OTHER("OTHER", "其他", "📦", Color(0xFF8A7B68), Color(0xFFC2B49C));

    companion object {
        fun fromKey(key: String): Category = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/** Pick the category tint that stays legible on the current surface. */
fun Category.tint(isDark: Boolean): Color = if (isDark) darkColor else color
