package id.web.agentvaedah.strideway.core.geo

/**
 * Accumulates climbing from a rolling mean of altitude over the last [windowM] metres of road, then applies a
 * [threshold] hysteresis band and credits only the part above it. Averaging over distance is what separates
 * real terrain from GPS vertical noise: a consumer fix wanders by metres between samples, and counting each
 * sample's swing (as a plain step counter does) invents hundreds of metres on a flat ride. The band stops the
 * remaining slow drift from trickling into the total.
 *
 * Samples with a vertical accuracy worse than [maxAccuracyM] are ignored. Not thread-safe; the recording
 * service feeds it from one coroutine.
 */
class ElevationGain(
    private val threshold: Double = 3.0,
    private val windowM: Double = 100.0,
    private val maxAccuracyM: Double = MAX_VERTICAL_ERROR_M,
) {
    /** Altitude samples still inside the distance window, oldest first. */
    private val window = ArrayDeque<Pair<Double, Double>>()
    private var reference: Double? = null

    var gainM: Double = 0.0
        private set
    var lossM: Double = 0.0
        private set

    /** Highest and lowest accepted sample, for the "Elevasi maks" figure; null until one arrives. */
    var maxEle: Double? = null
        private set
    var minEle: Double? = null
        private set

    /** [distanceM] is cumulative distance along the route, [ele] the altitude at that point. */
    fun add(distanceM: Double, ele: Double, verticalAccuracyM: Double? = null) {
        if (verticalAccuracyM != null && verticalAccuracyM > maxAccuracyM) return
        if (!ele.isFinite() || !distanceM.isFinite()) return
        if (maxEle == null || ele > maxEle!!) maxEle = ele
        if (minEle == null || ele < minEle!!) minEle = ele

        window.addLast(distanceM to ele)
        // A zero window means one sample: the mean is the sample itself, which makes the band exact.
        while (window.size > 1 && distanceM - window.first().first > windowM) window.removeFirst()

        val smooth = window.sumOf { it.second } / window.size
        val ref = reference
        if (ref == null) {
            reference = smooth
            return
        }
        val delta = smooth - ref
        if (delta >= threshold) {
            gainM += delta - threshold
            reference = smooth - threshold
        } else if (delta <= -threshold) {
            lossM += -delta - threshold
            reference = smooth + threshold
        }
    }

    /** Drops the reference and the window, so altitude gained while stopped is not booked as climbing. */
    fun reset() {
        window.clear()
        reference = null
    }

    companion object {
        const val MAX_VERTICAL_ERROR_M = 25.0
    }
}
