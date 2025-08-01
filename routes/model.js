// routes/predict.js
const express = require('express');
const router = express.Router();
const axios = require('axios').create({ timeout: 30000 });
const axiosRetry = require('axios-retry').default;

axiosRetry(axios, {
  retries: 3,
  retryDelay: axiosRetry.exponentialDelay,
  retryCondition: (error) => {
    return (
      axiosRetry.isNetworkOrIdempotentRequestError(error) ||
      [429, 502, 503, 504].includes(error?.response?.status)
    );
  }
});
  
const NodeCache = require('node-cache');
const cors = require('cors');
const pool = require('../db');
require('dotenv').config();

// Configuration
const config = {
  cacheTTL: process.env.CACHE_TTL || 3600,
  maxPredictionHours: 24,
  openMeteo: {
    baseUrl: 'https://air-quality-api.open-meteo.com/v1'
  }
};

const supportedCities = {
  "AndhraPradesh": ["Amravati", "Anantapur", "Chittoor", "Kadapa", "Rajamahendravaram", "Tirupati", "Vijayawada", "Visakhapatnam"],
  "ArunachalPradesh": ["Naharlagun"],
  "Assam": ["Guwahati", "Nagaon", "Nalbari", "Silchar"],
  "Bihar": ["Araria", "Arrah", "Aurangabad", "Begusarai", "Bettiah", "Bhagalpur", "Chhapra", "Gaya", "Patna"],
  "Chandigarh": ["Chandigarh"],
  "Chattisgarh": ["Bhilai", "Bilaspur", "Chhal", "Korba", "Milupara", "Raipur"],
  "Delhi": ["Delhi"],
  "Gujarat": ["Ahmedabad", "Ankleshwar", "Gandhinagar", "Nandesari", "Surat", "Vapi"],
  "Haryana": ["Ambala", "Bahadurgarh", "Ballabgarh", "Bhiwani", "Faridabad", "Fatehabad", "Gurugram", "Panipat", "Sirsa", "Sonipat"],
  "HimachalPradesh": ["Baddi"],
  "JK": ["Srinagar"],
  "Jharkhand": ["Dhanbad"],
  "Karnataka": ["Bengaluru", "Belgaum", "Dharwad", "Mangalore", "Mysuru", "Ramanagara", "Udupi", "Vijayapura"],
  "Kerala": ["Kannur", "Thiruvananthapuram", "Thrissur"],
  "MadhyaPradesh": ["Bhopal", "Dewas", "Gwalior", "Indore", "Ratlam", "Ujjain"],
  "Maharashtra": ["Aurangabad", "Amravati", "Chandrapur", "Mumbai", "Nagpur", "Nashik", "Navimumbai", "Pune"],
  "Manipur": ["Imphal"],
  "Meghalaya": ["Shillong"],
  "Mizoram": ["Aizawl"],
  "Nagaland": ["Kohima"],
  "Odisha": ["Angul", "Balasore", "Bhubaneswar", "Cuttack", "Rourkela", "Suakati"],
  "Puducherry": ["Puducherry"],
  "Punjab": ["Amritsar", "Bathinda", "Jalandhar", "Khanna", "Ludhiana", "Patiala", "Rupnagar"],
  "Rajasthan": ["Ajmer", "Alwar", "Bikaner", "Jaipur", "Jaisalmer", "Kota", "Sikar"],
  "Sikkim": ["Gangtok"],
  "TamilNadu": ["Chennai", "Coimbatore", "Ooty", "Ramanathapuram", "Vellore"],
  "Telangana": ["Hyderabad"],
  "Tripura": ["Agartala"],
  "UttarPradesh": ["Agra", "Kanpur", "Lucknow", "Varanasi", "Vrindavan"],
  "Uttarakhand": ["Dehradun", "Kashipur", "Rishikesh"],
  "WestBengal": ["Asansol", "Kolkata", "Siliguri"]
}
;

// Middleware
router.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  methods: ['GET', 'POST']
}));

const cache = new NodeCache({ stdTTL: config.cacheTTL });

// Helper Functions
async function fetchPollutionData(lat, lon, delayHours) {
  const cacheKey = `pollution_${lat}_${lon}_${delayHours}h`;
  if (cache.has(cacheKey)) return cache.get(cacheKey);

  try {
    const now = new Date();
    const targetHour = new Date(now.getTime() - delayHours * 60 * 60 * 1000);
    const targetHourISO = targetHour.toISOString().slice(0, 13); // YYYY-MM-DDTHH

    const url = `${config.openMeteo.baseUrl}/air-quality?latitude=${lat}&longitude=${lon}&hourly=pm10,pm2_5&timezone=auto`;
    const res = await axios.get(url);

    const { time, pm10, pm2_5 } = res.data.hourly;
    const index = time.findIndex(t => t.startsWith(targetHourISO));

    const data = {
      pm25: index !== -1 ? pm2_5[index] : 0,
      pm10: index !== -1 ? pm10[index] : 0
    };

    cache.set(cacheKey, data);
    return data;
  } catch (err) {
    console.error(`Open-Meteo AQI Error:`, err.response?.data || err.message);
    return { pm25: 0, pm10: 0 };
  }
}

