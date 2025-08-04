package com.hazardiqplus.ui.citizen.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.hazardiqplus.R
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.SosRequest
import com.hazardiqplus.data.SosResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class SosFragment : Fragment(R.layout.fragment_sos) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnSos: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sos, container, false)
        btnSos = view.findViewById(R.id.btnSos)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupEmergencyContacts(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSos.setOnClickListener {
            triggerSosCall()
        }
    }

    private fun setupEmergencyContacts(view: View) {
        setupContact(
            view.findViewById(R.id.contactFire),
            name = "Fire Department",
            number = "101",
            iconRes = R.drawable.fire_truck
        )

        setupContact(
            view.findViewById(R.id.contactPolice),
            name = "Police",
            number = "100",
            iconRes = R.drawable.police_car
        )

        setupContact(
            view.findViewById(R.id.contactMedical),
            name = "Medical Emergency",
            number = "108",
            iconRes = R.drawable.ambulance
        )

        setupContact(
            view.findViewById(R.id.contactDisaster),
            name = "Disaster Management",
            number = "1077",
            iconRes = R.drawable.warning_icon
        )
    }

    private fun setupContact(view: View, name: String, number: String, iconRes: Int) {
        view.findViewById<TextView>(R.id.tvName).text = name
        view.findViewById<TextView>(R.id.tvNumber).text = number
        view.findViewById<ImageView>(R.id.imgIcon).setImageResource(iconRes)

        view.findViewById<ImageView>(R.id.btnCall).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = "tel:$number".toUri()
            }
            startActivity(intent)
        }
    }

    private fun triggerSosCall() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
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
                                            requireContext(),
                                            "🚨 SOS Sent to ${response.body()?.sent} responders!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Log.e("SOS", "Response not successful: ${response.body().toString()}")
                                        Log.e("SOS", "Response not successful: ${response.errorBody()?.string()}")
                                        Toast.makeText(requireContext(), "❌ SOS Failed", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<SosResponse>, t: Throwable) {
                                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
            } else {
                Toast.makeText(requireContext(), "Location unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }
}