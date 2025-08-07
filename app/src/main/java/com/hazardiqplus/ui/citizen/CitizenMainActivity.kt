package com.hazardiqplus.ui.citizen

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.google.android.material.button.MaterialButtonToggleGroup
import com.hazardiqplus.R
import com.hazardiqplus.ui.citizen.fragments.CitizenSosFragment
import com.hazardiqplus.ui.citizen.fragments.home.CitizenHomeFragment

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
                            replace(R.id.fragmentContainer, CitizenSosFragment())
                        }
                    }
                }
            }
        }
    }
}
