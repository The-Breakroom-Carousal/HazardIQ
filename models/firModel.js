const pool = require('../db');
const admin = require('../firebase');

const registerUser = async (firebase_uid, name, email, role, location_lat, location_lng, fcm_token) => {
  const query = `
    INSERT INTO users (firebase_uid, name, email, role, location_lat, location_lng, fcm_token)
    VALUES ($1, $2, $3, $4, $5, $6, $7)
    ON CONFLICT (firebase_uid) DO UPDATE
    SET name = EXCLUDED.name,
        email = EXCLUDED.email,
        role = EXCLUDED.role,
        location_lat = EXCLUDED.location_lat,
        location_lng = EXCLUDED.location_lng,
        fcm_token = EXCLUDED.fcm_token
    RETURNING *;
  `;

  const values = [firebase_uid, name, email, role, location_lat, location_lng, fcm_token];
  const result = await pool.query(query, values);
  return result.rows[0];
};

const getAllUsers = async () => {
  const result = await pool.query('SELECT * FROM users ORDER BY id DESC');
  return result.rows;
};

const getUserById = async (firebase_uid) => {
  const result = await pool.query('SELECT * FROM users WHERE firebase_uid = $1', [firebase_uid]);
  return result.rows.length > 0 ? result.rows[0] : null;
};

const updateUser = async (firebase_uid, name, email, role, location_lat, location_lng, fcm_token) => {
  const result = await pool.query(
    `
    UPDATE users
    SET 
      name = COALESCE($1, name),
      email = COALESCE($2, email),
      role = COALESCE($3, role),
      location_lat = COALESCE($4, location_lat),
      location_lng = COALESCE($5, location_lng),
      fcm_token = COALESCE($6, fcm_token)
    WHERE firebase_uid = $7
    RETURNING *;
    `,
    [name, email, role, location_lat, location_lng, fcm_token, firebase_uid]
  );

  return result.rowCount > 0 ? result.rows[0] : null;
};

const getUserName = async (uid) => {
  const result = await pool.query('SELECT name FROM users WHERE firebase_uid = $1', [uid]);
  return result.rows.length > 0 ? result.rows[0] : null;
};

const deleteUser = async (firebase_uid) => {
  const result = await pool.query('DELETE FROM users WHERE firebase_uid = $1', [firebase_uid]);
  return result;
};

module.exports = {
  registerUser,
  getAllUsers,
  getUserById,
  updateUser,
  getUserName,
  deleteUser
};
