package id.web.agentvaedah.strideway.core.route

import id.web.agentvaedah.strideway.core.model.LatLon
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ItineraryTest {
    private val stops = (1..4).map { Endpoint("$it", LatLon(0.0, it.toDouble())) }

    @Test fun `stops and reverse change the order sent to routing`() {
        val (a, b, c, d) = stops
        val route = Itinerary(a, listOf(b, c), d)
        assertEquals(listOf(a, c, b, d).map { it.pos }, route.moveUp(1).points)
        assertEquals(route.points.reversed(), route.reverse().points)
    }

    @Test fun `loop needs distinct consecutive points and both endpoints`() {
        val (a, b, _, d) = stops
        assertFalse(Itinerary(to = b).canRoute)
        assertFalse(Itinerary(a, to = a).canRoute)
        assertFalse(Itinerary(a, listOf(b, b), d).canRoute)
        assertTrue(Itinerary(a, listOf(b), a).canRoute)
    }
}
