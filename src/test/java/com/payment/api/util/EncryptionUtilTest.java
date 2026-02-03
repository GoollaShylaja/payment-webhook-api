package com.payment.api.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;
    private static final String SECRET_KEY = "TestSecretKey12345TestSecretKey12";
    private static final String CARD_NUMBER = "4532015112830366";

    @BeforeEach
    void setUp() {
        encryptionUtil = new EncryptionUtil();
        ReflectionTestUtils.setField(encryptionUtil, "secretKey", SECRET_KEY);
    }

    @Test
    void encrypt_ReturnsEncryptedString() {
        // Act
        String encrypted = encryptionUtil.encrypt(CARD_NUMBER);

        // Assert
        assertNotNull(encrypted);
        assertNotEquals(CARD_NUMBER, encrypted);
        assertTrue(encrypted.length() > 0);
    }

    @Test
    void decrypt_ReturnsOriginalCardNumber() {
        // Arrange
        String encrypted = encryptionUtil.encrypt(CARD_NUMBER);

        // Act
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Assert
        assertEquals(CARD_NUMBER, decrypted);
    }

    @Test
    void encryptDecrypt_MultipleRounds() {
        // Test that encryption/decryption works consistently
        for (int i = 0; i < 10; i++) {
            String encrypted = encryptionUtil.encrypt(CARD_NUMBER);
            String decrypted = encryptionUtil.decrypt(encrypted);
            assertEquals(CARD_NUMBER, decrypted);
        }
    }

    @Test
    void maskCardNumber_ShowsLastFourDigits() {
        // Act
        String masked = encryptionUtil.maskCardNumber(CARD_NUMBER);

        // Assert
        assertEquals("****0366", masked);
    }

    @Test
    void maskCardNumber_HandlesShortNumber() {
        // Act
        String masked = encryptionUtil.maskCardNumber("123");

        // Assert
        assertEquals("****", masked);
    }

    @Test
    void maskCardNumber_HandlesNull() {
        // Act
        String masked = encryptionUtil.maskCardNumber(null);

        // Assert
        assertEquals("****", masked);
    }

    @Test
    void maskCardNumber_HandlesDifferentLengths() {
        assertEquals("****1234", encryptionUtil.maskCardNumber("1234567891234"));
        assertEquals("****5678", encryptionUtil.maskCardNumber("12345678"));
        assertEquals("****0005", encryptionUtil.maskCardNumber("37828224631000005"));
    }
}