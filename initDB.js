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

const init = async () => {
  await createUsersTable();
};

init();
