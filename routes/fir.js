// routes/fir.js
const express = require('express');
const router = express.Router();
const pool  = require('../db');
const admin=require('../firebase');


// Create an account
router.post('/register', async (req, res) => {
  const { idToken, name, email, role } = req.body;
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
    console.error('Error fetching FIRs:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

// // Get a specific FIR by ID
// router.get('/:id',async(req,res)=>
// {
//   try{
//         const result = await pool.query('SELECT * FROM fir WHERE id = $1',[id]);
//         if (result.rows.length === 0) return res.status(404).json({ error: 'FIR not found.' });
//         res.status(200).json(result.rows[0]);
        
//   }catch(err){
//     console.error('Error fetching FIR by ID:', err);
//       res.status(500).json({ error: 'Internal server error.' });
//   }
// }
// );

// //update  an fir
// router.put('./id',async(req,res)=>
// {
//   try{
//     const { id } = req.params;
//     const { officer_name, station, description, priority } = req.body;

//     const result = await pool.query(
//       'UPDATE fir SET officer_name = $1, station = $2, description = $3, priority = $4 WHERE id = $5 RETURNING *',
//       [officer_name, station, description, priority, id]
//     );
//     if (result.length==0 ) return res.status(404).json({error :'FIR NOT FOUND'});
//     res.status(200).json(result.rows[0]);


//   }catch(err){
//     console.error('Error deleting FIR:', err);
//     res.status(500).json({ error: 'Internal server error.' });
//   }
// })
  
// router.delete('./id',async(req,res)=>
// {
//   try{
//     const result=await pool.query('DELETE FROM fir WHERE id =$1',[id]);
//     if (result.length==0 ) return res.status(404).json({error :'FIR NOT FOUND'});
//     res.status(200).json(result.rows[0]);

//   }catch(err){
//     console.error('Error deleting FIR:', err);
//     res.status(500).json({ error: 'Internal server error.' });
//   }
// })



module.exports = router;

