package com.hazardiqplus.ui.responder

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hazardiqplus.R
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.locationcomponent.location
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import kotlin.math.roundToInt
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri

class ReactSosActitvity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvSOSType: TextView
    private lateinit var tvSOSSender: TextView
    private lateinit var tvDestinationName: TextView
    private lateinit var tvLatLng: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvEta: TextView
    private lateinit var btnOpenInMaps: Button
    private lateinit var btnCopyDetails: Button
    private var routeDrawn = false
    private var lastRouteDistanceMeters: Double = 0.0
    private var lastRouteDurationSec: Double = 0.0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_react_sos_actitvity)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reactSOSMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // extras (sender, lat/lng, etc.) — pass more as needed
        val lat = intent.getStringExtra("lat")?.toDoubleOrNull() ?: 0.0
        val lng = intent.getStringExtra("lng")?.toDoubleOrNull() ?: 0.0
        val type = intent.getStringExtra("type") ?: "Unknown"
        val requesterName = intent.getStringExtra("requesterName") ?: "Unknown"

        // views
        mapView = findViewById(R.id.mapView)
        tvSOSType = findViewById(R.id.tvSOSType)
        tvSOSSender = findViewById(R.id.tvSOSSender)
        tvDestinationName = findViewById(R.id.tvDestinationName)
        tvLatLng = findViewById(R.id.tvLatLng)
        tvDistance = findViewById(R.id.tvDistance)
        tvEta = findViewById(R.id.tvEta)
        btnOpenInMaps = findViewById(R.id.btnOpenInMaps)
        btnCopyDetails = findViewById(R.id.btnCopyDetails)

        // fill basic details
        val geocoder = Geocoder(this , Locale.getDefault())
        val address = geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
        if (address != null) {
            val street = address.thoroughfare ?: "Unknown Street"
            val locality = address.locality ?: "Unknown Locality"
            val subLocality = address.subLocality ?: "Unknown Area"

            val sb = StringBuilder()
            if (street != "Unknown Street") {
                sb.append("$street, ")
            }
            if (subLocality != "Unknown Area") {
                sb.append("$subLocality, ")
            }
            if (locality != "Unknown Locality") {
                sb.append(locality)
            }
            tvDestinationName.text = sb.toString()
        }
        tvSOSType.text = type.uppercase()
        tvSOSSender.text = requesterName
        tvLatLng.text = "Lat: %.4f, Lng: %.4f".format(Locale.getDefault(), lat, lng)

        // Map & route:
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            enableUserLocationOnMap()
            showSosMarkerAndRoute(style, lat, lng)
        }

        // open in google maps
        btnOpenInMaps.setOnClickListener {
            if (lat == 0.0 && lng == 0.0) {
                Toast.makeText(this, "No destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val gmmIntentUri = "google.navigation:q=$lat,$lng".toUri()
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val geo = Intent(Intent.ACTION_VIEW, "geo:$lat,$lng?q=$lat,$lng".toUri())
                startActivity(geo)
            }
        }

        // copy details
        btnCopyDetails.setOnClickListener {
            val details = buildDetailsString(requesterName, type, lat, lng)
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("SOS Details", details)
            cm.setPrimaryClip(clip)
            Toast.makeText(this, "Details copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildDetailsString(name: String, type: String, lat: Double, lng: Double): String {
        val sb = StringBuilder()
        sb.append("Requester: $name\n")
        sb.append("Type: $type\n")
        sb.append("Location: Lat ${"%.4f".format(lat)}, Lng ${"%.4f".format(lng)}\n")
        if (lastRouteDistanceMeters > 0) {
            sb.append("Distance: ${formatDistance(lastRouteDistanceMeters)}\n")
            sb.append("ETA: ${formatEtaMinutes(lastRouteDurationSec)}\n")
        }
        return sb.toString()
    }

    private fun enableUserLocationOnMap() {
        // make sure location puck is enabled on the map if you have permissions
        try {
            mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
        } catch (e: Exception) {
            Log.w("ReactSosActivity", "enableUserLocationOnMap: ${e.message}")
        }
    }

    private fun showSosMarkerAndRoute(style: Style, lat: Double, lng: Double) {
        val sosPoint = Point.fromLngLat(lng, lat)

        // source + symbol for sos
        val geoJsonSource = geoJsonSource("sos-source") {
            geometry(sosPoint)
        }
        style.addSource(geoJsonSource)

        style.addImage(
            "sos-icon",
            ContextCompat.getDrawable(this, R.drawable.sos_pin)!!.toBitmap(width = 96, height = 96)
        )

        val symbolLayer = symbolLayer("sos-layer", "sos-source") {
            iconImage("sos-icon")
            iconAllowOverlap(true)
        }
        style.addLayer(symbolLayer)

        // wait for user location; when available draw route
        mapView.location.addOnIndicatorPositionChangedListener { userPoint ->
            if (!routeDrawn && userPoint.longitude() != 0.0 && userPoint.latitude() != 0.0) {
                routeDrawn = true
                drawRoute(userPoint, sosPoint)
            }
        }
    }

    private fun drawRoute(origin: Point, destination: Point) {
        val client = MapboxDirections.builder()
            .origin(origin)
            .destination(destination)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .accessToken(getString(R.string.mapbox_access_token))
            .build()

        client.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                val route = response.body()?.routes()?.firstOrNull()
                if (route != null) {
                    val routeGeometry = route.geometry() ?: return
                    val routeLine = LineString.fromPolyline(routeGeometry, 6)
                    lastRouteDistanceMeters = route.distance()
                    lastRouteDurationSec = route.duration()

                    runOnUiThread {
                        drawRouteLine(routeLine)
                        tvDistance.text = formatDistance(lastRouteDistanceMeters)
                        tvEta.text = formatEtaMinutes(lastRouteDurationSec)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ReactSosActitvity, "No route found", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                runOnUiThread {
                    Toast.makeText(this@ReactSosActitvity, "Route failed: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun drawRouteLine(routeLine: LineString) {
        val style = mapView.mapboxMap.style ?: return

        val routeSourceId = "route-source"
        val routeLayerId = "route-layer"

        if (style.styleSourceExists(routeSourceId)) {
            val existingSource = style.getSourceAs<GeoJsonSource>(routeSourceId)
            existingSource?.geometry(routeLine)
        } else {
            val routeSource = geoJsonSource(routeSourceId) {
                geometry(routeLine)
            }
            style.addSource(routeSource)

            val lineLayer = lineLayer(routeLayerId, routeSourceId) {
                lineColor("#3b82f6".toColorInt())
                lineWidth(8.0)
            }
            style.addLayer(lineLayer)
        }

        // set camera to bounds covering route
        val coords = routeLine.coordinates()
        if (coords.isNotEmpty()) {
            val lngs = coords.map { it.longitude() }
            val lats = coords.map { it.latitude() }
            val west = lngs.minOrNull() ?: 0.0
            val east = lngs.maxOrNull() ?: 0.0
            val south = lats.minOrNull() ?: 0.0
            val north = lats.maxOrNull() ?: 0.0
            val centerLng = (west + east) / 2.0
            val centerLat = (south + north) / 2.0
            val cam = CameraOptions.Builder()
                .center(Point.fromLngLat(centerLng, centerLat))
                .zoom(13.0)
                .build()
            mapView.mapboxMap.setCamera(cam)
        }
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
        } else {
            String.format(Locale.getDefault(), "%d m", meters.roundToInt())
        }
    }

    private fun formatEtaMinutes(durationSec: Double): String {
        val mins = (durationSec / 60.0).roundToInt()
        return if (mins < 60) "$mins min ETA" else "${mins / 60}h ${mins % 60}m ETA"
    }
}