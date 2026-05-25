const express = require('express');
const router = express.Router();
const chatbotController = require('../controllers/chatbotController');

router.put('/ai-chat', chatbotController.aiChat);
router.get('/history', chatbotController.getHistory);
router.post('/restart', chatbotController.restartChat);

module.exports = router;
