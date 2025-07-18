// routes/predict.js
const express = require('express');
const router = express.Router();
const axios = require('axios').create({ timeout: 5000 });
const NodeCache = require('node-cache');
const cors = require('cors');
const pool = require('../db');
require('dotenv').config();

// Configuration
const config = {
  cacheTTL: process.env.CACHE_TTL || 3600,
  maxPredictionHours: 24,
  openMeteo: {
    baseUrl: process.env.OPEN_METEO_URL || 'https://api.open-meteo.com/v1'
  }
};

// Middleware
router.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  methods: ['GET', 'POST']
}));

const cache = new NodeCache({ stdTTL: config.cacheTTL });

// Helper Functions
async function fetchPollutionData(city, delayHours) {
  const cacheKey = `${city}_pollution_${delayHours}h`;
  if (cache.has(cacheKey)) return cache.get(cacheKey);

  const now = new Date();
  const pastTime = new Date(now.getTime() - delayHours * 60 * 60 * 1000);
  const isoTime = pastTime.toISOString();

  try {
    const res = await axios.get('https://api.openaq.org/v3/measurements', {
      params: {
        city,
        date_from: isoTime,
        limit: 2,
        parameter: ['pm25', 'pm10'],
        sort: 'desc'
      },
      headers: {
        'X-API-Key': process.env.OPENAQ_API_KEY
      }
    });

    const pm25Data = res.data.results.find(r => r.parameter === 'pm25');
    const pm10Data = res.data.results.find(r => r.parameter === 'pm10');

    const data = {
      pm25: pm25Data?.value || 0,
      pm10: pm10Data?.value || 0
    };

    cache.set(cacheKey, data);
    return data;
  } catch (err) {
    console.error(`OpenAQ Error for ${city}:`, err.response?.data || err.message);
    return { pm25: 0, pm10: 0 };
  }
}

async function fetchWeatherData(lat, lon) {
  const cacheKey = `weather_${lat}_${lon}`;
  if (cache.has(cacheKey)) return cache.get(cacheKey);

  try {
    const url = `${config.openMeteo.baseUrl}/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,rain,surface_pressure,wind_speed_10m,wind_direction_10m,wind_speed_100m,wind_direction_100m`;
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
        features: {
          ...input.features,
          city: input.features.city,
          'PM2.5 (µg/m³)_delay1': input.features['PM2.5 (µg/m³)_delay1'],
          'PM2.5 (µg/m³)_delay2': input.features['PM2.5 (µg/m³)_delay2'],
          'PM10 (µg/m³)_delay1': input.features['PM10 (µg/m³)_delay1'],
          'PM10 (µg/m³)_delay2': input.features['PM10 (µg/m³)_delay2']
        }
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

  try {
    const [delay1, delay2, weather] = await Promise.all([
      fetchPollutionData(city, 1),
      fetchPollutionData(city, 2),
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

      // Update for next iteration
      pm25_2 = pm25_1;
      pm25_1 = PM25;
      pm10_2 = pm10_1;
      pm10_1 = PM10;
    }

    // Database insert
    await pool.query(
      `INSERT INTO air_quality_predictions 
       (city, state, latitude, longitude, pm25_prediction, pm10_prediction)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [city, state, lat, lon, predictions[0].PM25, predictions[0].PM10]
    );

    res.json({ success: true, predictions });
  } catch (err) {
    console.error('Prediction Error:', err.message);
    res.status(500).json({ 
      error: 'Prediction failed',
      details: process.env.NODE_ENV === 'development' ? err.message : undefined
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
      WHERE earth_distance(ll_to_earth($1, $2), ll_to_earth(latitude, longitude)) < $3 * 1000
      ORDER BY timestamp DESC LIMIT 50
    `, [lat, lon, radius]);

    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Database Error:', err);
    res.status(500).json({ error: 'Failed to fetch nearby data' });
  }
});

module.exports = router;