package com.example.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Representa un punto de marcador en el mapa con sus coordenadas reales en BD.
 */
data class MapPinInput(
    val key: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Calcula desplazamientos visuales (exclusivamente en tiempo de renderizado) para marcadores
 * que comparten exactamente las mismas coordenadas o están demasiado próximos entre sí.
 *
 * Nunca modifica las coordenadas reales almacenadas en la base de datos.
 */
object MapWaypointOffsetCalculator {

    private const val OVERLAP_THRESHOLD_DEGREES = 0.00015
    private const val BASE_RING_RADIUS_DEGREES = 0.00022
    private const val MAX_PINS_PER_RING = 8

    fun computeDisplayCoordinates(pins: List<MapPinInput>): Map<String, Pair<Double, Double>> {
        if (pins.isEmpty()) return emptyMap()

        val visited = BooleanArray(pins.size)
        val result = LinkedHashMap<String, Pair<Double, Double>>(pins.size)

        for (i in pins.indices) {
            if (visited[i]) continue

            val clusterIndices = mutableListOf<Int>()
            val queue = ArrayDeque<Int>()
            queue.add(i)
            visited[i] = true

            while (queue.isNotEmpty()) {
                val currIdx = queue.removeFirst()
                clusterIndices.add(currIdx)
                val curr = pins[currIdx]

                for (j in pins.indices) {
                    if (!visited[j]) {
                        val candidate = pins[j]
                        if (abs(curr.latitude - candidate.latitude) <= OVERLAP_THRESHOLD_DEGREES &&
                            abs(curr.longitude - candidate.longitude) <= OVERLAP_THRESHOLD_DEGREES
                        ) {
                            visited[j] = true
                            queue.add(j)
                        }
                    }
                }
            }

            if (clusterIndices.size == 1) {
                val single = pins[clusterIndices.first()]
                result[single.key] = single.latitude to single.longitude
            } else {
                val centerLat = clusterIndices.map { pins[it].latitude }.average()
                val centerLon = clusterIndices.map { pins[it].longitude }.average()
                val cosLat = cos(Math.toRadians(centerLat)).coerceAtLeast(0.2)

                val totalInCluster = clusterIndices.size
                clusterIndices.forEachIndexed { orderIndex, pinIndex ->
                    val pin = pins[pinIndex]
                    val ringIndex = orderIndex / MAX_PINS_PER_RING
                    val indexInRing = orderIndex % MAX_PINS_PER_RING
                    val remaining = totalInCluster - (ringIndex * MAX_PINS_PER_RING)
                    val pinsInThisRing = remaining.coerceAtMost(MAX_PINS_PER_RING)

                    val radius = BASE_RING_RADIUS_DEGREES * (1.0 + ringIndex * 0.85)
                    val angleOffset = if (pinsInThisRing == 2) PI / 4.0 else (ringIndex * PI / 8.0)
                    val angle = angleOffset + (2.0 * PI * indexInRing / pinsInThisRing)

                    val displayLat = centerLat + radius * sin(angle)
                    val displayLon = centerLon + (radius * cos(angle)) / cosLat

                    result[pin.key] = displayLat to displayLon
                }
            }
        }

        return result
    }
}
