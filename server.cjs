// ─── IMPORTS ──────────────────────────────────────────────────────────────────
const express = require('express');        // Express is the web framework used to create the HTTP REST API server
const cors = require('cors');              // CORS (Cross-Origin Resource Sharing) middleware allows the frontend (port 5173) to call this backend (port 3001)
const sqlite3 = require('sqlite3').verbose(); // SQLite3 is the embedded database driver; .verbose() gives detailed error traces during development
const crypto = require('crypto');          // Node's built-in crypto module provides PBKDF2 hashing, AES encryption, and secure random bytes
const jwt = require('jsonwebtoken');       // jsonwebtoken is used to create and verify JSON Web Tokens (JWTs) for stateless authentication
const path = require('path');             // path is used to construct file-system paths in a cross-platform way

// ─── APP SETUP ────────────────────────────────────────────────────────────────
const app = express();                    // Create the Express application instance
app.use(cors());                          // Apply CORS middleware globally so all routes accept requests from any origin (the React dev server)
app.use(express.json());                  // Parse incoming request bodies with Content-Type: application/json so req.body is available

// ─── DATABASE SETUP ───────────────────────────────────────────────────────────
const dbPath = path.join(__dirname, '..', 'password_manager.db'); // Resolve absolute path to the SQLite database file located one directory above web-ui/
const db = new sqlite3.Database(dbPath, (err) => { // Open (or create) the SQLite database at dbPath; callback fires after the connection is established
    if (err) {
        console.error('Error opening database', err.message); // Print a descriptive error if the DB cannot be opened (e.g. bad path or permissions)
    } else {
        console.log('Connected to the SQLite database.');      // Confirm successful DB connection in the terminal
        // Make sure tables exist if not already created by Java app
        db.run(`CREATE TABLE IF NOT EXISTS settings(
            id INTEGER PRIMARY KEY,        -- Single row; stores the master password hash
            master_hash TEXT NOT NULL,     -- PBKDF2-derived hash of the master password (Base64)
            salt TEXT NOT NULL             -- Random 16-byte salt used for hashing and key derivation (Base64)
        )`);
        // Create the passwords table to store each vault entry; UNIQUE prevents duplicate (website, username) pairs
        db.run(`CREATE TABLE IF NOT EXISTS passwords(
            id INTEGER PRIMARY KEY AUTOINCREMENT, -- Auto-incrementing unique ID for each entry
            website TEXT NOT NULL,                -- The name/domain of the service (e.g. "Google")
            username TEXT NOT NULL,               -- The account username or email for that service
            password TEXT NOT NULL,               -- AES-256-GCM encrypted password stored as Base64
            UNIQUE(website, username)             -- Prevents saving two entries for the same site + user combo
        )`);
    }
});

// ─── JWT SECRET ───────────────────────────────────────────────────────────────
const JWT_SECRET = crypto.randomBytes(32).toString('hex'); // Generate a 32-byte cryptographically random secret for signing JWTs; regenerated on every server restart so old tokens are invalidated

// ─── CRYPTOGRAPHY CONSTANTS ───────────────────────────────────────────────────
const ITERATIONS = 65536;    // Number of PBKDF2 iterations; higher = slower brute-force attacks (NIST recommends ≥ 10,000)
const KEY_LENGTH = 32;       // Derived key length in bytes (32 bytes = 256 bits, matching AES-256 key size)
const DIGEST = 'sha256';     // Hash function used inside PBKDF2; SHA-256 is secure and widely supported
const ALGORITHM = 'aes-256-gcm'; // AES in Galois/Counter Mode (GCM) provides both encryption AND authentication tag, preventing tampering
const IV_SIZE = 12;          // GCM Initialization Vector (IV/nonce) size in bytes; 12 bytes is the NIST-recommended size for GCM

