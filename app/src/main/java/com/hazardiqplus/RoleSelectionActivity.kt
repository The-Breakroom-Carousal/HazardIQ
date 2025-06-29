package com.hazardiqplus

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.messaging.messaging
import com.google.protobuf.Api
import com.hazardiqplus.data.RetrofitClient
import com.hazardiqplus.data.UserRegisterRequest
import com.hazardiqplus.data.UserRegisterResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var roleDropdown: AutoCompleteTextView
    private lateinit var continueButton: Button
    private lateinit var firstName: TextInputEditText
    private lateinit var lastName: TextInputEditText
    private lateinit var emailInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        roleDropdown = findViewById(R.id.roleDropdown)
        continueButton = findViewById(R.id.continueButton)
        firstName = findViewById(R.id.firstName)
        lastName = findViewById(R.id.lastName)
        emailInput = findViewById(R.id.emailInput)

        // Auto-fill email from Firebase
        val user = Firebase.auth.currentUser
        emailInput.setText(user?.email)
        emailInput.isEnabled = false

        val roles = listOf("citizen", "responder", "admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        roleDropdown.setAdapter(adapter)

        continueButton.setOnClickListener {
            registerUserToBackend()
        }
    }

    private fun registerUserToBackend() {
        val name = "${firstName.text} ${lastName.text}".trim()
        val role = roleDropdown.text.toString().lowercase(Locale.ROOT)
        val email = Firebase.auth.currentUser?.email ?: return

        // Get FCM token
        Firebase.messaging.token.addOnSuccessListener { fcmToken ->
            // Get Firebase Auth ID token
            Firebase.auth.currentUser?.getIdToken(true)
                ?.addOnSuccessListener { tokenResult ->
                    val idToken = tokenResult.token ?: return@addOnSuccessListener

                    val request = UserRegisterRequest(
                        idToken = idToken,
                        name = name,
                        email = email,
                        role = role,
                        fcm_token = fcmToken
                    )

                    RetrofitClient.instance.registerUser(request)
                        .enqueue(object : Callback<UserRegisterResponse> {
                            override fun onResponse(
                                call: Call<UserRegisterResponse>,
                                response: Response<UserRegisterResponse>
                            ) {
                                if (response.isSuccessful && response.body()?.success == true) {
                                    Toast.makeText(this@RoleSelectionActivity, "✅ User registered", Toast.LENGTH_SHORT).show()
                                    // proceed to HomeActivity or next screen
                                } else {
                                    Toast.makeText(this@RoleSelectionActivity, "⚠️ Backend error", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<UserRegisterResponse>, t: Throwable) {
                                Toast.makeText(this@RoleSelectionActivity, "❌ Network failure", Toast.LENGTH_SHORT).show()
                            }
                        })
                }
        }
    }
}











