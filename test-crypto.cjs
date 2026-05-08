const crypto = require('crypto');

const ITERATIONS = 65536;
const KEY_LENGTH = 32; // 256 bits
const DIGEST = 'sha256';
const ALGORITHM = 'aes-256-gcm';
const IV_SIZE = 12;

function deriveKey(password, saltBase64) {
    const saltBuffer = Buffer.from(saltBase64, 'base64');
    return crypto.pbkdf2Sync(password, saltBuffer, ITERATIONS, KEY_LENGTH, DIGEST);
}

function encrypt(plainText, keyBuffer) {
    const iv = crypto.randomBytes(IV_SIZE);
    const cipher = crypto.createCipheriv(ALGORITHM, keyBuffer, iv);
    
    let encrypted = cipher.update(plainText, 'utf8'); // Returns Buffer
    encrypted = Buffer.concat([encrypted, cipher.final()]);
    
    const authTag = cipher.getAuthTag();
    const result = Buffer.concat([iv, encrypted, authTag]);
    return result.toString('base64');
}

function decrypt(encryptedTextBase64, keyBuffer) {
    const decoded = Buffer.from(encryptedTextBase64, 'base64');
    const iv = decoded.subarray(0, IV_SIZE);
    const encryptedAndTag = decoded.subarray(IV_SIZE);
    
    const authTag = encryptedAndTag.subarray(encryptedAndTag.length - 16);
    const encrypted = encryptedAndTag.subarray(0, encryptedAndTag.length - 16);
    
    const decipher = crypto.createDecipheriv(ALGORITHM, keyBuffer, iv);
    decipher.setAuthTag(authTag);
    
    let decrypted = decipher.update(encrypted, undefined, 'utf8');
    decrypted += decipher.final('utf8');
    return decrypted;
}

const salt = crypto.randomBytes(16).toString('base64');
const pass = "MySecretApp123";
const key = deriveKey(pass, salt);

const text = "SuperSecretPassword";
const cipherText = encrypt(text, key);
console.log("Encrypted:", cipherText);

const plain = decrypt(cipherText, key);
console.log("Decrypted:", plain);
if(plain === text) console.log("SUCCESS!");
