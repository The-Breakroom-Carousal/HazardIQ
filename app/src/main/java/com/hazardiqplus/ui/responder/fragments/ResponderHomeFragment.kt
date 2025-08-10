package com.hazardiqplus.ui.responder.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
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
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.hazardiqplus.R
import com.hazardiqplus.adapters.SosRequestAdapter
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.SosEvent
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

class ResponderHomeFragment : Fragment(R.layout.fragment_responder_home),
    SosRequestAdapter.SosActionListener {

    private lateinit var adapter: SosRequestAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerRequests: RecyclerView
    private lateinit var tvLocation: TextView
    private lateinit var progressCircular: CircularProgressIndicator
    private val allRequests = mutableListOf<SosEvent>()
    private val pendingRequests = mutableListOf<SosEvent>()
    private val declinedRequests = mutableListOf<SosEvent>()
    private val acceptedRequests = mutableListOf<SosEvent>()
    private lateinit var tvNoList: TextView
    private var currentLocation: String = "Unknown"
    private var fetchJob: Job? = null
    private var firstLoad = true

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
        progressCircular = view.findViewById(R.id.progressCircular)
        tvNoList = view.findViewById(R.id.tvNoList)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout.addTab(tabLayout.newTab().setText("Pending")
            .setIcon(R.drawable.round_access_time_24))
        tabLayout.addTab(tabLayout.newTab().setText("Accepted")
            .setIcon(R.drawable.round_task_alt_24))
        tabLayout.addTab(tabLayout.newTab().setText("All Requests")
            .setIcon(R.drawable.round_format_list_bulleted_24))

        recyclerRequests.layoutManager = LinearLayoutManager(requireContext())
        adapter = SosRequestAdapter(this@ResponderHomeFragment, requireContext())
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

    override fun onResume() {
        super.onResume()
        fetchJob = lifecycleScope.launch {
            while (isActive) {
                fetchSosRequests(currentLocation)
                delay(10_000)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fetchJob?.cancel()
    }

    private fun updateRecyclerForTab(position: Int) {
        val dataToShow = when (position) {
            0 -> pendingRequests
            1 -> acceptedRequests
            2 -> allRequests
            else -> allRequests
        }
        checkLists(dataToShow)
        adapter.updateData(dataToShow)
    }

    private fun fetchSosRequests(city: String) {
        if (firstLoad) progressCircular.visibility = View.VISIBLE
        RetrofitClient.instance.getSosEvents(city)
            .enqueue(object : Callback<List<SosEvent>> {
                override fun onResponse(
                    call: Call<List<SosEvent>>,
                    response: Response<List<SosEvent>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val responseData = response.body()!!

                        allRequests.clear()
                        allRequests.addAll(responseData.filter { it.progress == "pending" || it.progress == "acknowledged" })

                        pendingRequests.clear()
                        pendingRequests.addAll(responseData.filter { it.progress == "pending" })

                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        acceptedRequests.clear()
                        acceptedRequests.addAll(responseData.filter { it.progress == "acknowledged" && it.responder_uid == uid })

                        tabLayout.getTabAt(0)?.text = "Pending (${pendingRequests.size})"
                        tabLayout.getTabAt(1)?.text = "Accepted (${acceptedRequests.size})"
                        tabLayout.getTabAt(2)?.text = "All Requests (${allRequests.size})"

                        progressCircular.animate()
                            .alpha(0f)
                            .setDuration(500)
                            .withEndAction {
                                progressCircular.visibility = View.GONE
                            }
                            .start()
                        firstLoad = false

                        updateRecyclerForTab(tabLayout.selectedTabPosition)
                    } else {
                        progressCircular.visibility = View.GONE
                        Toast.makeText(requireContext(), "Failed to fetch requests", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<SosEvent>>, t: Throwable) {
                    progressCircular.visibility = View.GONE
                    Toast.makeText(requireContext(), "Network Error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onAccept(event: SosEvent) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                val token = result.token
                if (token != null) {
                    val request = UpdateProgressRequest(progress = "acknowledged", token)
                    RetrofitClient.instance.updateSosProgress(event.id, request)
                        .enqueue(object : Callback<UpdateProgressResponse> {
                            override fun onResponse(
                                call: Call<UpdateProgressResponse>,
                                response: Response<UpdateProgressResponse>
                            ) {
                                if (response.isSuccessful && response.body()?.message != null) {
                                    Toast.makeText(requireContext(), "Request Accepted", Toast.LENGTH_SHORT).show()

                                    pendingRequests.remove(event)

                                    updateRecyclerForTab(tabLayout.selectedTabPosition)
                                } else {
                                    Log.e("ResponderHomeFragment", "Failed to accept request: ${response.errorBody()?.string()}")
                                    Toast.makeText(requireContext(), "Failed to accept request", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<UpdateProgressResponse>, t: Throwable) {
                                Toast.makeText(requireContext(), "Network Error", Toast.LENGTH_SHORT).show()
                            }
                        })
                }
            }
    }

    override fun onDecline(event: SosEvent) {
        Toast.makeText(requireContext(), "Request Declined", Toast.LENGTH_SHORT).show()
        pendingRequests.remove(event)
        declinedRequests.add(event)
        updateRecyclerForTab(tabLayout.selectedTabPosition)
    }

    override fun onMarkAsCompleted(event: SosEvent) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                val token = result.token
                if (token != null) {
                    val request = UpdateProgressRequest(progress = "resolved", token)
                    RetrofitClient.instance.updateSosProgress(event.id, request)
                        .enqueue(object : Callback<UpdateProgressResponse> {
                            override fun onResponse(
                                call: Call<UpdateProgressResponse>,
                                response: Response<UpdateProgressResponse>
                            ) {
                                if (response.isSuccessful && response.body()?.message != null) {
                                    Toast.makeText(requireContext(), "SOS Request Resolved", Toast.LENGTH_SHORT).show()

                                    pendingRequests.remove(event)

                                    updateRecyclerForTab(tabLayout.selectedTabPosition)
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
        acceptedRequests.remove(event)
        allRequests.remove(event)
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
                    try {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
                        val city = address?.locality ?: "Unknown"
                        currentLocation = city
                        tvLocation.text = city
                        fetchSosRequests(city)
                    } catch (e: Exception) {
                        Log.e("Location", "Failed to get city name", e)
                        Toast.makeText(requireContext(), "Failed to get city name", Toast.LENGTH_SHORT).show()
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

    @SuppressLint("SetTextI18n")
    fun checkLists(typeOfList: MutableList<SosEvent>) {
        if (typeOfList.isEmpty()) {
            tvNoList.text = "No such requests found"
            tvNoList.visibility = View.VISIBLE
        } else {
            tvNoList.visibility = View.GONE
        }
    }
}