// test-db.js
const pool = require('./db');

(async () => {
  try {
    const result = await pool.query('SELECT NOW()');
    console.log('✅ Connected to DB:', result.rows[0]);
  } catch (e) {
    console.error('❌ Connection failed:', e.message);
  } finally {
    await pool.end();
  }
})();
