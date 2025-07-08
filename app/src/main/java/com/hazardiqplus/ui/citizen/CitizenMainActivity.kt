package com.hazardiqplus.ui.citizen

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.hazardiqplus.R
import com.hazardiqplus.data.RetrofitClient
import com.hazardiqplus.data.SosRequest
import com.hazardiqplus.data.SosResponse
import com.hazardiqplus.ui.citizen.ui.home.CitizenHomeFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class CitizenMainActivity : AppCompatActivity() {

    private lateinit var btnSOS: FloatingActionButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_citizen_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.citizenMain)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, CitizenHomeFragment())
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        btnSOS = findViewById(R.id.btnSOS)

        btnSOS.setOnClickListener {
            triggerSosCall()
        }
    }

    private fun triggerSosCall() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
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
                                            this@CitizenMainActivity,
                                            "🚨 SOS Sent to ${response.body()?.sent} responders!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Log.e("SOS", "Response not successful: ${response.body().toString()}")
                                        Log.e("SOS", "Response not successful: ${response.errorBody()?.string()}")
                                        Toast.makeText(this@CitizenMainActivity, "❌ SOS Failed", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<SosResponse>, t: Throwable) {
                                    Toast.makeText(this@CitizenMainActivity, "Network error", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
            } else {
                Toast.makeText(this@CitizenMainActivity, "Location unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
