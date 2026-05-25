const hazardChatController = require('../controllers/hazardChatController');

module.exports = function(io) {
  io.on('connection', (socket) => {
    console.log('🟢 User connected:', socket.id);

    socket.on('joinHazardRoom', (data) => {
      hazardChatController.handleJoinRoom(socket, data);
    });

    socket.on('sendMessage', (data) => {
      hazardChatController.handleSendMessage(socket, io, data);
    });

  });
};
