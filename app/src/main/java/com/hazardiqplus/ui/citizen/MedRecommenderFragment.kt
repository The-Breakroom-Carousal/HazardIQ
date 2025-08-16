package com.hazardiqplus.ui.citizen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.PredictionEventResponse
import com.hazardiqplus.data.PredictionRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MedRecommenderFragment : Fragment(R.layout.fragment_med_recommender) {

    private lateinit var chipGroupSymptoms: ChipGroup
    private lateinit var btnPredict: MaterialButton
    private lateinit var tvResult: TextView
    private val symptomList = listOf(
        "sinus_pressure", "back_pain", "excessive_hunger", "swollen_blood_vessels",
        "depression", "restlessness", "irritability", "blurred_and_distorted_vision",
        "mild_fever", "stomach_bleeding", "fluid_overload.1", "nodal_skin_eruptions",
        "phlegm", "muscle_weakness", "red_spots_over_body", "rusty_sputum",
        "visual_disturbances", "receiving_blood_transfusion", "pain_behind_the_eyes",
        "swelled_lymph_nodes", "weakness_in_limbs", "abnormal_menstruation", "acidity",
        "muscle_pain", "stiff_neck", "anxiety", "blood_in_sputum", "bruising",
        "movement_stiffness", "weight_loss", "skin_peeling", "slurred_speech", "knee_pain",
        "palpitations", "indigestion", "neck_pain", "silver_like_dusting", "yellow_urine",
        "dizziness", "throat_irritation", "fast_heart_rate", "internal_itching",
        "puffy_face_and_eyes", "mood_swings", "belly_pain", "small_dents_in_nails",
        "spinning_movements", "painful_walking", "toxic_look_(typhos)", "polyuria"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_med_recommender, container, false)

        chipGroupSymptoms = view.findViewById(R.id.chipGroupSymptoms)
        btnPredict = view.findViewById(R.id.btnPredictDisease)
        tvResult = view.findViewById(R.id.tvResult)

        // Dynamically add Chips
        for (symptom in symptomList) {
            val chip = Chip(requireContext()).apply {
                text = symptom.replace("_", " ").replace(".1", "").replaceFirstChar { it.uppercase() }
                isCheckable = true
            }
            chipGroupSymptoms.addView(chip)
        }

        btnPredict.setOnClickListener { predictDisease() }

        return view
    }

    private fun predictDisease() {
        val selectedBooleans = mutableListOf<Boolean>()

        for (i in 0 until chipGroupSymptoms.childCount) {
            val chip = chipGroupSymptoms.getChildAt(i) as Chip
            selectedBooleans.add(chip.isChecked) // true if selected, false otherwise
        }

        tvResult.text = "Predicting..."

        // Step 1: Call startPrediction API
        RetrofitClient.medInstance.startPrediction(PredictionRequest(selectedBooleans))
            .enqueue(object : Callback<PredictionEventResponse> {
                override fun onResponse(
                    call: Call<PredictionEventResponse>,
                    response: Response<PredictionEventResponse>
                ) {
                    if (response.isSuccessful) {
                        val eventId = response.body()?.event_id ?: return
                        fetchResult(eventId)
                    } else {
                        tvResult.text = "Error starting prediction."
                    }
                }

                override fun onFailure(call: Call<PredictionEventResponse>, t: Throwable) {
                    tvResult.text = "Failed: ${t.message}"
                }
            })
    }

    private fun fetchResult(eventId: String) {
        RetrofitClient.medInstance.getPredictionResult(eventId)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        var raw = response.body()?.string()?.trim()

                        // Extract after "data:"
                        raw = raw?.substringAfter("data:")?.trim()

                        val predictionText = if (raw?.startsWith("[") == true) {
                            try {
                                val jsonArray = org.json.JSONArray(raw)
                                jsonArray.optString(0)
                            } catch (e: Exception) {
                                raw
                            }
                        } else {
                            raw
                        }

                        // Replace escaped bullets (\u2022) with real bullets
                        tvResult.text = predictionText
                            ?.replace("\\u2022", "•")
                            ?: "No result found."
                    } else {
                        tvResult.text = "Error fetching result."
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    tvResult.text = "Failed: ${t.message}"
                }
            })
    }

}