const admin = require('../firebase');
const firModel = require('../models/firModel');

const register = async (req, res) => {
  const { idToken, name, email, role, location_lat, location_lng, fcm_token } = req.body;
  
  try {
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    const firebase_uid = decodedToken.uid;

    const user = await firModel.registerUser(firebase_uid, name, email, role, location_lat, location_lng, fcm_token);
    res.json({ success: true, user });
  } catch (err) {
    console.error('Error creating usersdata:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
};

const getRegistered = async (req, res) => {
  try {
    const users = await firModel.getAllUsers();
    res.status(200).json(users);
  } catch (err) {
    console.error('Error fetching user details:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
};

const getMe = async (req, res) => {
  const idToken = req.headers.idtoken;
  if (!idToken) return res.status(401).json({ error: 'Missing ID token' });

  try {
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    const firebase_uid = decodedToken.uid;

    const user = await firModel.getUserById(firebase_uid);
    if (!user) return res.status(404).json({ error: 'User not found' });

    res.status(200).json(user);
  } catch (err) {
    console.error('Error fetching user by ID:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
};

const updateUserInfo = async (req, res) => {
  const idToken = req.headers.idtoken;

  if (!idToken) return res.status(401).json({ error: 'Missing ID token' });

  try {
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    const firebase_uid = decodedToken.uid;

    const {
      name,
      email,
      role,
      location_lat,
      location_lng,
      fcm_token
    } = req.body;

    const user = await firModel.updateUser(firebase_uid, name, email, role, location_lat, location_lng, fcm_token);

    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.status(200).json(user);
  } catch (err) {
    console.error('❌ Error updating user:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
};

const getName = async (req, res) => {
  const uid = req.headers.uid;
  if (!uid) return res.status(401).json({ error: 'Missing uid' });
  try {
    const result = await firModel.getUserName(uid);
    if (!result) return res.status(404).json({ error: 'user NOT FOUND' });
    res.status(200).json(result);
  } catch (err) {
    console.error('Error finding user:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
};

const deleteUserAccount = async (req, res) => {
  const idToken = req.headers.idToken;
  if (!idToken) return res.status(401).json({ error: 'Missing ID token' });
  try {
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    const firebase_uid = decodedToken.uid;
    const result = await firModel.deleteUser(firebase_uid);
    if (result.rowCount === 0) return res.status(404).json({ error: 'user NOT FOUND' });
    res.status(200).json(result.rows[0]);
  } catch (err) {
    console.error('Error deleting user:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
};

module.exports = {
  register,
  getRegistered,
  getMe,
  updateUserInfo,
  getName,
  deleteUserAccount
};
