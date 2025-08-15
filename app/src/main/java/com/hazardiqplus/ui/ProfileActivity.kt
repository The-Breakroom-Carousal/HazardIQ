package com.hazardiqplus.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    // Views
    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileLocation: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAddress: TextView
    private lateinit var btnLogout: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearTop)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearFull)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()

        // Bind Views
        profileImage = findViewById(R.id.profile_image)
        profileName = findViewById(R.id.profile_name)
        profileLocation = findViewById(R.id.profile_location)
        tvRole = findViewById(R.id.tvRole)
        tvEmail = findViewById(R.id.tvEmail)
        tvAddress = findViewById(R.id.tvAddress)
        btnLogout = findViewById(R.id.btnLogout)

        // Fetch profile data
        fetchUserProfile()

        // Logout action
        btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun fetchUserProfile() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        user.getIdToken(true).addOnSuccessListener { result ->
            val idToken = result.token
            if (idToken != null) {
                val call = RetrofitClient.backendInstance.getUserDetails(idToken)

                call.enqueue(object : Callback<User> {
                    override fun onResponse(call: Call<User>, response: Response<User>) {
                        if (response.isSuccessful) {
                            val userProfile = response.body()
                            if (userProfile != null) {
                                updateUI(userProfile)
                            } else {
                                Toast.makeText(this@ProfileActivity, "Profile data not found.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("ProfileActivity", "Server error: ${response.code()}")
                            Toast.makeText(this@ProfileActivity, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<User>, t: Throwable) {
                        Log.e("ProfileActivity", "Network error: ${t.message}")
                        Toast.makeText(this@ProfileActivity, "Network error.", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }.addOnFailureListener { e ->
            Log.e("ProfileActivity", "Failed to get ID token", e)
            Toast.makeText(this, "Authentication error.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(user: User) {
        val geocoder = Geocoder(this , Locale.getDefault())
        val address = geocoder.getFromLocation(user.location_lat, user.location_lng, 1)?.firstOrNull()
        if (address != null) {
            val street = address.thoroughfare ?: "Unknown Street"
            val locality = address.locality ?: "Unknown Locality"
            val subLocality = address.subLocality ?: "Unknown Area"
            val city = address.subAdminArea ?: "Unknown City"
            val state = address.adminArea ?: "Unknown State"

            val sb = StringBuilder()
            if (street != "Unknown Street") {
                sb.append("$street, ")
            }
            if (subLocality != "Unknown Area") {
                sb.append("$subLocality, ")
            }
            if (locality != "Unknown Locality") {
                sb.append(locality)
            }
            tvAddress.text = sb.toString()
            profileLocation.text = "$city, $state"
        }
        profileName.text = user.name
        tvRole.text = user.role.capitalize(Locale.ROOT)
        tvEmail.text = user.email
    }

    private fun logoutUser() {
        firebaseAuth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}