const admin = require('../firebase');
const pool = require('../db');

module.exports = function(io) {
  io.on('connection', (socket) => {
    console.log('🟢 User connected:', socket.id);

    socket.on('joinHazardRoom', (data) =>{
      try {
        const { hazardId, firebaseToken } = data; 
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

    socket.on('sendMessage', async (data) => {
  try {
    const { hazardId, firebaseToken, message } = data;
    console.log('🔁 Received sendMessage:', { hazardId, message });

    const decoded = await admin.auth().verifyIdToken(firebaseToken);
    const userId = decoded.uid;
    const roomName = `hazard-${hazardId}`;
    console.log(`✅ Token valid. UID: ${userId}`);
    const hazardIdInt = parseInt(hazardId, 10);
    // Insert into DB
    const result = await pool.query(`
      INSERT INTO hazard_chat_messages (hazard_id, sender_uid, message)
      VALUES ($1, $2, $3) RETURNING *;
    `, [hazardIdInt, userId, message]);

    const savedMsg = result.rows[0];
    console.log('✅ Message saved to DB:', savedMsg);

    // Broadcast to room
    io.to(roomName).emit('newMessage', {
      hazardId,
      senderUid: userId,
      message,
      timestamp: savedMsg.timestamp.getTime()
    });

  } catch (err) {
    console.error('❌ sendMessage Error:', err.message);
    socket.emit('errorSending', { message: 'Message failed to send' });
  }
});

  });
};
