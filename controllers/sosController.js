const admin = require('../firebase');
const sosModel = require('../models/sosModel');

const sendSos = async (req, res) => {
  const { idToken, type, city, lat, lng } = req.body;

  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    const uid = decoded.uid;

    const sosId = await sosModel.saveSosEvent(uid, type, lat, lng, city);
    const name = await sosModel.getUserName(uid);

    const matchingTokens = await sosModel.getRespondersInCity(city);

    if (matchingTokens.length === 0) {
      return res.status(404).json({ message: "No responders in this city" });
    }

    const payload = {
      notification: {
        title: `🚨 SOS Alert: ${type}`,
        body: `${name} needs help in ${city}. Tap to respond.`,
      },
      data: { lat: String(lat), lng: String(lng), type, name }
    };

    const fcmResponse = await admin.messaging().sendEachForMulticast({
      tokens: matchingTokens,
      ...payload
    });

    res.json({ success: true, sosId, sent: fcmResponse.successCount, failed: fcmResponse.failureCount });

  } catch (err) {
    console.error("❌ Error in /send-sos:", err);
    res.status(500).json({ error: "Internal server error" });
  }
};

const getSosEventsByCity = async (req, res) => {
  try {
    const { city } = req.params;
    const events = await sosModel.getSosEventsByCity(city);
    res.json(events);
  } catch (err) {
    console.error("❌ Error fetching sos events:", err);
    res.status(500).json({ error: "Internal server error" });
  }
};

const updateSosProgress = async (req, res) => {
  const { id } = req.params;
  const { progress, idToken } = req.body;

  const allowed = ['pending', 'acknowledged', 'resolved'];
  if (!allowed.includes(progress)) {
    return res.status(400).json({ error: "Invalid progress value" });
  }

  try {
    let responderUid = null;
    if (progress === 'acknowledged') {
      const decoded = await admin.auth().verifyIdToken(idToken);
      responderUid = decoded.uid;
    }
    const result = await sosModel.updateSosProgress(id, progress, responderUid);

    if (result.rowCount === 0) {
      return res.status(404).json({ error: "SOS event not found" });
    }

    res.json({ message: "Progress updated", event: result.rows[0] });
  } catch (err) {
    console.error("❌ Error updating progress:", err);
    res.status(500).json({ error: "Internal server error" });
  }
};

const deleteSosEvent = async (req, res) => {
  const { id } = req.params;
  try {
    const result = await sosModel.deleteSosEvent(id);

    if (result.rowCount === 0) {
      return res.status(404).json({ error: "SOS event not found" });
    }

    res.json({ message: "SOS event deleted", deletedEvent: result.rows[0] });
  } catch (err) {
    console.error("❌ Error deleting SOS event:", err);
    res.status(500).json({ error: "Internal server error" });
  }
};

module.exports = {
  sendSos,
  getSosEventsByCity,
  updateSosProgress,
  deleteSosEvent
};
