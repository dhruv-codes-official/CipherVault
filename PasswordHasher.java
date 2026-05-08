package com.dhruv.security; // Declares this file belongs to the 'security' package — groups all cryptography-related classes

import javax.crypto.SecretKeyFactory; // Provides implementations for key derivation functions (KDFs) like PBKDF2
import javax.crypto.spec.PBEKeySpec;  // PBEKeySpec = Password-Based Encryption Key Specification — holds password, salt, iterations, and key length for PBKDF2
import java.security.SecureRandom;    // Cryptographically secure pseudo-random number generator (CSPRNG) for generating unpredictable salts
import java.util.Base64;              // Java's built-in Base64 encoder/decoder — converts raw byte arrays to storable text strings

/**
 * PasswordHasher — Responsible for hashing the master password and generating salts.
 *
 * Uses PBKDF2 (Password-Based Key Derivation Function 2) with HMAC-SHA256.
 * PBKDF2 is intentionally slow (configurable via iterations) to resist brute-force attacks.
 * A unique random salt is used for each user to prevent rainbow-table attacks.
 */
public class PasswordHasher {

    // ── Constants ──────────────────────────────────────────────────────────────
    private static final int ITERATIONS = 65536; // Number of PBKDF2 hash iterations; 65536 (2^16) makes each attempt take ~50ms, slowing brute force significantly
    private static final int KEY_LENGTH = 256;   // Output key length in bits (256 bits = 32 bytes); matches AES-256 key size for compatibility with EncryptionService

    /**
     * hashPassword — Derives a secure hash from the master password + salt using PBKDF2.
     *
     * @param password The plain-text master password entered by the user
     * @param salt     The random 16-byte salt previously generated for this user
     * @return         Base64-encoded string of the derived hash — safe to store in the database
     * @throws Exception if the cryptographic algorithm is unavailable (extremely rare on any JVM)
     */
    public static String hashPassword(String password, byte[] salt) throws Exception {

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), // Convert String to char[] — PBEKeySpec requires char[] for security (char[] can be zeroed from memory, String cannot)
                salt,                   // The random salt makes the hash unique even if two users share the same password
                ITERATIONS,             // Apply the hash function 65536 times to increase computation cost for attackers
                KEY_LENGTH              // Length of the output hash in bits
        );

        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"); // Get the PBKDF2 implementation that uses HMAC-SHA256 as the internal pseudorandom function (PRF)

        byte[] hash = factory.generateSecret(spec).getEncoded(); // Run the PBKDF2 derivation and get the raw key bytes

        return Base64.getEncoder().encodeToString(hash); // Encode the raw bytes as a Base64 string so it can be stored as TEXT in the SQLite database
    }

    /**
     * generateSalt — Creates a cryptographically random 16-byte salt.
     *
     * A salt is a unique random value added to the password before hashing.
     * It ensures that two identical passwords produce completely different hashes,
     * defeating rainbow table and precomputation attacks.
     *
     * @return A 16-byte (128-bit) random salt as a raw byte array
     */
    public static byte[] generateSalt() {

        SecureRandom random = new SecureRandom(); // Instantiate a CSPRNG backed by the OS's entropy source (e.g. /dev/urandom on Linux/macOS)
        byte[] salt = new byte[16];               // Allocate a 16-byte (128-bit) array — 128 bits is the standard recommended salt length
        random.nextBytes(salt);                   // Fill the array with cryptographically secure random bytes

        return salt; // Return the randomised salt to the caller (usually saved to the DB alongside the hash)
    }
}