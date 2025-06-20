// initDB.js
const pool = require('./db');

const createFIRTable = async () => {
  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS fir (
        id SERIAL PRIMARY KEY,
        officer_name TEXT NOT NULL,
        station TEXT NOT NULL,
        description TEXT NOT NULL,
        priority TEXT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);
    console.log("✅ FIR table created or already exists.");
  } catch (error) {
    console.error("❌ Error creating FIR table:", error);
  }
};

createFIRTable();
