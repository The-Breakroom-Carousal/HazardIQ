const pool = require('../db');

const getChatHistory = async (sosId) => {
  const messages = await pool.query(`
    SELECT scm.message, scm.timestamp, u.name AS sender_name, u.firebase_uid AS sender_uid
    FROM sos_chat_messages scm
    JOIN users u ON u.firebase_uid = scm.sender_uid
    WHERE scm.sos_id = $1 ORDER BY scm.timestamp ASC;
  `, [sosId]);

  return messages.rows.map(msg => ({
    message: msg.message,
    senderUid: msg.sender_uid,
    senderName: msg.sender_name,
    timestamp: msg.timestamp.getTime()
  }));
};

const getSenderName = async (uid) => {
  const userRes = await pool.query(`SELECT name FROM users WHERE firebase_uid = $1`, [uid]);
  return userRes.rows[0].name;
};

const saveMessage = async (sosId, uid, message) => {
  const result = await pool.query(`
    INSERT INTO sos_chat_messages (sos_id, sender_uid, message)
    VALUES ($1, $2, $3) RETURNING *;
  `, [sosId, uid, message]);

  return result.rows[0];
};

module.exports = {
  getChatHistory,
  getSenderName,
  saveMessage
};
