package com.hazardiqplus.ui.citizen.fragments.home

import android.Manifest
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
import kotlin.math.pow
import android.widget.Button
import android.widget.SeekBar
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
import com.hazardiqplus.R
import com.hazardiqplus.clients.CityModelMapper
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.NearbyAQIResponse
import com.hazardiqplus.data.PredictRequest
import com.hazardiqplus.data.PredictResponse
import com.hazardiqplus.ml.Model
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import kotlin.math.sin as kSin
import kotlin.math.cos as kCos
import kotlin.math.sqrt as kSqrt
import kotlin.math.atan2 as kAtan2
import kotlin.math.pow as kPow
import com.mapbox.maps.extension.style.expressions.dsl.generated.*
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
import java.util.*
import kotlin.math.atan2

class CitizenHomeFragment : Fragment(R.layout.fragment_citizen_home) {

    // UI Components
    private lateinit var tvCityName: TextView
    private lateinit var mapView: MapView
    private lateinit var btnSafeRoutes: Button
    private lateinit var hazardDetector: Button
    private lateinit var rangeSeekBar: SeekBar
    private lateinit var rangeText: TextView

    // Camera and permissions
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var imageUri: Uri? = null

    // Location tracking
    private var currentLat: Double? = null
    private var currentLon: Double? = null

    // ML Model labels
    private val labels = listOf(
        "Water_Disaster", "Non_Damaged_Wildlife_Forest", "Non_Damaged_sea",
        "Non_Damaged_Buildings_Street", "Non_Damaged_human", "Damaged_Infrastructure",
        "Earthquake", "Human_Damage", "Urban_Fire", "Wild_Fire", "Land_Slide", "Drought"
    )

    // Permission requests
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA
    )

    // Location permission callback
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            getUserLocation()
        } else {
            showToast("Location permission is required for full functionality")
        }
    }

    // Location change listener
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val lat = point.latitude()
        val lon = point.longitude()

        if (shouldUpdateLocation(lat, lon)) {
            currentLat = lat
            currentLon = lon

            updateMapCamera(point)
            updateCityName(lat, lon)
            fetchNearbyAQI(rangeSeekBar.progress)
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
        setupMap()
        setupRangeSeekBar()
        checkLocationPermission()
        return view
    }

    private fun initViews(view: View) {
        tvCityName = view.findViewById(R.id.tvCityName)
        mapView = view.findViewById(R.id.mapView)
        btnSafeRoutes = view.findViewById(R.id.btnSafeRoutes)
        hazardDetector = view.findViewById(R.id.hazard)
        rangeSeekBar = view.findViewById(R.id.seekBarRange)
        rangeText = view.findViewById(R.id.tvRange)

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
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS)
    }

    private fun setupRangeSeekBar() {
        rangeSeekBar.progress = 10
        rangeText.text = "Range: 10 km"
        rangeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val radius = progress.coerceAtLeast(1)
                rangeText.text = "Range: $radius km"
                fetchNearbyAQI(radius)
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

    private fun updateCityName(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            val city = address?.locality ?: "Unknown"
            val state = address?.adminArea ?: "Unknown"
            val normalizedState = CityModelMapper.normalizeStateName(state)
            tvCityName.text = "You are in: $city, $state"

            // Get nearest supported city and make prediction
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

            predictAirQuality(modelCity, normalizedState, lat, lon)

        } catch (e: Exception) {
            Log.e("CitizenHomeFragment", "Geocoder failed", e)
        }
    }

    private fun fetchNearbyAQI(radius: Int) {
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
                        response.body()?.data?.let { data ->
                            val features = data.map {
                                Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)).apply {
                                    addNumberProperty("aqi", it.AQI)
                                }
                            }
                            highlightAqiOnMap(features)
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
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
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