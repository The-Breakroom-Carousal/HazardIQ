const express = require('express');
const router = express.Router();
const sosController = require('../controllers/sosController');

require('dotenv').config();

router.post('/send-sos', sosController.sendSos);
router.get('/sos-events/:city', sosController.getSosEventsByCity);
router.put('/sos-events/:id/progress', sosController.updateSosProgress);
router.delete('/sos-events/:id', sosController.deleteSosEvent);

module.exports = router;
