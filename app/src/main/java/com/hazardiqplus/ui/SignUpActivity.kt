package com.hazardiqplus.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.hazardiqplus.R

class SignUpActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        firebaseAuth = FirebaseAuth.getInstance()

        val textView = findViewById<TextView>(R.id.textView)
        val emailEt=findViewById<EditText>(R.id.emailEt)
        val passET=findViewById<EditText>(R.id.passET)
        val confirmPassEt=findViewById<EditText>(R.id.confirmPass_Et)
        textView.setOnClickListener {
            val intent = Intent(this, LoginSignupActivity::class.java)
            startActivity(intent)
        }

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {

            val email =emailEt.text.toString().trim()
            val pass = passET.text.toString().trim()
            val confirmPass = confirmPassEt.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {
                    // Validate password strength
                    if (pass.length < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, LoginSignupActivity::class.java)
                            startActivity(intent)
                        } else {
                            handleFirebaseError(it.exception)
                        }
                    }

                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleFirebaseError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthWeakPasswordException -> {
                Toast.makeText(this, "Weak password: ${exception.reason}", Toast.LENGTH_SHORT).show()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            }
            is FirebaseAuthUserCollisionException -> {
                Toast.makeText(this, "This email is already in use", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Error: ${exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

