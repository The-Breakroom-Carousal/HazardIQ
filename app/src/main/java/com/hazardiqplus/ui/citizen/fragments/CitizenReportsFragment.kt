package com.hazardiqplus.ui.citizen.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.hazardiqplus.R
import com.hazardiqplus.adapters.ReportsAdapter
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.FindHazardResponse
import com.hazardiqplus.data.Hazard
import com.hazardiqplus.data.SaveHazardRequest
import com.hazardiqplus.data.SaveHazardResponse
import com.hazardiqplus.ml.BestModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Locale

class CitizenReportsFragment : Fragment(R.layout.fragment_citizen_reports) {

    private lateinit var btnCameraUpload: MaterialButton
    private lateinit var tvReport: TextView
    private lateinit var btnAddReport: MaterialButton
    private lateinit var recyclerReports: RecyclerView
    private lateinit var tvNoReports: TextView
    private lateinit var reportsAdapter: ReportsAdapter
    private val reportsList = mutableListOf<Hazard>()
    private var imageUri: Uri? = null
    private val labels = listOf(
        "Flood", "No Hazard", "No Hazard",
        "No Hazard", "No Hazard", "No Hazard",
        "Earthquake", "Injured People", "Urban Fire", "Wild Fire", "Landslide", "Drought"
    )
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            lifecycleScope.launch {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        requireContext().contentResolver,
                        imageUri
                    )
                    runModel(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(requireActivity(), "Failed to process image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_citizen_reports, container, false)
        btnCameraUpload = view.findViewById(R.id.btnOpenCamera)
        tvReport = view.findViewById(R.id.tvReport)
        btnAddReport = view.findViewById(R.id.btnAddReport)
        recyclerReports = view.findViewById(R.id.recyclerReports)
        tvNoReports = view.findViewById(R.id.tvNoReports)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reportsAdapter = ReportsAdapter(requireContext(), mutableListOf())
        recyclerReports.layoutManager = LinearLayoutManager(requireContext())
        recyclerReports.adapter = reportsAdapter

        btnCameraUpload.setOnClickListener {
            checkCameraPermission()
        }

        fetchReports()
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
                Toast.makeText(context, "Camera permission is needed for hazard detection.", Toast.LENGTH_SHORT).show()
                requestCameraPermission()
            }
            else -> {
                requestCameraPermission()
            }
        }
    }

    private fun fetchReports() {
        lifecycleScope.launch {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@launch
            }

            LocationServices.getFusedLocationProviderClient(requireContext())
                .lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val lat = location.latitude
                        val lon = location.longitude
                        RetrofitClient.instance.findHazard(lat, lon, 2000)
                            .enqueue(object : Callback<FindHazardResponse> {
                                override fun onResponse(
                                    call: Call<FindHazardResponse>,
                                    response: Response<FindHazardResponse>
                                ) {
                                    if (response.isSuccessful && response.body() != null) {
                                        val hazards = response.body()!!.data
                                        reportsList.clear()
                                        reportsList.addAll(hazards)
                                        reportsAdapter.updateReports(reportsList)

                                        if (reportsList.isEmpty()) {
                                            tvNoReports.visibility = View.VISIBLE
                                            recyclerReports.visibility = View.GONE
                                        } else {
                                            tvNoReports.visibility = View.GONE
                                            recyclerReports.visibility = View.VISIBLE
                                        }
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to load reports", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<FindHazardResponse>, t: Throwable) {
                                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                }
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            101
        )
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
            Toast.makeText(requireActivity(), "Failed to open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTempImageFile(): File {
        return File.createTempFile(
            "camera_img_${System.currentTimeMillis()}",
            ".jpg",
            requireContext().cacheDir
        ).apply { deleteOnExit() }
    }

    private fun runModel(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val model = BestModel.newInstance(requireContext())
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
                val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
                val std = floatArrayOf(0.229f, 0.224f, 0.225f)

                val chw = FloatArray(1 * 3 * 224 * 224)
                var idx = 0
                for (c in 0 until 3) {
                    for (y in 0 until 224) {
                        for (x in 0 until 224) {
                            val pixel = resized[x, y]
                            val value = when (c) {
                                0 -> (Color.red(pixel) / 255.0f - mean[0]) / std[0]
                                1 -> (Color.green(pixel) / 255.0f - mean[1]) / std[1]
                                else -> (Color.blue(pixel) / 255.0f - mean[2]) / std[2]
                            }
                            chw[idx++] = value
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
                    val prediction = labels.getOrElse(predictedIndex) { "Unknown" }
                    if (prediction == "No Hazard" || prediction == "Unknown") {
                        Snackbar.make(requireView(), "No hazard detected.", Snackbar.LENGTH_SHORT).show()
                    } else {
                        val textInputView = View.inflate(
                            requireContext(),
                            R.layout.dialouge_text_input,
                            null
                        )
                        val input = textInputView.findViewById<TextInputEditText>(R.id.textInput)
                        input.hint = "Enter hazard Radius in km"

                        val dialog =
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("It has detected $prediction. Enter its radius of catastrophe.")
                                .setView(textInputView)
                                .setPositiveButton("Proceed", null)
                                .setNegativeButton("Cancel", null)
                                .create()

                        dialog.show()

                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val radius = input.text.toString().trim()
                            if (radius.isNotBlank()) {
                                tvReport.text = "Detected: $prediction . Radius: $radius km"
                                tvReport.visibility = View.VISIBLE
                                btnAddReport.visibility = View.VISIBLE
                                btnAddReport.setOnClickListener {
                                    registerHazard(
                                        radius.toDouble(),
                                        labels.getOrElse(predictedIndex) { "Unknown" })
                                }
                                dialog.dismiss()
                            } else {
                                input.error = "Enter a valid radius"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun registerHazard(radius: Double, hazard: String) {
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
                    val request = SaveHazardRequest(radius, location.latitude, location.longitude, hazard)

                    RetrofitClient.instance.registerHazard(request)
                        .enqueue(object : Callback<SaveHazardResponse> {
                            override fun onResponse(
                                call: Call<SaveHazardResponse>,
                                response: Response<SaveHazardResponse>
                            ) {
                                if (response.isSuccessful && response.body()?.success == true) {
                                    btnAddReport.visibility = View.GONE
                                    tvReport.visibility = View.GONE
                                    Snackbar.make(requireView(), "Hazard registered successfully.", Snackbar.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(
                                call: Call<SaveHazardResponse?>,
                                t: Throwable
                            ) {

                                btnAddReport.visibility = View.GONE
                                tvReport.visibility = View.GONE
                                Log.e("Hazard", "Failed to register hazard", t)
                                Snackbar.make(requireView(), "Failed to register hazard. Please try again!", Snackbar.LENGTH_SHORT).show()
                            }
                        })
                } else {

                    btnAddReport.visibility = View.GONE
                    tvReport.visibility = View.GONE
                    Toast.makeText(requireContext(), "Could not get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                btnAddReport.visibility = View.GONE
                tvReport.visibility = View.GONE
                Log.e("Location", "Failed to get location", e)
                Toast.makeText(requireContext(), "Location unavailable", Toast.LENGTH_SHORT).show()
            }
    }
}