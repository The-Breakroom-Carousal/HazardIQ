const express = require('express');
const router = express.Router();
const pool = require('../db');
const admin = require('../firebase');

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

router.put('/ai-chat', async (req, res) => {
  const { idToken, msg } = req.body;
  try {
    
    const decoded = await admin.auth().verifyIdToken(idToken);
    const uid = decoded.uid;

    
    await pool.query(
      `INSERT INTO chat_history (user_id, role, message) VALUES ($1, $2, $3);`,
      [uid, "user", msg]
    );
    
    const response = await axios.post('https://hazard-iq-plus-rag.onrender.com/query', { uid });

    res.status(200).json(response.data);

  } catch (err) {
    console.error("❌ Error in /ai-chat:", err);
    res.status(500).json({ error: "Internal server error" });
  }
});

module.exports = router;
