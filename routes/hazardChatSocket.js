const admin = require('../firebase');
const pool = require('../db');

module.exports = function(io) {
  io.on('connection', (socket) => {
    console.log('🟢 User connected:', socket.id);

   
socket.on('joinHazardRoom', async (data) => { 
  try {
    const { hazardId, firebaseToken } = data; 
    
    const decodedToken = await admin.auth().verifyIdToken(firebaseToken);
    const userId = decodedToken.uid;
    const roomName = `hazard-${hazardId}`;
    socket.join(roomName);
    console.log(`✅ User ${userId} joined room ${roomName}`);
    socket.emit('joinedRoom', { room: roomName });

  
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

    const chatHistory = result.rows.map(row => ({
      senderUid: row.sender_uid, 
      senderName: row.sender_name, 
      message: row.message,
      timestamp: row.timestamp.getTime()
    }));

    socket.emit('chatHistory', chatHistory);

    console.log(`📦 Sent ${chatHistory.length} historical messages to user ${userId}`);


  } catch (error) {
    console.error('🔥 Error joining room or fetching history:', error);
    socket.emit('authError', { message: 'Invalid Firebase token or server error' });
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
    const userResult = await pool.query(`SELECT name FROM users WHERE firebase_uid = $1`, [userId]);
    const senderName = userResult.rows[0].name;

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
      senderName,
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
