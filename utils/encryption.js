// utils/encryption.js
const crypto = require('crypto');
const key = crypto.scryptSync(process.env.ENCRYPTION_SECRET, 'salt', 32);
const iv = Buffer.alloc(16, 0);

function encrypt(text) {
  if (!text) throw new Error('Attempted to encrypt undefined or null data');

  const cipher = crypto.createCipheriv('aes-256-cbc', key, iv);
  let encrypted = cipher.update(text, 'utf8', 'hex');
  encrypted += cipher.final('hex');
  return encrypted;
}

function decrypt(encrypted) {
  const decipher = crypto.createDecipheriv('aes-256-cbc', key, iv);
  let decrypted = decipher.update(encrypted, 'hex', 'utf8');
  decrypted += decipher.final('utf8'); 
  return decrypted;
}

module.exports = { encrypt, decrypt };
