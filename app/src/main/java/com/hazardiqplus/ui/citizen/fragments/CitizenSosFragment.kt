package com.hazardiqplus.ui.citizen.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.hazardiqplus.adapters.MySosAdapter
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.SosEvent
import com.hazardiqplus.data.SosRequest
import com.hazardiqplus.data.SosResponse
import com.hazardiqplus.data.UpdateProgressRequest
import com.hazardiqplus.data.UpdateProgressResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class CitizenSosFragment : Fragment(R.layout.fragment_citizen_sos) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnSos: Button
    private lateinit var tvSosBlockedMessage: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvCurrentTime: TextView
    private lateinit var mySosRecycler: RecyclerView
    private lateinit var mySosAdapter: MySosAdapter
    private lateinit var tvNoRequests: TextView
    private val mySosList = mutableListOf<SosEvent>()
    private var fetchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_citizen_sos, container, false)
        btnSos = view.findViewById(R.id.btnSos)
        tvLocation = view.findViewById(R.id.tvLocation)
        tvSosBlockedMessage = view.findViewById(R.id.tvSosBlockedMessage)
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        mySosRecycler = view.findViewById(R.id.recyclerMySos)
        tvNoRequests = view.findViewById(R.id.tvNoRequests)
        setupEmergencyContacts(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mySosAdapter = MySosAdapter(requireContext(), mySosList, object : MySosAdapter.OnActionListener {
            override fun onDelete(event: SosEvent) {
                deleteMySos(event)
            }
            override fun onMarkAsResolved(event: SosEvent) {
                markSosAsResolved(event)
            }
        })
        mySosRecycler.layoutManager = LinearLayoutManager(requireContext())
        mySosRecycler.adapter = mySosAdapter

        lifecycleScope.launch {
            getUserLocation()
        }

        btnSos.setOnClickListener {
            triggerSosCall()
        }

        if (mySosList.isEmpty()) {
            mySosRecycler.visibility = View.GONE
            tvNoRequests.visibility = View.VISIBLE
        } else {
            tvNoRequests.visibility = View.GONE
            mySosRecycler.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        fetchJob = lifecycleScope.launch {
            while (isActive) {
                getUserLocation()
                delay(1_000)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fetchJob?.cancel()
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

    private fun fetchMySosRequests(city: String) {
        Firebase.auth.currentUser?.uid?.let { uid ->
            RetrofitClient.backendInstance.getSosEvents(city)
                .enqueue(object : Callback<List<SosEvent>> {
                    override fun onResponse(call: Call<List<SosEvent>>, response: Response<List<SosEvent>>) {
                        if (response.isSuccessful && response.body() != null) {
                            mySosList.clear()
                            val activeRequests = response.body()!!.filter {
                                it.firebase_uid == uid && (it.progress == "pending" || it.progress == "acknowledged")
                            }
                            mySosList.addAll(activeRequests)
                            mySosAdapter.notifyDataSetChanged()

                            // Show/Hide empty state
                            if (mySosList.isEmpty()) {
                                mySosRecycler.visibility = View.GONE
                                tvNoRequests.visibility = View.VISIBLE
                            } else {
                                tvNoRequests.visibility = View.GONE
                                mySosRecycler.visibility = View.VISIBLE
                            }

                            // Toggle SOS button based on active requests
                            if (activeRequests.isNotEmpty()) {
                                btnSos.visibility = View.GONE
                                tvSosBlockedMessage.visibility = View.VISIBLE
                            } else {
                                btnSos.visibility = View.VISIBLE
                                tvSosBlockedMessage.visibility = View.GONE
                            }

                        } else {
                            Toast.makeText(requireContext(), "Couldn't fetch your requests", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<List<SosEvent>>, t: Throwable) {
                        Toast.makeText(requireContext(), "Network Error", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun deleteMySos(event: SosEvent) {
        RetrofitClient.backendInstance.deleteSosEvent(event.id)
            .enqueue(object : Callback<com.hazardiqplus.data.DeleteSosResponse> {
                override fun onResponse(
                    call: Call<com.hazardiqplus.data.DeleteSosResponse>,
                    response: Response<com.hazardiqplus.data.DeleteSosResponse>
                ) {
                    if (response.isSuccessful && response.body()?.message != null) {
                        mySosList.remove(event)
                        mySosAdapter.notifyDataSetChanged()
                        Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<com.hazardiqplus.data.DeleteSosResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun markSosAsResolved(event: SosEvent) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                val token = result.token
                if (token != null) {
                    val request = UpdateProgressRequest(progress = "resolved", token)
                    RetrofitClient.backendInstance.updateSosProgress(event.id, request)
                        .enqueue(object : Callback<UpdateProgressResponse> {
                            override fun onResponse(
                                call: Call<UpdateProgressResponse>,
                                response: Response<UpdateProgressResponse>
                            ) {
                                if (response.isSuccessful && response.body()?.message != null) {
                                    Toast.makeText(requireContext(), "SOS Request Resolved", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.e("ResponderHomeFragment", "Failed to resolve request: ${response.errorBody()?.string()}")
                                    Toast.makeText(requireContext(), "Failed to resolve request", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<UpdateProgressResponse>, t: Throwable) {
                                Toast.makeText(requireContext(), "Network Error", Toast.LENGTH_SHORT).show()
                            }
                        })
                }
            }
    }

    private fun triggerSosCall() {
        if (mySosList.size == 1) {

        }
        val emergencyTypes = arrayOf("Medical Emergency", "Fire", "Police", "Disaster Management")
        var selectedIndex = 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Emergency Type")
            .setSingleChoiceItems(emergencyTypes, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Send SOS") { dialog, _ ->
                dialog.dismiss()
                sendSos(emergencyTypes[selectedIndex])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun sendSos(type: String) {
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
                            type = type,
                            city = city,
                            lat = location.latitude,
                            lng = location.longitude
                        )
                        RetrofitClient.backendInstance.sendSosAlert(request)
                            .enqueue(object : Callback<SosResponse> {
                                override fun onResponse(
                                    call: Call<SosResponse>,
                                    response: Response<SosResponse>
                                ) {
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        Toast.makeText(
                                            requireContext(),
                                            "🚨 SOS Sent: $type to ${response.body()?.sent} responders!",
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
                    try {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
                        val city = address?.locality ?: "Unknown"
                        val state = address?.adminArea ?: "Unknown"
                        tvLocation.text = "Location: $city, $state"
                        tvCurrentTime.text = "Current time: ${java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())}"
                        fetchMySosRequests(city)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Failed to get location", e)
                Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show()
            }
    }
}