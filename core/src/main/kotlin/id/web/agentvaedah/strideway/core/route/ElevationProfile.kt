package id.web.agentvaedah.strideway.core.route

import id.web.agentvaedah.strideway.core.geo.Geo
import id.web.agentvaedah.strideway.core.model.TrackPoint

data class ElevationSample(val distanceM: Double, val altitudeM: Double?)

/** Sorted by distance along the path; missing heights remain gaps rather than sea level. */
class ElevationProfile(val samples: List<ElevationSample>) {
    val startM = samples.firstOrNull()?.distanceM ?: 0.0
    val endM = samples.lastOrNull()?.distanceM ?: 0.0
    val low = samples.minOfOrNull { it.altitudeM ?: Double.POSITIVE_INFINITY }?.takeIf { it.isFinite() }
    val high = samples.maxOfOrNull { it.altitudeM ?: Double.NEGATIVE_INFINITY }?.takeIf { it.isFinite() }

    fun altitudeAt(distanceM: Double): Double? {
        val index = samples.binarySearchBy(distanceM) { it.distanceM }
        if (index >= 0) return samples[index].altitudeM
        val after = -index - 1
        val a = samples.getOrNull(after - 1) ?: return null
        val b = samples.getOrNull(after) ?: return null
        val from = a.altitudeM ?: return null
        val to = b.altitudeM ?: return null
        return from + (to - from) * ((distanceM - a.distanceM) / (b.distanceM - a.distanceM))
    }

    companion object {
        fun fromTrack(points: List<TrackPoint>): ElevationProfile {
            var distance = 0.0
            val samples = ArrayList<ElevationSample>(points.size)
            points.forEachIndexed { i, p ->
                if (i > 0) distance += Geo.distanceM(points[i - 1].latLon, p.latLon)
                val h = p.ele?.takeIf { it.isFinite() }
                if (samples.lastOrNull()?.distanceM == distance) {
                    if (h != null) samples[samples.lastIndex] = ElevationSample(distance, h)
                } else samples += ElevationSample(distance, h)
            }
            return ElevationProfile(samples)
        }
    }
}
