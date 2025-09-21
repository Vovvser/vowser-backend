package com.vowser.backend.infrastructure.crypto;

import com.vowser.backend.common.exception.BusinessException;
import com.vowser.backend.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Slf4j
public class StringCryptoConverter {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;

    private final Key secretKey;

    public StringCryptoConverter(@Value("${vowser.db.encryption-key}") String encryptionKey) {
        if (encryptionKey == null || encryptionKey.length() != 32) {
            log.error("FATAL: Encryption key must be 32 bytes long.");
            throw new IllegalArgumentException("Encryption key must be 32 bytes long.");
        }
        this.secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
        log.info("StringCryptoConverter initialized successfully with AES/GCM.");
    }

    public String encrypt(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Error while encrypting data", e);
            throw new IllegalStateException("Error while encrypting data", e);
        }
    }

    public String decrypt(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_SIZE];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error while decrypting data", e);
            throw new BusinessException(ErrorCode.DECRYPTION_FAILED, e);
        }
    }
}