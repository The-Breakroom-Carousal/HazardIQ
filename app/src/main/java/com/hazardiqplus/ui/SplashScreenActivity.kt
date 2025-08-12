package com.hazardiqplus.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition { true }
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appNameText)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed( {
            firebaseAuth = FirebaseAuth.getInstance()
            if (firebaseAuth.currentUser != null) {
                firebaseAuth.currentUser?.getIdToken(true)
                    ?.addOnSuccessListener { result ->
                        val token = result.token
                        if (token != null) {
                            Log.d("Token", "Token: $token")
                            checkUserRoleAndRedirect(token)
                        }
                    }
            } else{
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
                Toast.makeText(this@SplashScreenActivity, "Role: $role", Toast.LENGTH_SHORT).show()
                when (role?.lowercase()) {
                    "citizen" -> startActivity(Intent(this@SplashScreenActivity, CitizenMainActivity::class.java))
                    "responder" -> startActivity(Intent(this@SplashScreenActivity, ResponderMainActivity::class.java))
                    else -> startActivity(Intent(this@SplashScreenActivity, SignUpActivity::class.java))
                }

                finish()
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                Toast.makeText(this@SplashScreenActivity, "Error verifying role", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@SplashScreenActivity, SignUpActivity::class.java))
                finish()
            }
        })
    }





}
