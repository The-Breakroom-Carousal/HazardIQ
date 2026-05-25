const pool = require('../db');
const admin = require('../firebase');
const NodeGeocoder = require('node-geocoder');
const geocoder = NodeGeocoder({
  provider: 'opencage',
  apiKey: process.env.GEOCODER_API_KEY 
});

const saveSosEvent = async (uid, type, lat, lng, city) => {
  const inserted = await pool.query(
    `INSERT INTO sos_events (firebase_uid, type, latitude, longitude, city) 
     VALUES ($1, $2, $3, $4, $5) RETURNING id`,
    [uid, type, lat, lng, city]
  );
  return inserted.rows[0].id;
};

const getUserName = async (uid) => {
  const userRes = await pool.query(
    'SELECT name FROM users WHERE firebase_uid = $1',
    [uid]
  );
  return userRes.rows[0]?.name || "Someone";
};

const getRespondersInCity = async (city) => {
  const respondersRes = await pool.query(
    `SELECT fcm_token, location_lat, location_lng 
     FROM users 
     WHERE role = 'responder' AND fcm_token IS NOT NULL`
  );

  const matchingTokens = [];

  for (const responder of respondersRes.rows) {
    const { fcm_token, location_lat, location_lng } = responder;

    if (!location_lat || !location_lng) continue;

    let responderCity = null;
    try {
      const geoRes = await geocoder.reverse({ lat: location_lat, lon: location_lng });
      responderCity = geoRes?.[0]?.city || geoRes?.[0]?.administrativeLevels?.level2long || '';
    } catch (err) {
      console.error("Reverse geocoding failed for responder:", err.message);
      continue;
    }

    if (responderCity.toLowerCase().includes(city.toLowerCase())) {
      matchingTokens.push(fcm_token);
    }
  }

  return matchingTokens;
};

const getSosEventsByCity = async (city) => {
  const result = await pool.query(`
    SELECT se.*, u.name, u.email 
    FROM sos_events se 
    LEFT JOIN users u ON u.firebase_uid = se.firebase_uid
    WHERE se.city = $1
    ORDER BY se.timestamp DESC
  `, [city]);
  return result.rows;
};

const updateSosProgress = async (id, progress, responderUid) => {
  const result = await pool.query(
    `UPDATE sos_events 
     SET progress = $1, responder_uid = COALESCE($2, responder_uid) 
     WHERE id = $3 
     RETURNING *`,
    [progress, responderUid, id]
  );
  return result;
};

const deleteSosEvent = async (id) => {
  const result = await pool.query(
    `DELETE FROM sos_events WHERE id = $1 RETURNING *`, 
    [id]
  );
  return result;
};

module.exports = {
  saveSosEvent,
  getUserName,
  getRespondersInCity,
  getSosEventsByCity,
  updateSosProgress,
  deleteSosEvent
};
