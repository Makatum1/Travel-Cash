package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.core.content.ContextCompat
import com.example.ui.theme.isEnglish
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.PaymentMethodCatalog
import com.example.data.model.TripEntity
import com.example.domain.DebtCalculator
import com.example.ui.components.TravelLocationPinIcon
import com.example.ui.components.createTravelMapPinDrawable
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OceanNavy
import com.example.ui.theme.TravelBlue
import com.example.util.FormatUtils
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

enum class OsmMapLayerType {
    GOOGLE_ROADMAP,
    GOOGLE_SATELLITE,
    GOOGLE_TERRAIN,
    CARTO_VOYAGER,
    OPEN_STREET_MAP
}

@Composable
fun MapTabScreen(
    trip: TripEntity,
    participants: List<ParticipantEntity>,
    expenses: List<ExpenseEntity>,
    userLocation: Pair<Double, Double>?,
    onRequestLocationRefresh: () -> Unit,
    onEditExpenseClick: (ExpenseEntity) -> Unit
) {
    val context = LocalContext.current
    val isEn = LocalAppLanguage.current.isEnglish()
    val participantMap = remember(participants) { participants.associateBy { it.id } }

    var shouldCenterOnUserLocation by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            shouldCenterOnUserLocation = true
            onRequestLocationRefresh()
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(userLocation, shouldCenterOnUserLocation) {
        if (shouldCenterOnUserLocation && userLocation != null) {
            shouldCenterOnUserLocation = false
            mapViewRef?.controller?.setZoom(16.0)
            mapViewRef?.controller?.animateTo(GeoPoint(userLocation.first, userLocation.second))
        }
    }

    val geoExpenses = remember(expenses) {
        expenses.filter { it.latitude != null && it.longitude != null }
    }

    // Agrupación de gastos geolocalizados por Categoría canónica
    val categoriesWithExpenses = remember(geoExpenses) {
        val grouped = LinkedHashMap<String, MutableList<ExpenseEntity>>()
        geoExpenses.forEach { exp ->
            val canonicalCatName = CategoryCatalog.getCategory(exp.category).name
            grouped.getOrPut(canonicalCatName) { mutableListOf() }.add(exp)
        }
        grouped.entries.map { it.key to it.value.toList() }
    }

    // Gasto seleccionado en el mapa
    var selectedExpense by remember(geoExpenses) {
        mutableStateOf<ExpenseEntity?>(null)
    }
    var expandedCategoryKey by remember { mutableStateOf<String?>(null) }
    var currentLayerType by remember { mutableStateOf(OsmMapLayerType.GOOGLE_ROADMAP) }
    var showLayerMenu by remember { mutableStateOf(false) }

    // Idioma del sistema para etiquetas y topónimos
    val systemLanguageTag = remember {
        val loc = java.util.Locale.getDefault()
        if (loc.toLanguageTag().isNotBlank()) loc.toLanguageTag() else "es-ES"
    }

    // Fuentes de mapas OpenSource / Web Tiles optimizadas con el idioma del sistema
    val googleRoadmapTileSource = remember(systemLanguageTag) {
        object : OnlineTileSourceBase(
            "GoogleRoadmap",
            0, 20, 256, ".png",
            arrayOf(
                "https://mt0.google.com/vt/lyrs=m&hl=$systemLanguageTag&",
                "https://mt1.google.com/vt/lyrs=m&hl=$systemLanguageTag&",
                "https://mt2.google.com/vt/lyrs=m&hl=$systemLanguageTag&",
                "https://mt3.google.com/vt/lyrs=m&hl=$systemLanguageTag&"
            )
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "${baseUrl}x=$x&y=$y&z=$zoom"
            }
        }
    }

    val googleSatelliteTileSource = remember(systemLanguageTag) {
        object : OnlineTileSourceBase(
            "GoogleSatelliteHybrid",
            0, 20, 256, ".png",
            arrayOf(
                "https://mt0.google.com/vt/lyrs=y&hl=$systemLanguageTag&",
                "https://mt1.google.com/vt/lyrs=y&hl=$systemLanguageTag&",
                "https://mt2.google.com/vt/lyrs=y&hl=$systemLanguageTag&",
                "https://mt3.google.com/vt/lyrs=y&hl=$systemLanguageTag&"
            )
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "${baseUrl}x=$x&y=$y&z=$zoom"
            }
        }
    }

    val googleTerrainTileSource = remember(systemLanguageTag) {
        object : OnlineTileSourceBase(
            "GoogleTerrain",
            0, 20, 256, ".png",
            arrayOf(
                "https://mt0.google.com/vt/lyrs=p&hl=$systemLanguageTag&",
                "https://mt1.google.com/vt/lyrs=p&hl=$systemLanguageTag&",
                "https://mt2.google.com/vt/lyrs=p&hl=$systemLanguageTag&",
                "https://mt3.google.com/vt/lyrs=p&hl=$systemLanguageTag&"
            )
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "${baseUrl}x=$x&y=$y&z=$zoom"
            }
        }
    }

    val cartoVoyagerTileSource = remember {
        object : OnlineTileSourceBase(
            "CartoVoyager",
            0, 19, 256, ".png",
            arrayOf(
                "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
            )
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "$baseUrl$zoom/$x/$y.png"
            }
        }
    }

    val activeTileSource: ITileSource = when (currentLayerType) {
        OsmMapLayerType.GOOGLE_ROADMAP -> googleRoadmapTileSource
        OsmMapLayerType.GOOGLE_SATELLITE -> googleSatelliteTileSource
        OsmMapLayerType.GOOGLE_TERRAIN -> googleTerrainTileSource
        OsmMapLayerType.CARTO_VOYAGER -> cartoVoyagerTileSource
        OsmMapLayerType.OPEN_STREET_MAP -> TileSourceFactory.MAPNIK
    }

    // Configuración de osmdroid con User-Agent de alta compatibilidad
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, prefs)
        Configuration.getInstance().userAgentValue =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 TravelCashApp/1.0"
    }

    fun fitAllPins(mapView: MapView?) {
        val map = mapView ?: return
        if (geoExpenses.isEmpty()) {
            val uLoc = userLocation
            if (uLoc != null) {
                map.controller.setZoom(14.0)
                map.controller.animateTo(GeoPoint(uLoc.first, uLoc.second))
            } else {
                map.controller.setZoom(5.0)
                map.controller.animateTo(GeoPoint(40.4168, -3.7038))
            }
            return
        }
        if (geoExpenses.size == 1 && geoExpenses.first().originLatitude == null) {
            val only = geoExpenses.first()
            map.controller.setZoom(15.5)
            map.controller.animateTo(GeoPoint(only.latitude!!, only.longitude!!))
        } else {
            val lats = geoExpenses.flatMap { listOfNotNull(it.latitude, it.originLatitude) }
            val lons = geoExpenses.flatMap { listOfNotNull(it.longitude, it.originLongitude) }
            val north = (lats.maxOrNull() ?: 0.0) + 0.025
            val south = (lats.minOrNull() ?: 0.0) - 0.025
            val east = (lons.maxOrNull() ?: 0.0) + 0.025
            val west = (lons.minOrNull() ?: 0.0) - 0.025
            try {
                val box = BoundingBox(north, east, south, west)
                map.zoomToBoundingBox(box, true, 96)
            } catch (_: Exception) {
                map.controller.setZoom(12.0)
                map.controller.animateTo(GeoPoint(lats.first(), lons.first()))
            }
        }
    }

    fun showAllGeolocatedExpenses() {
        selectedExpense = null
        expandedCategoryKey = null
        fitAllPins(mapViewRef)
    }

    // Segmentos de ruta para Vuelos, Transporte y Taxi
    val transitRouteSegments = remember(geoExpenses) {
        val sortedGeo = geoExpenses.sortedWith(
            compareBy<ExpenseEntity> { it.date }.thenBy { it.createdAt }
        )
        val segments = mutableListOf<Triple<GeoPoint, GeoPoint, Boolean>>()
        val seenPairs = mutableSetOf<String>()

        fun addCoordsSegment(fLat: Double, fLon: Double, tLat: Double, tLon: Double, isFlight: Boolean) {
            if (kotlin.math.abs(fLat - tLat) < 0.00001 && kotlin.math.abs(fLon - tLon) < 0.00001) return
            val p1 = String.format(java.util.Locale.US, "%.4f,%.4f", fLat, fLon)
            val p2 = String.format(java.util.Locale.US, "%.4f,%.4f", tLat, tLon)
            val key = listOf(p1, p2).sorted().joinToString("::")
            if (seenPairs.add(key)) {
                segments.add(Triple(GeoPoint(fLat, fLon), GeoPoint(tLat, tLon), isFlight))
            }
        }

        // 1. Origen y Destino propios del gasto
        sortedGeo.forEach { exp ->
            val oLat = exp.originLatitude
            val oLon = exp.originLongitude
            val dLat = exp.latitude
            val dLon = exp.longitude
            if (oLat != null && oLon != null && dLat != null && dLon != null) {
                val catName = CategoryCatalog.getCategory(exp.category).name
                val isFlight = catName.equals("Vuelos", ignoreCase = true)
                addCoordsSegment(oLat, oLon, dLat, dLon, isFlight)
            }
        }

        // 2. Conexión de ruta consecutiva
        for (i in 0 until sortedGeo.size - 1) {
            val a = sortedGeo[i]
            val b = sortedGeo[i + 1]
            val catA = CategoryCatalog.getCategory(a.category).name
            val catB = CategoryCatalog.getCategory(b.category).name
            val aIsFlight = catA.equals("Vuelos", ignoreCase = true)
            val bIsFlight = catB.equals("Vuelos", ignoreCase = true)
            val aIsTransport = CategoryCatalog.isRouteCategory(catA) && !aIsFlight
            val bIsTransport = CategoryCatalog.isRouteCategory(catB) && !bIsFlight

            val fLat = a.latitude ?: continue
            val fLon = a.longitude ?: continue
            val tLat = b.originLatitude ?: b.latitude ?: continue
            val tLon = b.originLongitude ?: b.longitude ?: continue

            if (aIsFlight || bIsFlight) {
                addCoordsSegment(fLat, fLon, tLat, tLon, isFlight = true)
            } else if (aIsTransport || bIsTransport) {
                addCoordsSegment(fLat, fLon, tLat, tLon, isFlight = false)
            }
        }

        segments.toList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Mapa interactivo OpenSource con osmdroid optimizado para Android
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("osmdroid_map_view"),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(activeTileSource)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    controller.setZoom(12.0)
                    mapViewRef = this
                }
            },
            update = { map ->
                if (map.tileProvider.tileSource?.name() != activeTileSource.name()) {
                    map.setTileSource(activeTileSource)
                }

                map.overlays.clear()

                // 1. Tap en el fondo del mapa para deseleccionar
                val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        if (selectedExpense != null) {
                            selectedExpense = null
                            expandedCategoryKey = null
                            fitAllPins(map)
                            return true
                        }
                        return false
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                })
                map.overlays.add(mapEventsOverlay)

                // 2. Líneas directas de transporte y vuelos
                transitRouteSegments.forEach { (startPoint, endPoint, isFlight) ->
                    val haloLine = Polyline(map).apply {
                        setPoints(listOf(startPoint, endPoint))
                        outlinePaint.apply {
                            color = if (isFlight) {
                                android.graphics.Color.argb(120, 186, 230, 253)
                            } else {
                                android.graphics.Color.argb(120, 153, 246, 228)
                            }
                            strokeWidth = if (isFlight) 13f else 11f
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            isAntiAlias = true
                        }
                        infoWindow = null
                    }
                    map.overlays.add(haloLine)

                    val routeLine = Polyline(map).apply {
                        setPoints(listOf(startPoint, endPoint))
                        outlinePaint.apply {
                            color = if (isFlight) {
                                android.graphics.Color.rgb(2, 132, 199)
                            } else {
                                android.graphics.Color.rgb(13, 148, 136)
                            }
                            strokeWidth = if (isFlight) 7.5f else 6.5f
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            pathEffect = if (isFlight) {
                                DashPathEffect(floatArrayOf(22f, 14f), 0f)
                            } else {
                                DashPathEffect(floatArrayOf(14f, 9f), 0f)
                            }
                            isAntiAlias = true
                        }
                        infoWindow = null
                    }
                    map.overlays.add(routeLine)
                }

                // 3. Marcadores de los gastos con pines con color por categoría
                geoExpenses.forEach { exp ->
                    val lat = exp.latitude ?: return@forEach
                    val lon = exp.longitude ?: return@forEach
                    val catInfo = CategoryCatalog.getCategory(exp.category)
                    val isPinSelected = selectedExpense?.id == exp.id
                    val payerName = if (exp.paidById == DebtCalculator.BOTE_COMUN_ID) {
                        if (isEn) "Common Pot" else "Bote Común"
                    } else {
                        participantMap[exp.paidById]?.name ?: if (isEn) "Traveler" else "Viajero"
                    }

                    // Marcador de origen
                    val oLat = exp.originLatitude
                    val oLon = exp.originLongitude
                    if (oLat != null && oLon != null &&
                        (kotlin.math.abs(oLat - lat) > 0.00001 || kotlin.math.abs(oLon - lon) > 0.00001)
                    ) {
                        val originMarker = Marker(map).apply {
                            position = GeoPoint(oLat, oLon)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = createTravelMapPinDrawable(
                                context = context,
                                pinColor = catInfo.color,
                                isSelected = isPinSelected
                            )
                            title = "${if (isEn) "Origin" else "Origen"}: ${exp.originName.ifBlank { exp.placeName }}"
                            snippet = "${FormatUtils.formatCurrency(exp.amountInBaseCurrency, trip.baseCurrency)} · ${if (isEn) "Paid by" else "Pagó"}: $payerName"
                            setOnMarkerClickListener { _, _ ->
                                if (selectedExpense?.id == exp.id) {
                                    selectedExpense = null
                                    fitAllPins(map)
                                } else {
                                    selectedExpense = exp
                                    map.controller.setZoom(14.5)
                                    map.controller.animateTo(GeoPoint(oLat, oLon))
                                }
                                true
                            }
                        }
                        map.overlays.add(originMarker)
                    }

                    // Marcador del gasto
                    val marker = Marker(map).apply {
                        position = GeoPoint(lat, lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = createTravelMapPinDrawable(
                            context = context,
                            pinColor = catInfo.color,
                            isSelected = isPinSelected
                        )
                        title = exp.placeName.ifBlank { CategoryCatalog.displayName(exp.category, isEn) }
                        snippet = "${FormatUtils.formatCurrency(exp.amountInBaseCurrency, trip.baseCurrency)} · ${if (isEn) "Paid by" else "Pagó"}: $payerName"
                        setOnMarkerClickListener { _, _ ->
                            if (selectedExpense?.id == exp.id) {
                                selectedExpense = null
                                fitAllPins(map)
                            } else {
                                selectedExpense = exp
                                map.controller.setZoom(16.0)
                                map.controller.animateTo(GeoPoint(lat, lon))
                            }
                            true
                        }
                    }
                    map.overlays.add(marker)
                }

                // 4. Marcador de la posición actual del usuario (GPS)
                userLocation?.let { (uLat, uLon) ->
                    val userMarker = Marker(map).apply {
                        position = GeoPoint(uLat, uLon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        val sizePx = (22 * context.resources.displayMetrics.density).toInt().coerceAtLeast(16)
                        val bmp = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bmp)
                        val paintOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            style = Paint.Style.FILL
                        }
                        val paintInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#0284C7")
                            style = Paint.Style.FILL
                        }
                        val radius = sizePx / 2f
                        canvas.drawCircle(radius, radius, radius, paintOuter)
                        canvas.drawCircle(radius, radius, radius - 2.5f * context.resources.displayMetrics.density, paintInner)
                        icon = android.graphics.drawable.BitmapDrawable(context.resources, bmp)
                        title = if (isEn) "My Location" else "Mi ubicación"
                    }
                    map.overlays.add(userMarker)
                }

                map.invalidate()
            }
        )

        LaunchedEffect(geoExpenses.size, mapViewRef) {
            if (mapViewRef != null) {
                fitAllPins(mapViewRef)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                mapViewRef?.onDetach()
            }
        }

        // Barra superior del mapa: Contador + Selector de capa + Botón Todos + Centrar + Mi Ubicación
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { showAllGeolocatedExpenses() },
                shape = RoundedCornerShape(12.dp),
                color = OceanNavy.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TravelLocationPinIcon(
                        tint = Color(0xFF38BDF8),
                        accentColor = Color(0xFFFBBF24),
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "${geoExpenses.size} places" else "${geoExpenses.size} lugares",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selector de capas (Google Maps, Satélite, Relieve, Carto, OSM)
                Box {
                    SmallFloatingActionButton(
                        onClick = { showLayerMenu = !showLayerMenu },
                        containerColor = Color.White,
                        contentColor = OceanNavy
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = if (isEn) "Map layers" else "Capas de mapa"
                        )
                    }

                    DropdownMenu(
                        expanded = showLayerMenu,
                        onDismissRequest = { showLayerMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isEn) "Google Maps (Roadmap)" else "Google Maps (Estándar)") },
                            onClick = {
                                currentLayerType = OsmMapLayerType.GOOGLE_ROADMAP
                                showLayerMenu = false
                            },
                            leadingIcon = {
                                if (currentLayerType == OsmMapLayerType.GOOGLE_ROADMAP) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = TravelBlue)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isEn) "Satellite (Hybrid)" else "Satélite (Híbrido)") },
                            onClick = {
                                currentLayerType = OsmMapLayerType.GOOGLE_SATELLITE
                                showLayerMenu = false
                            },
                            leadingIcon = {
                                if (currentLayerType == OsmMapLayerType.GOOGLE_SATELLITE) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = TravelBlue)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isEn) "Terrain (Relief)" else "Relieve (Terreno)") },
                            onClick = {
                                currentLayerType = OsmMapLayerType.GOOGLE_TERRAIN
                                showLayerMenu = false
                            },
                            leadingIcon = {
                                if (currentLayerType == OsmMapLayerType.GOOGLE_TERRAIN) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = TravelBlue)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isEn) "Carto Voyager" else "Carto Voyager (Limpio)") },
                            onClick = {
                                currentLayerType = OsmMapLayerType.CARTO_VOYAGER
                                showLayerMenu = false
                            },
                            leadingIcon = {
                                if (currentLayerType == OsmMapLayerType.CARTO_VOYAGER) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = TravelBlue)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isEn) "OpenStreetMap" else "OpenStreetMap Oficial") },
                            onClick = {
                                currentLayerType = OsmMapLayerType.OPEN_STREET_MAP
                                showLayerMenu = false
                            },
                            leadingIcon = {
                                if (currentLayerType == OsmMapLayerType.OPEN_STREET_MAP) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = TravelBlue)
                                }
                            }
                        )
                    }
                }


                // Mi ubicación GPS
                SmallFloatingActionButton(
                    onClick = {
                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            shouldCenterOnUserLocation = true
                            onRequestLocationRefresh()
                            userLocation?.let { (lat, lon) ->
                                mapViewRef?.controller?.setZoom(16.0)
                                mapViewRef?.controller?.animateTo(GeoPoint(lat, lon))
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    containerColor = TravelBlue,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = if (isEn) "Center on my GPS location" else "Centrar en mi ubicación GPS"
                    )
                }
            }
        }

        // Panel inferior: Tarjeta emergente del gasto seleccionado + Fila horizontal por Categoría
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tarjeta emergente al pulsar un pin
            AnimatedVisibility(visible = selectedExpense != null) {
                selectedExpense?.let { exp ->
                    val catInfo = CategoryCatalog.getCategory(exp.category)
                    val translatedCat = CategoryCatalog.displayName(exp.category, isEn)
                    val translatedMethod = PaymentMethodCatalog.displayName(exp.paymentMethod, isEn)
                    val payerName = if (exp.paidById == DebtCalculator.BOTE_COMUN_ID) {
                        if (isEn) "Common Pot" else "Bote Común"
                    } else {
                        participantMap[exp.paidById]?.name ?: if (isEn) "Traveler" else "Viajero"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = BorderStroke(1.5.dp, catInfo.color)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(catInfo.color.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = catInfo.icon,
                                    contentDescription = translatedCat,
                                    tint = catInfo.color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exp.placeName.ifBlank { translatedCat },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = OceanNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (exp.country.isNotBlank()) {
                                    Text(
                                        text = "📍 ${exp.country}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "$translatedCat · ${FormatUtils.formatShortDate(exp.date)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "${if (isEn) "Paid by" else "Pagó"}: $payerName ($translatedMethod)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = TravelBlue
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = FormatUtils.formatCurrency(exp.amountInBaseCurrency, trip.baseCurrency),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    ),
                                    color = OceanNavy
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Abrir en la app de Google Maps
                                    IconButton(
                                        onClick = {
                                            val lat = exp.latitude ?: return@IconButton
                                            val lon = exp.longitude ?: return@IconButton
                                            val label = Uri.encode(exp.placeName.ifBlank { "Gasto" })
                                            val gmmIntentUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($label)")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                                setPackage("com.google.android.apps.maps")
                                            }
                                            try {
                                                context.startActivity(mapIntent)
                                            } catch (_: Exception) {
                                                val webIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
                                                )
                                                context.startActivity(webIntent)
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = if (isEn) "Open in Google Maps" else "Abrir en Google Maps",
                                            tint = TravelBlue,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onEditExpenseClick(exp) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = if (isEn) "Edit expense" else "Editar gasto",
                                            tint = TravelBlue,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showAllGeolocatedExpenses() },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = if (isEn) "Deselect and show all" else "Deseleccionar y ver todos",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Fila horizontal: Botones desplegables por Categoría
            if (geoExpenses.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    items(categoriesWithExpenses, key = { it.first }) { (canonicalCatName, catExpenses) ->
                        val catInfo = CategoryCatalog.getCategory(canonicalCatName)
                        val translatedCatName = CategoryCatalog.displayName(canonicalCatName, isEn)
                        val isDropdownOpen = expandedCategoryKey == canonicalCatName
                        val hasSelectedInCat = selectedExpense != null &&
                                CategoryCatalog.getCategory(selectedExpense!!.category).name == canonicalCatName

                        Box {
                            Surface(
                                onClick = {
                                    expandedCategoryKey = if (isDropdownOpen) null else canonicalCatName
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (hasSelectedInCat) OceanNavy else Color.White.copy(alpha = 0.96f),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if (hasSelectedInCat || isDropdownOpen) catInfo.color else Color(0xFFCBD5E1)
                                ),
                                shadowElevation = 4.dp,
                                modifier = Modifier.testTag("map_category_dropdown_$canonicalCatName")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(catInfo.color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catInfo.icon,
                                            contentDescription = translatedCatName,
                                            tint = catInfo.color,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$translatedCatName (${catExpenses.size})",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (hasSelectedInCat) Color.White else OceanNavy
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isDropdownOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (hasSelectedInCat) Color.White else Color(0xFF475569),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            MaterialTheme(
                                colorScheme = MaterialTheme.colorScheme.copy(
                                    surface = Color.White,
                                    onSurface = Color(0xFF334155)
                                )
                            ) {
                                DropdownMenu(
                                    expanded = isDropdownOpen,
                                    onDismissRequest = { expandedCategoryKey = null },
                                    modifier = Modifier
                                        .background(Color.White)
                                        .heightIn(max = 320.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "📌 $translatedCatName (${catExpenses.size})",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                                color = catInfo.color
                                            )
                                        },
                                        onClick = {
                                            expandedCategoryKey = null
                                        }
                                    )
                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    catExpenses.forEach { exp ->
                                        val isExpSelected = selectedExpense?.id == exp.id
                                        val itemTitle = exp.placeName.ifBlank {
                                            CategoryCatalog.displayName(exp.category, isEn)
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = itemTitle,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = if (isExpSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                                            ),
                                                            color = if (isExpSelected) TravelBlue else OceanNavy,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${FormatUtils.formatShortDate(exp.date)} · ${FormatUtils.formatCurrency(exp.amountInBaseCurrency, trip.baseCurrency)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                    if (isExpSelected) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = TravelBlue,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                expandedCategoryKey = null
                                                if (isExpSelected) {
                                                    showAllGeolocatedExpenses()
                                                } else {
                                                    selectedExpense = exp
                                                    val lat = exp.latitude
                                                    val lon = exp.longitude
                                                    if (lat != null && lon != null) {
                                                        mapViewRef?.controller?.setZoom(16.0)
                                                        mapViewRef?.controller?.animateTo(GeoPoint(lat, lon))
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
