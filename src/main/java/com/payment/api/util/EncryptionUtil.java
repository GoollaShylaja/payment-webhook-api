package com.payment.api.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${encryption.secret.key}")
    private String secretKey;

    public String encrypt(String cardNumber) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            SecretKeySpec key = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting card number", e);
        }
    }

    public String decrypt(String encryptedCardNumber) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedCardNumber);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            SecretKeySpec key = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting card number", e);
        }
    }

    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        int maskLength = cardNumber.length() - 4;
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "*".repeat(maskLength) + lastFour;
    }

    private SecretKeySpec generateKey(String secret) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
        key = Arrays.copyOf(key, 32); // 256 bits for AES-256
        return new SecretKeySpec(key, ALGORITHM);
    }
}
