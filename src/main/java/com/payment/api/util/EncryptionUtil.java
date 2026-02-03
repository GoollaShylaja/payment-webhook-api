package com.payment.api.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    
    @Value("${encryption.secret.key}")
    private String secretKey;

    /**
     * Encrypts a card number using AES-256
     */
    public String encrypt(String cardNumber) {
        try {
            SecretKeySpec key = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting card number", e);
        }
    }

    /**
     * Decrypts an encrypted card number
     */
    public String decrypt(String encryptedCardNumber) {
        try {
            SecretKeySpec key = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedCardNumber));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting card number", e);
        }
    }

    /**
     * Masks a card number showing only last 4 digits
     */
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        int length = cardNumber.length();
        String lastFour = cardNumber.substring(length - 4);
        return "****" + lastFour;
    }

    /**
     * Generates a secret key from the provided secret
     */
    private SecretKeySpec generateKey(String secret) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // Use only first 128 bits
        return new SecretKeySpec(key, ALGORITHM);
    }
}