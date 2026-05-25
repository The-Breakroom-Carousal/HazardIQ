const admin = require('../firebase');
const chatbotModel = require('../models/chatbotModel');

const verifyIdTokenFromHeader = async (req) => {
  const idToken = req.header('idtoken');
  if (!idToken) throw { status: 401, message: 'Missing ID token' };
  const decoded = await admin.auth().verifyIdToken(idToken);
  return decoded.uid;
};

const aiChat = async (req, res) => {
  try {
    const uid = await verifyIdTokenFromHeader(req);
    const { msg } = req.body;
    if (!msg) return res.status(400).json({ error: 'msg is required' });
    
    await chatbotModel.saveChatMessage(uid, "user", msg);
    
    const response = await chatbotModel.callAIService(uid);
    const assistantMsg = response?.response || JSON.stringify(response);

    return res.json({ success: true, model: response });
  } catch (err) {
    console.error("❌ Error in /ai-chat:", err);
    res.status(500).json({ error: "Internal server error" });
  }
};

const getHistory = async (req, res) => {
  try {
    const uid = await verifyIdTokenFromHeader(req);
    const history = await chatbotModel.getChatHistory(uid);
    return res.json({ success: true, history });
  } catch (err) {
    console.error('❌ Error fetching history:', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
};

const restartChat = async (req, res) => {
  try {
    const uid = await verifyIdTokenFromHeader(req);
    await chatbotModel.clearChatHistory(uid);
    return res.json({ success: true });
  } catch (err) {
    console.error('❌ Error restarting chat:', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
};

module.exports = {
  aiChat,
  getHistory,
  restartChat
};
