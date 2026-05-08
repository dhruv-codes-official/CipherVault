package com.dhruv.security; // Declares this file belongs to the 'security' package alongside EncryptionService and PasswordHasher

import java.security.SecureRandom; // Cryptographically secure random number generator — essential for passwords; Math.random() is NOT secure enough

/**
 * PasswordGenerator — Generates cryptographically random passwords.
 *
 * Uses SecureRandom (backed by the OS entropy pool) rather than java.util.Random,
 * which is predictable and unsuitable for security-critical applications.
 * The character set covers uppercase, lowercase, digits, and symbols to maximise
 * entropy per character (approx. 6.5 bits/char for 88 chars).
 */
public class PasswordGenerator {

    // ── Character Pool ────────────────────────────────────────────────────────
    private static final String CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + // 26 uppercase letters — increases character space
            "abcdefghijklmnopqrstuvwxyz" + // 26 lowercase letters — increases character space
            "0123456789"                  + // 10 digits — required by most password policies
            "!@#$%^&*()_+-=";              // 15 special symbols — dramatically increases guessing difficulty
    // Total pool size: 77 characters → each character carries log2(77) ≈ 6.27 bits of entropy

    private static final SecureRandom random = new SecureRandom(); // Static instance reused across calls; SecureRandom is thread-safe and expensive to initialise

    /**
     * generate — Produces a random password of the specified length.
     *
     * Algorithm:
     *   For each position in the output string, pick a cryptographically random
     *   index into the CHARS pool; the character at that index is appended.
     *
     * @param length The number of characters in the generated password (e.g. 16)
     * @return       A randomly generated password string of the specified length
     */
    public static String generate(int length) {

        StringBuilder password = new StringBuilder(); // StringBuilder is used for efficient repeated string concatenation in a loop

        for (int i = 0; i < length; i++) {            // Iterate 'length' times — one character is chosen per iteration
            int index = random.nextInt(CHARS.length()); // Generate a secure random integer in range [0, CHARS.length()-1]; maps to one character position
            password.append(CHARS.charAt(index));        // Look up the character at that random index and append it to the result
        }

        return password.toString(); // Convert the StringBuilder to an immutable String and return it to the caller
    }
}