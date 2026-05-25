const admin = require('../firebase');
const hazardChatModel = require('../models/hazardChatModel');

const handleJoinRoom = async (socket, data) => {
  try {
    const { hazardId, firebaseToken } = data;
    
    const decodedToken = await admin.auth().verifyIdToken(firebaseToken);
    const userId = decodedToken.uid;
    const roomName = `hazard-${hazardId}`;
    
    socket.join(roomName);
    console.log(`✅ User ${userId} joined room ${roomName}`);
    socket.emit('joinedRoom', { room: roomName });

    const chatHistory = await hazardChatModel.getChatHistory(hazardId);
    socket.emit('chatHistory', chatHistory);

    console.log(`📦 Sent ${chatHistory.length} historical messages to user ${userId}`);

  } catch (error) {
    console.error('🔥 Error joining room or fetching history:', error);
    socket.emit('authError', { message: 'Invalid Firebase token or server error' });
  }
};

const handleSendMessage = async (socket, io, data) => {
  try {
    const { hazardId, firebaseToken, message } = data;
    console.log('🔁 Received sendMessage:', { hazardId, message });

    const decoded = await admin.auth().verifyIdToken(firebaseToken);
    const userId = decoded.uid;
    const roomName = `hazard-${hazardId}`;
    console.log(`✅ Token valid. UID: ${userId}`);

    const senderName = await hazardChatModel.getSenderName(userId);
    const savedMsg = await hazardChatModel.saveMessage(hazardId, userId, message);

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
};

module.exports = {
  handleJoinRoom,
  handleSendMessage
};
