package com.hazardiqplus.ui.citizen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hazardiqplus.R
import com.hazardiqplus.ui.ProfileActivity
import com.hazardiqplus.ui.AiChatActivity
import com.hazardiqplus.ui.citizen.fragments.CitizenSosFragment
import com.hazardiqplus.ui.citizen.fragments.CitizenHomeFragment
import com.hazardiqplus.ui.citizen.fragments.CitizenReportsFragment
import com.hazardiqplus.ui.citizen.fragments.CitizenWeatherFragment

class CitizenMainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var extendedFabChatbot: Button
    private lateinit var extendedFabProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_citizen_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.citizenMain)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        bottomNavigation = findViewById(R.id.bottomNavigation)
        extendedFabChatbot = findViewById(R.id.extendedFabChatbot)
        extendedFabProfile = findViewById(R.id.extendedFabProfile)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, CitizenHomeFragment())
            }
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, CitizenHomeFragment())
                        .commit()
                    true
                }
                R.id.nav_weather -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, CitizenWeatherFragment())
                        .commit()
                    true
                }
                R.id.nav_report -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, CitizenReportsFragment())
                        .commit()
                    true
                }
                R.id.nav_sos -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, CitizenSosFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        extendedFabChatbot.setOnClickListener {
            val intent = Intent(this, AiChatActivity::class.java)
            startActivity(intent)
        }
        extendedFabProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }
}
