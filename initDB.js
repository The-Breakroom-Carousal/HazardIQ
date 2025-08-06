const pool = require('./db');


const createUsersTable = async () => {
  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        firebase_uid TEXT UNIQUE NOT NULL,
        name TEXT,
        email TEXT,
        role TEXT CHECK (role IN ('citizen', 'responder', 'admin')),
        location_lat DOUBLE PRECISION,
        location_lng DOUBLE PRECISION,
        fcm_token TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);
    console.log("✅ Created 'users' table");
  } catch (error) {
    console.error("❌ Error creating 'users' table:", error);
  }
};

const air_quality_predictions = async () => {
  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS air_quality_predictions (
        id SERIAL PRIMARY KEY,
        city TEXT NOT NULL,
        state TEXT NOT NULL,
        latitude DOUBLE PRECISION NOT NULL,
        longitude DOUBLE PRECISION NOT NULL,
        pm25_prediction DOUBLE PRECISION NOT NULL,
        pm10_prediction DOUBLE PRECISION NOT NULL,
        timestamp TIMESTAMPTZ DEFAULT NOW()
      );
    `);
    console.log("✅ Created 'air_quality_predictions' table or already exists");
  } catch (error) {
    console.error("❌ Error creating 'air_quality_predictions' table:", error);
  }
};

const hazard_data = async () => {
  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS hazard_data (
      id SERIAL PRIMARY KEY,
      rad DOUBLE PRECISION NOT NULL,
      hazard TEXT NOT NULL,
      latitude DOUBLE PRECISION NOT NULL,
      longitude DOUBLE PRECISION NOT NULL,
      timestamp TIMESTAMPTZ DEFAULT NOW()
);
    `); 
    console.log("✅ Created 'hazard_data' table or already exists");
  } catch (error) {
    console.error("❌ Error creating 'hazard_data' table:", error);
  }
};

    
const hazardChatTable = async () => {
  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS hazard_chat_messages (
        id SERIAL PRIMARY KEY,
        hazard_id INTEGER REFERENCES hazard_data(id),
        sender_uid TEXT NOT NULL,
        message TEXT NOT NULL,
        timestamp TIMESTAMPTZ DEFAULT NOW()
      );
    `);
    console.log("✅ Created 'hazard_chat_messages' table");
  } catch (error) {
    console.error("❌ Error creating 'hazard_chat_messages' table:", error);
  }
};


const createSosEventsTable  = async () => {
  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS sos_events (
        id  SERIAL  PRIMARY KEY,
        firebase_uid TEXT NOT NULL,
        type TEXT NOT NULL,
        latitude DOUBLE PRECISION NOT NULL,
        longitude DOUBLE PRECISION NOT NULL,
        city TEXT NOT NULL,
        progress TEXT DEFAULT 'pending' CHECK (progress IN ('pending', 'acknowledged', 'resolved')),
        timestamp TIMESTAMPTZ DEFAULT NOW()
      );
    `);
    console.log("✅ Created 'sos_events' table");
  } catch (error) {
    console.error("❌ Error creating 'sos_events' table:", error);
  }
};


const init = async () => {
  await createUsersTable();
  await air_quality_predictions();
  await hazard_data();
  await hazardChatTable();
  await createSosEventsTable();
};

init();
