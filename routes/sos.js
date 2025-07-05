const express = require('express');
const router = express.Router();
const pool = require('../db');
const admin = require('../firebase');
const NodeGeocoder = require('node-geocoder');
require('dotenv').config();

const geocoder = NodeGeocoder({
  provider: process.env.GEOCODER_PROVIDER,
  apiKey: process.env.GEOCODER_API_KEY
});

router.post('/send-sos', async (req, res) => {
  const { idToken, type, city, lat, lng } = req.body;

  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    const uid = decoded.uid;

    const userRes = await pool.query(
      'SELECT name FROM users WHERE firebase_uid = $1',
      [uid]
    );
    const name = userRes.rows[0]?.name || "Someone";

    // Fetch all responders with location
    const respondersRes = await pool.query(
      `SELECT fcm_token, location_lat, location_lng 
       FROM users 
       WHERE role = 'responder' AND fcm_token IS NOT NULL`
    );
    

    const matchingTokens = [];

    for (const responder of respondersRes.rows) {
      const { fcm_token, location_lat, location_lng } = responder;

      if (!location_lat || !location_lng) continue;

      const geoRes = await geocoder.reverse({ lat: location_lat, lon: location_lng });
      const responderCity = geoRes?.[0]?.city || geoRes?.[0]?.administrativeLevels?.level2long;

      if (responderCity && responderCity.toLowerCase() === city.toLowerCase()) {
        matchingTokens.push(fcm_token);
      }
    }

    if (matchingTokens.length === 0) {
      return res.status(404).json({ message: "No responders in this city" });
    }
    
    // Send FCM
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

module.exports = router;
