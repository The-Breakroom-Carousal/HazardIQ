package com.hazardiqplus.ui.responder

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayout
import com.hazardiqplus.R
import com.hazardiqplus.adapters.SosRequestAdapter
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.SosEvent
import com.hazardiqplus.data.UpdateProgressRequest
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class ResponderHomeFragment : Fragment(R.layout.fragment_responder_home),
    SosRequestAdapter.SosActionListener {

    private lateinit var adapter: SosRequestAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerRequests: RecyclerView
    private lateinit var tvLocation: TextView
    private val allRequests = mutableListOf<SosEvent>()
    private val pendingRequests = mutableListOf<SosEvent>()
    private val declinedRequests = mutableListOf<SosEvent>()
    private val acceptedRequests = mutableListOf<SosEvent>()
    private var currentLocation: String = "Unknown"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_responder_home, container, false)
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerRequests = view.findViewById(R.id.recyclerRequests)
        tvLocation = view.findViewById(R.id.tvLocation)
        tvLocation.text = currentLocation
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout.addTab(tabLayout.newTab().setText("Pending"))
        tabLayout.addTab(tabLayout.newTab().setText("Accepted"))
        tabLayout.addTab(tabLayout.newTab().setText("All Requests"))

        recyclerRequests.layoutManager = LinearLayoutManager(requireContext())
        adapter = SosRequestAdapter(this@ResponderHomeFragment)
        recyclerRequests.adapter = adapter

        lifecycleScope.launch {
            getUserLocation()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateRecyclerForTab(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateRecyclerForTab(position: Int) {
        val dataToShow = when (position) {
            0 -> pendingRequests
            1 -> acceptedRequests
            2 -> allRequests
            else -> allRequests
        }
        adapter.updateData(dataToShow)
    }

    private fun fetchSosRequests(city: String) {
        RetrofitClient.instance.getSosEvents(city)
            .enqueue(object : Callback<List<SosEvent>> {
                override fun onResponse(
                    call: Call<List<SosEvent>>,
                    response: Response<List<SosEvent>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val responseData = response.body()!!

                        allRequests.clear()
                        allRequests.addAll(responseData)

                        pendingRequests.clear()
                        pendingRequests.addAll(responseData.filter { it.progress == "pending" })

                        acceptedRequests.clear()
                        acceptedRequests.addAll(responseData.filter { it.progress == "acknowledged" })

                        updateRecyclerForTab(tabLayout.selectedTabPosition)
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch requests", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<SosEvent>>, t: Throwable) {
                    Toast.makeText(requireContext(), "Network Error", Toast.LENGTH_SHORT).show()
                }
            })
    }
    override fun onAccept(event: SosEvent) {
        val request = UpdateProgressRequest(progress = "acknowledged")
        RetrofitClient.instance.updateSosProgress(event.id, request)
            .enqueue(object : Callback<com.hazardiqplus.data.UpdateProgressResponse> {
                override fun onResponse(
                    call: Call<com.hazardiqplus.data.UpdateProgressResponse>,
                    response: Response<com.hazardiqplus.data.UpdateProgressResponse>
                ) {
                    if (response.isSuccessful && response.body()?.message != null) {
                        Toast.makeText(requireContext(), "Request Accepted", Toast.LENGTH_SHORT).show()

                        // Move from pending to accepted
                        pendingRequests.remove(event)
                        val updatedEvent = event.copy(progress = "acknowledged")
                        acceptedRequests.add(updatedEvent)

                        updateRecyclerForTab(tabLayout.selectedTabPosition)
                    } else {
                        Toast.makeText(requireContext(), "Failed to accept request", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<com.hazardiqplus.data.UpdateProgressResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Network Error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onDecline(event: SosEvent) {
        Toast.makeText(requireContext(), "Request Declined", Toast.LENGTH_SHORT).show()
        pendingRequests.remove(event)
        declinedRequests.add(event)
        updateRecyclerForTab(tabLayout.selectedTabPosition)
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
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
                    val city = address?.locality ?: "Unknown"
                    currentLocation = city
                    tvLocation.text = city
                    fetchSosRequests(city)
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