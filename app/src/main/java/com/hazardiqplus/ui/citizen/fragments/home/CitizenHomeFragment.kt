package com.hazardiqplus.ui.citizen.fragments.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.material.loadingindicator.LoadingIndicator
import com.hazardiqplus.R
import com.hazardiqplus.clients.AirQualityApiClient
import com.hazardiqplus.clients.CityModelMapper
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.clients.WeatherApiClient
import com.hazardiqplus.data.CityPoint
import com.hazardiqplus.data.NearbyAQIResponse
import com.hazardiqplus.data.PredictRequest
import com.hazardiqplus.data.PredictResponse
import com.hazardiqplus.ml.Model
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import kotlin.math.sin as kSin
import kotlin.math.cos as kCos
import kotlin.math.sqrt as kSqrt
import kotlin.math.atan2 as kAtan2
import kotlin.math.pow as kPow
import com.mapbox.maps.extension.style.expressions.dsl.generated.*
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
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
import com.mapbox.maps.extension.style.style

class CitizenHomeFragment : Fragment(R.layout.fragment_citizen_home) {

    private lateinit var tvCityName: TextView
    private lateinit var tvAQI: TextView
    private lateinit var loadingIndicator: LoadingIndicator
    private lateinit var mapView: MapView
    private lateinit var btnSafeRoutes: Button
    private lateinit var hazardDetector: Button
    private lateinit var rangeSeekBar: SeekBar
    private lateinit var rangeText: TextView
    private lateinit var optionSelector: Spinner
    private enum class ViewMode { CURRENT_AQI, FORECAST, WEATHER, HAZARD }
    private var currentViewMode = ViewMode.CURRENT_AQI
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var imageUri: Uri? = null
    private var currentLat: Double? = null
    private var currentLon: Double? = null
    data class AQIData(val pm25: Double, val pm10: Double)
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

