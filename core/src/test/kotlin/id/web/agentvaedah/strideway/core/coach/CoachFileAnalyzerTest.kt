package id.web.agentvaedah.strideway.core.coach

import id.web.agentvaedah.strideway.core.model.Athlete
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoachFileAnalyzerTest {
    private val athlete = Athlete()

    @Test
    fun `analyzes gpx file into track metrics and splits`() {
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Strideway">
              <trk>
                <name>Morning Loop</name>
                <trkseg>
                  <trkpt lat="-6.2000" lon="106.8000"><ele>10.0</ele><time>2026-11-20T06:00:00Z</time></trkpt>
                  <trkpt lat="-6.2050" lon="106.8000"><ele>15.0</ele><time>2026-11-20T06:02:00Z</time></trkpt>
                  <trkpt lat="-6.2100" lon="106.8000"><ele>22.0</ele><time>2026-11-20T06:04:00Z</time></trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent().toByteArray()

        val result = CoachFileAnalyzer.analyze("morning.gpx", gpx, athlete)
        assertTrue(result.isSuccess)
        val att = result.getOrThrow()
        assertEquals("morning.gpx", att.fileName)
        assertTrue(att.summaryLabel.contains("km"))
        assertTrue(att.promptContext.contains("Morning Loop"))
        assertTrue(att.promptContext.contains("DATA FILE AKTIVITAS"))
    }

    @Test
    fun `analyzes csv data file`() {
        val csv = "km,pace,hr\n1,5:20,145\n2,5:15,152\n3,5:30,160\n".toByteArray()
        val result = CoachFileAnalyzer.analyze("splits.csv", csv, athlete)
        assertTrue(result.isSuccess)
        val att = result.getOrThrow()
        assertEquals("splits.csv", att.fileName)
        assertTrue(att.summaryLabel.contains("4 baris"))
        assertTrue(att.promptContext.contains("1,5:20,145"))
    }

    @Test
    fun `filters gps altitude noise in segment splits`() {
        val pts = (0..30).joinToString("\n") { i ->
            val lat = -6.2000 - i * 0.0005 // ~55 m per step, ~1.65 km total
            val ele = 10.0 + if (i % 2 == 0) 1.5 else -1.5 // flat road with ±1.5 m GPS jitter
            val sec = i * 10
            val min = sec / 60
            val rem = sec % 60
            """<trkpt lat="$lat" lon="106.8000"><ele>$ele</ele><time>2026-11-20T06:${"%02d".format(min)}:${"%02d".format(rem)}Z</time></trkpt>"""
        }
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Strideway">
              <trk><name>Flat Jitter</name><trkseg>$pts</trkseg></trk>
            </gpx>
        """.trimIndent().toByteArray()

        val att = CoachFileAnalyzer.analyze("flat.gpx", gpx, athlete).getOrThrow()
        assertTrue(att.promptContext.contains("KM 0-"))
        assertTrue(att.promptContext.contains(", +0 m"), "Expected filtered 0 m segment climb, got:\n${att.promptContext}")
    }

    @Test
    fun `rejects empty or binary files`() {
        assertTrue(CoachFileAnalyzer.analyze("empty.csv", ByteArray(0), athlete).isFailure)
        val bin = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x00, 0x01, 0x02)
        assertTrue(CoachFileAnalyzer.analyze("doc.pdf", bin, athlete).isFailure)
    }
}
