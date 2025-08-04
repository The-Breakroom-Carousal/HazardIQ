package com.hazardiqplus.ui.citizen

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.SosRequest
import com.hazardiqplus.data.SosResponse
import com.hazardiqplus.ui.citizen.fragments.SosFragment
import com.hazardiqplus.ui.citizen.fragments.home.CitizenHomeFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class CitizenMainActivity : AppCompatActivity() {

    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var btnHome: Button
    private lateinit var btnSos: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_citizen_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.citizenMain)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        toggleGroup = findViewById(R.id.tabToggleGroup)
        btnHome = findViewById(R.id.btnHome)
        btnSos = findViewById(R.id.btnSos)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, CitizenHomeFragment())
            }
            toggleGroup.check(R.id.btnHome)
        }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnHome -> {
                        supportFragmentManager.commit {
                            replace(R.id.fragmentContainer, CitizenHomeFragment())
                        }
                    }
                    R.id.btnSos -> {
                        supportFragmentManager.commit {
                            replace(R.id.fragmentContainer, SosFragment())
                        }
                    }
                }
            }
        }
    }
}
