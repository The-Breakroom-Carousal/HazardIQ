package com.hazardiqplus.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.gms.location.*
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.UserRegisterRequest
import com.hazardiqplus.data.UserRegisterResponse
import com.hazardiqplus.ui.citizen.CitizenMainActivity
import com.hazardiqplus.ui.responder.ResponderMainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var roleDropdown: AutoCompleteTextView
    private lateinit var continueButton: Button
    private lateinit var firstName: TextInputEditText
    private lateinit var lastName: TextInputEditText
    private lateinit var emailInput: TextInputEditText

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLat: Double? = null
    private var currentLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        roleDropdown = findViewById(R.id.roleDropdown)
        continueButton = findViewById(R.id.continueButton)
        firstName = findViewById(R.id.firstName)
        lastName = findViewById(R.id.lastName)
        emailInput = findViewById(R.id.emailInput)

        val user = FirebaseAuth.getInstance().currentUser
        emailInput.setText(user?.email)
        emailInput.isEnabled = false

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()

        val roles = listOf("citizen", "responder", "admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        roleDropdown.setAdapter(adapter)

        continueButton.setOnClickListener {
            registerUserToBackend()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchLocation()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) fetchLocation()
        else Toast.makeText(this, "⚠️ Location permission denied", Toast.LENGTH_SHORT).show()
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLat = it.latitude
                    currentLng = it.longitude
                }
            }
        }
    }

    private fun registerUserToBackend() {
        val name = "${firstName.text} ${lastName.text}".trim()
        val role = roleDropdown.text.toString().lowercase(Locale.ROOT)
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return

        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                ?.addOnSuccessListener { tokenResult ->
                    val idToken = tokenResult.token ?: return@addOnSuccessListener

                    val request = UserRegisterRequest(
                        idToken = idToken,
                        name = name,
                        email = email,
                        role = role,
                        fcm_token = fcmToken,
                        location_lat = currentLat,
                        location_lng = currentLng
                    )

                    RetrofitClient.instance.registerUser(request)
                        .enqueue(object : Callback<UserRegisterResponse> {
                            override fun onResponse(
                                call: Call<UserRegisterResponse>,
                                response: Response<UserRegisterResponse>
                            ) {
                                if (response.isSuccessful && response.body()?.success == true) {
                                    Toast.makeText(
                                        this@RoleSelectionActivity,
                                        "✅ Registered",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    val roleFromResponse = response.body()?.user?.role
                                    val next = when (roleFromResponse) {
                                        "citizen" -> CitizenMainActivity::class.java
                                        "responder" -> ResponderMainActivity::class.java
                                        else -> null
                                    }
                                    next?.let {
                                        startActivity(Intent(this@RoleSelectionActivity, it))
                                        finish()
                                    }
                                } else {
                                    Toast.makeText(
                                        this@RoleSelectionActivity,
                                        "⚠️ Backend registration failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onFailure(call: Call<UserRegisterResponse>, t: Throwable) {
                                Toast.makeText(
                                    this@RoleSelectionActivity,
                                    "❌ Network error",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
        }
    }
}
