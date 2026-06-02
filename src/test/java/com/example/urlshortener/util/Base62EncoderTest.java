package com.example.urlshortener.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link Base62Encoder}.
 *
 * Verifies encoding correctness, decode round-trip, minimum length padding,
 * alphabet-boundary behaviour, and error handling.
 */
class Base62EncoderTest {

    private Base62Encoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new Base62Encoder();
    }

    // ─── Encoding ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("encode(1) should produce a 6-character padded code")
    void encode_smallId_returnsMinLengthCode() {
        String code = encoder.encode(1L);
        assertThat(code).hasSize(6);
    }

    @Test
    @DisplayName("encode produces only Base62 characters")
    void encode_usesOnlyBase62Alphabet() {
        for (long id = 1; id <= 1000; id++) {
            String code = encoder.encode(id);
            assertThat(code).matches("[0-9a-zA-Z]+");
        }
    }

    @Test
    @DisplayName("encode(62) should equal 'a' with padding to 6 chars")
    void encode_base62Boundary() {
        // 62 = 1 * 62^1 + 0 → 'a' in Base62, padded to 6
        String code = encoder.encode(62L);
        assertThat(code).endsWith("a0");  // least-significant last char will be 'a'
        assertThat(code).hasSize(6);
    }

    @Test
    @DisplayName("Different IDs always produce different codes")
    void encode_collision_resistance() {
        long[] ids = {1L, 100L, 999L, 100000L, 56800235584L};
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (long id : ids) {
            codes.add(encoder.encode(id));
        }
        assertThat(codes).hasSize(ids.length);
    }

    @Test
    @DisplayName("encode throws for non-positive IDs")
    void encode_zeroOrNegative_throws() {
        assertThatThrownBy(() -> encoder.encode(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
        assertThatThrownBy(() -> encoder.encode(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── Round-trip decode ───────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(longs = {1L, 62L, 3844L, 238329L, 56800235584L, Long.MAX_VALUE / 1000})
    @DisplayName("encode → decode round-trip returns the original ID")
    void roundTrip_encodeDecodeReturnsOriginal(long originalId) {
        String code = encoder.encode(originalId);
        long decoded = encoder.decode(code);
        assertThat(decoded).isEqualTo(originalId);
    }

    // ─── Decode error handling ───────────────────────────────────────────────

    @Test
    @DisplayName("decode throws on blank input")
    void decode_blank_throws() {
        assertThatThrownBy(() -> encoder.decode(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> encoder.decode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("decode throws on invalid characters (e.g., '$', '!')")
    void decode_invalidCharacters_throws() {
        assertThatThrownBy(() -> encoder.decode("abc$12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Base62 character");
    }

    // ─── Validity check ──────────────────────────────────────────────────────

    @Test
    @DisplayName("isValidBase62 returns true for valid Base62 strings")
    void isValidBase62_validInput_returnsTrue() {
        assertThat(encoder.isValidBase62("abc123")).isTrue();
        assertThat(encoder.isValidBase62("ABCXYZ")).isTrue();
        assertThat(encoder.isValidBase62("000000")).isTrue();
    }

    @Test
    @DisplayName("isValidBase62 returns false for invalid characters")
    void isValidBase62_invalidChars_returnsFalse() {
        assertThat(encoder.isValidBase62("abc-12")).isFalse();
        assertThat(encoder.isValidBase62("hello!")).isFalse();
        assertThat(encoder.isValidBase62("")).isFalse();
        assertThat(encoder.isValidBase62(null)).isFalse();
    }
}
