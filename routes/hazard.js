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
  const { lat, lon, radius = 10 } = req.query;

  //console.log('find-hazard query:', { lat, lon, radius });

  const parsedLat = parseFloat(lat);
  const parsedLon = parseFloat(lon);
  const parsedRadius = parseFloat(radius);

  if (isNaN(parsedLat) || isNaN(parsedLon) || isNaN(parsedRadius)) {
    console.warn('Invalid coordinates or radius');
    return res.status(400).json({ error: 'Invalid coordinates or radius' });
  }

  try {
    const result = await pool.query(`
      SELECT * FROM hazard_data
      WHERE earth_distance(
        ll_to_earth($1::float8, $2::float8),
        ll_to_earth(latitude, longitude)
      ) < $3::float8 * 1000
      ORDER BY timestamp DESC LIMIT 50
    `, [parsedLat, parsedLon, parsedRadius]);

    console.log(`Hazards found: ${result.rows.length}`);
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Database Error:', err.message);
    res.status(500).json({ error: 'Failed to fetch hazard data' });
  }
});


router.post('/remove-hazard', async (req, res) => {
  const id =  req.headers.id;
  if (!id) {
    console.warn('Missing hazard ID');
    return res.status(400).json({ error: 'Missing hazard ID' });
  }
  try {
    const result = await pool.query(
      `DELETE FROM hazard_data WHERE id = $1 RETURNING *`,
      [id]
    );

    if (result.rowCount === 0) {
      console.warn('Hazard not found');
      return res.status(404).json({ error: 'Hazard not found' });
    }

    console.log('Hazard removed successfully:', result.rows[0]);
    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Error removing hazard:', err.message);
    res.status(500).json({ error: 'Failed to remove hazard' });
  }
})

module.exports = router;


