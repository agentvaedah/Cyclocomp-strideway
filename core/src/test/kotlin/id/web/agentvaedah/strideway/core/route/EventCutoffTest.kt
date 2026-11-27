package id.web.agentvaedah.strideway.core.route

import id.web.agentvaedah.strideway.core.format.Format
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EventCutoffTest {
    @Test fun `home forecast for a century ride includes stops and qualifies the finish margin`() {
        val plan = EventCutoff.parse("6:00", "30", "20")!!
        val finish = plan.estimatedMs(100_000.0)
        assertEquals("5 jam 30 mnt", Format.elapsedWords(finish))
        assertEquals("30 mnt", Format.elapsedWords(plan.remainingMs(finish)))
        assertEquals(0L, plan.remainingMs(plan.estimatedMs(110_000.0)))
        assertEquals(-1_800_000L, plan.remainingMs(plan.estimatedMs(120_000.0)))
        assertEquals("25 jam 5 dtk", Format.elapsedWords(90_005_000L))
    }

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
