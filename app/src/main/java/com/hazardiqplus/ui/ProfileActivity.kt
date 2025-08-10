package com.hazardiqplus.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient // Import the new Retrofit client
import com.hazardiqplus.data.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var profileName: TextView
    private lateinit var profileRole: TextView
    private lateinit var profileEmail: TextView
    private lateinit var profileUid: TextView
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firebaseAuth = FirebaseAuth.getInstance()

        profileName = findViewById(R.id.profile_name)
        profileRole = findViewById(R.id.profile_role)
        profileEmail = findViewById(R.id.profile_email)
        profileUid = findViewById(R.id.profile_uid)
        logoutButton = findViewById(R.id.btn_logout)

        fetchUserProfile()

        logoutButton.setOnClickListener {
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
                // Use Retrofit to make the API call
                val call = RetrofitClient.instance.getUserDetails(idToken)

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

    private fun updateUI(user: User) {
        profileName.text = user.name
        profileRole.text = user.role.capitalize(Locale.ROOT)
        profileEmail.text = user.email
        profileUid.text = user.firebase_uid

    }

    private fun logoutUser() {
        firebaseAuth.signOut()
        val intent = Intent(this, LoginSignupActivity::class.java) // Replace with your login activity
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}