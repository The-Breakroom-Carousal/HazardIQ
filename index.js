const express = require('express');
const cors = require('cors');
require('dotenv').config();

const firRoutes = require('./routes/fir');
const sosRoutes=require('./routes/sos');
const createTables = require('./initDB');
const app = express();

app.use(cors());
app.use(express.json());

app.use('/api/', firRoutes);
app.use('./api',sosRoutes);
const PORT = process.env.PORT || 3001;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));

