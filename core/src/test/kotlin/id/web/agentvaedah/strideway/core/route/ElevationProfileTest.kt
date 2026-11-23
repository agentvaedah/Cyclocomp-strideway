package id.web.agentvaedah.strideway.core.route

import id.web.agentvaedah.strideway.core.model.TrackPoint
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ElevationProfileTest {
    @Test fun `interpolates altitude by road distance and retains gaps`() {
        val profile = ElevationProfile.fromTrack(listOf(
            TrackPoint(0.0, 0.0, -5.0), TrackPoint(0.0, 0.0, null), TrackPoint(0.0, .01, 15.0),
            TrackPoint(0.0, .03, null), TrackPoint(0.0, .04, 30.0),
        ))
        assertEquals(4, profile.samples.size)
        assertEquals(-5.0, profile.low)
        assertEquals(30.0, profile.high)
        assertEquals(5.0, profile.altitudeAt(profile.endM / 8)!!, .01)
        assertNull(profile.altitudeAt(profile.endM / 2))
        assertNull(profile.altitudeAt(profile.endM + 1))
    }

    @Test fun `missing and flat elevation are distinct`() {
        assertNull(ElevationProfile.fromTrack(emptyList()).low)
        assertNull(ElevationProfile.fromTrack(listOf(TrackPoint(0.0, 0.0, Double.NaN))).high)
        val flat = ElevationProfile.fromTrack(listOf(TrackPoint(0.0, 0.0, 0.0), TrackPoint(0.0, .01, 0.0)))
        assertEquals(0.0, flat.altitudeAt(flat.endM * .5))
    }
}
