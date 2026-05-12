package com.apksigner.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CryptoUtil 工具类测试")
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
    @DisplayName("AES 加密解密正确性 - 普通文本")
    void testEncryptDecrypt_NormalText() {
        String plainText = "Hello, APK Signer!";
        String encrypted = CryptoUtil.encrypt(plainText);
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);

        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("AES 加密解密正确性 - 密码特殊字符")
    void testEncryptDecrypt_SpecialChars() {
        String plainText = "P@ssw0rd!#$%^&*()";
        String encrypted = CryptoUtil.encrypt(plainText);
        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("AES 加密解密正确性 - 中文文本")
    void testEncryptDecrypt_Chinese() {
        String plainText = "测试密码123";
        String encrypted = CryptoUtil.encrypt(plainText);
        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("随机 IV 每次加密结果不同")
    void testEncrypt_RandomIV_DifferentResults() {
        String plainText = "same-content";
        Set<String> encryptedResults = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            encryptedResults.add(CryptoUtil.encrypt(plainText));
        }

        assertEquals(10, encryptedResults.size(), "每次加密应该产生不同的密文（因为IV随机）");
    }

    @Test
    @DisplayName("随机 IV 解密后明文相同")
    void testDecrypt_RandomIV_SamePlaintext() {
        String plainText = "same-content";
        String encrypted1 = CryptoUtil.encrypt(plainText);
        String encrypted2 = CryptoUtil.encrypt(plainText);

        assertNotEquals(encrypted1, encrypted2);
        assertEquals(CryptoUtil.decrypt(encrypted1), CryptoUtil.decrypt(encrypted2));
        assertEquals(plainText, CryptoUtil.decrypt(encrypted1));
    }

    @Test
    @DisplayName("空值处理 - 加密 null 不崩溃")
    void testEncrypt_NullInput() {
        assertNull(CryptoUtil.encrypt(null));
    }

    @Test
    @DisplayName("空值处理 - 加密空字符串不崩溃")
    void testEncrypt_EmptyInput() {
        assertEquals("", CryptoUtil.encrypt(""));
    }

    @Test
    @DisplayName("空值处理 - 解密 null 不崩溃")
    void testDecrypt_NullInput() {
        assertNull(CryptoUtil.decrypt(null));
    }

    @Test
    @DisplayName("空值处理 - 解密空字符串不崩溃")
    void testDecrypt_EmptyInput() {
        assertEquals("", CryptoUtil.decrypt(""));
    }

    @Test
    @DisplayName("密码脱敏 - 长密码正确脱敏")
    void testMask_LongPassword() {
        assertEquals("p*********3", CryptoUtil.mask("password123"));
    }

    @Test
    @DisplayName("密码脱敏 - 刚好2个字符")
    void testMask_TwoChars() {
        assertEquals("***", CryptoUtil.mask("ab"));
    }

    @Test
    @DisplayName("密码脱敏 - 只有1个字符")
    void testMask_OneChar() {
        assertEquals("***", CryptoUtil.mask("a"));
    }

    @Test
    @DisplayName("密码脱敏 - null 值")
    void testMask_Null() {
        assertEquals("***", CryptoUtil.mask(null));
    }

    @Test
    @DisplayName("密码脱敏 - 空字符串")
    void testMask_Empty() {
        assertEquals("***", CryptoUtil.mask(""));
    }

    @Test
    @DisplayName("测试密钥长度验证")
    void testSetKeyForTesting_InvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.setKeyForTesting("short"));
    }
}
