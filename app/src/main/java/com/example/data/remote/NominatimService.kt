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
     * Consulta el Geocoder nativo del dispositivo (Google Play Services / Google Maps)
     * complementado con la API de OpenStreetMap Nominatim con prioridad por cercanía GPS,
     * devolviendo comercios, direcciones, restaurantes y puntos de interés equivalentes a Google Maps.
     */
    suspend fun searchPlacesByProximity(
        query: String,
        userLat: Double?,
        userLon: Double?,
        context: Context? = null
    ): List<NominatimPlaceSuggestion> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        val list = mutableListOf<NominatimPlaceSuggestion>()
        val seenCoords = mutableSetOf<String>()

        // 1. Intentar primero con Android Geocoder nativo (Google Maps / Google Location Services)
        if (context != null) {
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                val addresses = mutableListOf<android.location.Address>()

                // Búsqueda con prioridad en proximidad geográfica si hay GPS disponible
                if (userLat != null && userLon != null) {
                    val delta = 1.0 // ~110 km
                    val localResults = geocoder.getFromLocationName(
                        cleanQuery, 10,
                        userLat - delta, userLon - delta,
                        userLat + delta, userLon + delta
                    )
                    if (!localResults.isNullOrEmpty()) {
                        addresses.addAll(localResults)
                    }
                }

                // Búsqueda global para comercios, monumentos o ciudades más lejanas
                val globalResults = geocoder.getFromLocationName(cleanQuery, 10)
                if (!globalResults.isNullOrEmpty()) {
                    addresses.addAll(globalResults)
                }

                for (addr in addresses) {
                    val lat = addr.latitude
                    val lon = addr.longitude
                    val key = String.format(java.util.Locale.US, "%.3f,%.3f", lat, lon)
                    if (!seenCoords.add(key)) continue

                    val feature = addr.featureName.orEmpty()
                    val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                    val country = addr.countryName.orEmpty()
                    val fullAddress = (0..addr.maxAddressLineIndex)
                        .mapNotNull { addr.getAddressLine(it) }
                        .joinToString(", ")
                        .ifBlank {
                            listOf(feature, locality, country).filter { it.isNotBlank() }.joinToString(", ")
                        }

                    val shortName = when {
                        feature.isNotBlank() && locality.isNotBlank() && !feature.equals(locality, ignoreCase = true) ->
                            "$feature, $locality"
                        feature.isNotBlank() -> feature
                        locality.isNotBlank() -> locality
                        else -> fullAddress.take(50)
                    }

                    val euclid = if (userLat != null && userLon != null) {
                        sqrt((lat - userLat).pow(2.0) + (lon - userLon).pow(2.0))
                    } else null

                    val distKm = if (userLat != null && userLon != null) {
                        haversineKm(userLat, userLon, lat, lon)
                    } else null

                    list.add(
                        NominatimPlaceSuggestion(
                            displayName = fullAddress,
                            shortPlaceName = shortName,
                            country = country,
                            cityOrTown = locality,
                            latitude = lat,
                            longitude = lon,
                            euclideanDistance = euclid,
                            distanceKm = distKm
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // 2. Complemento con OpenStreetMap Nominatim (con bounded=0 para no excluir resultados lejanos)
        try {
            val langTag = java.util.Locale.getDefault().language.ifBlank { "es" }
            val urlBuilder = "https://nominatim.openstreetmap.org/search".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("q", cleanQuery)
                ?.addQueryParameter("format", "jsonv2")
                ?.addQueryParameter("addressdetails", "1")
                ?.addQueryParameter("limit", "20")
                ?.addQueryParameter("accept-language", langTag)
                ?: return@withContext list

            if (userLat != null && userLon != null) {
                val leftLon = userLon - 0.5
                val topLat = userLat + 0.5
                val rightLon = userLon + 0.5
                val bottomLat = userLat - 0.5
                urlBuilder.addQueryParameter("viewbox", "$leftLon,$topLat,$rightLon,$bottomLat")
                urlBuilder.addQueryParameter("bounded", "0")
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "TravelCashAndroidApp/1.0 ($langTag; GestorGastosViajes)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val array = JSONArray(bodyStr)
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                            val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                            val key = String.format(java.util.Locale.US, "%.3f,%.3f", lat, lon)
                            if (!seenCoords.add(key)) continue

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
                    }
                }
            }
        } catch (_: Exception) {}

        // Ordenar por cercanía si tenemos GPS activo
        if (userLat != null && userLon != null) {
            list.sortedBy { it.euclideanDistance ?: Double.MAX_VALUE }
        } else {
            list
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
