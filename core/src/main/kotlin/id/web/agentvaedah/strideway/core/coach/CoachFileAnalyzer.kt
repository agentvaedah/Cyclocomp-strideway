package id.web.agentvaedah.strideway.core.coach

import id.web.agentvaedah.strideway.core.format.Format
import id.web.agentvaedah.strideway.core.geo.Geo
import id.web.agentvaedah.strideway.core.model.Athlete
import id.web.agentvaedah.strideway.core.model.Sport
import id.web.agentvaedah.strideway.core.model.TrackPoint
import id.web.agentvaedah.strideway.core.track.ActivityStatsCalculator
import id.web.agentvaedah.strideway.core.track.TrackFormats
import id.web.agentvaedah.strideway.core.track.TrackParseException
import kotlin.math.roundToInt

/** Parsed file attached to an AI Coach prompt. */
data class CoachAttachment(
    val fileName: String,
    /** Short human-readable summary shown in the attachment chip and chat header. */
    val summaryLabel: String,
    /** Structured text block sent to the LLM for data analysis. */
    val promptContext: String,
)

/**
 * Parses uploaded user files (GPX/TCX/FIT tracks or CSV/JSON/TXT data files) into a compact,
 * high-signal analysis block that fits comfortably inside an LLM prompt.
 */
object CoachFileAnalyzer {
    private const val MAX_TEXT_CHARS = 7_500
    private val TRACK_EXTS = setOf("gpx", "tcx", "fit")
    private val TEXT_EXTS = setOf("csv", "tsv", "json", "txt", "md", "log")

    fun analyze(fileName: String, bytes: ByteArray, athlete: Athlete): Result<CoachAttachment> {
        if (bytes.isEmpty()) return Result.failure(IllegalArgumentException("File kosong."))
        val safeName = fileName.trim().ifBlank { "data" }
        val ext = safeName.substringAfterLast('.', "").lowercase()

        if (ext in TRACK_EXTS) {
            return parseTrack(safeName, bytes, athlete)
        }
        if (ext in TEXT_EXTS || looksLikeText(bytes)) {
            return parseText(safeName, bytes)
        }
        return Result.failure(
            IllegalArgumentException("Format file belum didukung. Gunakan GPX, TCX, FIT, CSV, JSON, atau TXT."),
        )
    }

    private fun parseTrack(fileName: String, bytes: ByteArray, athlete: Athlete): Result<CoachAttachment> {
        val track = try {
            TrackFormats.parse(fileName, bytes)
        } catch (e: TrackParseException) {
            return Result.failure(IllegalArgumentException(e.message ?: "File aktivitas tidak bisa dibaca."))
        }
        if (track.points.size < 2) {
            return Result.failure(IllegalArgumentException("File aktivitas tidak memiliki cukup titik jalur."))
        }
        val sport = track.sport ?: Sport.RIDE
        val stats = ActivityStatsCalculator.compute(track.points, sport, athlete)
        val kind = if (sport == Sport.RIDE) "Ride" else "Run"
        val distKm = Format.km(stats.distanceM)
        val dur = if (stats.movingMs > 0) Format.duration(stats.movingMs) else "tanpa waktu"
        val gain = "${stats.elevationGainM.roundToInt()} m naik"
        val label = "$kind · $distKm km · $dur · $gain"

        val splits = computeSplits(track.points, stats.distanceM)
        val prompt = buildString {
            appendLine("=== DATA FILE AKTIVITAS: $fileName ===")
            track.name?.takeIf { it.isNotBlank() }?.let { appendLine("Nama rute/sesi: $it") }
            appendLine("Olahraga: $kind · Jumlah titik: ${track.points.size}")
            appendLine("Jarak: $distKm km · Elevasi naik: ${stats.elevationGainM.roundToInt()} m" + (stats.maxElevationM?.let { " · Elevasi maks: ${it.roundToInt()} m" } ?: ""))
            if (stats.movingMs > 0) {
                val paceOrSpeed = if (sport == Sport.RUN) {
                    "Pace rata-rata: ${Format.pace(stats.avgSpeedKmh / 3.6)} /km"
                } else {
                    "Kecepatan rata-rata: ${Format.oneDecimal(stats.avgSpeedKmh)} km/j (maks ${Format.oneDecimal(stats.maxSpeedKmh)} km/j)"
                }
                appendLine("Waktu bergerak: ${Format.duration(stats.movingMs)} (total ${Format.duration(stats.elapsedMs)})")
                appendLine(paceOrSpeed)
                appendLine("Kalori: ${stats.calories.roundToInt()} kkal · TRIMP: ${stats.trimp.roundToInt()}")
            }
            if (stats.avgHr != null) {
                appendLine("HR rata-rata: ${stats.avgHr} bpm" + (stats.maxHr?.let { " (maks $it bpm)" } ?: ""))
            }
            stats.avgPower?.let { appendLine("Power rata-rata: $it W") }
            val cadences = track.points.mapNotNull { it.cadence }.filter { it > 0 }
            if (cadences.isNotEmpty()) {
                appendLine("Cadence rata-rata: ${cadences.average().roundToInt()} rpm")
            }
            if (splits.isNotEmpty()) {
                appendLine("Ringkasan segmen jarak:")
                splits.forEach { appendLine("- $it") }
            }
            append("=== AKHIR DATA FILE ===")
        }
        return Result.success(CoachAttachment(fileName, label, prompt))
    }

