const express = require('express');
const cors = require('cors');

const http = require('http');   
require('dotenv').config();

const firRoutes = require('./routes/fir');
const sosRoutes=require('./routes/sos');
const modelRoutes = require('./routes/model');
const hazardRoutes=require('./routes/hazard');
const createTables = require('./initDB');

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);

const { Server } = require('socket.io');
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
}); 

app.use('/api/', firRoutes);
app.use('/api/',sosRoutes);
app.use('/api/',modelRoutes);
app.use('/api/',hazardRoutes);

require('./routes/hazardChatSocket')(io);

const PORT = process.env.PORT || 3001;
server.listen(PORT, () => console.log(`Server running on port ${PORT}`));

