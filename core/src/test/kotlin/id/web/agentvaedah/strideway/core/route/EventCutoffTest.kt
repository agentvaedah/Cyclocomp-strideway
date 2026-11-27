package id.web.agentvaedah.strideway.core.route

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EventCutoffTest {
    @Test fun `elapsed budget includes planned breaks and supports multi-day events`() {
        val plan = EventCutoff.parse("30:00", "60", "20,0")!!
        assertEquals(26 * 3_600_000L, plan.estimatedMs(500_000.0))
        assertEquals(4 * 3_600_000L, plan.remainingMs(plan.estimatedMs(500_000.0)))
        assertEquals(0L, plan.remainingMs(30 * 3_600_000L))
        assertEquals(-60_000L, plan.remainingMs(30 * 3_600_000L + 60_000L))
    }

    @Test fun `invalid budgets are not zero or infinite forecasts`() {
        for (d in listOf("", "8", "8:60", "0:00")) assertNull(EventCutoff.parse(d, "", "10"))
        for (s in listOf("NaN", "Infinity", "0", "-10")) assertNull(EventCutoff.parse("4:00", "", s))
        assertNull(EventCutoff.parse("4:00", "240", "10"))
    }
}
