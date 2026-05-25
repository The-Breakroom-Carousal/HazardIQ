// routes/fir.js
const express = require('express');
const router = express.Router();
const firController = require('../controllers/firController');

// Create an account / Register user
router.post('/register', firController.register);

// Get all users
router.get('/registered', firController.getRegistered);

// Get current user
router.get('/me', firController.getMe);

// Update user info
router.put('/user', firController.updateUserInfo);

// Get user name by uid
router.get('/get-name', firController.getName);

// Delete user account
router.delete('/user', firController.deleteUserAccount);

module.exports = router;

  