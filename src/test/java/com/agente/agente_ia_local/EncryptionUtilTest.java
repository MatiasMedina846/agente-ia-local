package com.agente.agente_ia_local;

import com.agente.agente_ia_local.encryption.EncryptionUtil;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionUtilTest {

    @Test
    @DisplayName("Encriptar y desencriptar texto plano")
    void encryptDecrypt() {
        String original = "sk-test-api-key-12345";
        String encrypted = EncryptionUtil.encrypt(original);
        String decrypted = EncryptionUtil.decrypt(encrypted);

        assertNotEquals(original, encrypted, "Cifrado debe ser diferente al original");
        assertEquals(original, decrypted, "Desencriptado debe ser igual al original");
    }

    @Test
    @DisplayName("Encriptar null retorna null")
    void encryptNull() {
        assertNull(EncryptionUtil.encrypt(null));
    }

    @Test
    @DisplayName("Encriptar vacío retorna vacío")
    void encryptBlank() {
        assertEquals("", EncryptionUtil.encrypt(""));
        assertEquals("   ", EncryptionUtil.encrypt("   "));
    }

    @Test
    @DisplayName("Desencriptar null retorna null")
    void decryptNull() {
        assertNull(EncryptionUtil.decrypt(null));
    }

    @Test
    @DisplayName("Desencriptar vacío retorna vacío")
    void decryptBlank() {
        assertEquals("", EncryptionUtil.decrypt(""));
    }

    @Test
    @DisplayName("Textos diferentes producen cifrados diferentes")
    void differentInputs() {
        String enc1 = EncryptionUtil.encrypt("texto-uno");
        String enc2 = EncryptionUtil.encrypt("texto-dos");
        assertNotEquals(enc1, enc2);
    }

    @Test
    @DisplayName("Mismo texto produce cifrados diferentes (IV aleatorio)")
    void ivRandomness() {
        String enc1 = EncryptionUtil.encrypt("mismo texto");
        String enc2 = EncryptionUtil.encrypt("mismo texto");
        assertNotEquals(enc1, enc2, "IV aleatorio debe producir cifrados diferentes");
        assertEquals("mismo texto", EncryptionUtil.decrypt(enc1));
        assertEquals("mismo texto", EncryptionUtil.decrypt(enc2));
    }

    @Test
    @DisplayName("Texto largo se encripta correctamente")
    void longText() {
        String longText = "Esta es una cadena de texto muy larga que debería encriptarse correctamente sin problemas. ".repeat(10);
        String encrypted = EncryptionUtil.encrypt(longText);
        assertEquals(longText, EncryptionUtil.decrypt(encrypted));
    }
}
