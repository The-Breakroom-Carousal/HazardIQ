const express = require('express');
const router = express.Router();
const pool  = require('../db');
const admin=require('../firebase');
const NodeGeocoder = require('node-geocoder');
const geocoder = NodeGeocoder({
  provider: 'opencage',
  apiKey: process.env.GEOCODER_API_KEY 
});
require('dotenv').config();

router.post('/send-sos', async (req, res) => {
  const { idToken, type, city, lat, lng } = req.body;

  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    const uid = decoded.uid;

    await pool.query(
      `INSERT INTO sos_events (firebase_uid, type, latitude, longitude) VALUES ($1, $2, $3, $4)`,
      [uid, type, lat, lng]
    );

    const userRes = await pool.query(
      'SELECT name FROM users WHERE firebase_uid = $1',
      [uid]
    );
    const name = userRes.rows[0]?.name || "Someone";

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

    if (matchingTokens.length === 0) {
      return res.status(404).json({ message: "No responders in this city" });
    }

    const payload = {
      notification: {
        title: `🚨 SOS Alert: ${type}`,
        body: `${name} needs help in ${city}. Tap to respond.`,
      },
      data: { lat: String(lat), lng: String(lng), type, name }
    };

    const fcmResponse = await admin.messaging().sendEachForMulticast({
      tokens: matchingTokens,
      ...payload
    });

    res.json({ success: true, sent: fcmResponse.successCount, failed: fcmResponse.failureCount });

  } catch (err) {
    console.error("❌ Error in /send-sos:", err);
    res.status(500).json({ error: "Internal server error" });
  }
});


router.get('/sos-events', async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT se.*, u.name, u.email 
      FROM sos_events se
      LEFT JOIN users u ON u.firebase_uid = se.firebase_uid
      ORDER BY se.timestamp DESC
    `);
    res.json(result.rows);
  } catch (err) {
    console.error("❌ Error fetching sos events:", err);
    res.status(500).json({ error: "Internal server error" });
  }
});


router.put('/sos-events/:id/progress', async (req, res) => {
  const { id } = req.params;
  const { progress } = req.body;

  const allowed = ['pending', 'acknowledged', 'resolved'];
  if (!allowed.includes(progress)) {
    return res.status(400).json({ error: "Invalid progress value" });
  }

  try {
    const result = await pool.query(
      `UPDATE sos_events SET progress = $1 WHERE id = $2 RETURNING *`,
      [progress, id]
    );
    
    if (result.rowCount === 0) {
      return res.status(404).json({ error: "SOS event not found" });
    }

    res.json({ message: "Progress updated", event: result.rows[0] });
  } catch (err) {
    console.error("❌ Error updating progress:", err);
    res.status(500).json({ error: "Internal server error" });
  }
});

module.exports = router;