            updateMapCamera(point)
            updateCityName(lat, lon)
            refreshCurrentView()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_citizen_home, container, false)
        initViews(view)
        setupSpinner(view)
        setupCameraLauncher()
        setupPermissions()
        setupMap()
        setupRangeSeekBar()
        checkLocationPermission()
        return view
    }

    private fun setupSpinner(view: View) {
        optionSelector = view.findViewById(R.id.optionSpinner)
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.aqi_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            optionSelector.adapter = adapter
        }

        optionSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentViewMode = when (position) {
                    0 -> ViewMode.CURRENT_AQI
                    1 -> ViewMode.FORECAST
                    2 -> ViewMode.WEATHER
                    3 -> ViewMode.HAZARD
                    else -> ViewMode.CURRENT_AQI
                }
                refreshCurrentView()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun refreshCurrentView() {
        when (currentViewMode) {
            ViewMode.CURRENT_AQI -> loadCurrentAQI()
            ViewMode.FORECAST -> loadForecast()
            ViewMode.WEATHER -> loadWeather()
            ViewMode.HAZARD -> loadHazardMap()
        }
    }

    private fun loadCurrentAQI() {
        loadingIndicator.visibility = View.VISIBLE
        clearMap()
        currentLat?.let { lat ->
            currentLon?.let { lon ->
                lifecycleScope.launch {
                    try {
                        val features = mutableListOf<Feature>()
                        loadCurrentAQIofUser(currentLat!!, currentLon!!, features)
                        fetchNearbyAQI( rangeSeekBar.progress, features)
                        for ((city, cityLat, cityLon) in cityCapitals) {
                            val aqiData = getLiveAQIData(cityLat, cityLon)
                            val aqiValue = calculateAQI(aqiData.pm25, aqiData.pm10)

                            val feature = Feature.fromGeometry(Point.fromLngLat(cityLon, cityLat))
                            feature.addNumberProperty("aqi", aqiValue)
                            feature.addStringProperty("label", "$city | AQI: $aqiValue")
                            features.add(feature)
                        }
                        updateMapFeatures(features, "aqi-layer", "aqi-source")
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

                val pm25 = if (index != -1) hourly.pm25[index] else hourly.pm25.firstOrNull() ?: 0.0
                val pm10 = if (index != -1) hourly.pm10[index] else hourly.pm10.firstOrNull() ?: 0.0

                val aqi = calculateAQI(pm25, pm10)
                tvAQI.text = "AQI: $aqi"

                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                val city = address?.locality ?: "Unknown"

                val feature = Feature.fromGeometry(Point.fromLngLat(lon, lat))
                feature.addNumberProperty("aqi", aqi)
                feature.addStringProperty("label", "$city | AQI: $aqi")

                features.add(feature)
            } else {
                tvAQI.text = "AQI: --"
            }
        } catch (e: Exception) {
            Log.e("OpenMeteo", "Failed to load AQI", e)
            tvAQI.text = "AQI: Error"
        }
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

            when (currentViewMode) {
                ViewMode.CURRENT_AQI, ViewMode.FORECAST -> {
                    style.addLayer(
                        circleLayer(layerId, sourceId) {
                            circleRadius(8.0)
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
                            circleOpacity(0.8)
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

                ViewMode.WEATHER -> {
                    style.addLayer(
                        symbolLayer(layerId, sourceId) {
                            iconImage(Expression.get("icon"))
                            iconSize(1.5)
                            textField(Expression.get("label"))
                            textSize(12.0)
                            textColor("#000000")
                            textHaloColor("#FFFFFF")
                            textHaloWidth(1.0)
                            textOffset(listOf(0.0, 1.5))
                        }
                    )
                }

                ViewMode.HAZARD -> {
                    style.addLayer(
                        circleLayer(layerId, sourceId) {
                            circleRadius(10.0)
                            circleColor("#FF0000")
                            circleOpacity(0.7)
                        }
                    )
                }
            }
        }
    }

    private fun calculateAQI(pm25: Double, pm10: Double): Int {
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

    private fun loadWeather() {
        currentLat?.let { lat ->
            currentLon?.let { lon ->
                lifecycleScope.launch {
                    try {
                        val weather = getWeatherData(lat, lon)
                        showWeatherData(weather)

                        val feature = Feature.fromGeometry(Point.fromLngLat(lon, lat))
                        feature.addStringProperty("icon", getWeatherIcon(weather.weatherCode))
                        feature.addStringProperty("label", "${weather.temperature}°C | ${weather.weatherCondition}")

                        updateMapFeatures(listOf(feature), "weather-layer", "weather-source")
                    } catch (_: Exception) {
                        showToast("Failed to load weather data")
                    }
                }
            }
        }
    }

    private fun loadHazardMap() {
        clearMap()
        showToast("Hazard map will show detected hazards in the area")
    }

    suspend fun getLiveAQIData(lat: Double, lon: Double): AQIData {
        return withContext(Dispatchers.IO) {
            try {
                val response = AirQualityApiClient.api.getAQIHourly(lat, lon)

                if (response.hourly != null) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val currentHourString = sdf.format(Date())

                    Log.d("AQI_DEBUG", "Looking for timestamp: $currentHourString")
                    Log.d("AQI_DEBUG", "Sample response times: ${response.hourly.time.take(5)}")

                    val index = response.hourly.time.indexOf(currentHourString)

                    if (index != -1) {
                        val pm25Value = response.hourly.pm25.getOrNull(index) ?: 0.0
                        val pm10Value = response.hourly.pm10.getOrNull(index) ?: 0.0
                        Log.d("AQI_DEBUG", "Matched PM2.5: $pm25Value, PM10: $pm10Value")
                        AQIData(pm25 = pm25Value, pm10 = pm10Value)
                    } else {
                        Log.w("AQI_DEBUG", "Hour not found, fallback to first index")
                        val pm25Value = response.hourly.pm25.firstOrNull() ?: 0.0
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

    private fun getWeatherIcon(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "clear-day"
            1, 2, 3 -> "partly-cloudy-day"
            45, 48 -> "fog"
            in 51..57 -> "drizzle"
            in 61..67 -> "rain"
            in 71..77 -> "snow"
            in 80..82 -> "rain"
            in 85..86 -> "snow"
            in 95..99 -> "thunderstorms"
            else -> "not-available"
        }
    }

    private fun showWeatherData(weather: WeatherData) {
        val weatherText = """
            Temperature: ${weather.temperature}°C
            Condition: ${weather.weatherCondition}
            Humidity: ${weather.humidity}%
            Wind: ${weather.windSpeed} km/h
        """.trimIndent()

        showToast(weatherText, Toast.LENGTH_LONG)
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
        tvAQI = view.findViewById(R.id.tvAqiText)
        tvCityName = view.findViewById(R.id.tvCityName)
        mapView = view.findViewById(R.id.mapView)
        btnSafeRoutes = view.findViewById(R.id.btnSafeRoutes)
        hazardDetector = view.findViewById(R.id.hazard)
        rangeSeekBar = view.findViewById(R.id.seekBarRange)
        rangeText = view.findViewById(R.id.tvRange)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)

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
                    restrictMapToIndia()
                }
            }
        }
    }

    private fun restrictMapToIndia() {
        val bounds = CoordinateBounds(
            Point.fromLngLat(68.1, 6.5),     // Southwest corner
            Point.fromLngLat(97.4, 37.6)     // Northeast corner
        )

        val cameraBoundsOptions = CameraBoundsOptions.Builder()
            .bounds(bounds)
            .minZoom(4.5)    // Optional: limit zoom out
            .maxZoom(16.0)   // Optional: limit zoom in
            .build()

        mapView.mapboxMap.setBounds(cameraBoundsOptions)
    }


    @SuppressLint("SetTextI18n")
    private fun setupRangeSeekBar() {
        rangeSeekBar.progress = 10
        rangeText.text = "Range: 10 km"
        rangeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val radius = progress.coerceAtLeast(1)
                rangeText.text = "Range: $radius km"
                loadCurrentAQI()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
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

    private fun shouldUpdateLocation(newLat: Double, newLon: Double): Boolean {
        return currentLat == null || currentLon == null ||
                calculateDistance(newLat, newLon, currentLat!!, currentLon!!) > 0.1
    }

    private fun updateMapCamera(point: Point) {
        mapView.mapboxMap.setCamera(CameraOptions.Builder().center(point).zoom(12.0).build())
        mapView.gestures.focalPoint = mapView.mapboxMap.pixelForCoordinate(point)
    }

    @SuppressLint("SetTextI18n")
    private fun updateCityName(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            val city = address?.locality ?: "Unknown"
            val state = address?.adminArea ?: "Unknown"
            val normalizedState = CityModelMapper.normalizeStateName(state)
            tvCityName.text = "You are in: $city, $state"
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

    private fun fetchNearbyAQI(radius: Int, features: MutableList<Feature>) {
        val lat = currentLat ?: return
        val lon = currentLon ?: return
        Log.d("FetchAQI", "Fetching AQI for lat=$lat, lon=$lon, radius=$radius")
        RetrofitClient.instance.getNearbyAQI(lat, lon, radius)
            .enqueue(object : Callback<NearbyAQIResponse> {
                override fun onResponse(
                    call: Call<NearbyAQIResponse>,
                    response: Response<NearbyAQIResponse>
                ) {
                    if (response.isSuccessful) {
                        Log.d("NearbyAQI", "Server response: ${response.body()}")
                        response.body()?.data?.let { data ->
                            val nearbyFeatures = data.map {
                                Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)).apply {
                                    addNumberProperty("aqi", it.AQI)
                                }
                            }
                            features.addAll(nearbyFeatures)
                        }
                    } else {
                        Log.e("NearbyAQI", "Server error: ${response.code()}")
                    }
                }
                override fun onFailure(call: Call<NearbyAQIResponse>, t: Throwable) {
                    Log.e("NearbyAQI", "Network error", t)
                }
            })
    }

    private fun predictAirQuality(city: String, state: String, lat: Double, lon: Double) {
        val request = PredictRequest(city, state, lat, lon)

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
        response.predictions?.firstOrNull()?.let { prediction ->
            showAqiPrediction(prediction.AQI, lat, lon)
        } ?: showToast("No prediction data available")
    }

    private fun handleUnsupportedLocation(response: PredictResponse?) {
        val message = response?.message ?: "Location not supported"
        val supportedCities = response?.supportedCities?.joinToString(", ") ?: ""
        showToast("$message. Supported cities: $supportedCities")
    }

    private fun handlePredictionError(error: String?) {
        showToast("Prediction failed: ${error ?: "Unknown error"}")
    }

    private fun showAqiPrediction(aqi: Int, lat: Double, lon: Double) {
        val quality = when {
            aqi < 50 -> "Good"
            aqi < 100 -> "Moderate"
            aqi < 200 -> "Unhealthy"
            else -> "Hazardous"
        }

        showToast("Air Quality: $quality (AQI: $aqi)")
        highlightAqiOnMap(listOf(
            Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                addNumberProperty("aqi", aqi)
            }
        ))
    }

    private fun highlightAqiOnMap(features: List<Feature>) {
        mapView.mapboxMap.getStyle { style ->
            val sourceId = "aqi-source"
            val circleLayerId = "aqi-circle-layer"
            val labelLayerId = "aqi-label-layer"
            val collection = FeatureCollection.fromFeatures(features)

            val firstFeature = features.firstOrNull()
            val point = firstFeature?.geometry() as? Point

            if (point != null) {
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)  // Adjust zoom to focus on prediction point
                        .build()
                )
            }

            // Add GeoJson source if not already present
            if (style.getSource(sourceId) == null) {
                style.addSource(geoJsonSource(sourceId) {
                    featureCollection(collection)
                })

                // AQI Circle Layer
                style.addLayer(
                    circleLayer(circleLayerId, sourceId) {
                        circleRadius(8.0)
                        circleColor(
                            interpolate {
                                linear()
                                get { literal("aqi") }
                                stop { literal(0); rgb(0.0, 255.0, 0.0) }     // Green
                                stop { literal(100); rgb(255.0, 255.0, 0.0) } // Yellow
                                stop { literal(200); rgb(255.0, 0.0, 0.0) }   // Red
                            }
                        )
                        circleOpacity(0.8)
                    }
                )

                // AQI Text Label Layer
                style.addLayer(
                    symbolLayer(labelLayerId, sourceId) {
                        textField(
                            concat {
                                literal("AQI: ")
                                toString {
                                    get { literal("aqi") }
                                }
                            }
                        )
                        textSize(13.0)
                        textColor("#000000")
                        textHaloColor("#FFFFFF")
                        textHaloWidth(1.2)
                        textOffset(listOf(0.0, 1.5)) // Position label above the point
                        textAnchor(TextAnchor.TOP)
                    }
                )
            } else {
                (style.getSource(sourceId) as? GeoJsonSource)?.featureCollection(collection)
            }
        }
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

    private fun runModel(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val model = Model.newInstance(requireContext())
                val input = preprocessImage(bitmap)
                val output = model.process(input)
                val results = output.outputFeature0AsTensorBuffer.floatArray
                model.close()

                val (maxIndex, confidence) = results.getTopPrediction()
                withContext(Dispatchers.Main) {
                    showToast("Prediction: ${labels.getOrElse(maxIndex) { "Unknown" }} (${"%.1f".format(confidence * 100)}%)")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.message}")
                }
            }
        }
    }

    private fun preprocessImage(bitmap: Bitmap): TensorBuffer {
        val resized = bitmap.scale(224, 224)
        val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 3, 224, 224), DataType.FLOAT32)
        val floatArray = FloatArray(224 * 224 * 3)

        val pixels = IntArray(224 * 224)
        resized.getPixels(pixels, 0, 224, 0, 0, 224, 224)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            floatArray[i * 3] = ((pixel shr 16) and 0xFF) / 255.0f
            floatArray[i * 3 + 1] = ((pixel shr 8) and 0xFF) / 255.0f
            floatArray[i * 3 + 2] = (pixel and 0xFF) / 255.0f
        }

        inputBuffer.loadArray(floatArray)
        return inputBuffer
    }

    private fun FloatArray.getTopPrediction(): Pair<Int, Float> {
        var maxIndex = 0
        var maxValue = this[0]
        for (i in 1 until size) {
            if (this[i] > maxValue) {
                maxValue = this[i]
                maxIndex = i
            }
        }
        return Pair(maxIndex, maxValue)
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
                location?.let {
                    currentLat = it.latitude
                    currentLon = it.longitude
                    updateCityName(it.latitude, it.longitude)
                } ?: showToast("Could not get location")
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Failed to get location", e)
                showToast("Location unavailable")
            }
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), message, duration).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
    }
}