package com.agente.agente_ia_local.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
public class EncryptionUtil implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptionUtil.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final String MASTER_KEY_ENV = "ENCRYPTION_MASTER_KEY";

    private static SecretKey getSecretKey() {
        String keyEnv = System.getenv(MASTER_KEY_ENV);
        if (keyEnv == null || keyEnv.isBlank()) {
            keyEnv = "agenteia256bitkeychangemeinprod!"; // default for dev
        }
        byte[] keyBytes = new byte[32];
        byte[] srcBytes = keyEnv.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(srcBytes, 0, keyBytes, 0, Math.min(srcBytes.length, 32));
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        try {
            SecretKey key = getSecretKey();
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Error al encriptar dato", e);
            return plaintext;
        }
    }

    public static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return encrypted;
        }
        try {
            SecretKey key = getSecretKey();
            byte[] combined = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error al desencriptar dato", e);
            return encrypted;
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return decrypt(dbData);
    }
}
