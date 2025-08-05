package com.hazardiqplus.ui.citizen.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.hazardiqplus.R
import com.hazardiqplus.clients.AirQualityApiClient
import com.hazardiqplus.data.CityPoint
import com.hazardiqplus.ui.citizen.fragments.home.CitizenHomeFragment.AQIData
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.core.graphics.createBitmap
import com.google.android.material.loadingindicator.LoadingIndicator
import com.google.android.material.snackbar.Snackbar
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.FindHazardResponse
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.getLayer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FullScreenMapFragment : Fragment(R.layout.fragment_full_screen_map) {

    private lateinit var mapView: MapView
    private lateinit var mapLoadingIndicator: LoadingIndicator
    private val cityCapitals = listOf(
        CityPoint("Delhi", 28.6139, 77.2090),
        CityPoint("Mumbai", 19.0760, 72.8777),
        CityPoint("Chennai", 13.0827, 80.2707),
        CityPoint("Kolkata", 22.5726, 88.3639),
        CityPoint("Bengaluru", 12.9716, 77.5946),
        CityPoint("Hyderabad", 17.3850, 78.4867),
        CityPoint("Ahmedabad", 23.0225, 72.5714),
        CityPoint("Jaipur", 26.9124, 75.7873),
        CityPoint("Bhopal", 23.2599, 77.4126),
        CityPoint("Lucknow", 26.8467, 80.9462),
        CityPoint("Patna", 25.5941, 85.1376),
        CityPoint("Thiruvananthapuram", 8.5241, 76.9366),
        CityPoint("Raipur", 21.2514, 81.6296),
        CityPoint("Bhubaneswar", 20.2961, 85.8245),
        CityPoint("Dispur", 26.1445, 91.7362),
        CityPoint("Ranchi", 23.3441, 85.3096),
        CityPoint("Imphal", 24.8170, 93.9368),
        CityPoint("Shillong", 25.5788, 91.8933),
        CityPoint("Aizawl", 23.7271, 92.7176),
        CityPoint("Kohima", 25.6701, 94.1077),
        CityPoint("Agartala", 23.8315, 91.2868),
        CityPoint("Itanagar", 27.0844, 93.6053),
        CityPoint("Gangtok", 27.3314, 88.6138),
        CityPoint("Panaji", 15.4909, 73.8278),
        CityPoint("Shimla", 31.1048, 77.1734),
        CityPoint("Dehradun", 30.3165, 78.0322),
        CityPoint("Chandigarh", 30.7333, 76.7794)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_full_screen_map, container, false)
        mapView = view.findViewById(R.id.fullscreenMapView)
        mapLoadingIndicator = view.findViewById(R.id.mapLoadingIndicator)
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS){ style ->
            mapLoadingIndicator.visibility = View.VISIBLE
            lifecycleScope.launchWhenStarted {
                val aqiFeatures = mutableListOf<Feature>()
                loadCurrentAQIofUser(aqiFeatures)
                for ((city, cityLat, cityLon) in cityCapitals) {
                    val aqiData = getLiveAQIData(cityLat, cityLon)
                    val aqiValue = calculateAQI(aqiData.pm25, aqiData.pm10)

                    val feature = Feature.fromGeometry(Point.fromLngLat(cityLon, cityLat))
                    feature.addNumberProperty("aqi", aqiValue)
                    feature.addStringProperty("label", "$city | AQI: $aqiValue")
                    aqiFeatures.add(feature)
                }
                updateAqiMapFeatures(aqiFeatures)

                val hazardFeatures = mutableListOf<Feature>()
                RetrofitClient.instance.findHazard(23.51, 80.32, 2000)
                    .enqueue(object : Callback<FindHazardResponse> {
                        override fun onResponse(
                            call: Call<FindHazardResponse?>,
                            response: Response<FindHazardResponse?>
                        ) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                Log.d("Hazard", "Loaded hazard: ${response.body()}")
                                response.body()?.data?.forEach { hazard ->
                                    val feature = Feature.fromGeometry(Point.fromLngLat(hazard.longitude, hazard.latitude))
                                    feature.addNumberProperty("radius", hazard.rad)
                                    feature.addStringProperty("hazard", hazard.hazard)
                                    feature.addNumberProperty("hazard_id", hazard.id)
                                    hazardFeatures.add(feature)
                                }
                                updateHazardFeatures(hazardFeatures)
                            }
                        }

                        override fun onFailure(
                            call: Call<FindHazardResponse?>,
                            t: Throwable
                        ) {
                            Log.e("Hazard", "Failed to load hazard", t)
                            Snackbar.make(requireView(), "Failed to load hazard", Snackbar.LENGTH_SHORT).show()
                        }
                    })
                withContext(Dispatchers.Main) {
                    mapLoadingIndicator.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction {
                            mapLoadingIndicator.visibility = View.GONE
                        }
                        .start()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun loadCurrentAQIofUser(features: MutableList<Feature>) {
        try {
            var lat = 0.0
            var lon = 0.0
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            LocationServices.getFusedLocationProviderClient(requireContext())
                .lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        lat = location.latitude
                        lon = location.longitude
                        mapView.mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(lon, lat))
                                .zoom(10.0)
                                .build()
                        )
                        restrictMapToIndia()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Location", "Failed to get location", e)
                }
            val response = AirQualityApiClient.api.getAQIHourly(lat, lon)

            val hourly = response.hourly
            if (hourly != null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val currentHour = sdf.format(Date())

                val index = hourly.time.indexOf(currentHour)

                val pm25 = if (index != -1) hourly.pm2_5[index] else hourly.pm2_5.firstOrNull() ?: 0.0
                val pm10 = if (index != -1) hourly.pm10[index] else hourly.pm10.firstOrNull() ?: 0.0

                val aqi = calculateAQI(pm25, pm10)

                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                val city = address?.locality ?: "Unknown"

                val feature = Feature.fromGeometry(Point.fromLngLat(lon, lat))
                feature.addNumberProperty("aqi", aqi)
                feature.addStringProperty("label", "$city | AQI: $aqi")

                features.add(feature)
            } else {
                Log.d("OpenMeteo", "No hourly data available")
            }
        } catch (e: Exception) {
            Log.e("OpenMeteo", "Failed to load AQI", e)
        }
    }

    private fun updateAqiMapFeatures(features: List<Feature>) {
        mapView.mapboxMap.getStyle { style ->
            val layerId = "aqi-layer"
            val sourceId = "aqi-source"
            val featureCollection = FeatureCollection.fromFeatures(features)
            style.removeStyleSource(sourceId)
            style.removeStyleLayer(layerId)
            val source = style.getSource(sourceId)
            if (source is GeoJsonSource) {
                source.featureCollection(featureCollection)
            } else {
                style.addSource(geoJsonSource(sourceId) {
                    featureCollection(featureCollection)
                })
            }
            style.addLayer(
                circleLayer(layerId, sourceId) {
                    circleRadius(32.0)
                    circleColor(
                        interpolate {
                            linear()
                            get { literal("aqi") }
                            stop { literal(0); rgb(0.0, 255.0, 0.0) }
                            stop { literal(50); rgb(155.0, 255.0, 0.0) }
                            stop { literal(100); rgb(255.0, 255.0, 0.0) }
                            stop { literal(150); rgb(255.0, 126.0, 0.0) }
                            stop { literal(200); rgb(255.0, 0.0, 0.0) }
                            stop { literal(300); rgb(153.0, 0.0, 76.0) }
                            stop { literal(500); rgb(126.0, 0.0, 35.0) }
                        }
                    )
                    circleOpacity(0.6)
                }
            )
            style.addLayer(
                symbolLayer("${layerId}-label", sourceId) {
                    textField(Expression.get("label"))
                    textSize(12.0)
                    textColor("#000000")
                    textHaloColor("#FFFFFF")
                    textHaloWidth(1.0)
                    textOffset(listOf(0.0, 1.5))
                }
            )
        }
    }

    private fun updateHazardFeatures(features: List<Feature>) {
        val style = mapView.mapboxMap.style ?: return

        // Build separate feature lists
        val polygonFeatures = mutableListOf<Feature>()
        val pointFeatures = mutableListOf<Feature>()

        for (hazard in features) {
            Log.d("Hazard", "$hazard")
            val point = hazard.geometry() as? Point ?: continue
            val radiusKm = hazard.getNumberProperty("radius")?.toDouble() ?: 1.0
            val hazardType = hazard.getStringProperty("hazard") ?: "Hazard"
            val hazardId = hazard.getNumberProperty("hazard_id")?.toLong() ?: 0L

            // Polygon for radius
            val polygon = TurfTransformation.circle(point, radiusKm * 1000, 64, TurfConstants.UNIT_METERS)
            val polygonFeature = Feature.fromGeometry(polygon)
            polygonFeature.addStringProperty("hazard", hazardType)
            polygonFeatures.add(polygonFeature)

            // Point for icon and label
            val pointFeature = Feature.fromGeometry(point)
            pointFeature.addStringProperty("hazard", hazardType)
            pointFeatures.add(pointFeature)
        }

        // Add sources
        style.addSource(geoJsonSource("hazard-polygon-source") {
            featureCollection(FeatureCollection.fromFeatures(polygonFeatures))
        })
        style.addSource(geoJsonSource("hazard-point-source") {
            featureCollection(FeatureCollection.fromFeatures(pointFeatures))
        })

        // Fill polygon for radius
        style.addLayer(
            com.mapbox.maps.extension.style.layers.generated.fillLayer("hazard-radius-layer", "hazard-polygon-source") {
                fillColor("#FF0000")
                fillOpacity(0.2)
            }
        )

        // Add hazard icon as bitmap
        val bitmap = getBitmapFromVectorDrawable(R.drawable.warning_24px)
        if (bitmap != null && style.getStyleImage("hazard-icon") == null) {
            style.addImage("hazard-icon", bitmap)
        }

        // Symbol for icon
        style.addLayer(
            symbolLayer("hazard-symbol-layer", "hazard-point-source") {
                iconImage("hazard-icon")
                iconColor("#FFFFFF")
                iconSize(1.0)
                iconAllowOverlap(true)
                iconIgnorePlacement(true)
            }
        )

        // Label above icon
        val labelLayer = style.getLayer("hazard-label-layer")
        if (labelLayer !is Layer)
        style.addLayer(
            symbolLayer("hazard-label-layer", "hazard-point-source") {
                textField(Expression.get("hazard"))
                textSize(12.0)
                textColor("#000000")
                textHaloColor("#FFFFFF")
                textHaloWidth(1.2)
                textOffset(listOf(0.0, 2.0))
            }
        )


    }

    suspend fun getLiveAQIData(lat: Double, lon: Double): AQIData {
        return withContext(Dispatchers.IO) {
            try {
                val response = AirQualityApiClient.api.getAQIHourly(lat, lon)

                if (response.hourly != null) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val currentHourString = sdf.format(Date())

                    val index = response.hourly.time.indexOf(currentHourString)

                    if (index != -1) {
                        val pm25Value = response.hourly.pm2_5.getOrNull(index) ?: 0.0
                        val pm10Value = response.hourly.pm10.getOrNull(index) ?: 0.0
                        AQIData(pm25 = pm25Value, pm10 = pm10Value)
                    } else {
                        val pm25Value = response.hourly.pm2_5.firstOrNull() ?: 0.0
                        val pm10Value = response.hourly.pm10.firstOrNull() ?: 0.0
                        AQIData(pm25 = pm25Value, pm10 = pm10Value)
                    }
                } else {
                    Log.e("AQI_DEBUG", "Hourly data missing")
                    AQIData(pm25 = 0.0, pm10 = 0.0)
                }
            } catch (e: Exception) {
                Log.e("AQI_DEBUG", "Failed to fetch AQI", e)
                AQIData(pm25 = 0.0, pm10 = 0.0)
            }
        }
    }

    private fun calculateAQI(pm25: Double, pm10: Double): Int {
        val aqi25 = when {
            pm25 <= 12.0 -> ((50.0/12.0) * pm25)
            pm25 <= 35.4 -> 50 + ((50.0/(35.4-12.0)) * (pm25 - 12.0))
            pm25 <= 55.4 -> 100 + ((50.0/(55.4-35.4)) * (pm25 - 35.4))
            pm25 <= 150.4 -> 150 + ((100.0/(150.4-55.4)) * (pm25 - 55.4))
            pm25 <= 250.4 -> 200 + ((100.0/(250.4-150.4)) * (pm25 - 150.4))
            pm25 <= 350.4 -> 300 + ((100.0/(350.4-250.4)) * (pm25 - 250.4))
            pm25 <= 500.4 -> 400 + ((100.0/(500.4-350.4)) * (pm25 - 350.4))
            else -> 500
        }
        val aqi10 = when {
            pm10 <= 54.0 -> pm10
            pm10 <= 154.0 -> 50 + ((50.0/(154.0-54.0)) * (pm10 - 54.0))
            pm10 <= 254.0 -> 100 + ((50.0/(254.0-154.0)) * (pm10 - 154.0))
            pm10 <= 354.0 -> 150 + ((100.0/(354.0-254.0)) * (pm10 - 254.0))
            pm10 <= 424.0 -> 200 + ((100.0/(424.0-354.0)) * (pm10 - 354.0))
            pm10 <= 504.0 -> 300 + ((100.0/(504.0-424.0)) * (pm10 - 424.0))
            pm10 <= 604.0 -> 400 + ((100.0/(604.0-504.0)) * (pm10 - 504.0))
            else -> 500
        }

        return maxOf(aqi25.toInt(), aqi10.toInt())
    }

    private fun restrictMapToIndia() {
        val bounds = CoordinateBounds(
            Point.fromLngLat(68.1, 6.5),
            Point.fromLngLat(97.4, 37.6)
        )

        val cameraBoundsOptions = CameraBoundsOptions.Builder()
            .bounds(bounds)
            .minZoom(4.5)
            .maxZoom(16.0)
            .build()

        mapView.mapboxMap.setBounds(cameraBoundsOptions)
    }

    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(requireContext(), drawableId) ?: return null
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}