// ─── HELPER: Hash a password with a given salt using PBKDF2 ──────────────────
function hashPassword(password, saltBase64) {
    const saltBuffer = Buffer.from(saltBase64, 'base64'); // Decode the Base64-encoded salt string back into raw bytes
    const hashBuffer = crypto.pbkdf2Sync(                 // Run PBKDF2 synchronously (blocking) — acceptable here since it's a one-off auth operation
        password,       // The plain-text master password entered by the user
        saltBuffer,     // The random salt that was generated during setup
        ITERATIONS,     // Number of hash iterations to increase computation time
        KEY_LENGTH,     // Length of the output hash in bytes
        DIGEST          // The underlying hash function (SHA-256)
    );
    return hashBuffer.toString('base64'); // Convert the raw hash bytes to Base64 for safe storage/comparison in the DB
}

// ─── HELPER: Derive AES encryption key from the master password ───────────────
function deriveKey(password, saltBase64) {
    // Java PBEKeySpec uses salt bytes
    const saltBuffer = Buffer.from(saltBase64, 'base64'); // Decode the salt from Base64 to raw bytes so it matches Java's byte[] representation
    return crypto.pbkdf2Sync(password, saltBuffer, ITERATIONS, KEY_LENGTH, DIGEST); // Return 32 raw bytes to be used directly as the AES-256 key
}

// ─── HELPER: Decrypt an AES-256-GCM ciphertext ────────────────────────────────
function decrypt(encryptedTextBase64, keyBuffer) {
    try {
        const decoded = Buffer.from(encryptedTextBase64, 'base64'); // Decode the stored Base64 ciphertext back into a raw Buffer
        const iv = decoded.subarray(0, IV_SIZE);                     // Extract the first 12 bytes as the IV (prepended during encryption)
        const encryptedAndTag = decoded.subarray(IV_SIZE);           // The remaining bytes contain the encrypted data + the 16-byte GCM auth tag

        // GCM tag is 16 bytes at the end
        const authTag = encryptedAndTag.subarray(encryptedAndTag.length - 16); // Slice the last 16 bytes as the GCM authentication tag
        const encrypted = encryptedAndTag.subarray(0, encryptedAndTag.length - 16); // Everything before the tag is the actual ciphertext

        const decipher = crypto.createDecipheriv(ALGORITHM, keyBuffer, iv); // Create a GCM decipher using the derived key and the extracted IV
        decipher.setAuthTag(authTag); // Provide the authentication tag; decryption will throw if data has been tampered with

        let decrypted = decipher.update(encrypted, undefined, 'utf8'); // Decrypt the ciphertext and start converting bytes to a UTF-8 string
        decrypted += decipher.final('utf8'); // Finalize decryption and flush any remaining buffered bytes; also verifies the auth tag
        return decrypted; // Return the plain-text password as a UTF-8 string
    } catch (e) {
        console.error('Decryption error:', e.message); // Log the specific decryption error (e.g. auth tag mismatch = tampering detected)
        throw e; // Re-throw so the caller can handle the failure gracefully (returns '[Decryption Failed]')
    }
}

// ─── HELPER: Encrypt plain-text with AES-256-GCM ─────────────────────────────
function encrypt(plainText, keyBuffer) {
    const iv = crypto.randomBytes(IV_SIZE); // Generate a fresh random 12-byte IV for every encryption — must never reuse an IV with the same key
    const cipher = crypto.createCipheriv(ALGORITHM, keyBuffer, iv); // Create a GCM cipher using the AES-256 key and the random IV

    let encrypted = cipher.update(plainText, 'utf8'); // Feed the plain-text password (UTF-8 string) into the cipher and get partial ciphertext bytes
    encrypted = Buffer.concat([encrypted, cipher.final()]); // Finalize encryption and concatenate any remaining buffered bytes

    const authTag = cipher.getAuthTag(); // Retrieve the 16-byte GCM authentication tag generated during encryption

    // Concatenate IV + encrypted bytes + authTag
    // Java Cipher.doFinal attaches the GCM tag implicitly at the end of the byte array that it returns.
    const result = Buffer.concat([iv, encrypted, authTag]); // Pack: [12-byte IV][ciphertext][16-byte auth tag] into a single Buffer

    return result.toString('base64'); // Encode the packed Buffer as Base64 so it can be safely stored as a TEXT field in SQLite
}

