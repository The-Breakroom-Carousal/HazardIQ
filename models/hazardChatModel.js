const pool = require('../db');

const getChatHistory = async (hazardId) => {
  const result = await pool.query(`
    SELECT 
      hcm.message, 
      hcm.timestamp, 
      u.name AS sender_name,
      u.firebase_uid AS sender_uid 
    FROM hazard_chat_messages hcm
    INNER JOIN users u ON hcm.sender_uid = u.firebase_uid
    WHERE hcm.hazard_id = $1 
    ORDER BY hcm.timestamp ASC;
  `, [hazardId]);

  return result.rows.map(row => ({
    senderUid: row.sender_uid, 
    senderName: row.sender_name, 
    message: row.message,
    timestamp: row.timestamp.getTime()
  }));
};

const getSenderName = async (userId) => {
  const userResult = await pool.query(`SELECT name FROM users WHERE firebase_uid = $1`, [userId]);
  return userResult.rows[0].name;
};

const saveMessage = async (hazardId, userId, message) => {
  const hazardIdInt = parseInt(hazardId, 10);
  const result = await pool.query(`
    INSERT INTO hazard_chat_messages (hazard_id, sender_uid, message)
    VALUES ($1, $2, $3) RETURNING *;
  `, [hazardIdInt, userId, message]);

  return result.rows[0];
};

module.exports = {
  getChatHistory,
  getSenderName,
  saveMessage
};
