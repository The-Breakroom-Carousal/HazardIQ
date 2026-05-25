// routes/model.js
const express = require('express');
const router = express.Router();
const cors = require('cors');
const modelController = require('../controllers/modelController');

require('dotenv').config();

// Middleware
router.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  methods: ['GET', 'POST']
}));

// Routes
router.post('/predict-air-quality', modelController.predictAirQuality);
router.get('/aqi-nearby', modelController.aqiNearby);

module.exports = router;