const modelModel = require('../models/modelModel');

const predictAirQuality = async (req, res) => {
  const { city, state, lat, lon, hours = 1 } = req.body;

  if (!city || !state || isNaN(lat) || isNaN(lon)) {
    return res.status(400).json({ 
      error: 'Missing required fields: city, state, lat, lon' 
    });
  }

  if (!modelModel.supportedCities[state]?.includes(city)) {
    return res.status(400).json({
      error: 'Unsupported location',
      message: `Predictions not available for ${city}, ${state}`,
      supportedCities: modelModel.supportedCities[state] || []
    });
  }

  try {
    const predictions = await modelModel.predictAirQuality(city, state, lat, lon, hours);
    res.json({ success: true, predictions });
  } catch (err) {
    console.error('Prediction Error:', err.message, err.stack);
    res.status(500).json({ 
      error: 'Prediction failed',
      details: err.message,
      stack: err.stack
    });
  }
};

const aqiNearby = async (req, res) => {
  const { lat, lon, radius = 10 } = req.query;

  if (isNaN(lat) || isNaN(lon)) {
    return res.status(400).json({ error: 'Invalid coordinates' });
  }

  try {
    const data = await modelModel.getNearbyAQI(lat, lon, radius);
    res.json({ success: true, data });
  } catch (err) {
    console.error('Database Error:', err.message);
    res.status(500).json({ error: 'Failed to fetch nearby data' });
  }
};

module.exports = {
  predictAirQuality,
  aqiNearby
};
