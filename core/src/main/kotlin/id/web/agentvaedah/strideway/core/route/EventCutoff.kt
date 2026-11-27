package id.web.agentvaedah.strideway.core.route

import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

/** Finish cutoff relative to the event start; all pauses count against it. */
@Serializable
data class EventCutoff(val limitMin: Int, val stopMin: Int, val speedKmh: Double) {
    fun remainingMs(elapsedMs: Long) = limitMin * 60_000L - elapsedMs
    fun estimatedMs(distanceM: Double) = (distanceM / speedKmh * 3600).roundToLong() + stopMin * 60_000L

    companion object {
        fun parse(duration: String, stops: String, speed: String): EventCutoff? {
            val (h, m) = Regex("""(\d{1,3}):([0-5]\d)""").matchEntire(duration.trim())?.destructured ?: return null
            val limit = h.toInt() * 60 + m.toInt()
            val rest = stops.trim().ifEmpty { "0" }.toIntOrNull() ?: return null
            val kmh = speed.trim().replace(',', '.').toDoubleOrNull() ?: return null
            return if (rest in 0 until limit && kmh.isFinite() && kmh in 0.1..200.0) EventCutoff(limit, rest, kmh) else null
        }
    }
}
