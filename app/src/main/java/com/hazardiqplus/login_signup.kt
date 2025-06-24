package com.hazardiqplus

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class login_signup : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_signup)
        firebaseAuth = FirebaseAuth.getInstance()
        val signup =findViewById<TextView>(R.id.textView)
        val Sgnbtn =findViewById<Button>(R.id.sgninbutton)
        val emailEt=findViewById<EditText>(R.id.emailEt)
        val passEt=findViewById<EditText>(R.id.passET)
        val fgtpass=findViewById<TextView>(R.id.forgotPasswordText)
        fgtpass.setOnClickListener{
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
        }



        signup.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        Sgnbtn.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val pass = passEt.text.toString().trim()
            if (!validateInput(email, pass)) return@setOnClickListener
            firebaseAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Invalid Email or Password !", Toast.LENGTH_SHORT).show()
                    }

                }



        }


    }





    private fun validateInput(email: String, password: String): Boolean {
        val emailEt=findViewById<EditText>(R.id.emailEt)
        val passET=findViewById<EditText>(R.id.passET)

        if (email.isEmpty()) {
            emailEt.error = "Email is required"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEt.error = "Enter a valid email address"
            return false
        }
        if (password.isEmpty()) {
            passET.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            passET.error = "Password must be at least 6 characters"
            return false
        }
        return true
    }

    override fun onStart() {

        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser!=null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}