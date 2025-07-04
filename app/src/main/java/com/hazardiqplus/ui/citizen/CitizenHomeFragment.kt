package com.hazardiqplus.ui.citizen
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.hazardiqplus.R
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.hazardiqplus.data.RetrofitClient
import com.hazardiqplus.data.SosRequest
import com.hazardiqplus.data.SosResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import java.util.Locale

class CitizenHomeFragment: Fragment(R.layout.fragment_home) {
    private lateinit var sosButton: FloatingActionButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        sosButton = view.findViewById(R.id.sosButton)
        sosButton.setOnClickListener {
            triggerSosCall()
        }

    }

    //@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun triggerSosCall() {
        val context = requireContext()
        val activity = requireActivity()

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            // You may also request permissions here using ActivityCompat.requestPermissions()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val city = addressList?.get(0)?.locality ?: "Unknown"

                Firebase.auth.currentUser?.getIdToken(true)
                    ?.addOnSuccessListener { result ->
                        val request = SosRequest(
                            idToken = result.token ?: "",
                            type = "Medical Emergency",
                            city = city,
                            lat = location.latitude,
                            lng = location.longitude
                        )

                        RetrofitClient.instance.sendSosAlert(request)
                            .enqueue(object : Callback<SosResponse> {
                                override fun onResponse(
                                    call: Call<SosResponse>,
                                    response: Response<SosResponse>
                                ) {
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        Toast.makeText(
                                            context,
                                            "🚨 SOS Sent to ${response.body()?.sent} responders!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "❌ SOS Failed", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<SosResponse>, t: Throwable) {
                                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
            } else {
                Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

}