package com.dhruv.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12;

    private static SecretKey secretKey;

    static {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(KEY_SIZE);
            secretKey = keyGenerator.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            byte[] iv = new byte[IV_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            GCMParameterSpec spec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes());

            byte[] encryptedWithIv = new byte[iv.length + encrypted.length];

            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(String encryptedText) {
        try {

            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[IV_SIZE];
            byte[] encrypted = new byte[decoded.length - IV_SIZE];

            System.arraycopy(decoded, 0, iv, 0, IV_SIZE);
            System.arraycopy(decoded, IV_SIZE, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialize the encryption key from a master password and salt.
     * This method derives a 256-bit AES key using PBKDF2 from the master password.
     *
     * @param password The master password
     * @param salt     The salt byte array (same one used for password hashing)
     */
    public static void initializeKey(String password, byte[] salt) {
        try {
            // Derive a 256-bit key from the password and salt using SHA-256 KDF
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] key = digest.digest(password.getBytes());

            // Ensure key is exactly 32 bytes (256 bits) for AES-256
            if (key.length < 32) {
                byte[] extendedKey = new byte[32];
                System.arraycopy(key, 0, extendedKey, 0, key.length);
                key = extendedKey;
            } else if (key.length > 32) {
                key = Arrays.copyOf(key, 32);
            }

            // Create and store the SecretKey for encryption/decryption
            secretKey = new SecretKeySpec(key, 0, 32, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }
}