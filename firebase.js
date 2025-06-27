require('dotenv').config();

const admin = require('firebase-admin');

const base64Key = process.env.FIREBASE_SERVICE_ACCOUNT_KEY_BASE64;
if (!base64Key) {
  throw new Error("FIREBASE_SERVICE_ACCOUNT_KEY_BASE64 is not defined in .env");
}

const serviceAccountJSON = Buffer.from(base64Key, 'base64').toString('utf8');
const serviceAccount = JSON.parse(serviceAccountJSON);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

module.exports = admin;
