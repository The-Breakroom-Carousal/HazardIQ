// routes/predict.js
const express = require('express');
const router = express.Router();
const axios = require('axios');
const NodeCache = require('node-cache');
require('dotenv').config();

const cache = new NodeCache({ stdTTL: 3600 }); // Cache for 1 hour

// Helper to fetch pollution data from OpenAQ for delay1 and delay2

async function fetchPollutionData(city, delayHours) {
  const cacheKey = `${city}_pollution_delay${delayHours}`;
  if (cache.has(cacheKey)) return cache.get(cacheKey);

  const now = new Date();
  const pastTime = new Date(now.getTime() - delayHours * 60 * 60 * 1000);

  const isoTime = pastTime.toISOString();
  const openaqUrl = `https://api.openaq.org/v2/measurements?city=${encodeURIComponent(
    city
  )}&date_from=${isoTime}&limit=100&sort=desc`;

  try {
    const res = await axios.get(openaqUrl);
    const results = res.data.results;
    const pm25 = results.find((r) => r.parameter === 'pm25')?.value || 0;
    const pm10 = results.find((r) => r.parameter === 'pm10')?.value || 0;

    const data = { pm25, pm10 };
    cache.set(cacheKey, data);
    return data;
  } catch (err) {
    console.error(`Error fetching pollution data for ${city}:`, err);
    return { pm25: 0, pm10: 0 }; // Fallback
  }
}

// Helper to fetch weather data (e.g., Open-Meteo or WeatherAPI)
async function fetchWeatherData(lat, lon) {
  const cacheKey = `${lat}_${lon}_weather`;
  if (cache.has(cacheKey)) return cache.get(cacheKey);

  const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,rain,surface_pressure,wind_speed_10m,wind_direction_10m,wind_speed_100m,wind_direction_100m`;

  try {
    const res = await axios.get(url);
    const current = res.data.current;
    const data = {
      'temperature_2m (°C)': current.temperature_2m,
      'relative_humidity_2m (%)': current.relative_humidity_2m,
      'rain (mm)': current.rain,
      'surface_pressure (hPa)': current.surface_pressure,
      'wind_speed_10m (km/h)': current.wind_speed_10m,
      'wind_speed_100m (km/h)': current.wind_speed_100m,
      'wind_direction_10m (°)': current.wind_direction_10m,
      'wind_direction_100m (°)': current.wind_direction_100m
    };
    cache.set(cacheKey, data);
    return data;
  } catch (err) {
    console.error(`Error fetching weather for ${lat},${lon}:`, err);
    return null;
  }
}

// Predict endpoint
router.post('/predict-air-quality', async (req, res) => {
  const { city, state, lat, lon } = req.body;

  if (!city || !state || !lat || !lon) {
    return res.status(400).json({ error: 'Missing city, state, lat or lon' });
  }

  try {
    const [delay1, delay2, weather] = await Promise.all([
      fetchPollutionData(city, 1),
      fetchPollutionData(city, 2),
      fetchWeatherData(lat, lon)
    ]);

    if (!weather) return res.status(500).json({ error: 'Weather data unavailable' });

    const input = {
      state,
      features: {
        ...weather,
        city,
        'PM2.5 (µg/m³)_delay1': delay1.pm25,
        'PM2.5 (µg/m³)_delay2': delay2.pm25,
        'PM10 (µg/m³)_delay1': delay1.pm10,
        'PM10 (µg/m³)_delay2': delay2.pm10
      }
    };

    const mlResponse = await axios.post('https://hazardiq.onrender.com/predict', input);
    res.json({ success: true, prediction: mlResponse.data });
  } catch (err) {
    console.error('Error in /predict-air-quality:', err);
    res.status(500).json({ error: 'Prediction failed' });
  }
});

module.exports = router;