    private fun computeSplits(points: List<TrackPoint>, totalDistanceM: Double): List<String> {
        val timed = points.filter { it.timeMs != null }
        if (timed.size < 2 || totalDistanceM < 1_000.0) return emptyList()
        val bucketM = when {
            totalDistanceM >= 50_000.0 -> 10_000.0
            totalDistanceM >= 12_000.0 -> 5_000.0
            else -> 1_000.0
        }
        val bucketKm = (bucketM / 1_000.0).roundToInt()
        val out = ArrayList<String>()
        var accDist = 0.0
        var accTimeMs = 0L
        var accGain = 0.0
        var hrSum = 0L
        var hrCount = 0
        var powerSum = 0L
        var powerCount = 0
        var idx = 1

        fun flush(distM: Double) {
            if (distM < 200.0 || accTimeMs <= 0L) return
            val kmh = distM / (accTimeMs / 1000.0) * 3.6
            val hrPart = if (hrCount > 0) ", HR ${(hrSum / hrCount)} bpm" else ""
            val pwrPart = if (powerCount > 0) ", ${(powerSum / powerCount)} W" else ""
            val startKm = (idx - 1) * bucketKm
            val endKm = startKm + (distM / 1000.0)
            out += "KM $startKm-${Format.oneDecimal(endKm)}: ${Format.oneDecimal(kmh)} km/j, +${accGain.roundToInt()} m$hrPart$pwrPart"
            idx++
            accDist = 0.0
            accTimeMs = 0L
            accGain = 0.0
            hrSum = 0L
            hrCount = 0
            powerSum = 0L
            powerCount = 0
        }

        for ((a, b) in timed.zipWithNext()) {
            val dt = b.timeMs!! - a.timeMs!!
            if (dt <= 0L || dt > 30_000L) continue
            val d = Geo.distanceM(a.latLon, b.latLon)
            accDist += d
            accTimeMs += dt
            val e1 = a.ele
            val e2 = b.ele
            if (e1 != null && e2 != null && e2 > e1) accGain += (e2 - e1)
            b.hr?.takeIf { it in 25..250 }?.let { hrSum += it; hrCount++ }
            b.power?.takeIf { it >= 0 }?.let { powerSum += it; powerCount++ }
            if (accDist >= bucketM && out.size < 16) {
                flush(accDist)
            }
        }
        if (accDist >= 500.0 && out.size < 16) flush(accDist)
        return out
    }

    private fun parseText(fileName: String, bytes: ByteArray): Result<CoachAttachment> {
        val raw = bytes.toString(Charsets.UTF_8).trim()
        if (raw.isEmpty()) return Result.failure(IllegalArgumentException("File teks kosong."))
        val lines = raw.lineSequence().count()
        val truncated = raw.length > MAX_TEXT_CHARS
        val excerpt = if (truncated) raw.take(MAX_TEXT_CHARS) + "\n... (dipotong dari ${raw.length} karakter)" else raw
        val kb = (bytes.size / 1024.0).coerceAtLeast(0.1)
        val label = "$lines baris · ${Format.oneDecimal(kb)} KB"
        val prompt = buildString {
            appendLine("=== DATA FILE: $fileName ($lines baris) ===")
            appendLine(excerpt)
            append("=== AKHIR DATA FILE ===")
        }
        return Result.success(CoachAttachment(fileName, label, prompt))
    }

    private fun looksLikeText(bytes: ByteArray): Boolean {
        val sampleSize = minOf(bytes.size, 1024)
        var nonPrintable = 0
        for (i in 0 until sampleSize) {
            val b = bytes[i].toInt() and 0xFF
            if (b == 0) return false
            if (b < 0x09 || (b in 0x0E..0x1F)) nonPrintable++
        }
        return nonPrintable.toDouble() / sampleSize < 0.05
    }
}
