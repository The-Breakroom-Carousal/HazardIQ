const pool = require('../db');

const saveHazard = async (rad, hazard, lat, lng) => {
  try {
    await pool.query(
      `INSERT INTO hazard_data (rad, hazard, latitude, longitude) VALUES ($1, $2, $3, $4)`,
      [rad, hazard, lat, lng]
    );
    console.log('Hazard saved successfully');
    return { success: true };
  } catch (err) {
    console.error('hazard saving Error:', err.message, err.stack);
    throw err;
  }
};

const findHazard = async (parsedLat, parsedLon, parsedRadius) => {
  try {
    const result = await pool.query(`
      SELECT * FROM hazard_data
      WHERE earth_distance(
        ll_to_earth($1::float8, $2::float8),
        ll_to_earth(latitude, longitude)
      ) < $3::float8 * 1000
      ORDER BY timestamp DESC LIMIT 50
    `, [parsedLat, parsedLon, parsedRadius]);

    console.log(`Hazards found: ${result.rows.length}`);
    return { success: true, data: result.rows };
  } catch (err) {
    console.error('Database Error:', err.message);
    throw err;
  }
};

const removeHazard = async (id) => {
  try {
    const result = await pool.query(
      `DELETE FROM hazard_data WHERE id = $1 RETURNING *`,
      [id]
    );

    if (result.rowCount === 0) {
      const err = new Error('Hazard not found');
      err.statusCode = 404;
      throw err;
    }

    console.log('Hazard removed successfully:', result.rows[0]);
    return { success: true, data: result.rows[0] };
  } catch (err) {
    console.error('Error removing hazard:', err.message);
    throw err;
  }
};

module.exports = {
  saveHazard,
  findHazard,
  removeHazard
};
