package id.web.agentvaedah.strideway.core.route

import id.web.agentvaedah.strideway.core.geo.Geo
import id.web.agentvaedah.strideway.core.model.LatLon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class RouteFlyoverTest {
    private val start = LatLon(-6.6, 106.8)
    private fun east(m: Double) = Geo.destination(start, 90.0, m)

    @Test
    fun `starts at the first point and ends at the last`() {
        val route = listOf(start, east(2_000.0), Geo.destination(east(2_000.0), 0.0, 2_000.0))
        val f = RouteFlyover(route)
        assertEquals(0.0, Geo.distanceM(f.at(0.0).position, route.first()), 0.5)
        assertEquals(0.0, Geo.distanceM(f.at(1.0).position, route.last()), 0.5)
        assertEquals(4_000.0, f.lengthM, 5.0)
    }

    @Test
    fun `progress is by distance, so the middle is halfway along the road`() {
        val route = listOf(start, east(1_000.0), east(3_000.0))
        val mid = RouteFlyover(route).at(0.5)
        assertEquals(1_500.0, Geo.distanceM(start, mid.position), 2.0)
        assertEquals(1_500.0, mid.distanceM, 0.5)
    }

    @Test
    fun `line gradient follows the puck in Mercator rather than ground metres`() {
        val fly = RouteFlyover(listOf(LatLon(0.0, 0.0), LatLon(30.0, 0.0), LatLon(60.0, 0.0)))
        // y(30°)/y(60°) and y(45°)/y(60°) along this meridian.
        assertEquals(0.41710, fly.lineProgressAt(fly.at(0.5).distanceM), 0.0001)
        assertEquals(0.66925, fly.lineProgressAt(fly.at(0.75).distanceM), 0.0001)
        assertEquals(0.0, fly.lineProgressAt(0.0))
        assertEquals(1.0, fly.lineProgressAt(fly.lengthM))
    }

    @Test
    fun `bearing looks ahead along a straight road`() {
        val f = RouteFlyover(listOf(start, east(5_000.0)))
        assertEquals(90.0, f.at(0.3).bearing, 0.5)
    }

    @Test
    fun `bearing turns smoothly through north instead of spinning the long way`() {
        // Heading 350 then turning to 10: the camera should pass through 0, never through 180.
        val a = Geo.destination(start, 350.0, 1_000.0)
        val b = Geo.destination(a, 10.0, 1_000.0)
        val f = RouteFlyover(listOf(start, a, b))
        var prev = f.at(0.0).bearing
        for (i in 1..200) {
            val now = f.at(i / 200.0).bearing
            val step = abs(((now - prev + 540.0) % 360.0) - 180.0)
            assertTrue(step < 10.0, "bearing jumped $step degrees at step $i")
            prev = now
        }
        val atEnd = f.at(1.0).bearing
        assertTrue(atEnd < 20.0 || atEnd > 340.0, "end bearing $atEnd should be near north")
    }

    @Test
    fun `one times keeps a stable speed even on long routes`() {
        assertEquals(20_000L, RouteFlyover(listOf(start, east(100.0))).durationMs)
        for (length in listOf(500.0, 30_000.0, 400_000.0)) {
            val f = RouteFlyover(listOf(start, east(length)))
            assertEquals(15.0, f.lengthM / (f.durationMs / 1000.0), .01)
        }
    }

    @Test
    fun `arrows advance on the route and face its direction through bends`() {
        val corner = east(200.0)
        val fly = RouteFlyover(listOf(start, corner, Geo.destination(corner, 0.0, 200.0)))
        val first = fly.arrows(0.0)
        val next = fly.arrows(1.0)
        assertTrue(first.isNotEmpty())
        assertEquals(first.size, next.size)
        assertTrue(first.zip(next).all { (a, b) -> b.distanceM > a.distanceM })
        assertEquals(90.0, next.first().bearing, 1.0)
        assertTrue(next.last().bearing < 1.0 || next.last().bearing > 359.0)
        assertTrue(RouteFlyover(listOf(start, start)).arrows(10.0).isEmpty())
    }

    @Test
    fun `a two point route still has frames`() {
        val f = RouteFlyover(listOf(start, east(10.0)))
        assertEquals(0.0, Geo.distanceM(f.at(0.0).position, start), 0.5)
        assertTrue(f.lengthM > 9.0)
    }
}
