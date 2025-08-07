const admin = require('../firebase');
const pool = require('../db');

module.exports = function(io) {
  io.on('connection', (socket) => {
    console.log('🟢 Socket connected:', socket.id);

    socket.on('joinSosRoom', async ({ sosId, firebaseToken }) => {
      try {
        const decoded = await admin.auth().verifyIdToken(firebaseToken);
        const uid = decoded.uid;
        const room = `sos-${sosId}`;
        socket.join(room);

        const messages = await pool.query(`
          SELECT scm.message, scm.timestamp, u.name AS sender_name, u.firebase_uid AS sender_uid
          FROM sos_chat_messages scm
          JOIN users u ON u.firebase_uid = scm.sender_uid
          WHERE scm.sos_id = $1 ORDER BY scm.timestamp ASC;
        `, [sosId]);

        socket.emit('chatHistory', messages.rows.map(msg => ({
          message: msg.message,
          senderUid: msg.sender_uid,
          senderName: msg.sender_name,
          timestamp: msg.timestamp.getTime()
        })));

        console.log(`📦 User ${uid} joined SOS chat room sos-${sosId}`);
      } catch (err) {
        console.error("❌ Error joining SOS room:", err.message);
        socket.emit('authError', { message: 'Invalid token or server error' });
      }
    });

    socket.on('sendSosMessage', async ({ sosId, firebaseToken, message }) => {
      try {
        const decoded = await admin.auth().verifyIdToken(firebaseToken);
        const uid = decoded.uid;

        const userRes = await pool.query(`SELECT name FROM users WHERE firebase_uid = $1`, [uid]);
        const senderName = userRes.rows[0].name;

        const result = await pool.query(`
          INSERT INTO sos_chat_messages (sos_id, sender_uid, message)
          VALUES ($1, $2, $3) RETURNING *;
        `, [sosId, uid, message]);

        const msg = result.rows[0];
        io.to(`sos-${sosId}`).emit('newMessage', {
          message,
          senderUid: uid,
          senderName,
          timestamp: msg.timestamp.getTime()
        });
      } catch (err) {
        console.error('❌ sendSosMessage Error:', err.message);
        socket.emit('errorSending', { message: 'Message failed to send' });
      }
    });
  });
};
