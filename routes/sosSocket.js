const sosChatController = require('../controllers/sosChatController');

module.exports = function(io) {
  io.on('connection', (socket) => {
    console.log('🟢 Socket connected:', socket.id);

    socket.on('joinSosRoom', (data) => {
      sosChatController.handleJoinRoom(socket, data);
    });

    socket.on('sendSosMessage', (data) => {
      sosChatController.handleSendMessage(socket, io, data);
    });
  });
};
