package com.hazardiqplus.ui.responder

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
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
import retrofit2.Response
import retrofit2.Callback

class ReactSosActitvity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvSOSType: TextView
    private lateinit var tvSOSSender: TextView

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

        val lat = intent.getStringExtra("lat")?.toDouble() ?: 0.0
        val lng = intent.getStringExtra("lng")?.toDouble() ?: 0.0
        val type = intent.getStringExtra("type")
        val requesterName = intent.getStringExtra("requesterName")

        mapView = findViewById(R.id.mapView)
        tvSOSType = findViewById(R.id.tvSOSType)
        tvSOSSender = findViewById(R.id.tvSOSSender)

        tvSOSType.text = "SOS Type: ${type ?: "Unknown"}"
        tvSOSSender.text = "Sender: ${requesterName ?: "Unknown"}"

        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            enableUserLocation()
            showSosMarkerAndRoute(style, lat, lng)
        }
    }

    private fun enableUserLocation() {
        mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
        }
    }

    private fun showSosMarkerAndRoute(style: Style, lat: Double, lng: Double) {
        val sosPoint = Point.fromLngLat(lng, lat)

        val geoJsonSource = geoJsonSource("sos-source") {
            geometry(sosPoint)
        }
        style.addSource(geoJsonSource)

        style.addImage(
            "sos-icon",
            ContextCompat.getDrawable(this, R.drawable.sos_pin)!!.toBitmap(width = 48, height = 48)
        )

        val symbolLayer = symbolLayer("sos-layer", "sos-source") {
            iconImage("sos-icon")
            iconAllowOverlap(true)
        }
        style.addLayer(symbolLayer)

        mapView.location.addOnIndicatorPositionChangedListener { userPoint ->
            drawRoute(userPoint, sosPoint)
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
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                val route = response.body()?.routes()?.firstOrNull()
                if (route != null) {
                    val routeGeometry = route.geometry() ?: return
                    val routeLine = LineString.fromPolyline(
                        routeGeometry,
                        6
                    )
                    drawRouteLine(routeLine)
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
                lineColor(Color.BLUE)
                lineWidth(5.0)
            }
            style.addLayer(lineLayer)
        }

        val bounds = routeLine.coordinates().let { coords ->
            val lngs = coords.map { it.longitude() }
            val lats = coords.map { it.latitude() }
            val west = lngs.minOrNull() ?: 0.0
            val east = lngs.maxOrNull() ?: 0.0
            val south = lats.minOrNull() ?: 0.0
            val north = lats.maxOrNull() ?: 0.0
            CameraOptions.Builder()
                .center(Point.fromLngLat((west + east) / 2, (south + north) / 2))
                .zoom(12.0)
                .build()
        }

        mapView.mapboxMap.setCamera(bounds)
    }
}