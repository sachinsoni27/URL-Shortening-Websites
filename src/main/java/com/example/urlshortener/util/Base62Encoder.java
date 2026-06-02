package com.example.urlshortener.util;

import org.springframework.stereotype.Component;

/**
 * Base62 encoder / decoder for generating short URL codes.
 *
 * Alphabet (62 characters): 0-9, a-z, A-Z
 * Encoding a Long ID to Base62 always produces a deterministic,
 * URL-safe string. IDs >= 1 will produce codes of 1–7 characters
 * for IDs up to 62^7 = ~3.5 trillion.
 *
 * Example:
 *   encode(1)         → "1"
 *   encode(62)        → "a"
 *   encode(3844)      → "100" (62^2)
 *   encode(56800235584L) → "ZZZZZZ" (6 chars max for IDs up to this)
 *
 * Note: codes are left-padded to a minimum length of 6 characters
 * for consistent URL aesthetics.
 */
@Component
public class Base62Encoder {

    private static final String ALPHABET =
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int BASE = ALPHABET.length();           // 62
    private static final int MIN_LENGTH = 6;

    /**
     * Encode a positive Long ID into a Base62 string.
     * Result is left-padded with '0' to ensure at least MIN_LENGTH characters.
     *
     * @param id positive numeric identifier (> 0)
     * @return Base62-encoded string
     * @throws IllegalArgumentException if id <= 0
     */
    public String encode(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be positive, got: " + id);
        }

        StringBuilder sb = new StringBuilder();
        long remaining = id;

        while (remaining > 0) {
            sb.append(ALPHABET.charAt((int) (remaining % BASE)));
            remaining /= BASE;
        }

        // Pad to minimum length
        while (sb.length() < MIN_LENGTH) {
            sb.append('0');
        }

        // The digits were appended least-significant-first; reverse for correct order
        return sb.reverse().toString();
    }

    /**
     * Decode a Base62 string back to its original Long ID.
     *
     * @param code Base62-encoded string
     * @return the original numeric ID
     * @throws IllegalArgumentException if the string contains invalid characters
     */
    public long decode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code must not be blank");
        }

        long result = 0;
        for (char c : code.toCharArray()) {
            int index = ALPHABET.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException(
                    "Invalid Base62 character '" + c + "' in code: " + code);
            }
            result = result * BASE + index;
        }
        return result;
    }

    /**
     * Quick check whether a string is a valid Base62 code.
     */
    public boolean isValidBase62(String code) {
        if (code == null || code.isBlank()) return false;
        for (char c : code.toCharArray()) {
            if (ALPHABET.indexOf(c) == -1) return false;
        }
        return true;
    }
}
