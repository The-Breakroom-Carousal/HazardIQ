const admin = require('../firebase');
const pool = require('../db');

module.exports = function(io) {
  io.on('connection', (socket) => {
    console.log('🟢 User connected:', socket.id);

    socket.on('joinHazardRoom', ({ hazardId, firebaseToken }) => {
      try {
        admin.auth().verifyIdToken(firebaseToken)
          .then((decodedToken) => {
            const userId = decodedToken.uid;
            const roomName = `hazard-${hazardId}`;
            socket.join(roomName);
            console.log(`✅ User ${userId} joined room ${roomName}`);
            socket.emit('joinedRoom', { room: roomName });
          })
          .catch((err) => {
            console.warn('❌ Invalid token:', err.message);
            socket.emit('authError', { message: 'Invalid Firebase token' });
          });
      } catch (error) {
        console.error('🔥 Error joining room:', error);
      }
    });

    socket.on('sendMessage', async ({ hazardId, firebaseToken, message }) => {
      try {
        const decoded = await admin.auth().verifyIdToken(firebaseToken);
        const userId = decoded.uid;
        const roomName = `hazard-${hazardId}`;

        // Save to DB
        const result = await pool.query(`
          INSERT INTO hazard_chat_messages (hazard_id, sender_uid, message)
          VALUES ($1, $2, $3) RETURNING *;
        `, [hazardId, userId, message]);

        const savedMsg = result.rows[0];

        // Broadcast to room
        io.to(roomName).emit('newMessage', {
          hazardId,
          senderUid: userId,
          message,
          timestamp: savedMsg.timestamp
        });

      } catch (err) {
        console.error('❌ sendMessage Error:', err.message);
        socket.emit('errorSending', { message: 'Message failed to send' });
      }
    });

    socket.on('disconnect', () => {
      console.log('🔴 User disconnected:', socket.id);
    });
  });
};
