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
  // ... (keep your existing supportedCities object)
};

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
    const targetHourISO = targetHour.toISOString().slice(0, 13);

    const url = `${config.openMeteo.baseUrl}/air-quality?latitude=${lat}&longitude=${lon}&hourly=pm10,pm2_5,ozone,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide&timezone=auto`;
    const res = await axios.get(url);

    const { time, pm10, pm2_5, ozone, carbon_monoxide, nitrogen_dioxide, sulphur_dioxide } = res.data.hourly;
    const index = time.findIndex(t => t.startsWith(targetHourISO));

    const data = {
      pm25: index !== -1 ? pm2_5[index] : 0,
      pm10: index !== -1 ? pm10[index] : 0,
      ozone: index !== -1 ? ozone[index] : 0,
      co: index !== -1 ? carbon_monoxide[index] : 0,
      no2: index !== -1 ? nitrogen_dioxide[index] : 0,
      so2: index !== -1 ? sulphur_dioxide[index] : 0
    };

    cache.set(cacheKey, data);
    return data;
  } catch (err) {
    console.error(`Open-Meteo AQI Error:`, err.response?.data || err.message);
    return { pm25: 0, pm10: 0, ozone: 0, co: 0, no2: 0, so2: 0 };
  }
}

async function fetchWeatherData(lat, lon) {
  const cacheKey = `weather_${lat}_${lon}`;
  if (cache.has(cacheKey)) return cache.get(cacheKey);

  try {
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,rain,surface_pressure,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,relative_humidity_2m,precipitation,pressure_msl,wind_speed_10m`;
    const res = await axios.get(url);

    const weather = {
      current: {
        temperature: res.data.current.temperature_2m,
        humidity: res.data.current.relative_humidity_2m,
        rain: res.data.current.rain,
        pressure: res.data.current.surface_pressure,
        wind_speed: res.data.current.wind_speed_10m,
        wind_direction: res.data.current.wind_direction_10m
      },
      hourly: {
        time: res.data.hourly.time,
        temperature: res.data.hourly.temperature_2m,
        humidity: res.data.hourly.relative_humidity_2m,
        precipitation: res.data.hourly.precipitation,
        pressure: res.data.hourly.pressure_msl,
        wind_speed: res.data.hourly.wind_speed_10m
      }
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
        },
        timeout: 10000
      }
    );

    // Validate response structure
    if (!response.data || !response.data.prediction) {
      throw new Error('Invalid prediction API response structure');
    }

    // Safely extract values with defaults
    const pred = response.data.prediction;
    return {
      PM25: typeof pred.PM25 === 'number' ? pred.PM25 : 0,
      PM10: typeof pred.PM10 === 'number' ? pred.PM10 : 0,
      Ozone: typeof pred.Ozone === 'number' ? pred.Ozone : 0,
      CO: typeof pred.CO === 'number' ? pred.CO : 0,
      NO2: typeof pred.NO2 === 'number' ? pred.NO2 : 0,
      SO2: typeof pred.SO2 === 'number' ? pred.SO2 : 0
    };
  } catch (err) {
    console.error('Predict API Error:', {
      status: err.response?.status,
      data: err.response?.data,
      input: input
    });
    return {
      PM25: 0,
      PM10: 0,
      Ozone: 0,
      CO: 0,
      NO2: 0,
      SO2: 0
    };
  }
}

function calculateAQI(pollutants) {
  const breakpoints = {
    pm25: [
      [0, 12, 0, 50],
      [12.1, 35.4, 51, 100],
      [35.5, 55.4, 101, 150],
      [55.5, 150.4, 151, 200],
      [150.5, 250.4, 201, 300],
      [250.5, 350.4, 301, 400],
      [350.5, 500.4, 401, 500]
    ],
    pm10: [
      [0, 54, 0, 50],
      [55, 154, 51, 100],
      [155, 254, 101, 150],
      [255, 354, 151, 200],
      [355, 424, 201, 300],
      [425, 504, 301, 400],
      [505, 604, 401, 500]
    ],
    ozone: [
      [0, 54, 0, 50],
      [55, 70, 51, 100],
      [71, 85, 101, 150],
      [86, 105, 151, 200],
      [106, 200, 201, 300]
    ],
    co: [
      [0, 4.4, 0, 50],
      [4.5, 9.4, 51, 100],
      [9.5, 12.4, 101, 150],
      [12.5, 15.4, 151, 200],
      [15.5, 30.4, 201, 300],
      [30.5, 40.4, 301, 400],
      [40.5, 50.4, 401, 500]
    ],
    no2: [
      [0, 53, 0, 50],
      [54, 100, 51, 100],
      [101, 360, 101, 150],
      [361, 649, 151, 200],
      [650, 1249, 201, 300],
      [1250, 1649, 301, 400],
      [1650, 2049, 401, 500]
    ],
    so2: [
      [0, 35, 0, 50],
      [36, 75, 51, 100],
      [76, 185, 101, 150],
      [186, 304, 151, 200],
      [305, 604, 201, 300],
      [605, 804, 301, 400],
      [805, 1004, 401, 500]
    ]
  };

  const aqiValues = [];
  
  for (const [pollutant, value] of Object.entries(pollutants)) {
    if (!breakpoints[pollutant] || value === undefined || value === null) continue;
    
    const bp = breakpoints[pollutant].find(
      ([low, high]) => value >= low && value <= high
    );
    
    if (bp) {
      const [cLow, cHigh, iLow, iHigh] = bp;
      const aqi = ((iHigh - iLow) / (cHigh - cLow)) * (value - cLow) + iLow;
      aqiValues.push(Math.round(aqi));
    }
  }

  return aqiValues.length > 0 ? Math.max(...aqiValues) : 0;
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
    const [delay1, delay2, delay3, weather] = await Promise.all([
      fetchPollutionData(lat, lon, 1),
      fetchPollutionData(lat, lon, 2),
      fetchPollutionData(lat, lon, 3),
      fetchWeatherData(lat, lon)
    ]);

    const predictions = [];
    const pollutionHistory = {
      pm25: [delay3.pm25, delay2.pm25, delay1.pm25],
      pm10: [delay3.pm10, delay2.pm10, delay1.pm10],
      ozone: [delay3.ozone, delay2.ozone, delay1.ozone],
      co: [delay3.co, delay2.co, delay1.co],
      no2: [delay3.no2, delay2.no2, delay1.no2],
      so2: [delay3.so2, delay2.so2, delay1.so2]
    };

    for (let h = 1; h <= Math.min(hours, config.maxPredictionHours); h++) {
      const forecastHour = new Date();
      forecastHour.setHours(forecastHour.getHours() + h);
      const forecastIndex = weather.hourly.time.findIndex(t => 
        t.startsWith(forecastHour.toISOString().slice(0, 13))
      );
      
      const hourlyWeather = forecastIndex !== -1 ? {
        temperature: weather.hourly.temperature[forecastIndex],
        humidity: weather.hourly.humidity[forecastIndex],
        precipitation: weather.hourly.precipitation[forecastIndex],
        pressure: weather.hourly.pressure[forecastIndex],
        wind_speed: weather.hourly.wind_speed[forecastIndex]
      } : weather.current;

      const input = {
        state,
        features: {
          ...hourlyWeather,
          city,
          'PM2.5 (µg/m³)_delay1': pollutionHistory.pm25[2],
          'PM2.5 (µg/m³)_delay2': pollutionHistory.pm25[1],
          'PM2.5 (µg/m³)_delay3': pollutionHistory.pm25[0],
          'PM10 (µg/m³)_delay1': pollutionHistory.pm10[2],
          'PM10 (µg/m³)_delay2': pollutionHistory.pm10[1],
          'PM10 (µg/m³)_delay3': pollutionHistory.pm10[0],
          'Ozone (µg/m³)_delay1': pollutionHistory.ozone[2],
          'Ozone (µg/m³)_delay2': pollutionHistory.ozone[1],
          'CO (µg/m³)_delay1': pollutionHistory.co[2],
          'NO2 (µg/m³)_delay1': pollutionHistory.no2[2],
          'SO2 (µg/m³)_delay1': pollutionHistory.so2[2]
        }
      };

      const prediction = await callPredictAPI(input);

      // Update history for next prediction
      pollutionHistory.pm25 = [pollutionHistory.pm25[1], pollutionHistory.pm25[2], prediction.PM25];
      pollutionHistory.pm10 = [pollutionHistory.pm10[1], pollutionHistory.pm10[2], prediction.PM10];
      pollutionHistory.ozone = [pollutionHistory.ozone[1], pollutionHistory.ozone[2], prediction.Ozone];
      pollutionHistory.co = [pollutionHistory.co[1], pollutionHistory.co[2], prediction.CO];
      pollutionHistory.no2 = [pollutionHistory.no2[1], pollutionHistory.no2[2], prediction.NO2];
      pollutionHistory.so2 = [pollutionHistory.so2[1], pollutionHistory.so2[2], prediction.SO2];

      predictions.push({
        hourAhead: h,
        PM25: prediction.PM25,
        PM10: prediction.PM10,
        Ozone: prediction.Ozone,
        CO: prediction.CO,
        NO2: prediction.NO2,
        SO2: prediction.SO2,
        AQI: calculateAQI({
          pm25: prediction.PM25,
          pm10: prediction.PM10,
          ozone: prediction.Ozone,
          co: prediction.CO,
          no2: prediction.NO2,
          so2: prediction.SO2
        }),
        weather: hourlyWeather,
        predictionSource: prediction.PM25 === 0 ? 'fallback' : 'api'
      });
    }

    await pool.query(
      `INSERT INTO air_quality_predictions 
       (city, state, latitude, longitude, pm25_prediction, pm10_prediction, 
        ozone, co, no2, so2, aqi, weather_data, prediction_source)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)`,
      [
        city, state, lat, lon, 
        predictions[0].PM25, predictions[0].PM10,
        predictions[0].Ozone, predictions[0].CO,
        predictions[0].NO2, predictions[0].SO2,
        predictions[0].AQI,
        JSON.stringify(predictions[0].weather),
        predictions[0].predictionSource
      ]
    );

    res.json({ success: true, predictions });
  } catch (err) {
    console.error('Prediction Error:', err.message, err.stack);
    res.status(500).json({ 
      error: 'Prediction failed',
      details: err.message,
      stack: process.env.NODE_ENV === 'development' ? err.stack : undefined
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