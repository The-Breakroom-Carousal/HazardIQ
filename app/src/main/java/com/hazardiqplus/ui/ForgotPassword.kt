package com.hazardiqplus.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.hazardiqplus.R

class ForgotPassword : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        val emaileT=findViewById<EditText>(R.id.emailEt)
        val btnResetPassword=findViewById<Button>(R.id.btnResetPassword)
        btnResetPassword.setOnClickListener {
            val email = emaileT.text.toString().trim()
            if (email.isEmpty()) {
                emaileT.error = "Email is required"
                return@setOnClickListener
            }
            else{
                val auth=FirebaseAuth.getInstance()
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to send password reset email", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}