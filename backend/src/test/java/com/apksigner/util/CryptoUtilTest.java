package com.apksigner.util;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CryptoUtil 单元测试：加解密、脱敏、密钥校验
 */
class CryptoUtilTest {

    private static final String TEST_KEY = "TestKey123456789"; // 16 bytes

    @BeforeEach
    void setUp() {
        CryptoUtil.setKeyForTesting(TEST_KEY);
    }

    @AfterEach
    void tearDown() {
        CryptoUtil.resetKey();
    }

    @Test
    @DisplayName("加密后解密应还原明文")
    void encryptDecrypt_shouldReturnOriginal() {
        String plainText = "mySecretPassword123";
        String encrypted = CryptoUtil.encrypt(plainText);

        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted, "密文不应等于明文");

        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(plainText, decrypted, "解密后应还原明文");
    }

    @Test
    @DisplayName("相同明文两次加密应产生不同密文（随机IV）")
    void encrypt_samePlainText_shouldProduceDifferentCipherText() {
        String plainText = "password";
        String encrypted1 = CryptoUtil.encrypt(plainText);
        String encrypted2 = CryptoUtil.encrypt(plainText);

        assertNotEquals(encrypted1, encrypted2, "随机IV应使每次加密结果不同");

        // 但两者解密后应相同
        assertEquals(CryptoUtil.decrypt(encrypted1), CryptoUtil.decrypt(encrypted2));
    }

    @Test
    @DisplayName("null和空字符串应原样返回")
    void encryptDecrypt_nullAndEmpty_shouldPassThrough() {
        assertNull(CryptoUtil.encrypt(null));
        assertEquals("", CryptoUtil.encrypt(""));
        assertNull(CryptoUtil.decrypt(null));
        assertEquals("", CryptoUtil.decrypt(""));
    }

    @Test
    @DisplayName("中文和特殊字符加解密")
    void encryptDecrypt_unicodeAndSpecialChars() {
        String[] testCases = {"密码123", "p@$$w0rd!#%", "🔐key", "a b\tc\nd"};
        for (String text : testCases) {
            String encrypted = CryptoUtil.encrypt(text);
            assertEquals(text, CryptoUtil.decrypt(encrypted),
                    "Unicode/特殊字符加解密失败: " + text);
        }
    }

    @Test
    @DisplayName("篡改密文应导致解密失败")
    void decrypt_tamperedCipherText_shouldFail() {
        String encrypted = CryptoUtil.encrypt("secret");
        // 篡改密文中间部分
        char[] chars = encrypted.toCharArray();
        chars[20] = (chars[20] == 'A') ? 'B' : 'A';
        String tampered = new String(chars);

        assertThrows(RuntimeException.class, () -> CryptoUtil.decrypt(tampered),
                "篡改密文后解密应抛出异常");
    }

    @Test
    @DisplayName("未配置密钥时应抛出 IllegalStateException")
    void getKey_withoutEnvVar_shouldThrow() {
        CryptoUtil.resetKey();
        // 在没有环境变量的测试环境中，resetKey 后 getKey 应失败
        // 注意：如果测试环境恰好设置了 ENCRYPT_KEY，此测试会跳过
        String envKey = System.getenv("ENCRYPT_KEY");
        if (envKey == null || envKey.isBlank()) {
            assertThrows(IllegalStateException.class, CryptoUtil::getKey,
                    "未配置 ENCRYPT_KEY 时应抛出异常");
        }
    }

    @Test
    @DisplayName("密钥长度不为16应抛出异常")
    void setKeyForTesting_wrongLength_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> CryptoUtil.setKeyForTesting("short"),
                "密钥长度不为16时应抛出异常");
        assertThrows(IllegalArgumentException.class,
                () -> CryptoUtil.setKeyForTesting("thisKeyIsTooLong!!!!"),
                "密钥长度不为16时应抛出异常");
    }

    @Test
    @DisplayName("mask 脱敏测试")
    void mask_shouldHideMiddleChars() {
        assertEquals("***", CryptoUtil.mask(null));
        assertEquals("***", CryptoUtil.mask("ab"));
        assertEquals("a*c", CryptoUtil.mask("abc"));
        assertEquals("1****6", CryptoUtil.mask("123456"));
        assertEquals("p********d", CryptoUtil.mask("p@ssword!d"));
    }
}
