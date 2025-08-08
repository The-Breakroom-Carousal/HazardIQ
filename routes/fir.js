// routes/fir.js
const express = require('express');
const router = express.Router();
const pool  = require('../db');
const admin=require('../firebase');


// Create an account
router.post('/register', async (req, res) => {
  const { idToken, name, email, role, location_lat, location_lng, fcm_token } = req.body;
  
  try {
    const decodedToken=await admin.auth().verifyIdToken(idToken);
    const firebase_uid = decodedToken.uid;

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
    res.json({ success: true, user: result.rows[0] });
    //res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('Error creating usersdata:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

// Get all users
router.get('/registered', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM users ORDER BY id DESC');
    res.status(200).json(result.rows);
  } catch (err) {
    console.error('Error fetching user details:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

// // Get a specific user detail by ID
router.get('/me', async (req, res) => {
  const idToken = req.headers.idtoken;
  if (!idToken) return res.status(401).json({ error: 'Missing ID token' });

  try {
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    const firebase_uid = decodedToken.uid;

    const result = await pool.query('SELECT * FROM users WHERE firebase_uid = $1', [firebase_uid]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });

    res.status(200).json(result.rows[0]);
  } catch (err) {
    console.error('Error fetching user by ID:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});


//update  any userinfo
router.put('/user', async (req, res) => {
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

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.status(200).json(result.rows[0]);
  } catch (err) {
    console.error('❌ Error updating user:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});


router.get('/get-name',async(req,res)=>{
  const uid=req.headers.uid;
  if (!uid) return res.status(401).json({ error: 'Missing uid' });
  try{
    const result=await pool.query('SELECT name FROM users WHERE firebase_uid =$1',[uid]);
    if (result.length==0 ) return res.status(404).json({error :'user NOT FOUND'});
    res.status(200).json(result.rows[0]);

  }catch(err){
    console.error('Error finding user:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
})


  
router.delete('/user',async(req,res)=>
{
  const idToken=req.headers.idToken;
  if (!idToken) return res.status(401).json({ error: 'Missing ID token' });
  try{
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    const firebase_uid = decodedToken.uid;
    const result=await pool.query('DELETE FROM users WHERE firebase_uid =$1',[firebase_uid]);
    if (result.length==0 ) return res.status(404).json({error :'user NOT FOUND'});
    res.status(200).json(result.rows[0]);

  }catch(err){
    console.error('Error deleting user:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
})



module.exports = router;

  