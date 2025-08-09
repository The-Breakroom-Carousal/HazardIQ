package com.hazardiqplus.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.User
import com.hazardiqplus.ui.citizen.CitizenMainActivity
import com.hazardiqplus.ui.responder.ResponderMainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginSignupActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_signup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginMain)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginBottomSheet)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()
        val signup =findViewById<TextView>(R.id.textView)
        val Sgnbtn =findViewById<Button>(R.id.sgninbutton)
        val emailEt=findViewById<EditText>(R.id.emailEt)
        val passEt=findViewById<EditText>(R.id.passET)
        val fgtpass=findViewById<TextView>(R.id.forgotPasswordText)

        checkAndRequestPermission()

        fgtpass.setOnClickListener{
            val textInputView = View.inflate(this, R.layout.dialouge_text_input, null)
            val input = textInputView.findViewById<TextInputEditText>(R.id.textInput)
            input.hint = "Enter your email"

            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Forgot Password?")
                .setView(textInputView)
                .setPositiveButton("Send reset mail", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newText = input.text.toString().trim()
                if (newText.isNotBlank()) {
                    if (Patterns.EMAIL_ADDRESS.matcher(newText).matches()) {
                        resetPassword(newText)
                        dialog.dismiss()
                    } else {
                        input.error = "Enter a valid email address"
                    }
                } else {
                    input.error = "Email cannot be empty"
                }
            }
        }

        val googlesgn=findViewById<Button>(R.id.sign_in_button)

        googlesgn.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent,RC_SIGN_IN
            )
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
                        firebaseAuth.currentUser?.getIdToken(true)
                            ?.addOnSuccessListener { result ->
                                val token = result.token
                                if (token != null) {
                                    checkUserRoleAndRedirect(token)
                                } else {
                                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                                    finish()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Invalid Email or Password !", Toast.LENGTH_SHORT).show()
                    }

                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode==RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    Toast.makeText(this, "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    firebaseAuth.currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { result ->
                            val token = result.token
                            if (token != null) {
                                checkUserRoleAndRedirect(token)
                            } else {
                                startActivity(Intent(this, RoleSelectionActivity::class.java))
                                finish()
                            }
                        }

                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
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
        firebaseAuth.currentUser?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                val token = result.token
                if (token != null) {
                    Log.d("Token", "Token: $token")
                    checkUserRoleAndRedirect(token)
                }
            }

    }

    private fun checkUserRoleAndRedirect(idToken: String) {
        val call = RetrofitClient.instance.getUserDetails(idToken)
        call.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {

                Log.d("DEBUG", "Response JSON: ${response.body()}")
                val role = response.body()?.role
                Toast.makeText(this@LoginSignupActivity, "Role: $role", Toast.LENGTH_SHORT).show()
                when (role?.lowercase()) {
                    "citizen" -> startActivity(Intent(this@LoginSignupActivity, CitizenMainActivity::class.java))
                    "responder" -> startActivity(Intent(this@LoginSignupActivity, ResponderMainActivity::class.java))
                    "admin" -> Toast.makeText(this@LoginSignupActivity, "Admin not implemented", Toast.LENGTH_SHORT).show()
                    else -> startActivity(Intent(this@LoginSignupActivity, RoleSelectionActivity::class.java))
                }

                finish()
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                Toast.makeText(this@LoginSignupActivity, "Error verifying role", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginSignupActivity, RoleSelectionActivity::class.java))
                finish()
            }
        })
    }

    private fun checkAndRequestPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA
                    ), 101
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Background Location Permission")
                .setMessage("This app requires background location permission for accurate location tracking.")
                .setPositiveButton("Grant") { _, _ ->
                    ActivityCompat.requestPermissions(
                            this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    101
                    )
                }
                .setNegativeButton("Deny") { _, _ ->
                    Toast.makeText(this, "Background location permission denied", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun resetPassword(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to send password reset email", Toast.LENGTH_SHORT).show()
            }
        }
    }
}