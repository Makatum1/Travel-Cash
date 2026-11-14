package com.example.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class NominatimPlaceSuggestion(
    val displayName: String,
    val shortPlaceName: String,
    val country: String,
    val cityOrTown: String,
    val latitude: Double,
    val longitude: Double,
    val euclideanDistance: Double?,
    val distanceKm: Double?
)

object NominatimService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Consulta la API pública de OpenStreetMap Nominatim enviando un viewbox centrado en las
     * coordenadas GPS actuales del usuario (lon-0.5, lat+0.5, lon+0.5, lat-0.5) y ordena
     * los resultados por distancia euclidiana real respecto a la ubicación GPS del usuario.
     */
    suspend fun searchPlacesByProximity(
        query: String,
        userLat: Double?,
        userLon: Double?
    ): List<NominatimPlaceSuggestion> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        try {
            val urlBuilder = "https://nominatim.openstreetmap.org/search".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("q", cleanQuery)
                ?.addQueryParameter("format", "jsonv2")
                ?.addQueryParameter("addressdetails", "1")
                ?.addQueryParameter("limit", "15")
                ?.addQueryParameter("accept-language", "es")
                ?: return@withContext emptyList()

            if (userLat != null && userLon != null) {
                val leftLon = userLon - 0.5
                val topLat = userLat + 0.5
                val rightLon = userLon + 0.5
                val bottomLat = userLat - 0.5
                urlBuilder.addQueryParameter("viewbox", "$leftLon,$topLat,$rightLon,$bottomLat")
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "TravelCashAndroidApp/1.0 (es; GestorGastosViajes)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = mutableListOf<NominatimPlaceSuggestion>()

                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                    val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                    val displayName = obj.optString("display_name", "")
                    val nameProp = obj.optString("name", "")
                    val addr = obj.optJSONObject("address")
                    val country = addr?.optString("country", "") ?: ""
                    val city = addr?.optString("city")
                        ?.takeIf { it.isNotBlank() }
                        ?: addr?.optString("town")?.takeIf { it.isNotBlank() }
                        ?: addr?.optString("village")?.takeIf { it.isNotBlank() }
                        ?: addr?.optString("municipality")?.takeIf { it.isNotBlank() }
                        ?: ""

                    val firstSegment = displayName.substringBefore(",").trim()
                    val shortName = when {
                        nameProp.isNotBlank() && city.isNotBlank() && !nameProp.equals(city, ignoreCase = true) ->
                            "$nameProp, $city"
                        nameProp.isNotBlank() -> nameProp
                        firstSegment.isNotBlank() && city.isNotBlank() -> "$firstSegment, $city"
                        firstSegment.isNotBlank() -> firstSegment
                        else -> displayName.take(50)
                    }

                    val euclid = if (userLat != null && userLon != null) {
                        sqrt((lat - userLat).pow(2.0) + (lon - userLon).pow(2.0))
                    } else null

                    val distKm = if (userLat != null && userLon != null) {
                        haversineKm(userLat, userLon, lat, lon)
                    } else null

                    list.add(
                        NominatimPlaceSuggestion(
                            displayName = displayName,
                            shortPlaceName = shortName,
                            country = country,
                            cityOrTown = city,
                            latitude = lat,
                            longitude = lon,
                            euclideanDistance = euclid,
                            distanceKm = distKm
                        )
                    )
                }

                // Ordenar por distancia euclidiana real respecto a la ubicación GPS del usuario
                if (userLat != null && userLon != null) {
                    list.sortedBy { it.euclideanDistance ?: Double.MAX_VALUE }
                } else {
                    list
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentDeviceLocation(context: Context): Pair<Double, Double>? {
        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val loc = suspendCancellableCoroutine<android.location.Location?> { cont ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                    .addOnCanceledListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
            if (loc != null) {
                loc.latitude to loc.longitude
            } else {
                // Fallback a lastLocation del LocationManager
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val providers = lm?.getProviders(true).orEmpty()
                var best: android.location.Location? = null
                for (p in providers) {
                    val l = lm?.getLastKnownLocation(p) ?: continue
                    if (best == null || l.accuracy < best.accuracy) {
                        best = l
                    }
                }
                best?.let { it.latitude to it.longitude }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resuelve el nombre del lugar y el país para unas coordenadas GPS dadas (lat, lon).
     * Utiliza primero Geocoder del sistema y como respaldo la API pública de Nominatim.
     */
    @Suppress("DEPRECATION")
    suspend fun reverseGeocodeLocation(context: Context, lat: Double, lon: Double): Pair<String, String>? = withContext(Dispatchers.IO) {
        // 1. Intentar con Geocoder nativo de Android
        try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val feature = addr.featureName
                val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                val country = addr.countryName ?: ""
                val place = when {
                    !feature.isNullOrBlank() && locality.isNotBlank() && feature != locality -> "$feature, $locality"
                    !feature.isNullOrBlank() -> feature
                    locality.isNotBlank() -> locality
                    else -> addr.getAddressLine(0)?.substringBefore(",") ?: ""
                }
                if (place.isNotBlank()) {
                    return@withContext (place to country)
                }
            }
        } catch (_: Exception) {}

        // 2. Respaldo mediante OpenStreetMap Nominatim Reverse
        try {
            val url = "https://nominatim.openstreetmap.org/reverse".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("lat", lat.toString())
                ?.addQueryParameter("lon", lon.toString())
                ?.addQueryParameter("format", "jsonv2")
                ?.addQueryParameter("addressdetails", "1")
                ?.addQueryParameter("accept-language", "es")
                ?.build() ?: return@withContext null

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TravelCashAndroidApp/1.0 (es; GestorGastosViajes)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val obj = org.json.JSONObject(bodyStr)
                val displayName = obj.optString("display_name", "")
                val nameProp = obj.optString("name", "")
                val addr = obj.optJSONObject("address")
                val country = addr?.optString("country", "") ?: ""
                val city = addr?.optString("city")
                    ?.takeIf { it.isNotBlank() }
                    ?: addr?.optString("town")?.takeIf { it.isNotBlank() }
                    ?: addr?.optString("village")?.takeIf { it.isNotBlank() }
                    ?: addr?.optString("municipality")?.takeIf { it.isNotBlank() }
                    ?: ""

                val firstSegment = displayName.substringBefore(",").trim()
                val shortName = when {
                    nameProp.isNotBlank() && city.isNotBlank() && !nameProp.equals(city, ignoreCase = true) -> "$nameProp, $city"
                    nameProp.isNotBlank() -> nameProp
                    firstSegment.isNotBlank() && city.isNotBlank() -> "$firstSegment, $city"
                    firstSegment.isNotBlank() -> firstSegment
                    else -> displayName.take(50)
                }
                return@withContext (shortName to country)
            }
        } catch (_: Exception) {
            null
        }
    }
}
