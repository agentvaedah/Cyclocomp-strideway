package id.web.agentvaedah.strideway.core.route

import id.web.agentvaedah.strideway.core.model.LatLon

data class Endpoint(val name: String, val pos: LatLon)

data class Itinerary(
    val from: Endpoint? = null,
    val stops: List<Endpoint> = emptyList(),
    val to: Endpoint? = null,
) {
    val points: List<LatLon> get() = (listOfNotNull(from) + stops + listOfNotNull(to)).map { it.pos }
    val canRoute: Boolean get() = from != null && to != null && points.zipWithNext().all { (a, b) -> a != b }

    fun reverse() = Itinerary(to, stops.reversed(), from)
    fun moveUp(i: Int) = if (i in 1..stops.lastIndex) copy(stops = stops.toMutableList().apply {
        add(i - 1, removeAt(i))
    }) else this
}
