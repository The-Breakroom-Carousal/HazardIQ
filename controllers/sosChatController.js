const admin = require('../firebase');
const sosChatModel = require('../models/sosChatModel');

const handleJoinRoom = async (socket, data) => {
  try {
    const { sosId, firebaseToken } = data;
    const decoded = await admin.auth().verifyIdToken(firebaseToken);
    const uid = decoded.uid;
    const room = `sos-${sosId}`;
    
    socket.join(room);

    const chatHistory = await sosChatModel.getChatHistory(sosId);
    socket.emit('chatHistory', chatHistory);

    console.log(`📦 User ${uid} joined SOS chat room sos-${sosId}`);
  } catch (err) {
    console.error("❌ Error joining SOS room:", err.message);
    socket.emit('authError', { message: 'Invalid token or server error' });
  }
};

const handleSendMessage = async (socket, io, data) => {
  try {
    const { sosId, firebaseToken, message } = data;
    const decoded = await admin.auth().verifyIdToken(firebaseToken);
    const uid = decoded.uid;

    const senderName = await sosChatModel.getSenderName(uid);
    const msg = await sosChatModel.saveMessage(sosId, uid, message);

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
};

module.exports = {
  handleJoinRoom,
  handleSendMessage
};
