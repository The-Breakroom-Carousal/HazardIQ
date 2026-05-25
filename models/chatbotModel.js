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

const saveChatMessage = async (uid, role, message) => {
  await pool.query(
    `INSERT INTO chat_history (user_id, role, message) VALUES ($1, $2, $3);`,
    [uid, role, message]
  );
};

const callAIService = async (uid) => {
  const response = await axios.post('https://hazard-iq-plus-rag.onrender.com/query', { user_id: uid });
  return response.data;
};

const getChatHistory = async (uid) => {
  const result = await pool.query(
    `SELECT role, message, EXTRACT(EPOCH FROM timestamp)::BIGINT AS ts FROM chat_history WHERE user_id = $1 ORDER BY timestamp ASC`,
    [uid]
  );
  return result.rows;
};

const clearChatHistory = async (uid) => {
  await pool.query(`DELETE FROM chat_history WHERE user_id = $1`, [uid]);
};

module.exports = {
  saveChatMessage,
  callAIService,
  getChatHistory,
  clearChatHistory
};
