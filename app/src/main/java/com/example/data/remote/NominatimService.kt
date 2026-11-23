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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
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
     * Comprueba si un fragmento de texto representa un número de portal, planta, piso o descriptor
     * genérico (ej. "Bajo", "12", "1º A", "s/n"), para evitar que se tome erróneamente como título del POI.
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
     * Resuelve de forma segura la API Key de Google Places desde BuildConfig (.env),
     * variables de entorno o metadatos de la aplicación.
     */
    fun resolveGooglePlacesApiKey(context: Context?): String {
        val buildConfigKey = try {
            com.example.BuildConfig::class.java.getField("GOOGLE_PLACES_API_KEY").get(null) as? String
        } catch (_: Exception) { null }
        if (!buildConfigKey.isNullOrBlank() && !buildConfigKey.startsWith("MY_") && !buildConfigKey.startsWith("DEFAULT_")) return buildConfigKey.trim()

        val mapsKey = try {
            com.example.BuildConfig::class.java.getField("GOOGLE_MAPS_API_KEY").get(null) as? String
        } catch (_: Exception) { null }
        if (!mapsKey.isNullOrBlank() && !mapsKey.startsWith("MY_") && !mapsKey.startsWith("DEFAULT_")) return mapsKey.trim()

        val envKey = System.getenv("GOOGLE_PLACES_API_KEY") ?: System.getenv("GOOGLE_MAPS_API_KEY")
        if (!envKey.isNullOrBlank()) return envKey.trim()

        if (context != null) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
                val metaKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY")
                if (!metaKey.isNullOrBlank() && !metaKey.startsWith("MY_") && !metaKey.contains("\${")) return metaKey.trim()
            } catch (_: Exception) {}
        }
        return ""
    }

    /**
     * Realiza la búsqueda de lugares mediante Google Places API (Places Text Search).
     * Soporta Google Places API (New) con fallback a Google Places API (Legacy).
     * Devuelve resultados de alta precisión con Título del POI en shortPlaceName y Dirección en formattedAddress.
     */
    suspend fun searchWithGooglePlaces(
        query: String,
        apiKey: String,
        userLat: Double?,
        userLon: Double?
    ): List<NominatimPlaceSuggestion> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2 || apiKey.isBlank()) return@withContext emptyList()

        val list = mutableListOf<NominatimPlaceSuggestion>()
        val seenCoords = mutableSetOf<String>()
        val langTag = java.util.Locale.getDefault().language.ifBlank { "es" }

        // 1. Google Places API (New) - POST https://places.googleapis.com/v1/places:searchText
        try {
            val jsonBody = JSONObject().apply {
                put("textQuery", cleanQuery)
                put("languageCode", langTag)
                if (userLat != null && userLon != null) {
                    val locationBias = JSONObject().apply {
                        val circle = JSONObject().apply {
                            val center = JSONObject().apply {
                                put("latitude", userLat)
                                put("longitude", userLon)
                            }
                            put("center", center)
                            put("radius", 50000.0) // 50 km de radio preferente
                        }
                        put("circle", circle)
                    }
                    put("locationBias", locationBias)
                }
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("https://places.googleapis.com/v1/places:searchText")
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.location")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val json = JSONObject(bodyStr)
                        val placesArray = json.optJSONArray("places")
                        for (i in 0 until (placesArray?.length() ?: 0)) {
                            val place = placesArray?.optJSONObject(i) ?: continue
                            val displayNameObj = place.optJSONObject("displayName")
                            val placeTitle = displayNameObj?.optString("text")?.trim().orEmpty()
                            val formattedAddress = place.optString("formattedAddress", "").trim()
                            val locationObj = place.optJSONObject("location")
                            val lat = locationObj?.optDouble("latitude") ?: 0.0
                            val lon = locationObj?.optDouble("longitude") ?: 0.0

                            if (placeTitle.isBlank()) continue
                            val key = String.format(java.util.Locale.US, "%.3f,%.3f", lat, lon)
                            if (!seenCoords.add(key)) continue

                            val euclid = if (userLat != null && userLon != null) {
                                sqrt((lat - userLat).pow(2.0) + (lon - userLon).pow(2.0))
                            } else null

                            val distKm = if (userLat != null && userLon != null) {
                                haversineKm(userLat, userLon, lat, lon)
                            } else null

                            android.util.Log.d(
                                "GooglePlacesService",
                                "Google Places (New) resultado para '$cleanQuery': Nombre='$placeTitle', Dirección='$formattedAddress', Coords=($lat, $lon)"
                            )

                            list.add(
                                NominatimPlaceSuggestion(
                                    displayName = if (formattedAddress.isNotBlank()) "$placeTitle, $formattedAddress" else placeTitle,
                                    shortPlaceName = placeTitle,
                                    country = "",
                                    cityOrTown = "",
                                    latitude = lat,
                                    longitude = lon,
                                    euclideanDistance = euclid,
                                    distanceKm = distKm,
                                    formattedAddress = formattedAddress
                                )
                            )
                        }
                        if (list.isNotEmpty()) return@withContext list
                    }
                } else {
                    android.util.Log.w("GooglePlacesService", "Google Places (New) código HTTP ${response.code}: ${response.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GooglePlacesService", "Error llamando a Google Places (New): ${e.message}")
        }

        // 2. Fallback a Google Places API Text Search (Legacy)
        try {
            val urlBuilder = "https://maps.googleapis.com/maps/api/place/textsearch/json".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("query", cleanQuery)
                ?.addQueryParameter("language", langTag)
                ?.addQueryParameter("key", apiKey)

            if (userLat != null && userLon != null) {
                urlBuilder?.addQueryParameter("location", "$userLat,$userLon")
                urlBuilder?.addQueryParameter("radius", "50000")
            }

            val url = urlBuilder?.build() ?: return@withContext list
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val json = JSONObject(bodyStr)
                        val resultsArray = json.optJSONArray("results")
                        for (i in 0 until (resultsArray?.length() ?: 0)) {
                            val place = resultsArray?.optJSONObject(i) ?: continue
                            val placeTitle = place.optString("name", "").trim()
                            val formattedAddress = place.optString("formatted_address", "").trim()
                            val geom = place.optJSONObject("geometry")?.optJSONObject("location")
                            val lat = geom?.optDouble("lat") ?: 0.0
                            val lon = geom?.optDouble("lng") ?: 0.0

                            if (placeTitle.isBlank()) continue
                            val key = String.format(java.util.Locale.US, "%.3f,%.3f", lat, lon)
                            if (!seenCoords.add(key)) continue

                            val euclid = if (userLat != null && userLon != null) {
                                sqrt((lat - userLat).pow(2.0) + (lon - userLon).pow(2.0))
                            } else null

                            val distKm = if (userLat != null && userLon != null) {
                                haversineKm(userLat, userLon, lat, lon)
                            } else null

                            android.util.Log.d(
                                "GooglePlacesService",
                                "Google Places (Legacy) resultado para '$cleanQuery': Nombre='$placeTitle', Dirección='$formattedAddress', Coords=($lat, $lon)"
                            )

                            list.add(
                                NominatimPlaceSuggestion(
                                    displayName = if (formattedAddress.isNotBlank()) "$placeTitle, $formattedAddress" else placeTitle,
                                    shortPlaceName = placeTitle,
                                    country = "",
                                    cityOrTown = "",
                                    latitude = lat,
                                    longitude = lon,
                                    euclideanDistance = euclid,
                                    distanceKm = distKm,
                                    formattedAddress = formattedAddress
                                )
                            )
                        }
                    }
                } else {
                    android.util.Log.w("GooglePlacesService", "Google Places (Legacy) código HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GooglePlacesService", "Error llamando a Google Places (Legacy): ${e.message}")
        }

        list
    }

    /**
     * Parsea la respuesta GeoJSON del motor de búsqueda Open Source Photon (Komoot / OpenStreetMap).
     * Extrae de forma limpia el nombre del POI (Lugar) en shortPlaceName y la dirección formateada completa en formattedAddress.
     */
    fun parsePhotonGeoJson(
        jsonStr: String,
        query: String = "",
        userLat: Double? = null,
        userLon: Double? = null
    ): List<NominatimPlaceSuggestion> {
        val list = mutableListOf<NominatimPlaceSuggestion>()
        val seenCoords = mutableSetOf<String>()

        try {
            val json = JSONObject(jsonStr)
            val features = json.optJSONArray("features") ?: return emptyList()

            for (i in 0 until features.length()) {
                val feature = features.optJSONObject(i) ?: continue
                val geometry = feature.optJSONObject("geometry")
                val coordinates = geometry?.optJSONArray("coordinates")
                val lon = coordinates?.optDouble(0) ?: 0.0
                val lat = coordinates?.optDouble(1) ?: 0.0

                if (lat == 0.0 && lon == 0.0) continue

                val key = String.format(java.util.Locale.US, "%.4f,%.4f", lat, lon)
                if (!seenCoords.add(key)) continue

                val properties = feature.optJSONObject("properties") ?: continue
                val rawName = properties.optString("name", "").trim()
                val street = properties.optString("street", "").trim()
                val housenumber = properties.optString("housenumber", "").trim()
                val postcode = properties.optString("postcode", "").trim()
                val city = (properties.optString("city").takeIf { it.isNotBlank() }
                    ?: properties.optString("town").takeIf { it.isNotBlank() }
                    ?: properties.optString("village").takeIf { it.isNotBlank() }
                    ?: properties.optString("municipality").takeIf { it.isNotBlank() }
                    ?: properties.optString("locality").takeIf { it.isNotBlank() }
                    ?: properties.optString("district").takeIf { it.isNotBlank() }
                    ?: "").trim()
                val locality = (properties.optString("locality").takeIf { it.isNotBlank() }
                    ?: properties.optString("district").takeIf { it.isNotBlank() }
                    ?: properties.optString("suburb").takeIf { it.isNotBlank() }
                    ?: "").trim()
                val county = properties.optString("county", "").trim()
                val state = properties.optString("state", "").trim()
                val country = properties.optString("country", "").trim()

                val streetWithNumber = when {
                    street.isNotBlank() && housenumber.isNotBlank() -> "$street, $housenumber"
                    street.isNotBlank() -> street
                    else -> ""
                }

                // Título del POI (Línea 1): Nombre comercial, camping, albergue, asociación o vía
                val placeTitle = when {
                    rawName.isNotBlank() && !isAddressFragment(rawName) -> rawName
                    rawName.isNotBlank() -> rawName
                    streetWithNumber.isNotBlank() -> streetWithNumber
                    city.isNotBlank() -> city
                    else -> query.trim().ifBlank { "Lugar" }
                }

                // Dirección formateada (Línea 2): Calle, número, código postal, localidad, provincia, país
                val addressParts = mutableListOf<String>()
                if (streetWithNumber.isNotBlank() && !streetWithNumber.equals(placeTitle, ignoreCase = true)) {
                    addressParts.add(streetWithNumber)
                }

                if (locality.isNotBlank() && !locality.equals(city, ignoreCase = true) && !locality.equals(placeTitle, ignoreCase = true) && !addressParts.contains(locality)) {
                    addressParts.add(locality)
                }

                val postalCity = when {
                    postcode.isNotBlank() && city.isNotBlank() -> "$postcode $city"
                    city.isNotBlank() -> city
                    postcode.isNotBlank() -> postcode
                    else -> ""
                }
                if (postalCity.isNotBlank() && !postalCity.equals(placeTitle, ignoreCase = true)) {
                    addressParts.add(postalCity)
                } else if (city.isNotBlank() && !city.equals(placeTitle, ignoreCase = true) && !addressParts.contains(city)) {
                    addressParts.add(city)
                }

                if (county.isNotBlank() && !county.equals(city, ignoreCase = true) && !county.equals(placeTitle, ignoreCase = true) && !addressParts.contains(county)) {
                    addressParts.add(county)
                }
                if (state.isNotBlank() && !state.equals(city, ignoreCase = true) && !state.equals(county, ignoreCase = true) && !state.equals(placeTitle, ignoreCase = true) && !addressParts.contains(state)) {
                    addressParts.add(state)
                }
                if (country.isNotBlank() && !country.equals(placeTitle, ignoreCase = true)) {
                    addressParts.add(country)
                }

                val formattedAddress = addressParts.joinToString(", ").trim()
                val displayName = if (formattedAddress.isNotBlank()) "$placeTitle, $formattedAddress" else placeTitle

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
                        formattedAddress = formattedAddress
                    )
                )
            }
        } catch (_: Exception) {}

        return if (userLat != null && userLon != null) {
            list.sortedBy { it.euclideanDistance ?: Double.MAX_VALUE }
        } else {
            list
        }
    }

    /**
     * Limpia preposiciones y conectores comunes en español e inglés ("en", "cerca de", "del", "el", "la", etc.)
     * para facilitar la coincidencia de POIs en motores de búsqueda de texto libre.
     */
    fun cleanSearchQuery(query: String): String {
        val stopWordsRegex = Regex(
            "\\b(cerca de|cerca|en|de|del|el|la|los|las|un|una|unos|unas|at|in|near|the|of)\\b",
            RegexOption.IGNORE_CASE
        )
        return query.replace(stopWordsRegex, " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun queryPhotonEndpoint(
        query: String,
        userLat: Double?,
        userLon: Double?
    ): List<NominatimPlaceSuggestion> {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return emptyList()

        try {
            val urlBuilder = "https://photon.komoot.io/api/".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("q", cleanQuery)
                ?.addQueryParameter("limit", "15")

            if (userLat != null && userLon != null) {
                urlBuilder?.addQueryParameter("lat", userLat.toString())
                urlBuilder?.addQueryParameter("lon", userLon.toString())
            }

            val url = urlBuilder?.build() ?: return emptyList()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TravelCashAndroidApp/1.0 (GestorGastosViajes; OpenSource)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        return parsePhotonGeoJson(bodyStr, cleanQuery, userLat, userLon)
                    }
                } else {
                    android.util.Log.w("PhotonService", "Photon HTTP ${response.code}: ${response.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("PhotonService", "Error llamando a Photon API: ${e.message}")
        }
        return emptyList()
    }

    /**
     * Realiza la búsqueda de lugares y POIs mediante el motor Open Source Photon (Komoot).
     * No requiere clave de API y utiliza Elasticsearch sobre los datos de OpenStreetMap
     * permitiendo búsquedas de texto libre precisas (ej. campings, asociaciones, albergues).
     * Si la consulta contiene conectores y devuelve pocos resultados, consulta automáticamente sin conectores.
     */
    suspend fun searchWithPhoton(
        query: String,
        userLat: Double?,
        userLon: Double?
    ): List<NominatimPlaceSuggestion> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        val results = mutableListOf<NominatimPlaceSuggestion>()
        val seenCoords = mutableSetOf<String>()

        // 1. Intento con la consulta directa
        val directResults = queryPhotonEndpoint(cleanQuery, userLat, userLon)
        for (item in directResults) {
            val key = String.format(java.util.Locale.US, "%.4f,%.4f", item.latitude, item.longitude)
            if (seenCoords.add(key)) {
                results.add(item)
            }
        }

        // 2. Si la consulta tiene conectores/preposiciones ("en", "cerca de") y devuelve pocos resultados (< 3),
        // consultar también con la consulta limpia sin preposiciones
        val queryWithoutStopWords = cleanSearchQuery(cleanQuery)
        if (results.size < 3 && queryWithoutStopWords.length >= 2 && !queryWithoutStopWords.equals(cleanQuery, ignoreCase = true)) {
            val extraResults = queryPhotonEndpoint(queryWithoutStopWords, userLat, userLon)
            for (item in extraResults) {
                val key = String.format(java.util.Locale.US, "%.4f,%.4f", item.latitude, item.longitude)
                if (seenCoords.add(key)) {
                    results.add(item)
                }
            }
        }

        if (userLat != null && userLon != null) {
            results.sortedBy { it.euclideanDistance ?: Double.MAX_VALUE }
        } else {
            results
        }
    }

    /**
     * Consulta el Geocoder nativo del dispositivo (Google Play Services / Google Maps)
     * complementado con la API Open Source Photon (Komoot) y OpenStreetMap Nominatim con prioridad por cercanía GPS,
     * devolviendo comercios, direcciones, restaurantes y puntos de interés equivalentes a Google Maps.
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

        // 1. Google Places API (si hay clave de API configurada en .env / BuildConfig / entorno / manifest)
        val apiKey = resolveGooglePlacesApiKey(context)
        if (apiKey.isNotBlank()) {
            val googleResults = searchWithGooglePlaces(cleanQuery, apiKey, userLat, userLon)
            if (googleResults.isNotEmpty()) {
                return@withContext googleResults
            }
        }

        val list = mutableListOf<NominatimPlaceSuggestion>()
        val seenCoords = mutableSetOf<String>()

        // 2. Motor Open Source Photon (Komoot / OpenStreetMap POI Search)
        val photonResults = searchWithPhoton(cleanQuery, userLat, userLon)
        for (item in photonResults) {
            val key = String.format(java.util.Locale.US, "%.3f,%.3f", item.latitude, item.longitude)
            if (seenCoords.add(key)) {
                list.add(item)
            }
        }

        // Si Photon devolvió suficientes resultados de calidad (>= 5), retornar directamente
        if (list.size >= 5) {
            return@withContext if (userLat != null && userLon != null) {
                list.sortedBy { it.euclideanDistance ?: Double.MAX_VALUE }
            } else {
                list
            }
        }

        // 3. Intentar con Android Geocoder nativo (Google Maps / Google Location Services)
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

                    // Extraer Título (POI / Nombre del sitio prioritario):
                    val placeTitle = when {
                        // Si featureName es un nombre propio real (no un "Bajo", no un número "12", ni igual a la calle ni localidad)
                        feature.isNotBlank() && !isAddressFragment(feature) && !feature.equals(locality, ignoreCase = true) && !feature.equals(thoroughfare, ignoreCase = true) ->
                            feature
                        // Si el primer segmento de la línea devuelta por Geocoder contiene el nombre del POI (ej. "Asoc. Peregrinus Dezae")
                        firstSegment.isNotBlank() && !isAddressFragment(firstSegment) && !firstSegment.equals(locality, ignoreCase = true) && !firstSegment.equals(thoroughfare, ignoreCase = true) ->
                            firstSegment
                        // Si coincide con la consulta de búsqueda comercial o asociación
                        cleanQuery.isNotBlank() && !isAddressFragment(cleanQuery) && fullLine0.contains(cleanQuery, ignoreCase = true) -> {
                            if (firstSegment.contains(cleanQuery, ignoreCase = true)) firstSegment else cleanQuery
                        }
                        // Si hay nombre de vía
                        thoroughfare.isNotBlank() ->
                            if (subThoroughfare.isNotBlank()) "$thoroughfare, $subThoroughfare" else thoroughfare
                        feature.isNotBlank() && !isAddressFragment(feature) ->
                            feature
                        firstSegment.isNotBlank() ->
                            firstSegment
                        locality.isNotBlank() ->
                            locality
                        else -> cleanQuery.ifBlank { "Lugar" }
                    }

                    // Extraer Dirección completa formateada (Línea 2):
                    val addressOnly = when {
                        fullAddress.startsWith(placeTitle, ignoreCase = true) ->
                            fullAddress.substring(placeTitle.length).trim().removePrefix(",").trim()
                        fullLine0.startsWith(placeTitle, ignoreCase = true) ->
                            fullLine0.substring(placeTitle.length).trim().removePrefix(",").trim()
                        fullAddress.equals(placeTitle, ignoreCase = true) ->
                            ""
                        else -> fullAddress.trim()
                    }

                    android.util.Log.d("NominatimService", "Geocoder query='$cleanQuery' -> Feature='$feature', Locality='$locality', Line0='$fullLine0' => Title='$placeTitle', Address='$addressOnly'")

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

                            val firstSegment = displayName.substringBefore(",").trim()
                            val placeTitle = when {
                                nameProp.isNotBlank() && !isAddressFragment(nameProp) -> nameProp
                                firstSegment.isNotBlank() && !isAddressFragment(firstSegment) -> firstSegment
                                nameProp.isNotBlank() -> nameProp
                                firstSegment.isNotBlank() -> firstSegment
                                else -> cleanQuery.ifBlank { "Lugar" }
                            }

                            val addressOnly = when {
                                displayName.startsWith(placeTitle, ignoreCase = true) ->
                                    displayName.substring(placeTitle.length).trim().removePrefix(",").trim()
                                displayName.equals(placeTitle, ignoreCase = true) ->
                                    ""
                                displayName.contains(",") ->
                                    displayName.substringAfter(",").trim()
                                else -> ""
                            }

                            android.util.Log.d("NominatimService", "OSM query='$cleanQuery' -> nameProp='$nameProp', firstSegment='$firstSegment' => Title='$placeTitle', Address='$addressOnly'")

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
