package id.web.agentvaedah.strideway.core.geo

import id.web.agentvaedah.strideway.core.model.LatLon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeoTest {
    private val monas = LatLon(-6.1754, 106.8272)
    private val bundaranHi = LatLon(-6.1950, 106.8230)

    @Test
    fun haversineMonasToBundaranHi() {
        assertEquals(2228.33, Geo.distanceM(monas, bundaranHi), 0.05)
    }

    @Test
    fun oneDegreeOfLatitude() {
        assertEquals(111_195.08, Geo.distanceM(LatLon(0.0, 0.0), LatLon(1.0, 0.0)), 0.05)
    }

    @Test
    fun bearingMonasToBundaranHi() {
        assertEquals(192.03, Geo.bearingDeg(monas, bundaranHi), 0.01)
    }

    @Test
    fun destinationRoundTrip() {
        val d = Geo.destination(monas, 192.026, 2228.33)
        assertEquals(bundaranHi.lat, d.lat, 1e-5)
        assertEquals(bundaranHi.lon, d.lon, 1e-5)
    }
}

class ElevationGainTest {
    /** Feeds 1 m of road per sample, so a window of 100 m spans 100 samples. */
    private fun feed(g: ElevationGain, eles: List<Double>, stepM: Double = 1.0, accuracy: Double? = null) {
        eles.forEachIndexed { i, e -> g.add(i * stepM, e, accuracy) }
    }

    @Test
    fun ignoresNoiseBelowThreshold() {
        val g = ElevationGain(3.0)
        feed(g, listOf(100.0, 101.0, 99.5, 101.5, 100.0))
        assertEquals(0.0, g.gainM, 0.0)
    }

    @Test
    fun noiseAboveThresholdNoLongerAddsPhantomClimb() {
        // The old counter booked 8 m per cycle here. The band now books one metre at the very first rise and
        // then holds still, which is the difference between "noise counted" and "noise ignored".
        val g = ElevationGain(3.0, windowM = 0.0)
        feed(g, listOf(100.0, 104.0, 100.0, 104.0, 100.0))
        assertTrue(g.gainM <= 1.0, "gain was ${g.gainM}")
    }

    @Test
    fun countsRealClimbAndDescent() {
        // windowM = 0 makes the mean the sample itself, isolating the band.
        val g = ElevationGain(3.0, windowM = 0.0)
        feed(g, listOf(100.0, 104.0, 110.0, 105.0, 100.0))
        // The band absorbs one threshold at each turnaround: 100 -> 110 credits 7 m, and back down loses 4 m.
        assertEquals(7.0, g.gainM, 0.01)
        assertTrue(g.lossM in 3.5..4.5, "loss was ${g.lossM}")
    }

    @Test
    fun aFastOscillationInsideTheWindowAveragesOut() {
        val g = ElevationGain(3.0, windowM = 100.0)
        // Alternating ±6 m at 1 m spacing: every 100 m window averages to the same height.
        feed(g, List(600) { 100.0 + if (it % 2 == 0) 6.0 else -6.0 })
        assertEquals(0.0, g.gainM, 0.01)
    }

    @Test
    fun aSustainedClimbStillCounts() {
        val g = ElevationGain(3.0, windowM = 100.0)
        // 300 m of steady 10% road: 30 m of climbing. The trailing mean lags by about half the window,
        // so a short climb reads a little low; the test pins that ceiling rather than the exact 30.
        feed(g, List(300) { 100.0 + it * 0.1 })
        assertTrue(g.gainM in 20.0..30.0, "gain was ${g.gainM}")
    }

    @Test
    fun poorVerticalAccuracyIsSkipped() {
        val g = ElevationGain(3.0, windowM = 0.0)
        feed(g, listOf(100.0, 130.0, 160.0), accuracy = 60.0)
        assertEquals(0.0, g.gainM, 0.0)
        assertNull(g.maxEle)
    }

    @Test
    fun tracksHighestAndLowestPoint() {
        val g = ElevationGain(3.0, windowM = 0.0)
        feed(g, listOf(100.0, 140.0, 120.0, 90.0))
        assertEquals(140.0, g.maxEle!!, 1e-9)
        assertEquals(90.0, g.minEle!!, 1e-9)
    }

    @Test
    fun resetDropsTheReferenceSoAPauseIsNotClimb() {
        val g = ElevationGain(3.0, windowM = 0.0)
        g.add(0.0, 100.0)
        g.add(1.0, 101.0)
        g.reset()
        // Same road continues, but the rider stopped for an hour at the top: nothing was climbed.
        g.add(2.0, 160.0)
        g.add(3.0, 161.0)
        assertEquals(0.0, g.gainM, 0.01)
    }
}

class GradeWindowTest {
    @Test
    fun nullUntilWindowHalfFull() {
        val w = GradeWindow(50.0)
        assertNull(w.add(0.0, 100.0))
        assertNull(w.add(10.0, 100.5))
    }

    @Test
    fun fivePercentClimb() {
        val w = GradeWindow(50.0)
        var last: Double? = null
        for (i in 0..20) last = w.add(i * 10.0, 100.0 + i * 0.5)
        assertEquals(5.0, last!!, 1e-9)
    }
}

class PolylineTest {
    @Test
    fun encodesGoogleReferenceExample() {
        val pts = listOf(LatLon(38.5, -120.2), LatLon(40.7, -120.95), LatLon(43.252, -126.453))
        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", Polyline.encode(pts))
    }

    @Test
    fun decodeRoundTrip() {
        val pts = listOf(LatLon(-6.17540, 106.82720), LatLon(-6.19500, 106.82300))
        val back = Polyline.decode(Polyline.encode(pts))
        assertEquals(pts.size, back.size)
        assertEquals(pts[1].lat, back[1].lat, 1e-6)
        assertEquals(pts[1].lon, back[1].lon, 1e-6)
    }
}

class TileMathTest {
    @Test
    fun monasTileAtZoom12() {
        assertEquals(3263, TileMath.lonToTileX(106.8272, 12))
        assertEquals(2118, TileMath.latToTileY(-6.1754, 12))
    }

    @Test
    fun tileBoundsContainPoint() {
        val x = TileMath.lonToTileX(106.8272, 12)
        val y = TileMath.latToTileY(-6.1754, 12)
        assertTrue(TileMath.tileWest(x, 12) <= 106.8272 && TileMath.tileWest(x + 1, 12) > 106.8272)
        assertTrue(TileMath.tileNorth(y, 12) >= -6.1754 && TileMath.tileNorth(y + 1, 12) < -6.1754)
    }
}
