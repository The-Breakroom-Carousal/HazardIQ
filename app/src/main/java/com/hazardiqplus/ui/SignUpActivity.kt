package com.hazardiqplus.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.UserRegisterRequest
import com.hazardiqplus.data.UserRegisterResponse
import com.hazardiqplus.ui.citizen.CitizenMainActivity
import com.hazardiqplus.ui.responder.ResponderMainActivity
import com.hazardiqplus.utils.PrefsHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var cardCitizen: MaterialCardView
    private lateinit var cardResponder: MaterialCardView
    private lateinit var btnCreateAccount: MaterialButton
    private lateinit var btnGoogleSignUp: MaterialButton
    private lateinit var tvLogin: TextView
    private var selectedRole: String? = null
    private var signedUpWithGoogle: Boolean = false
    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) fetchLocation()
        else Toast.makeText(this, "⚠️ Location permission denied", Toast.LENGTH_SHORT).show()
    }
    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signupMain)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cardCitizen = findViewById(R.id.cardCitizen)
        cardResponder = findViewById(R.id.cardResponder)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp)
        tvLogin = findViewById(R.id.tvSignIn)

        requestLocationPermission()

        tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val cards = listOf(cardCitizen, cardResponder)
        cards.forEach { card ->
            card.setOnClickListener {
                cards.forEach { it.isChecked = false }
                card.isChecked = true
                selectedRole = if (card.id == R.id.cardCitizen) "citizen" else "responder"
            }
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            etEmail.setText(user.email)
            etEmail.isEnabled = false
            etFullName.setText(user.displayName)
            etPassword.isEnabled = false
            etConfirmPassword.isEnabled = false
            signedUpWithGoogle = true
        }

        btnCreateAccount.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email =etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            if (signedUpWithGoogle) {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                    FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { tokenResult ->
                            val idToken =
                                tokenResult.token ?: return@addOnSuccessListener

                            val request = UserRegisterRequest(
                                idToken = idToken,
                                name = fullName,
                                email = email,
                                role = selectedRole!!,
                                fcm_token = fcmToken,
                                location_lat = currentLat,
                                location_lng = currentLng
                            )

                            RetrofitClient.backendInstance.registerUser(request)
                                .enqueue(object :
                                    Callback<UserRegisterResponse> {
                                    override fun onResponse(
                                        call: Call<UserRegisterResponse>,
                                        response: Response<UserRegisterResponse>
                                    ) {
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            Toast.makeText(
                                                this@SignUpActivity,
                                                "✅ Registered",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            val roleFromResponse =
                                                response.body()?.user?.role
                                            if (roleFromResponse != null) {
                                                PrefsHelper.saveUserRole(this@SignUpActivity, roleFromResponse)
                                            }
                                            val next = when (roleFromResponse) {
                                                "citizen" -> CitizenMainActivity::class.java
                                                "responder" -> ResponderMainActivity::class.java
                                                else -> null
                                            }
                                            next?.let {
                                                startActivity(
                                                    Intent(
                                                        this@SignUpActivity,
                                                        it
                                                    )
                                                )
                                                finish()
                                            }
                                        } else {
                                            Toast.makeText(
                                                this@SignUpActivity,
                                                "⚠️ Backend registration failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    override fun onFailure(
                                        call: Call<UserRegisterResponse>,
                                        t: Throwable
                                    ) {
                                        Toast.makeText(
                                            this@SignUpActivity,
                                            "❌ Network error",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                        }
                }
            }
            else if (fullName.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty() && selectedRole != null) {
                if (pass == confirmPass) {
                    if (pass.length < 6) {
                        etPassword.error = "Password must be at least 6 characters long"
                        return@setOnClickListener
                    }
                    firebaseAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                                    FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                        ?.addOnSuccessListener { tokenResult ->
                                            val idToken =
                                                tokenResult.token ?: return@addOnSuccessListener

                                            val request = UserRegisterRequest(
                                                idToken = idToken,
                                                name = fullName,
                                                email = email,
                                                role = selectedRole!!,
                                                fcm_token = fcmToken,
                                                location_lat = currentLat,
                                                location_lng = currentLng
                                            )

                                            RetrofitClient.backendInstance.registerUser(request)
                                                .enqueue(object :
                                                    Callback<UserRegisterResponse> {
                                                    override fun onResponse(
                                                        call: Call<UserRegisterResponse>,
                                                        response: Response<UserRegisterResponse>
                                                    ) {
                                                        if (response.isSuccessful && response.body()?.success == true) {
                                                            Toast.makeText(
                                                                this@SignUpActivity,
                                                                "✅ Registered",
                                                                Toast.LENGTH_SHORT
                                                            ).show()

                                                            val roleFromResponse =
                                                                response.body()?.user?.role
                                                            if (roleFromResponse != null) {
                                                                PrefsHelper.saveUserRole(this@SignUpActivity, roleFromResponse)
                                                            }
                                                            val next = when (roleFromResponse) {
                                                                "citizen" -> CitizenMainActivity::class.java
                                                                "responder" -> ResponderMainActivity::class.java
                                                                else -> null
                                                            }
                                                            next?.let {
                                                                startActivity(
                                                                    Intent(
                                                                        this@SignUpActivity,
                                                                        it
                                                                    )
                                                                )
                                                                finish()
                                                            }
                                                        } else {
                                                            Toast.makeText(
                                                                this@SignUpActivity,
                                                                "⚠️ Backend registration failed",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }

                                                    override fun onFailure(
                                                        call: Call<UserRegisterResponse>,
                                                        t: Throwable
                                                    ) {
                                                        Toast.makeText(
                                                            this@SignUpActivity,
                                                            "❌ Network error",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                })
                                        }
                                }
                            } else {
                                handleFirebaseError(it.exception)
                            }
                        }
                } else {
                    Snackbar.make(findViewById(R.id.signupMain), "Passwords do not match", Snackbar.LENGTH_SHORT).show()
                }
            } else if (selectedRole == null) {
                Snackbar.make(findViewById(R.id.signupMain), "Please select a role", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(findViewById(R.id.signupMain), "All fields are required", Snackbar.LENGTH_SHORT).show()
            }
        }

        btnGoogleSignUp.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent,RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode== RC_SIGN_IN) {
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
                    Snackbar.make(findViewById(R.id.signupMain), "Please fill the other details", Snackbar.LENGTH_SHORT).show()
                    etFullName.setText(user?.displayName)
                    etEmail.setText(user?.email)
                    etEmail.isEnabled = false
                    etPassword.isEnabled = false
                    etConfirmPassword.isEnabled = false
                    signedUpWithGoogle = true
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
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

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchLocation()
        }
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
}

