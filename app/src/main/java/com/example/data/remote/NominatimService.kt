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
    val distanceKm: Double?,
    val formattedAddress: String = ""
) {
    /**
     * Devuelve la dirección formateada limpia (sin el nombre del lugar).
     * Si no hay dirección formateada (o es idéntica al nombre), devuelve cadena vacía.
     */
    fun resolveFormattedAddress(): String {
        if (formattedAddress.isNotBlank()) return formattedAddress.trim()
        val trimmedDisplay = displayName.trim()
        val trimmedName = shortPlaceName.trim()
        return when {
            trimmedName.isNotBlank() && trimmedDisplay.startsWith(trimmedName, ignoreCase = true) -> {
                val after = trimmedDisplay.substring(trimmedName.length).trim()
                after.removePrefix(",").trim()
            }
            trimmedName.isNotBlank() && trimmedDisplay.equals(trimmedName, ignoreCase = true) -> ""
            else -> trimmedDisplay
        }
    }
}

object NominatimService {

    /**
     * Comprueba si un fragmento representa un descriptor de dirección (ej. "Bajo", "s/n", "12", "1º A"),
     * para evitar asignarlo erróneamente como título de Lugar.
     */
    fun isAddressFragment(str: String): Boolean {
        val lower = str.trim().lowercase(java.util.Locale.ROOT)
        if (lower.isEmpty()) return true
        if (lower.matches(Regex("^(bajo|s/n|sn|piso|planta|ático|atico|entreplanta|entresuelo|local|nave|puerta|bloque|escalera|esc|km\\.?\\s*\\d+).*"))) return true
        if (lower.matches(Regex("^[0-9]+[a-zA-Zºª\\s\\-/]*$"))) return true
        return false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Consulta el Geocoder nativo del dispositivo (Google Play Services / Google Maps)
     * complementado con la API de OpenStreetMap Nominatim con prioridad por cercanía GPS,
     * extrayendo de forma precisa el título del establecimiento o POI para asignarlo como Lugar
     * y evitando colocar fragmentos como "Bajo" o el número de portal en el título.
     */
    @Suppress("DEPRECATION")
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

                    val feature = addr.featureName.orEmpty().trim()
                    val thoroughfare = addr.thoroughfare.orEmpty().trim()
                    val subThoroughfare = addr.subThoroughfare.orEmpty().trim()
                    val locality = (addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "").trim()
                    val country = addr.countryName.orEmpty().trim()
                    val fullLine0 = addr.getAddressLine(0).orEmpty().trim()
                    val allLines = (0..addr.maxAddressLineIndex)
                        .mapNotNull { addr.getAddressLine(it)?.trim() }
                        .filter { it.isNotBlank() }
                    val fullAddress = if (allLines.isNotEmpty()) allLines.joinToString(", ") else fullLine0.ifBlank {
                        listOf(thoroughfare, subThoroughfare, locality, country).filter { it.isNotBlank() }.joinToString(", ")
                    }

                    val firstSegment = fullLine0.substringBefore(",").trim()

                    // Determinación limpia del título de Lugar (evitando 'Bajo', números de portal y nombres de calle cuando hay un POI/nombre real)
                    val placeTitle = when {
                        // Si featureName es un nombre propio real (no un descriptor como 'Bajo', ni un número, ni la calle ni la localidad)
                        feature.isNotBlank() && !isAddressFragment(feature) && !feature.equals(thoroughfare, ignoreCase = true) && !feature.equals(locality, ignoreCase = true) ->
                            feature
                        // Si el primer segmento de la línea devuelta por Geocoder es el nombre del POI (ej. "Asoc. Peregrinus Dezae")
                        firstSegment.isNotBlank() && !isAddressFragment(firstSegment) && !firstSegment.equals(thoroughfare, ignoreCase = true) && !firstSegment.equals(locality, ignoreCase = true) ->
                            firstSegment
                        // Si la búsqueda coincide con un nombre comercial o asociación que aparece en la dirección
                        cleanQuery.isNotBlank() && !isAddressFragment(cleanQuery) && fullAddress.contains(cleanQuery, ignoreCase = true) -> {
                            if (firstSegment.contains(cleanQuery, ignoreCase = true)) firstSegment else cleanQuery
                        }
                        // Si hay nombre de vía con portal (ej. "Rúa Rosalía de Castro, 12")
                        thoroughfare.isNotBlank() ->
                            if (subThoroughfare.isNotBlank()) "$thoroughfare, $subThoroughfare" else thoroughfare
                        feature.isNotBlank() && !isAddressFragment(feature) ->
                            feature
                        firstSegment.isNotBlank() && !isAddressFragment(firstSegment) ->
                            firstSegment
                        locality.isNotBlank() ->
                            locality
                        else ->
                            cleanQuery.ifBlank { "Lugar" }
                    }

                    // Extraer Dirección formateada (evitando duplicar el título)
                    val addressOnly = when {
                        fullAddress.startsWith(placeTitle, ignoreCase = true) ->
                            fullAddress.substring(placeTitle.length).trim().removePrefix(",").trim()
                        fullLine0.startsWith(placeTitle, ignoreCase = true) ->
                            fullLine0.substring(placeTitle.length).trim().removePrefix(",").trim()
                        fullAddress.equals(placeTitle, ignoreCase = true) ->
                            ""
                        else -> fullAddress.trim()
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
                            shortPlaceName = placeTitle,
                            country = country,
                            cityOrTown = locality,
                            latitude = lat,
                            longitude = lon,
                            euclideanDistance = euclid,
                            distanceKm = distKm,
                            formattedAddress = addressOnly
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

                            val displayName = obj.optString("display_name", "").trim()
                            val nameProp = obj.optString("name", "").trim()
                            val addr = obj.optJSONObject("address")
                            val country = addr?.optString("country", "")?.trim() ?: ""
                            val city = (addr?.optString("city")
                                ?.takeIf { it.isNotBlank() }
                                ?: addr?.optString("town")?.takeIf { it.isNotBlank() }
                                ?: addr?.optString("village")?.takeIf { it.isNotBlank() }
                                ?: addr?.optString("municipality")?.takeIf { it.isNotBlank() }
                                ?: "").trim()
                            val road = addr?.optString("road", "")?.trim() ?: ""

                            val firstSegment = displayName.substringBefore(",").trim()

                            // Determinación limpia del título de Lugar
                            val placeTitle = when {
                                // 1. Si nameProp es un nombre de POI real (no un descriptor como 'Bajo', ni un número, ni igual a la calle ni a la ciudad)
                                nameProp.isNotBlank() && !isAddressFragment(nameProp) && !nameProp.equals(road, ignoreCase = true) && !nameProp.equals(city, ignoreCase = true) ->
                                    nameProp
                                // 2. Si el primer segmento del nombre devuelto es un POI real
                                firstSegment.isNotBlank() && !isAddressFragment(firstSegment) && !firstSegment.equals(road, ignoreCase = true) && !firstSegment.equals(city, ignoreCase = true) ->
                                    firstSegment
                                // 3. Si coincide con la consulta comercial buscada
                                cleanQuery.isNotBlank() && !isAddressFragment(cleanQuery) && displayName.contains(cleanQuery, ignoreCase = true) -> {
                                    if (firstSegment.contains(cleanQuery, ignoreCase = true)) firstSegment else cleanQuery
                                }
                                // 4. Si nameProp no es un fragmento de dirección
                                nameProp.isNotBlank() && !isAddressFragment(nameProp) ->
                                    nameProp
                                // 5. Si firstSegment no es un fragmento de dirección
                                firstSegment.isNotBlank() && !isAddressFragment(firstSegment) ->
                                    firstSegment
                                cleanQuery.isNotBlank() && !isAddressFragment(cleanQuery) ->
                                    cleanQuery
                                city.isNotBlank() ->
                                    city
                                else ->
                                    cleanQuery.ifBlank { "Lugar" }
                            }

                            // Extraer Dirección formateada
                            val addressOnly = when {
                                displayName.startsWith(placeTitle, ignoreCase = true) ->
                                    displayName.substring(placeTitle.length).trim().removePrefix(",").trim()
                                displayName.equals(placeTitle, ignoreCase = true) ->
                                    ""
                                displayName.contains(",") ->
                                    displayName.substringAfter(",").trim()
                                else -> ""
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
                                    shortPlaceName = placeTitle,
                                    country = country,
                                    cityOrTown = city,
                                    latitude = lat,
                                    longitude = lon,
                                    euclideanDistance = euclid,
                                    distanceKm = distKm,
                                    formattedAddress = addressOnly
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
            val loc = suspendCancellableCoroutine { cont ->
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
                val feature = addr.featureName.orEmpty().trim()
                val locality = (addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "").trim()
                val country = addr.countryName.orEmpty().trim()
                val thoroughfare = addr.thoroughfare.orEmpty().trim()
                val firstSegment = addr.getAddressLine(0)?.substringBefore(",")?.trim().orEmpty()

                val place = when {
                    feature.isNotBlank() && !isAddressFragment(feature) && !feature.equals(thoroughfare, ignoreCase = true) && !feature.equals(locality, ignoreCase = true) ->
                        feature
                    firstSegment.isNotBlank() && !isAddressFragment(firstSegment) && !firstSegment.equals(thoroughfare, ignoreCase = true) && !firstSegment.equals(locality, ignoreCase = true) ->
                        firstSegment
                    feature.isNotBlank() && !isAddressFragment(feature) ->
                        feature
                    firstSegment.isNotBlank() && !isAddressFragment(firstSegment) ->
                        firstSegment
                    locality.isNotBlank() ->
                        locality
                    else -> ""
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
                val displayName = obj.optString("display_name", "").trim()
                val nameProp = obj.optString("name", "").trim()
                val addr = obj.optJSONObject("address")
                val country = addr?.optString("country", "")?.trim() ?: ""
                val city = (addr?.optString("city")
                    ?.takeIf { it.isNotBlank() }
                    ?: addr?.optString("town")?.takeIf { it.isNotBlank() }
                    ?: addr?.optString("village")?.takeIf { it.isNotBlank() }
                    ?: addr?.optString("municipality")?.takeIf { it.isNotBlank() }
                    ?: "").trim()
                val road = addr?.optString("road", "")?.trim() ?: ""

                val firstSegment = displayName.substringBefore(",").trim()
                val place = when {
                    nameProp.isNotBlank() && !isAddressFragment(nameProp) && !nameProp.equals(road, ignoreCase = true) && !nameProp.equals(city, ignoreCase = true) ->
                        nameProp
                    firstSegment.isNotBlank() && !isAddressFragment(firstSegment) && !firstSegment.equals(road, ignoreCase = true) && !firstSegment.equals(city, ignoreCase = true) ->
                        firstSegment
                    nameProp.isNotBlank() && !isAddressFragment(nameProp) ->
                        nameProp
                    firstSegment.isNotBlank() && !isAddressFragment(firstSegment) ->
                        firstSegment
                    city.isNotBlank() ->
                        city
                    else -> ""
                }
                return@withContext (place to country)
            }
        } catch (_: Exception) {
            null
        }
    }
}
