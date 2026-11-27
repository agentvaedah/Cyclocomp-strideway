package id.web.agentvaedah.strideway.core.route

import id.web.agentvaedah.strideway.core.geo.Geo
import id.web.agentvaedah.strideway.core.model.LatLon
import kotlin.math.asinh
import kotlin.math.hypot
import kotlin.math.tan

data class FlyFrame(val position: LatLon, val bearing: Double, val distanceM: Double)

/**
 * Camera path for previewing a planned route: frames are placed by distance along the road, so routes
 * without timestamps play at an even pace. The camera looks [lookAheadM] down the road and its bearing is
 * the circular mean of several look-ahead directions, so turns ease in and it never spins the long way.
 */
class RouteFlyover(private val route: List<LatLon>, private val lookAheadM: Double = 150.0) {
    private val cum: DoubleArray = DoubleArray(route.size).also { c ->
        for (i in 1 until route.size) c[i] = c[i - 1] + Geo.distanceM(route[i - 1], route[i])
    }
    private fun mercatorY(lat: Double) = asinh(tan(Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))))
    private val y by lazy { DoubleArray(route.size) { mercatorY(route[it].lat) } }
    private val projected by lazy {
        DoubleArray(route.size).also { c ->
            for (i in 1 until route.size) {
                val dx = Math.toRadians(route[i].lon - route[i - 1].lon)
                c[i] = c[i - 1] + hypot(dx, y[i] - y[i - 1])
            }
        }
    }

    val lengthM: Double get() = cum.lastOrNull() ?: 0.0

    /** At 1× the camera covers at most 15 metres per second, even on all-day routes. */
    val durationMs: Long get() = (lengthM / 15.0 * 1_000).toLong().coerceAtLeast(20_000L)

    fun at(progress: Double): FlyFrame {
        require(route.size >= 2) { "A route needs two points" }
        val d = progress.coerceIn(0.0, 1.0) * lengthM
        val here = pointAt(d)
        // Average several look-ahead directions on the unit circle: smooth through turns and through north.
        var sx = 0.0
        var sy = 0.0
        for (k in 1..5) {
            val ahead = pointAt(d + lookAheadM * k / 5.0)
            val from = if (Geo.distanceM(here, ahead) < 1.0) pointAt(d - lookAheadM * k / 5.0) to here else here to ahead
            if (Geo.distanceM(from.first, from.second) < 1.0) continue
            val b = Math.toRadians(Geo.bearingDeg(from.first, from.second))
            sx += Math.sin(b)
            sy += Math.cos(b)
        }
        val bearing = if (sx == 0.0 && sy == 0.0) Geo.bearingDeg(route[route.size - 2], route.last())
        else (Math.toDegrees(Math.atan2(sx, sy)) + 360.0) % 360.0
        return FlyFrame(here, bearing, d)
    }

    private fun segmentAt(d: Double): Int {
        var lo = 0
        var hi = cum.lastIndex
        while (lo < hi - 1) {
            val mid = (lo + hi) / 2
            if (cum[mid] <= d) lo = mid else hi = mid
        }
        return lo
    }

    private fun groundFraction(i: Int, d: Double) = (d - cum[i]) / (cum[i + 1] - cum[i])

    private fun latitude(i: Int, f: Double) = route[i].lat + (route[i + 1].lat - route[i].lat) * f

    private fun projectedFraction(i: Int, f: Double): Double {
        val dy = y[i + 1] - y[i]
        return if (dy == 0.0) f else ((mercatorY(latitude(i, f)) - y[i]) / dy).coerceIn(0.0, 1.0)
    }

    /** MapLibre's line-progress uses Web Mercator length, not metres on the ground. */
    fun lineProgressAt(d: Double): Double {
        if (d <= 0.0 || lengthM == 0.0) return 0.0
        if (d >= lengthM) return 1.0
        val i = segmentAt(d)
        val t = projectedFraction(i, groundFraction(i, d))
        val total = projected.last()
        return if (total > 0.0) (projected[i] + (projected[i + 1] - projected[i]) * t) / total else 0.0
    }

    /** Point [d] metres along the route, clamped to the ends. */
    fun pointAt(d: Double): LatLon {
        if (d <= 0) return route.first()
        if (d >= lengthM) return route.last()
        val i = segmentAt(d)
        val f = groundFraction(i, d)
        val lon = route[i].lon + (route[i + 1].lon - route[i].lon) * projectedFraction(i, f)
        return LatLon(latitude(i, f), lon)
    }

    /** Equally spaced arrows travel on the geometry rather than sliding straight through bends. */
    fun arrows(elapsedSeconds: Double): List<FlyFrame> {
        if (lengthM < 1.0) return emptyList()
        val count = kotlin.math.ceil(lengthM / 140.0).toInt().coerceIn(1, 48)
        val spacing = lengthM / count
        val phase = (spacing * 0.25 + elapsedSeconds * minOf(14.0, spacing / 5)) % spacing
        return List(count) { i ->
            val d = i * spacing + phase
            FlyFrame(pointAt(d), Geo.bearingDeg(pointAt((d - 1).coerceAtLeast(0.0)), pointAt(d + 1)), d)
        }
    }
}
