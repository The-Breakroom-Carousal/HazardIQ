const hazardModel = require('../models/hazardModel');

const saveHazard = async (req, res) => {
  const { rad, hazard, lat, lng } = req.body;
  console.log('save-hazard payload:', { rad, hazard, lat, lng });

  if (!rad || !hazard || isNaN(lat) || isNaN(lng)) {
    console.warn('Missing or invalid data');
    return res.status(400).json({
      error: 'Missing required fields: rad, hazard, lat, lon' 
    });
  }

  try {
    const result = await hazardModel.saveHazard(rad, hazard, lat, lng);
    res.json(result);
  } catch (err) {
    res.status(500).json({ 
      error: 'hazard saving failed',
      details: err.message,
      stack: err.stack
    });
  }
};

const findHazard = async (req, res) => {
  const { lat, lon, radius = 10 } = req.query;

  const parsedLat = parseFloat(lat);
  const parsedLon = parseFloat(lon);
  const parsedRadius = parseFloat(radius);

  if (isNaN(parsedLat) || isNaN(parsedLon) || isNaN(parsedRadius)) {
    console.warn('Invalid coordinates or radius');
    return res.status(400).json({ error: 'Invalid coordinates or radius' });
  }

  try {
    const result = await hazardModel.findHazard(parsedLat, parsedLon, parsedRadius);
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch hazard data' });
  }
};

const removeHazard = async (req, res) => {
  const id = req.headers.id;
  
  if (!id) {
    console.warn('Missing hazard ID');
    return res.status(400).json({ error: 'Missing hazard ID' });
  }

  try {
    const result = await hazardModel.removeHazard(id);
    res.json(result);
  } catch (err) {
    if (err.statusCode === 404) {
      return res.status(404).json({ error: 'Hazard not found' });
    }
    res.status(500).json({ error: 'Failed to remove hazard' });
  }
};

module.exports = {
  saveHazard,
  findHazard,
  removeHazard
};