// ─── MIDDLEWARE: JWT Authentication ───────────────────────────────────────────
function authenticateToken(req, res, next) {
    const authHeader = req.headers['authorization']; // Read the Authorization header from the incoming HTTP request
    const token = authHeader && authHeader.split(' ')[1]; // Extract the Bearer token: "Authorization: Bearer <token>" → split on space, take index 1

    if (token == null) return res.sendStatus(401); // If no token was sent, respond with HTTP 401 Unauthorized and stop processing

    jwt.verify(token, JWT_SECRET, (err, user) => { // Verify the token's signature and expiry using the server's secret key
        if (err) return res.sendStatus(403); // If the token is invalid or expired, respond with HTTP 403 Forbidden
        req.user = user; // Attach the decoded JWT payload (contains { salt }) to req.user for use in route handlers
        next(); // Call next() to pass control to the actual route handler
    });
}

// ─── ROUTE: POST /api/auth/setup ──────────────────────────────────────────────
app.post('/api/auth/setup', (req, res) => {
    // Setup initial master password
    const { password } = req.body; // Destructure the password from the JSON request body

    db.get('SELECT * FROM settings LIMIT 1', [], (err, row) => { // Query the settings table to check if a master password has already been configured
        if (err) return res.status(500).json({ error: err.message }); // Internal server error if the DB query itself fails
        if (row) return res.status(400).json({ error: 'Master password already set' }); // Prevent re-setup if a master password already exists

        const saltBuffer = crypto.randomBytes(16);         // Generate a cryptographically secure 16-byte random salt for this user
        const saltBase64 = saltBuffer.toString('base64');  // Encode the salt as Base64 so it can be stored as text in the DB
        const hashBase64 = hashPassword(password, saltBase64); // Derive the PBKDF2 hash of the master password using the new salt

        db.run('INSERT INTO settings (master_hash, salt) VALUES (?, ?)', [hashBase64, saltBase64], function(err) { // Store the hash and salt as a single row in the settings table
            if (err) return res.status(500).json({ error: err.message }); // Handle any DB write errors
            res.json({ message: 'Master password set successfully' }); // Respond with success message
        });
    });
});

// ─── ROUTE: POST /api/auth/login ─────────────────────────────────────────────
app.post('/api/auth/login', (req, res) => {
    const { password } = req.body; // Read the plain-text master password from the request body

    db.get('SELECT master_hash, salt FROM settings LIMIT 1', [], (err, row) => { // Fetch the stored hash and salt from the settings table
        if (err) return res.status(500).json({ error: err.message }); // Handle DB read errors
        if (!row) return res.status(404).json({ error: 'Master password not set. Setup required.' }); // If no settings row exists, the app hasn't been set up yet

        const hashBase64 = hashPassword(password, row.salt); // Re-derive the PBKDF2 hash of the submitted password using the stored salt

        if (hashBase64 === row.master_hash) { // Compare the freshly derived hash with the stored hash in constant-time fashion (note: ideally use crypto.timingSafeEqual for timing-attack resistance)
            // Generate token, and derive key so we can hold it in memory or send to client
            // For security, we just send a JWT, and keeping salt to derive for operations
            const token = jwt.sign({ salt: row.salt }, JWT_SECRET, { expiresIn: '1h' }); // Create a signed JWT payload containing the salt; expires in 1 hour so sessions auto-expire
            res.json({ token, salt: row.salt }); // Return the JWT and salt to the client; the client uses the salt + master password to derive the decryption key
        } else {
            res.status(401).json({ error: 'Invalid master password' }); // Password mismatch: respond with HTTP 401 Unauthorized
        }
    });
});

