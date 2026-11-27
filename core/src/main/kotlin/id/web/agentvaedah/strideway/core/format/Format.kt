package id.web.agentvaedah.strideway.core.format

import java.util.Locale
import kotlin.math.roundToLong

object Format {
    fun elapsedWords(ms: Long): String {
        if (ms in 1..999) return "<1 dtk"
        val seconds = ms.coerceAtLeast(0) / 1000
        return listOf(seconds / 3600 to "jam", seconds / 60 % 60 to "mnt", seconds % 60 to "dtk")
            .filter { it.first > 0 }.joinToString(" ") { (n, unit) -> "$n $unit" }.ifEmpty { "0 dtk" }
    }

    fun duration(ms: Long): String {
        val total = (ms.coerceAtLeast(0) / 1000)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun km(meters: Double): String = String.format(Locale.US, "%.2f", meters / 1000.0)

    fun speedKmh(mps: Double): Double = mps * 3.6

    fun pace(mps: Double): String {
        if (mps < 0.3) return "--:--"
        val secPerKm = (1000.0 / mps).roundToLong()
        return String.format(Locale.US, "%d:%02d", secPerKm / 60, secPerKm % 60)
    }

    fun oneDecimal(v: Double): String = String.format(Locale.US, "%.1f", v)

    fun integer(v: Double): String = v.roundToLong().toString()
}
