package com.hazardiqplus.ui.citizen.fragments.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.material.loadingindicator.LoadingIndicator
import com.hazardiqplus.R
import com.hazardiqplus.clients.AirQualityApiClient
import com.hazardiqplus.clients.CityModelMapper
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.clients.WeatherApiClient
import com.hazardiqplus.data.PredictRequest
import com.hazardiqplus.data.PredictResponse
import com.hazardiqplus.ml.Model
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.*
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.scale
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.hazardiqplus.adapters.PredictedAqi
import com.hazardiqplus.adapters.PredictedAqiAdapter
import com.hazardiqplus.clients.HazardGeofenceReceiver
import com.hazardiqplus.data.FindHazardResponse
import com.hazardiqplus.data.SaveHazardRequest
import com.hazardiqplus.data.SaveHazardResponse
import com.hazardiqplus.ui.citizen.fragments.FullScreenMapFragment
import com.hazardiqplus.utils.WeatherReportScheduler
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfTransformation
import kotlin.math.sin as kSin
import kotlin.math.cos as kCos
import kotlin.math.sqrt as kSqrt
import kotlin.math.atan2 as kAtan2
import kotlin.math.pow as kPow

class CitizenHomeFragment : Fragment(R.layout.fragment_citizen_home) {

    private lateinit var tvCity: TextView
    private lateinit var tvAqi: TextView
    private lateinit var tvAqiStatus: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvWeather: TextView
    private lateinit var tvCurrentTime: TextView
    private lateinit var predictedAdapter: PredictedAqiAdapter
    private lateinit var predictedRecycler: RecyclerView
    private lateinit var loadingIndicator: LoadingIndicator
    private lateinit var mapView: MapView
    private lateinit var btnFullScreenMap: ImageView
    private lateinit var btnSafeRoutes: Button
    private lateinit var hazardDetector: Button
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var imageUri: Uri? = null
    private var currentLat: Double? = null
    private var currentLon: Double? = null
    data class AQIData(val pm25: Double, val pm10: Double)
    private lateinit var geofencingClient: GeofencingClient
    private data class WeatherData(
        val temperature: Double,
        val weatherCode: Int,
        val weatherCondition: String,
        val windSpeed: Double,
        val humidity: Double
    )
    private val labels = listOf(
        "Water_Disaster", "Non_Damaged_Wildlife_Forest", "Non_Damaged_sea",
        "Non_Damaged_Buildings_Street", "Non_Damaged_human", "Damaged_Infrastructure",
        "Earthquake", "Human_Damage", "Urban_Fire", "Wild_Fire", "Land_Slide", "Drought"
    )
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            getUserLocation()
        } else {
            showToast("Location permission is required for full functionality")
        }
    }
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val lat = point.latitude()
        val lon = point.longitude()

        if (shouldUpdateLocation(lat, lon)) {
            currentLat = lat
            currentLon = lon
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_citizen_home, container, false)
        initViews(view)
        setupCameraLauncher()
        setupPermissions()
        getUserLocation()
        setupMap()
        checkAndScheduleAqiReport()
        checkLocationPermission()
        return view
    }

    private fun checkAndScheduleAqiReport() {
        val infos = WorkManager.getInstance(requireContext())
            .getWorkInfosForUniqueWorkLiveData("WeatherReport")
            .value
        Log.d("WeatherReport", "Infos: $infos")
        if (infos.isNullOrEmpty()) {
            WeatherReportScheduler().scheduleWeatherReport(requireContext())
        }
    }

    private fun loadViews() {
        loadingIndicator.visibility = View.VISIBLE
        clearMap()
        Log.d("LatLon", "Current Lat: $currentLat, Current Lon: $currentLon")
        currentLat?.let { lat ->
            currentLon?.let { lon ->
                lifecycleScope.launch {
                    try {
                        val features = mutableListOf<Feature>()
                        loadCurrentAQIofUser(currentLat!!, currentLon!!, features)
                        updateMapFeatures(features, "aqi-layer", "aqi-source")
                        val hazardFeatures = mutableListOf<Feature>()
                        loadHazardInUserLocation(currentLat, currentLon, hazardFeatures)
                        withContext(Dispatchers.Main) {
                            loadingIndicator.animate()
                                .alpha(0f)
                                .setDuration(250)
                                .withEndAction {
                                    loadingIndicator.visibility = View.GONE
                                }
                                .start()
                        }
                    } catch (e: Exception) {
                        Log.e("CurrentAQI", "Failed to load capital cities AQI", e)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun loadCurrentAQIofUser(lat: Double, lon: Double, features: MutableList<Feature>) {
        try {
            val response = AirQualityApiClient.api.getAQIHourly(lat, lon)

            val hourly = response.hourly
            if (hourly != null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val currentHour = sdf.format(Date())

                val index = hourly.time.indexOf(currentHour)

                val pm25 = if (index != -1) hourly.pm2_5[index] else hourly.pm2_5.firstOrNull() ?: 0.0
                val pm10 = if (index != -1) hourly.pm10[index] else hourly.pm10.firstOrNull() ?: 0.0

                val aqi = calculateAQIforShow(pm25, pm10)
                tvAqi.text = "$aqi"

                val quality = when {
                    aqi < 50 -> "Good"
                    aqi < 100 -> "Moderate"
                    aqi < 200 -> "Unhealthy"
                    else -> "Hazardous"
                }
                tvAqiStatus.text = quality

                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                val city = address?.locality ?: "Unknown"

                val feature = Feature.fromGeometry(Point.fromLngLat(lon, lat))
                feature.addNumberProperty("aqi", aqi)
                feature.addStringProperty("label", "$city | AQI: $aqi")
                features.add(feature)
            } else {
                tvAqi.text = "--"
            }
        } catch (e: Exception) {
            Log.e("OpenMeteo", "Failed to load AQI", e)
            tvAqi.text = "Error"
        }
    }

    private fun loadHazardInUserLocation(currentLat: Double?, currentLon: Double?, hazardFeatures: MutableList<Feature>) {
        RetrofitClient.instance.findHazard(currentLat, currentLon, 2000)
            .enqueue(object : Callback<FindHazardResponse> {
                override fun onResponse(
                    call: Call<FindHazardResponse?>,
                    response: Response<FindHazardResponse?>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("Hazard", "Loaded hazard: ${response.body()}")
                        response.body()?.data?.forEachIndexed { index, hazard ->
                            val feature = Feature.fromGeometry(Point.fromLngLat(hazard.longitude, hazard.latitude))
                            feature.addNumberProperty("radius", hazard.rad)
                            feature.addStringProperty("label", hazard.hazard)
                            feature.addNumberProperty("hazard_id", hazard.id)
                            hazardFeatures.add(feature)
                            setupHazardGeofence(hazard.latitude, hazard.longitude, hazard.rad, index)
                        }
                        updateMapFeatures(hazardFeatures, "hazard-layer", "hazard-source")
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
    }
    private fun updateMapFeatures(features: List<Feature>, layerId: String, sourceId: String) {
        mapView.mapboxMap.getStyle { style ->
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
            if (sourceId == "aqi-source") {
                style.addLayer(
                    circleLayer(layerId, sourceId) {
                        circleRadius(100.0)
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
            } else if (sourceId == "hazard-source") {
                Log.d("Hazard", "Showing Hazards")
                // Build separate feature lists
                val polygonFeatures = mutableListOf<Feature>()
                val pointFeatures = mutableListOf<Feature>()

                for (hazard in features) {
                    val point = hazard.geometry() as? Point ?: continue
                    val radiusKm = hazard.getNumberProperty("radius")?.toDouble() ?: 1.0
                    val hazardType = hazard.getStringProperty("label") ?: "Hazard"

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
                        iconSize(1.0)
                        iconAllowOverlap(true)
                        iconIgnorePlacement(true)
                    }
                )

                // Label above icon
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
        }
    }

    private fun calculateAQIforShow(pm25: Double, pm10: Double): Int {
        // Calculate AQI based on PM2.5
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

        // Calculate AQI based on PM10
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

        // Return the higher of the two AQI values
        return maxOf(aqi25.toInt(), aqi10.toInt())
    }

    private fun calculateAQI(
        pm25: Double,
        pm10: Double,
        no2: Double,
        so2: Double,
        co: Double,
        o3: Double
    ): Int {
        val pm25Index = calculateSubIndex(pm25, arrayOf(0.0,30.0,60.0,90.0,120.0,250.0,500.0))
        val pm10Index = calculateSubIndex(pm10, arrayOf(0.0,50.0,100.0,250.0,350.0,430.0,600.0))
        val no2Index  = calculateSubIndex(no2, arrayOf(0.0,40.0,80.0,180.0,280.0,400.0,540.0))
        val so2Index  = calculateSubIndex(so2, arrayOf(0.0,40.0,80.0,380.0,800.0,1600.0,2130.0))
        val coIndex   = calculateSubIndex(co, arrayOf(0.0,1100.0,2100.0,10000.0,17000.0,34000.0,45000.0))
        val o3Index   = calculateSubIndex(o3, arrayOf(0.0,50.0,100.0,168.0,208.0,748.0,1000.0))

        return maxOf(pm25Index, pm10Index, no2Index, so2Index, coIndex, o3Index)
    }

    private fun calculateSubIndex(concentration: Double, breakpoints: Array<Double>): Int {
        val aqiLevels = arrayOf(0,50,100,200,300,400,500)

        for (i in 0 until breakpoints.size-1) {
            if (concentration <= breakpoints[i+1]) {
                val cLow = breakpoints[i]
                val cHigh = breakpoints[i+1]
                val iLow = aqiLevels[i]
                val iHigh = aqiLevels[i+1]
                val subIndex = (((iHigh - iLow)/(cHigh-cLow) * (concentration - cLow) + iLow).toInt())
                return subIndex
            }
        }
        return 500 // beyond severe
    }

    private fun loadForecast() {
        clearMap()
        currentLat?.let { lat ->
            currentLon?.let { lon ->
                try {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                    val city = address?.locality ?: "Unknown"
                    val state = address?.adminArea ?: "Unknown"
                    val normalizedState = CityModelMapper.normalizeStateName(state)
                    val (modelCity, _) = CityModelMapper.getNearestSupportedCity(
                        requireContext(),
                        normalizedState,
                        city,
                        lat,
                        lon
                    )
                    predictAirQuality(modelCity, normalizedState, lat, lon)
                } catch (_: Exception) {
                    showToast("Failed to get location data for forecast")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadWeather() {
        currentLat?.let { lat ->
            currentLon?.let { lon ->
                lifecycleScope.launch {
                    try {
                        val weather = getWeatherData(lat, lon)
                        tvTemperature.text = "${weather.temperature}°C"
                        tvWeather.text = weather.weatherCondition
                    } catch (_: Exception) {
                        showToast("Failed to load weather data")
                    }
                }
            }
        }
    }

    private suspend fun getWeatherData(lat: Double, lon: Double): WeatherData {
        return withContext(Dispatchers.IO) {
            try {
                val response = WeatherApiClient.api.getWeather(lat, lon)
                if (response.isSuccessful) {
                    response.body()?.current?.let { current ->
                        WeatherData(
                            temperature = current.temperature,
                            weatherCode = current.weatherCode,
                            weatherCondition = mapWeatherCode(current.weatherCode),
                            windSpeed = current.windSpeed,
                            humidity = current.humidity
                        )
                    } ?: throw Exception("Empty weather response")
                } else {
                    throw Exception("Weather API error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("Weather", "Failed to fetch weather data", e)
                throw e
            }
        }
    }
    private fun mapWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }

    private fun clearMap() {
        mapView.mapboxMap.getStyle { style ->
            // Remove layers
            try {
                style.removeStyleLayer("aqi-layer")
                style.removeStyleLayer("forecast-layer")
                style.removeStyleLayer("weather-layer")
                style.removeStyleLayer("hazard-layer")
            } catch (e: Exception) {
                Log.e("MapStyle", "Error removing layers", e)
            }

            // Remove sources
            try {
                style.removeStyleSource("aqi-source")
                style.removeStyleSource("forecast-source")
                style.removeStyleSource("weather-source")
                style.removeStyleSource("hazard-source")
            } catch (e: Exception) {
                Log.e("MapStyle", "Error removing sources", e)
            }
        }
    }

    private fun initViews(view: View) {
        tvAqi = view.findViewById(R.id.tvAqi)
        tvCity = view.findViewById(R.id.tvCity)
        tvAqiStatus = view.findViewById(R.id.tvAqiStatus)
        tvTemperature = view.findViewById(R.id.tvTemperature)
        tvWeather = view.findViewById(R.id.tvWeather)
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime)
        mapView = view.findViewById(R.id.mapView)
        btnFullScreenMap = view.findViewById(R.id.btnFullScreenMap)
        predictedRecycler = view.findViewById(R.id.hrvPredictedAqi)
        predictedRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        btnSafeRoutes = view.findViewById(R.id.btnSafeRoutes)
        hazardDetector = view.findViewById(R.id.hazard)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        tvCurrentTime.text = SimpleDateFormat("EEEE, hh:mm a", Locale.getDefault()).format(Date())
        btnFullScreenMap.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.coordinatorLayout, FullScreenMapFragment())
                .addToBackStack(null)
                .commit()
        }
        btnSafeRoutes.setOnClickListener {
            showToast("Safe Routes functionality coming soon")
        }
        hazardDetector.setOnClickListener {
            checkCameraPermission()
        }
    }

    private fun setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && imageUri != null) {
                lifecycleScope.launch {
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(
                            requireContext().contentResolver,
                            imageUri
                        )
                        runModel(bitmap)
                    } catch (e: Exception) {
                        showToast("Failed to process image: ${e.message}")
                    }
                }
            }
        }
    }

    private fun setupPermissions() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.all { it.value }) {
                openCamera()
            } else {
                showToast("Camera permission required for hazard detection")
            }
        }
    }

    private fun setupMap() {
        mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            currentLat?.let { lat ->
                currentLon?.let { lon ->
                    mapView.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(lon, lat))
                            .zoom(10.0)
                            .build()
                    )
                }
            }
        }
    }

    private fun setupHazardGeofence(lat: Double, lon: Double, radius: Double, index: Int) {
        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        val geofence = Geofence.Builder()
            .setRequestId("hazard_zone_$index")
            .setCircularRegion(lat, lon, (radius * 1000).toFloat()) // meters
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setLoiteringDelay(0)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(requireContext(), HazardGeofenceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener { Log.d("HazardGeofence", "Geofence added!") }
                .addOnFailureListener { e ->
                    if (e is ApiException) {
                        Log.e(
                            "HazardGeofence",
                            "Failed: ${e.statusCode} ${GeofenceStatusCodes.getStatusCodeString(e.statusCode)}"
                        )
                    } else {
                        Log.e("HazardGeofence", "Failed: ${e.message}")
                    }
                }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getUserLocation()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                showToast("Location permission is needed for accurate air quality data")
                requestLocationPermission()
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.CAMERA
            ) -> {
                showToast("Camera permission is needed for hazard detection")
                requestCameraPermission()
            }
            else -> {
                requestCameraPermission()
            }
        }
    }

    private fun requestCameraPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }

    @SuppressLint("SetTextI18n")
    private fun updateCityName(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            val city = address?.locality ?: "Unknown"
            val state = address?.adminArea ?: "Unknown"
            val normalizedState = CityModelMapper.normalizeStateName(state)
            tvCity.text = city
            val (modelCity, distance) = CityModelMapper.getNearestSupportedCity(
                requireContext(),
                normalizedState,
                city,
                lat,
                lon
            )
            if (distance > 0) {
                showToast("Using data from nearest supported location: $modelCity (${"%.1f".format(distance)} km away)")
            }
        } catch (e: Exception) {
            Log.e("CitizenHomeFragment", "Geocoder failed", e)
        }
    }

    private fun predictAirQuality(city: String, state: String, lat: Double, lon: Double) {
        val request = PredictRequest(city, state, lat, lon, 23)
        RetrofitClient.instance.predictAirQuality(request)
            .enqueue(object : Callback<PredictResponse> {
                override fun onResponse(
                    call: Call<PredictResponse>,
                    response: Response<PredictResponse>
                ) {
                    when {
                        response.isSuccessful && response.body()?.success == true -> {
                            handlePredictionSuccess(response.body()!!, lat, lon)
                        }
                        response.code() == 400 -> {
                            handleUnsupportedLocation(response.body())
                        }
                        else -> {
                            handlePredictionError(response.errorBody()?.string())
                        }
                    }
                }

                override fun onFailure(call: Call<PredictResponse>, t: Throwable) {
                    showToast("Network error: ${t.message}")
                }
            })
    }

    private fun handlePredictionSuccess(response: PredictResponse, lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {
                // 1. Fetch additional pollutants from Open-Meteo
                val openMeteoResponse = withContext(Dispatchers.IO) {
                    AirQualityApiClient.api.getAQIHourly(lat, lon)
                }
                val hourly = openMeteoResponse.hourly ?: return@launch

                // 2. Generate hourly predictions
                val hourlyPredictions = mutableListOf<PredictedAqi>()

                val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                val currentTime = System.currentTimeMillis()

                response.predictions?.take(24)?.forEachIndexed { index, prediction ->
                    if (index >= hourly.nitrogen_dioxide.size ||
                        index >= hourly.sulphur_dioxide.size ||
                        index >= hourly.carbon_monoxide.size ||
                        index >= hourly.ozone.size
                    ) {
                        return@forEachIndexed // skip if open-meteo data incomplete
                    }

                    val time = formatter.format(Date(currentTime + (index + 1) * 3600000L))
                    val no2 = hourly.nitrogen_dioxide[index]
                    val so2 = hourly.sulphur_dioxide[index]
                    val co = hourly.carbon_monoxide[index]
                    val o3 = hourly.ozone[index]

                    val aqi = calculateAQI(prediction.PM25, prediction.PM10, no2, so2, co, o3)
                    hourlyPredictions.add(PredictedAqi(time, aqi))
                }

                // 3. Update RecyclerView on main thread
                withContext(Dispatchers.Main) {
                    predictedAdapter = PredictedAqiAdapter(hourlyPredictions)
                    predictedRecycler.adapter = predictedAdapter
                }
            } catch (e: Exception) {
                Log.e("Forecast", "Error handling prediction success", e)
                showToast("Failed to process forecast")
            }
        }
    }

    private fun handleUnsupportedLocation(response: PredictResponse?) {
        val message = response?.message ?: "Location not supported"
        val supportedCities = response?.supportedCities?.joinToString(", ") ?: ""
        showToast("$message. Supported cities: $supportedCities")
    }

    private fun handlePredictionError(error: String?) {
        showToast("Prediction failed: ${error ?: "Unknown error"}")
    }

    private fun runModel(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val model = Model.newInstance(requireContext())
                val resized = bitmap.scale(224, 224)
                val floatValues = FloatArray(224 * 224 * 3)
                val intValues = IntArray(224 * 224)
                resized.getPixels(intValues, 0, 224, 0, 0, 224, 224)
                for (i in intValues.indices) {
                    val pixel = intValues[i]
                    floatValues[i * 3 + 0] = ((pixel shr 16 and 0xFF) / 255.0f) // R
                    floatValues[i * 3 + 1] = ((pixel shr 8 and 0xFF) / 255.0f)  // G
                    floatValues[i * 3 + 2] = ((pixel and 0xFF) / 255.0f)        // B
                }
                val chw = FloatArray(3 * 224 * 224)
                for (y in 0 until 224) {
                    for (x in 0 until 224) {
                        for (c in 0 until 3) {
                            chw[c * 224 * 224 + y * 224 + x] =
                                floatValues[y * 224 * 3 + x * 3 + c]
                        }
                    }
                }
                val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 3, 224, 224), DataType.FLOAT32)
                inputBuffer.loadArray(chw)
                val outputs = model.process(inputBuffer)
                val result = outputs.outputFeature0AsTensorBuffer.floatArray
                val predictedIndex = result.indices.maxByOrNull { result[it] } ?: -1
                model.close()

                withContext(Dispatchers.Main) {
                    val textInputView = View.inflate(this@CitizenHomeFragment.requireContext(), R.layout.dialouge_text_input, null)
                    val input = textInputView.findViewById<TextInputEditText>(R.id.textInput)
                    input.hint = "Enter hazard Radius in km"

                    val dialog = MaterialAlertDialogBuilder(this@CitizenHomeFragment.requireContext())
                        .setTitle("It has detected a ${labels.getOrElse(predictedIndex) { "Unknown" }} hazard. Enter its radius of catastrophe.")
                        .setView(textInputView)
                        .setPositiveButton("Proceed", null)
                        .setNegativeButton("Cancel", null)
                        .create()

                    dialog.show()

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val radius = input.text.toString().trim()
                        if (radius.isNotBlank()) {
                            registerHazard(radius.toDouble(), labels.getOrElse(predictedIndex) { "Unknown" })
                            dialog.dismiss()
                        } else {
                            input.error = "Enter a valid radius"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.message}")
                }
            }
        }
    }

    private fun registerHazard(radius: Double, hazard: String) {
        val request = SaveHazardRequest(radius, currentLat!!, currentLon!!, hazard)

        RetrofitClient.instance.registerHazard(request)
            .enqueue(object : Callback<SaveHazardResponse> {
                override fun onResponse(
                    call: Call<SaveHazardResponse>,
                    response: Response<SaveHazardResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Snackbar.make(requireView(), "Hazard registered successfully.", Snackbar.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(
                    call: Call<SaveHazardResponse?>,
                    t: Throwable
                ) {
                    Log.e("Hazard", "Failed to register hazard", t)
                    Snackbar.make(requireView(), "Failed to register hazard. Please try again!", Snackbar.LENGTH_SHORT).show()
                }
            })
    }

    private fun openCamera() {
        try {
            val photoFile = createTempImageFile()
            imageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                photoFile
            )
            imageUri?.let { cameraLauncher.launch(it) }
        } catch (e: Exception) {
            showToast("Failed to open camera: ${e.message}")
        }
    }

    private fun createTempImageFile(): File {
        return File.createTempFile(
            "camera_img_${System.currentTimeMillis()}",
            ".jpg",
            requireContext().cacheDir
        ).apply { deleteOnExit() }
    }

    private fun getUserLocation() {
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
                    currentLat = location.latitude
                    currentLon = location.longitude
                    updateCityName(location.latitude, location.longitude)

                    loadViews()
                    loadWeather()
                    loadForecast()
                } else {
                    showToast("Could not get location")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Failed to get location", e)
                showToast("Location unavailable")
            }
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), message, duration).show()
    }

    private fun shouldUpdateLocation(newLat: Double, newLon: Double): Boolean {
        return currentLat == null || currentLon == null || calculateDistance(newLat, newLon, currentLat!!, currentLon!!) > 0.1
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kSin(dLat / 2).kPow(2) +
                kCos(Math.toRadians(lat1)) * kCos(Math.toRadians(lat2)) *
                kSin(dLon / 2).kPow(2)
        val distance = earthRadius * 2 * kAtan2(kSqrt(a), kSqrt(1 - a))
        return distance
    }

    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(requireContext(), drawableId) ?: return null
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)

    }
}