// ─── ROUTE: GET /api/passwords ────────────────────────────────────────────────
app.get('/api/passwords', authenticateToken, (req, res) => {
    // Requires master password to decrypt for now, let's take it via headers for simplicity
    const masterPassword = req.headers['x-master-password']; // Read the master password from a custom header (x-master-password) to derive the decryption key
    if (!masterPassword) return res.status(400).json({ error: 'Master password required for decryption' }); // Reject if the header is missing — can't decrypt without it

    const keyBuffer = deriveKey(masterPassword, req.user.salt); // Derive the 256-bit AES key from the master password and the salt stored in the JWT payload

    db.all('SELECT * FROM passwords', [], (err, rows) => { // Fetch ALL password rows from the DB
        if (err) return res.status(500).json({ error: err.message }); // Handle DB read errors

        const decryptedRows = rows.map(row => { // Iterate over all stored encrypted passwords and decrypt each one
            let decryptedPass = '[Decryption Failed]'; // Default fallback value if decryption throws an error
            try {
                decryptedPass = decrypt(row.password, keyBuffer); // Attempt AES-256-GCM decryption using the derived key
            } catch (e) {
                // Fallback to plain text if old entry
                decryptedPass = row.password; // If decryption fails (e.g. old plaintext entry), return the raw value so old data isn't lost
            }
            return {
                id: row.id,             // The unique database ID of this entry
                website: row.website,   // The website/service name
                username: row.username, // The account username
                password: decryptedPass, // The now-decrypted plain-text password
                updated: 'Just now'     // Placeholder timestamp (no created_at column in the DB schema currently)
            };
        });

        res.json(decryptedRows); // Send the array of decrypted entries back to the frontend as JSON
    });
});

// ─── ROUTE: POST /api/passwords ───────────────────────────────────────────────
app.post('/api/passwords', authenticateToken, (req, res) => {
    const masterPassword = req.headers['x-master-password']; // Read the master password header so we can derive the encryption key
    if (!masterPassword) return res.status(400).json({ error: 'Master password required for encryption' }); // Cannot encrypt without the master password

    const { website, username, password } = req.body; // Destructure the new vault entry details from the request body
    const keyBuffer = deriveKey(masterPassword, req.user.salt); // Derive the AES key from the master password and JWT salt

    let encryptedPassword; // Will hold the Base64-encoded ciphertext after encryption
    try {
        encryptedPassword = encrypt(password, keyBuffer); // AES-256-GCM encrypt the plain-text password using the derived key
    } catch (e) {
        return res.status(500).json({ error: 'Encryption failed' }); // Return 500 if encryption throws (should rarely happen unless key is bad)
    }

    db.run('INSERT INTO passwords (website, username, password) VALUES (?, ?, ?)', // Parameterized query prevents SQL injection
        [website, username, encryptedPassword], // Bind the values: website, username, and the encrypted password blob
        function(err) {
            if (err) {
                if (err.message.includes('UNIQUE')) { // SQLite throws a UNIQUE constraint violation if (website, username) already exists
                    return res.status(400).json({ error: 'Password already exists for this website and username' }); // Inform the user of the duplicate
                }
                return res.status(500).json({ error: err.message }); // Handle any other DB write errors
            }
            res.json({ id: this.lastID, website, username }); // Return the new entry's auto-generated ID and the website/username to confirm success
        }
    );
});

// ─── ROUTE: DELETE /api/passwords/:id ────────────────────────────────────────
app.delete('/api/passwords/:id', authenticateToken, (req, res) => {
    db.run('DELETE FROM passwords WHERE id = ?', [req.params.id], function(err) { // Delete the row matching the ID from the URL parameter; parameterized to prevent SQL injection
        if (err) return res.status(500).json({ error: err.message }); // Handle DB deletion errors
        res.json({ message: 'Deleted successfully' }); // Confirm deletion to the frontend
    });
});

// ─── SERVER START ─────────────────────────────────────────────────────────────
const PORT = process.env.PORT || 3001; // Use environment variable PORT if set (e.g. in production), otherwise default to port 3001
app.listen(PORT, () => {
    console.log(`Express server running on port ${PORT}`); // Confirm the server is listening; this prints in the terminal when you run npm run dev
});
