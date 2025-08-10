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

async function verifyIdTokenFromHeader(req) {
  const idToken = req.header('idtoken');
  if (!idToken) throw { status: 401, message: 'Missing ID token' };
  const decoded = await admin.auth().verifyIdToken(idToken);
  return decoded.uid;
}

router.put('/ai-chat', async (req, res) => {
  const { idToken, msg } = req.body;
  try {
    
    const uid = await verifyIdTokenFromHeader(req);
    const { msg } = req.body;
    if (!msg) return res.status(400).json({ error: 'msg is required' });
    
    await pool.query(
      `INSERT INTO chat_history (user_id, role, message) VALUES ($1, $2, $3);`,
      [uid, "user", msg]
    );
    
    const response = await axios.post('https://hazard-iq-plus-rag.onrender.com/query', { user_id: uid });

    const assistantMsg = response?.data?.response || JSON.stringify(response.data);
    await pool.query(
      `INSERT INTO chat_history (user_id, role, message) VALUES ($1, $2, $3)`,
      [uid, 'assistant', assistantMsg]
    );

    return res.json({ success: true, model: response.data });
  } catch (err) {
    console.error("❌ Error in /ai-chat:", err);
    res.status(500).json({ error: "Internal server error" });
  }
});


router.get('/history', async (req, res) => {
  try {
    const uid = await verifyIdTokenFromHeader(req);
    const result = await pool.query(
      `SELECT role, message, EXTRACT(EPOCH FROM timestamp)::BIGINT AS ts FROM chat_history WHERE user_id = $1 ORDER BY timestamp ASC`,
      [uid]
    );
    return res.json({ success: true, history: result.rows });
  } catch (err) {
    console.error('❌ Error fetching history:', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
});

router.post('/restart', async (req, res) => {
  try {
    const uid = await verifyIdTokenFromHeader(req);
    await pool.query(`DELETE FROM chat_history WHERE user_id = $1`, [uid]);
    return res.json({ success: true });
  } catch (err) {
    console.error('❌ Error restarting chat:', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
});


module.exports = router;
