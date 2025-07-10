package com.hazardiqplus.ui

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.User
import com.hazardiqplus.ui.citizen.CitizenMainActivity
import com.hazardiqplus.ui.responder.ResponderMainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Splash_Screen : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        val handler = android.os.Handler(Looper.getMainLooper())

        handler.postDelayed( {
            firebaseAuth = FirebaseAuth.getInstance()
            if (firebaseAuth.currentUser != null) {
                firebaseAuth.currentUser?.getIdToken(true)
                    ?.addOnSuccessListener { result ->
                        val token = result.token
                        if (token != null) {
                            checkUserRoleAndRedirect(token)
                        }
                    }
            }

            else{
                startActivity(Intent(this, LoginSignupActivity::class.java))
                finish()
            }

        },3000)

    }
    private fun checkUserRoleAndRedirect(idToken: String) {
        val call = RetrofitClient.instance.getUserDetails(idToken)
        call.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {

                Log.d("DEBUG", "Response JSON: ${response.body()}")
                val role = response.body()?.role
                Toast.makeText(this@Splash_Screen, "Role: $role", Toast.LENGTH_SHORT).show()
                when (role?.lowercase()) {
                    "citizen" -> startActivity(Intent(this@Splash_Screen, CitizenMainActivity::class.java))
                    "responder" -> startActivity(Intent(this@Splash_Screen, ResponderMainActivity::class.java))
                    "admin" -> Toast.makeText(this@Splash_Screen, "Admin not implemented", Toast.LENGTH_SHORT).show()
                    else -> startActivity(Intent(this@Splash_Screen, RoleSelectionActivity::class.java))
                }

                finish()
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                Toast.makeText(this@Splash_Screen, "Error verifying role", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@Splash_Screen, RoleSelectionActivity::class.java))
                finish()
            }
        })
    }





}
