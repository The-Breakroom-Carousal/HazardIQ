const express = require('express');
const router = express.Router();
const pool  = require('../db');

require('dotenv').config();
router.post('/save-hazard', async (req, res) => {
  const { rad, hazard, lat, lng } = req.body;
  console.log('save-hazard payload:', { rad, hazard, lat, lng });

  if (!rad || !hazard || isNaN(lat) || isNaN(lng)) {
    console.warn('Missing or invalid data');
    return res.status(400).json({ 
      error: 'Missing required fields: rad, hazard, lat, lon' 
    });
  }
                                                                    
  try {
    await pool.query(
      `INSERT INTO hazard_data (rad, hazard, latitude, longitude) VALUES ($1, $2, $3, $4)`,
      [rad, hazard, lat, lng]
    );
    console.log('Hazard saved successfully');
    res.json({ success: true });
  } catch (err) {
    console.error('hazard saving Error:', err.message, err.stack);
    res.status(500).json({ 
      error: 'hazard saving failed',
      details: err.message,
      stack: err.stack
    });
  }
});

router.get('/find-hazard', async (req, res) => {
  try {
    let { lat, lon, radius } = req.query;

    // Validate input
    lat = parseFloat(lat);
    lon = parseFloat(lon);
    radius = parseFloat(radius || 10); // Default to 10 km if not given

    if (isNaN(lat) || isNaN(lon)) {
      return res.status(400).json({ error: 'Invalid coordinates' });
    }

    const result = await pool.query(`
      SELECT * FROM hazard_data
      WHERE earth_distance(
        ll_to_earth($1::float8, $2::float8),
        ll_to_earth(latitude, longitude)
      ) < $3::float8 * 1000
      ORDER BY timestamp DESC
      LIMIT 50;
    `, [lat, lon, radius]);

    res.status(200).json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Database error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});


module.exports = router;


