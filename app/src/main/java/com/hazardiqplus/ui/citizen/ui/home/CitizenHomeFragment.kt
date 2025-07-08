package com.hazardiqplus.ui.citizen.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import com.hazardiqplus.R
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.heatmapLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import java.util.Locale

class CitizenHomeFragment : Fragment(R.layout.fragment_citizen_home) {

    private lateinit var tvCityName: TextView
    private lateinit var mapView: MapView
    private lateinit var btnSafeRoutes: Button
    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                enableUserLocation()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location permission is required to show your location on the map.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    private var change = false
    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.mapboxMap.setCamera(CameraOptions.Builder().bearing(it).build())
    }
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        if (!change) {
            mapView.mapboxMap.setCamera(CameraOptions.Builder().center(it).zoom(12.0).build())
            change = true
        }
        mapView.gestures.focalPoint = mapView.mapboxMap.pixelForCoordinate(it)
        updateCityName(it.latitude(), it.longitude())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_citizen_home, container, false)
        tvCityName = view.findViewById(R.id.tvCityName)
        mapView = view.findViewById(R.id.mapView)
        btnSafeRoutes = view.findViewById(R.id.btnSafeRoutes)
        mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
            //loadDummyAQIHeatmap(it)
            //loadDummyHazardMarkers(it)
        }

        btnSafeRoutes.setOnClickListener {
            Toast.makeText(requireContext(), "Safe Routes clicked!", Toast.LENGTH_SHORT).show()
            //showDummySafeRoute()
        }
    }

    private fun checkLocationPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableUserLocation()
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableUserLocation() {
        mapView.location.addOnIndicatorPositionChangedListener { point ->
            updateCityName(point.latitude(), point.longitude())
        }
    }

    @SuppressLint("SetTextI18n")
    @Suppress("DEPRECATION")
    private fun updateCityName(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val city = addresses?.firstOrNull()?.locality ?: "Unknown"
            tvCityName.text = "You are in: $city"
        } catch (e: Exception) {
            Log.e("CitizenHomeFragment", "Geocoder failed: ${e.message}")
        }
    }

    private fun loadDummyAQIHeatmap(style: Style) {
        try {
            // Dummy AQI data
            val aqiFeatures = listOf(
                Feature.fromGeometry(Point.fromLngLat(77.5946, 12.9716)),
                Feature.fromGeometry(Point.fromLngLat(77.5976, 12.9746)),
                Feature.fromGeometry(Point.fromLngLat(77.5800, 12.9800))
            )

            val geoJsonSource = geoJsonSource("aqi-source") {
                featureCollection(FeatureCollection.fromFeatures(aqiFeatures))
            }

            style.addSource(geoJsonSource)

            val heatmapLayer = heatmapLayer("aqi-heatmap", "aqi-source") {
                maxZoom(15.5)
                heatmapColor(
                    interpolate {
                        linear()
                        heatmapDensity()
                        stop(
                            Expression.literal(0.0),
                            Expression.rgba {
                                literal(0.0)      // R
                                literal(255.0)    // G
                                literal(0.0)      // B
                                literal(0.3)      // Alpha
                            }
                        )
                        stop(
                            Expression.literal(0.5),
                            Expression.rgba {
                                literal(255.0)
                                literal(255.0)
                                literal(0.0)
                                literal(0.3)
                            }
                        )
                        stop(
                            Expression.literal(1.0),
                            Expression.rgba {
                                literal(255.0)
                                literal(0.0)
                                literal(0.0)
                                literal(0.3)
                            }
                        )
                    }
                )

                heatmapIntensity(
                    interpolate {
                        linear()
                        zoom()
                        stop(0.0, 1.0)
                        stop(15.0, 3.0)
                    }
                )
                heatmapWeight(
                    interpolate {
                        linear()
                        get { literal("aqi") }
                        stop(0.0, 0.0)
                        stop(300.0, 1.0)
                    }
                )
            }

            style.addLayerBelow(heatmapLayer, "road-label")
        } catch (e: Exception) {
            Log.e("CitizenHomeFragment", "Geocoder failed: ${e.message}")
        }
    }

    private fun loadDummyHazardMarkers(style: Style) {
        try {
            val hazardFeatures = listOf(
                Feature.fromGeometry(Point.fromLngLat(77.5900, 12.9720)),
                Feature.fromGeometry(Point.fromLngLat(77.6000, 12.9750))
            )

            val hazardSource = geoJsonSource("hazard-source") {
                featureCollection(FeatureCollection.fromFeatures(hazardFeatures))
            }

            style.addSource(hazardSource)

            val symbolLayer = symbolLayer("hazard-layer", "hazard-source") {
                iconImage("marker-15")
                iconAllowOverlap(true)
                textField(Expression.get("title"))
                textSize(12.0)
                textOffset(listOf(0.0, 1.5))
                textAnchor(literal("top"))
            }

            style.addLayer(symbolLayer)
        } catch (e: Exception) {
            Log.e("CitizenHomeFragment", "Geocoder failed: ${e.message}")
        }
    }

    private fun showDummySafeRoute() {
        try {
            val routePoints = listOf(
                Point.fromLngLat(77.5900, 12.9720),
                Point.fromLngLat(77.6000, 12.9780),
                Point.fromLngLat(77.6100, 12.9800)
            )

            val routeFeature = Feature.fromGeometry(LineString.fromLngLats(routePoints))
            val routeSource = geoJsonSource("route-source") {
                feature(routeFeature)
            }

            mapView.mapboxMap.getStyle {
                it.addSource(routeSource)

                val routeLayer = lineLayer("route-layer", "route-source") {
                    lineColor(Color.BLUE)
                    lineWidth(4.0)
                }
                it.addLayer(routeLayer)
            }
        } catch (e: Exception) {
            Log.e("CitizenHomeFragment", "Geocoder failed: ${e.message}")
        }
    }
}