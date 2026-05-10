package com.apksigner.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    private static final String TEST_KEY = "TestKey!16Bytes!";

    @BeforeEach
    void setUp() {
        CryptoUtil.setKeyForTesting(TEST_KEY);
    }

    @AfterEach
    void tearDown() {
        CryptoUtil.resetKey();
    }

    @Test
    @DisplayName("AES 加解密正确性 - 加密后解密应还原原始字符串")
    void testEncryptAndDecrypt() {
        String plainText = "Hello, APK Signer!";

        String encrypted = CryptoUtil.encrypt(plainText);
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);

        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("AES 加解密正确性 - 支持中文和特殊字符")
    void testEncryptAndDecryptWithChineseAndSpecialChars() {
        String plainText = "测试密码123!@#$%^&*()";

        String encrypted = CryptoUtil.encrypt(plainText);
        String decrypted = CryptoUtil.decrypt(encrypted);

        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("随机 IV - 相同明文每次加密结果不同")
    void testRandomIvProducesDifferentOutput() {
        String plainText = "same_content";

        String encrypted1 = CryptoUtil.encrypt(plainText);
        String encrypted2 = CryptoUtil.encrypt(plainText);

        assertNotEquals(encrypted1, encrypted2, "相同明文每次加密结果应不同（随机IV）");

        String decrypted1 = CryptoUtil.decrypt(encrypted1);
        String decrypted2 = CryptoUtil.decrypt(encrypted2);
        assertEquals(plainText, decrypted1);
        assertEquals(plainText, decrypted2);
    }

    @Test
    @DisplayName("空值处理 - encrypt 传入 null 或空字符串应直接返回不崩溃")
    void testEncryptNullAndEmpty() {
        assertNull(CryptoUtil.encrypt(null));
        assertEquals("", CryptoUtil.encrypt(""));
    }

    @Test
    @DisplayName("空值处理 - decrypt 传入 null 或空字符串应直接返回不崩溃")
    void testDecryptNullAndEmpty() {
        assertNull(CryptoUtil.decrypt(null));
        assertEquals("", CryptoUtil.decrypt(""));
    }

    @Test
    @DisplayName("密码脱敏 - 长度大于2的字符串只保留首尾字符")
    void testMaskLongString() {
        assertEquals("t**t", CryptoUtil.mask("test"));
        assertEquals("a**********z", CryptoUtil.mask("abcdefghijkz"));
    }

    @Test
    @DisplayName("密码脱敏 - 长度小于等于2的字符串返回 ***")
    void testMaskShortString() {
        assertEquals("***", CryptoUtil.mask(null));
        assertEquals("***", CryptoUtil.mask(""));
        assertEquals("***", CryptoUtil.mask("a"));
        assertEquals("***", CryptoUtil.mask("ab"));
    }

    @Test
    @DisplayName("密码脱敏 - 真实密码场景测试")
    void testMaskRealPassword() {
        String password = "MySuperSecretPassword123!";
        String masked = CryptoUtil.mask(password);

        assertFalse(masked.contains("Super"));
        assertFalse(masked.contains("Secret"));
        assertTrue(masked.startsWith("M"));
        assertTrue(masked.endsWith("!"));
        assertTrue(masked.contains("*"));
    }

    @Test
    @DisplayName("getKey - 测试密钥注入正常工作")
    void testGetKey() {
        assertEquals(TEST_KEY, CryptoUtil.getKey());
    }

    @Test
    @DisplayName("setKeyForTesting - 非法长度密钥应抛出异常")
    void testSetKeyForTestingInvalidLength() {
        CryptoUtil.resetKey();
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.setKeyForTesting("short"));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.setKeyForTesting("this_key_is_too_long_1234567890"));
    }
}
