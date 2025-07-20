package com.hazardiqplus.clients

import android.content.Context
import android.location.Geocoder
import android.util.Log
import java.util.*
import kotlin.math.*

object CityModelMapper {

    // ---------------------------------------
    // City dataset: exactly those supported by the ML model
    // ---------------------------------------
    private val cityDatabase = mapOf(
        "AndhraPradesh" to listOf("Amravati", "Anantapur", "Chittoor", "Kadapa", "Rajamahendravaram", "Tirupati", "Vijayawada", "Visakhapatnam"),
        "ArunachalPradesh" to listOf("Naharlagun"),
        "Assam" to listOf("Guwahati", "Nagaon", "Nalbari", "Silchar"),
        "Bihar" to listOf("Araria", "Arrah", "Aurangabad", "Begusarai", "Bettiah", "Bhagalpur", "Chhapra", "Gaya", "Patna"),
        "Chandigarh" to listOf("Chandigarh"),
        "Chattisgarh" to listOf("Bhilai", "Bilaspur", "Chhal", "Korba", "Milupara", "Raipur"),
        "Delhi" to listOf("Delhi"),
        "Gujarat" to listOf("Ahmedabad", "Ankleshwar", "Gandhinagar", "Nandesari", "Surat", "Vapi"),
        "Haryana" to listOf("Ambala", "Bahadurgarh", "Ballabgarh", "Bhiwani", "Faridabad", "Fatehabad", "Gurugram", "Panipat", "Sirsa", "Sonipat"),
        "HimachalPradesh" to listOf("Baddi"),
        "JK" to listOf("Srinagar"),
        "Jharkhand" to listOf("Dhanbad"),
        "Karnataka" to listOf("Bengaluru", "Belgaum", "Dharwad", "Mangalore", "Mysuru", "Ramanagara", "Udupi", "Vijayapura"),
        "Kerala" to listOf("Kannur", "Thiruvananthapuram", "Thrissur"),
        "MadhyaPradesh" to listOf("Bhopal", "Dewas", "Gwalior", "Indore", "Ratlam", "Ujjain"),
        "Maharashtra" to listOf("Aurangabad", "Amravati", "Chandrapur", "Mumbai", "Nagpur", "Nashik", "Navimumbai", "Pune"),
        "Manipur" to listOf("Imphal"),
        "Meghalaya" to listOf("Shillong"),
        "Mizoram" to listOf("Aizawl"),
        "Nagaland" to listOf("Kohima"),
        "Odisha" to listOf("Angul", "Balasore", "Bhubaneswar", "Cuttack", "Rourkela", "Suakati"),
        "Puducherry" to listOf("Puducherry"),
        "Punjab" to listOf("Amritsar", "Bathinda", "Jalandhar", "Khanna", "Ludhiana", "Patiala", "Rupnagar"),
        "Rajasthan" to listOf("Ajmer", "Alwar", "Bikaner", "Jaipur", "Jaisalmer", "Kota", "Sikar"),
        "Sikkim" to listOf("Gangtok"),
        "TamilNadu" to listOf("Chennai", "Coimbatore", "Ooty", "Ramanathapuram", "Vellore"),
        "Telangana" to listOf("Hyderabad"),
        "Tripura" to listOf("Agartala"),
        "UttarPradesh" to listOf("Agra", "Kanpur", "Lucknow", "Varanasi", "Vrindavan"),
        "Uttarakhand" to listOf("Dehradun", "Kashipur", "Rishikesh"),
        "WestBengal" to listOf("Asansol", "Kolkata", "Siliguri")
    )

    // ---------------------------------------
    // Mapping for adminArea → model expected state name
    // ---------------------------------------
    private val stateNameMapping = mapOf(
        "Andhra Pradesh" to "AndhraPradesh",
        "Arunachal Pradesh" to "ArunachalPradesh",
        "Assam" to "Assam",
        "Bihar" to "Bihar",
        "Chandigarh" to "Chandigarh",
        "Chhattisgarh" to "Chattisgarh",
        "Delhi" to "Delhi",
        "Goa" to "Goa",
        "Gujarat" to "Gujarat",
        "Haryana" to "Haryana",
        "Himachal Pradesh" to "HimachalPradesh",
        "Jammu and Kashmir" to "JK",
        "Jharkhand" to "Jharkhand",
        "Karnataka" to "Karnataka",
        "Kerala" to "Kerala",
        "Madhya Pradesh" to "MadhyaPradesh",
        "Maharashtra" to "Maharashtra",
        "Manipur" to "Manipur",
        "Meghalaya" to "Meghalaya",
        "Mizoram" to "Mizoram",
        "Nagaland" to "Nagaland",
        "Odisha" to "Odisha",
        "Puducherry" to "Puducherry",
        "Punjab" to "Punjab",
        "Rajasthan" to "Rajasthan",
        "Sikkim" to "Sikkim",
        "Tamil Nadu" to "TamilNadu",
        "Telangana" to "Telangana",
        "Tripura" to "Tripura",
        "Uttar Pradesh" to "UttarPradesh",
        "Uttarakhand" to "Uttarakhand",
        "West Bengal" to "WestBengal"
    )

    // Normalize Geocoder state name to match cityDatabase key
    fun normalizeStateName(rawState: String): String {
        return stateNameMapping[rawState.trim()] ?: rawState.replace(" ", "")
    }

    // Check if the city is available in model training set
    fun isCitySupported(state: String, city: String): Boolean {
        return cityDatabase[state]?.contains(city) == true
    }

    // Return nearest supported city name in same state + distance
    fun getNearestSupportedCity(
        context: Context,
        state: String,
        userCity: String,
        lat: Double,
        lon: Double
    ): Pair<String, Double> {
        val normalizedState = normalizeStateName(state)

        // If exact match exists, return
        if (isCitySupported(normalizedState, userCity)) {
            return Pair(userCity, 0.0)
        }

        val supportedCities = cityDatabase[normalizedState] ?: return Pair(userCity, Double.MAX_VALUE)

        var nearestCity = userCity
        var minDistance = Double.MAX_VALUE

        supportedCities.forEach { candidate ->
            getCoordinatesForCity(context, candidate, normalizedState)?.let { (clat, clon) ->
                val distance = calculateHaversineDistance(lat, lon, clat, clon)
                if (distance < minDistance) {
                    nearestCity = candidate
                    minDistance = distance
                }
            }
        }

        Log.d("CityMapper", "Mapped '$userCity' → '$nearestCity' (${String.format("%.1f", minDistance)} km) in state: $normalizedState")
        return Pair(nearestCity, minDistance)
    }

    // Geocode any city+state to lat/lon
    private fun getCoordinatesForCity(context: Context, city: String, state: String): Pair<Double, Double>? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val result = geocoder.getFromLocationName("$city, $state, India", 1)?.firstOrNull()
            if (result != null) {
                Pair(result.latitude, result.longitude)
            } else null
        } catch (e: Exception) {
            Log.e("CityMapper", "Failed to geocode $city, $state: ${e.message}")
            null
        }
    }

    // Haversine distance formula (km)
    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }
}
