const express = require('express');
const router = express.Router();
const hazardController = require('../controllers/hazardController');

require('dotenv').config();

router.post('/save-hazard', hazardController.saveHazard);
router.get('/find-hazard', hazardController.findHazard);
router.post('/remove-hazard', hazardController.removeHazard);

module.exports = router;