async function fetchWeatherData(lat, lon) {
  const cacheKey = `weather_${lat}_${lon}`;
  if (cache.has(cacheKey)) return cache.get(cacheKey);

  try {
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,rain,surface_pressure,wind_speed_10m,wind_direction_10m,wind_speed_100m,wind_direction_100m`;
    const res = await axios.get(url);

    const weather = {
      'temperature_2m (°C)': res.data.current.temperature_2m,
      'relative_humidity_2m (%)': res.data.current.relative_humidity_2m,
      'rain (mm)': res.data.current.rain,
      'surface_pressure (hPa)': res.data.current.surface_pressure,
      'wind_speed_10m (km/h)': res.data.current.wind_speed_10m,
      'wind_speed_100m (km/h)': res.data.current.wind_speed_100m,
      'wind_direction_10m (°)': res.data.current.wind_direction_10m,
      'wind_direction_100m (°)': res.data.current.wind_direction_100m
    };

    cache.set(cacheKey, weather);
    return weather;
  } catch (err) {
    console.error(`Weather API Error:`, err.response?.data || err.message);
    throw new Error('Weather data unavailable');
  }
}

async function callPredictAPI(input) {
  try {
    const response = await axios.post(
      'https://hazardiq.onrender.com/predict',
      {
        state: input.state,
        features: input.features
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      }
    );
    return response.data;
  } catch (err) {
    console.error('Predict API Error:', {
      status: err.response?.status,
      data: err.response?.data,
      input: input
    });
    throw err;
  }
}

function calculateAQI(pm25, pm10) {
  const aqi25 = pm25 > 250 ? 500 : (pm25 / 250) * 500;
  const aqi10 = pm10 > 430 ? 500 : (pm10 / 430) * 500;
  return Math.round(Math.max(aqi25, aqi10));
}

// Routes
router.post('/predict-air-quality', async (req, res) => {
  const { city, state, lat, lon, hours = 1 } = req.body;

  if (!city || !state || isNaN(lat) || isNaN(lon)) {
    return res.status(400).json({ 
      error: 'Missing required fields: city, state, lat, lon' 
    });
  }

  if (!supportedCities[state]?.includes(city)) {
    return res.status(400).json({
      error: 'Unsupported location',
      message: `Predictions not available for ${city}, ${state}`,
      supportedCities: supportedCities[state] || []
    });
  }

  try {
    const [delay1, delay2, weather] = await Promise.all([
      fetchPollutionData(lat, lon, 1),
      fetchPollutionData(lat, lon, 2),
      fetchWeatherData(lat, lon)
    ]);

    const predictions = [];
    let pm25_1 = delay1.pm25;
    let pm25_2 = delay2.pm25;
    let pm10_1 = delay1.pm10;
    let pm10_2 = delay2.pm10;

    for (let h = 1; h <= Math.min(hours, config.maxPredictionHours); h++) {
      const input = {
        state,
        features: {
          ...weather,
          city,
          'PM2.5 (µg/m³)_delay1': pm25_1,
          'PM2.5 (µg/m³)_delay2': pm25_2,
          'PM10 (µg/m³)_delay1': pm10_1,
          'PM10 (µg/m³)_delay2': pm10_2
        }
      };

      const prediction = await callPredictAPI(input);
      const { PM25, PM10 } = prediction.prediction;

      predictions.push({
        hourAhead: h,
        PM25,
        PM10,
        AQI: calculateAQI(PM25, PM10)
      });

      pm25_2 = pm25_1;
      pm25_1 = PM25;
      pm10_2 = pm10_1;
      pm10_1 = PM10;
    }

    await pool.query(
      `INSERT INTO air_quality_predictions 
       (city, state, latitude, longitude, pm25_prediction, pm10_prediction)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [city, state, lat, lon, predictions[0].PM25, predictions[0].PM10]
    );

    res.json({ success: true, predictions });
  } catch (err) {
    console.error('Prediction Error:', err.message, err.stack);
    res.status(500).json({ 
      error: 'Prediction failed',
      details: err.message,
      stack: err.stack
    });
  }
});

router.get('/aqi-nearby', async (req, res) => {
  const { lat, lon, radius = 10 } = req.query;

  if (isNaN(lat) || isNaN(lon)) {
    return res.status(400).json({ error: 'Invalid coordinates' });
  }

  try {
    const result = await pool.query(`
      SELECT * FROM air_quality_predictions
      WHERE earth_distance(ll_to_earth($1::float8, $2::float8), ll_to_earth(latitude, longitude)) < $3::float8 * 1000
      ORDER BY timestamp DESC LIMIT 50
    `, [lat, lon, radius]);

    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Database Error:', err.message);
    res.status(500).json({ error: 'Failed to fetch nearby data' });
  }
});

module.exports = router;
