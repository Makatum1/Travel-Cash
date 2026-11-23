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
     * Resuelve de forma segura la API Key de Google Places.
     * Prioridad:
     * 1. Clave introducida por el usuario en Ajustes (SharedPreferences).
     * 2. BuildConfig (.env) GOOGLE_PLACES_API_KEY o GOOGLE_MAPS_API_KEY.
     * 3. Variables de entorno.
     * 4. Metadatos del AndroidManifest.
     */
    fun resolveGooglePlacesApiKey(context: Context?): String {
        // 1. SharedPreferences (Ajustes de la app)
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences("travel_cash_settings", Context.MODE_PRIVATE)
                val userKey = prefs.getString("google_places_api_key", null)?.trim()
                if (!userKey.isNullOrBlank() && !userKey.startsWith("DEFAULT_") && !userKey.startsWith("MY_")) {
                    return userKey
                }
            } catch (_: Exception) {}
        }

        // 2. BuildConfig
        val buildConfigKey = try {
            com.example.BuildConfig::class.java.getField("GOOGLE_PLACES_API_KEY").get(null) as? String
        } catch (_: Exception) { null }
        if (!buildConfigKey.isNullOrBlank() && !buildConfigKey.startsWith("MY_") && !buildConfigKey.startsWith("DEFAULT_")) return buildConfigKey.trim()

        val mapsKey = try {
            com.example.BuildConfig::class.java.getField("GOOGLE_MAPS_API_KEY").get(null) as? String
        } catch (_: Exception) { null }
        if (!mapsKey.isNullOrBlank() && !mapsKey.startsWith("MY_") && !mapsKey.startsWith("DEFAULT_")) return mapsKey.trim()

        // 3. Entorno
        val envKey = System.getenv("GOOGLE_PLACES_API_KEY") ?: System.getenv("GOOGLE_MAPS_API_KEY")
        if (!envKey.isNullOrBlank()) return envKey.trim()

        // 4. Manifest
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
     * Valida si una API Key de Google Places es válida y tiene los permisos activos en Google Cloud.
     * Devuelve Pair(éxito, mensajeExplicativo).
     */
    suspend fun validateGooglePlacesApiKey(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || cleanKey.startsWith("DEFAULT_") || cleanKey.startsWith("MY_")) {
            return@withContext false to "Clave vacía o con formato no válido."
        }

        // 1. Probar con Google Places API (New)
        try {
            val jsonBody = JSONObject().apply {
                put("textQuery", "Google")
                put("languageCode", "es")
            }
            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://places.googleapis.com/v1/places:searchText")
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", cleanKey)
                .header("X-Goog-FieldMask", "places.id,places.displayName")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext true to "Conexión exitosa con Google Places API (New)."
                }
            }
        } catch (_: Exception) {}

        // 2. Probar con Google Places API (Legacy Text Search)
        try {
            val url = "https://maps.googleapis.com/maps/api/place/textsearch/json".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("query", "Google")
                ?.addQueryParameter("key", cleanKey)
                ?.build() ?: return@withContext false to "URL de Google inválida."

            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    val json = JSONObject(bodyStr)
                    val status = json.optString("status")
                    if (status == "OK" || status == "ZERO_RESULTS") {
                        return@withContext true to "Conexión exitosa con Google Places API."
                    } else {
                        val errMsg = json.optString("error_message", status)
                        return@withContext false to "Google rechazó la clave ($status: $errMsg)"
                    }
                } else {
                    return@withContext false to "Error HTTP ${response.code} de Google Places"
                }
            }
        } catch (e: Exception) {
            return@withContext false to "Error de red al conectar con Google: ${e.message}"
        }
    }

    /**
     * Limpia palabras de parada (stop-words) comunes en consultas en lenguaje natural (ej. "en", "de", "cerca de")
     * para facilitar la coincidencia en motores como OpenStreetMap Nominatim o Photon.
     */
    fun sanitizeQuery(query: String): String {
        return query
            .replace(Regex("\\b(en|de|del|cerca de|cerca|in|near|near to|at|por|el|la|los|las)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
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
     * Realiza la búsqueda de comercios, campings, restaurantes, alojamientos y puntos de interés (POIs)
     * mediante la API de Photon (Komoot / OpenStreetMap Elasticsearch).
     * Es gratuita, no requiere clave de API y ofrece tolerancia a errores y búsqueda en lenguaje natural.
     */
    suspend fun searchWithPhoton(
        query: String,
        userLat: Double?,
        userLon: Double?
    ): List<NominatimPlaceSuggestion> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        val list = mutableListOf<NominatimPlaceSuggestion>()
        val seenCoords = mutableSetOf<String>()
        val langTag = java.util.Locale.getDefault().language.ifBlank { "es" }

        try {
            val urlBuilder = "https://photon.komoot.io/api/".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("q", cleanQuery)
                ?.addQueryParameter("limit", "15")
                ?.addQueryParameter("lang", langTag)

            if (userLat != null && userLon != null) {
                urlBuilder?.addQueryParameter("lat", userLat.toString())
                urlBuilder?.addQueryParameter("lon", userLon.toString())
            }

            val url = urlBuilder?.build() ?: return@withContext emptyList()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TravelCashAndroidApp/1.0 ($langTag; GestorGastosViajes)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val json = JSONObject(bodyStr)
                        val features = json.optJSONArray("features")
                        for (i in 0 until (features?.length() ?: 0)) {
                            val feature = features?.optJSONObject(i) ?: continue
                            val geom = feature.optJSONObject("geometry")
                            val coords = geom?.optJSONArray("coordinates")
                            val lon = coords?.optDouble(0) ?: continue
                            val lat = coords.optDouble(1)

                            val key = String.format(java.util.Locale.US, "%.3f,%.3f", lat, lon)
                            if (!seenCoords.add(key)) continue

                            val props = feature.optJSONObject("properties") ?: continue
                            val name = props.optString("name", "").trim()
                            val street = props.optString("street", "").trim()
                            val housenumber = props.optString("housenumber", "").trim()
                            val city = (props.optString("city").takeIf { it.isNotBlank() }
                                ?: props.optString("town").takeIf { it.isNotBlank() }
                                ?: props.optString("village").takeIf { it.isNotBlank() }
                                ?: props.optString("locality").takeIf { it.isNotBlank() }
                                ?: props.optString("district").takeIf { it.isNotBlank() }
                                ?: "").trim()
                            val state = props.optString("state", "").trim()
                            val country = props.optString("country", "").trim()

                            val streetPart = when {
                                street.isNotBlank() && housenumber.isNotBlank() -> "$street, $housenumber"
                                street.isNotBlank() -> street
                                else -> ""
                            }

                            val placeTitle = when {
                                name.isNotBlank() && !isAddressFragment(name) -> name
                                streetPart.isNotBlank() -> streetPart
                                city.isNotBlank() -> city
                                else -> cleanQuery
                            }

                            val addressParts = listOf(streetPart, city, state, country)
                                .filter { it.isNotBlank() && !it.equals(placeTitle, ignoreCase = true) }
                            val formattedAddress = addressParts.joinToString(", ")

                            val euclid = if (userLat != null && userLon != null) {
                                sqrt((lat - userLat).pow(2.0) + (lon - userLon).pow(2.0))
                            } else null

                            val distKm = if (userLat != null && userLon != null) {
                                haversineKm(userLat, userLon, lat, lon)
                            } else null

                            android.util.Log.d(
                                "PhotonService",
                                "Photon resultado para '$cleanQuery': Nombre='$placeTitle', Dirección='$formattedAddress', Coords=($lat, $lon)"
                            )

                            list.add(
                                NominatimPlaceSuggestion(
                                    displayName = if (formattedAddress.isNotBlank()) "$placeTitle, $formattedAddress" else placeTitle,
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
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("PhotonService", "Error llamando a Photon: ${e.message}")
        }

        list
    }

    /**
     * Consulta el Geocoder nativo del dispositivo (Google Play Services / Google Maps)
     * complementado con la API de OpenStreetMap Nominatim con prioridad por cercanía GPS,
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

        // 1. Google Places API (si hay clave de API configurada en Ajustes / .env / BuildConfig / entorno / manifest)
        val apiKey = resolveGooglePlacesApiKey(context)
        if (apiKey.isNotBlank()) {
            val googleResults = searchWithGooglePlaces(cleanQuery, apiKey, userLat, userLon)
            if (googleResults.isNotEmpty()) {
                return@withContext googleResults
            }
        }

        val list = mutableListOf<NominatimPlaceSuggestion>()
        val seenCoords = mutableSetOf<String>()

        // 2. Intentar primero con Photon (Komoot / OpenStreetMap POI search engine)
        try {
            val photonResults = searchWithPhoton(cleanQuery, userLat, userLon)
            for (res in photonResults) {
                val key = String.format(java.util.Locale.US, "%.3f,%.3f", res.latitude, res.longitude)
                if (seenCoords.add(key)) {
                    list.add(res)
                }
            }
        } catch (_: Exception) {}

        // 3. Geocoder nativo de Android (Google Play Services / Red local)
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

        // 5. Si hay pocos resultados y la consulta original contenía stop-words (ej. "en Prado"),
        // reintentar con la consulta saneada (ej. "Camping Huttopia Prado") mediante Photon.
        if (list.size < 3) {
            val sanitized = sanitizeQuery(cleanQuery)
            if (sanitized.length >= 2 && !sanitized.equals(cleanQuery, ignoreCase = true)) {
                try {
                    val sanitizedPhoton = searchWithPhoton(sanitized, userLat, userLon)
                    for (res in sanitizedPhoton) {
                        val key = String.format(java.util.Locale.US, "%.3f,%.3f", res.latitude, res.longitude)
                        if (seenCoords.add(key)) {
                            list.add(res)
                        }
                    }
                } catch (_: Exception) {}
            }
        }

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
