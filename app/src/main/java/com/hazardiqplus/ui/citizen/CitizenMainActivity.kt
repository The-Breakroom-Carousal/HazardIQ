package com.hazardiqplus.ui.citizen

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.hazardiqplus.R
import com.hazardiqplus.ui.citizen.fragments.CitizenSosFragment
import com.hazardiqplus.ui.citizen.fragments.CitizenHomeFragment
import com.hazardiqplus.ui.citizen.fragments.CitizenReportsFragment

class CitizenMainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

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
                R.id.nav_sos -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, CitizenSosFragment())
                        .commit()
                    true
                }
                R.id.nav_report -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, CitizenReportsFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
