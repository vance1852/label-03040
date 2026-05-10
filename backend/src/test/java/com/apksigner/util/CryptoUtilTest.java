package com.apksigner.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Nested
    @DisplayName("AES 加解密正确性")
    class EncryptDecryptTests {

        @Test
        @DisplayName("加密后再解密应还原为原始明文")
        void encryptThenDecrypt_shouldReturnOriginalText() {
            String plain = "mySecretPassword";
            String encrypted = CryptoUtil.encrypt(plain);
            assertNotNull(encrypted);
            assertNotEquals(plain, encrypted);
            String decrypted = CryptoUtil.decrypt(encrypted);
            assertEquals(plain, decrypted);
        }

        @Test
        @DisplayName("不同明文加密后解密均正确")
        void encryptDecrypt_differentPlaintexts() {
            String[] plains = {"android", "P@ssw0rd!#$", "短", "a-very-long-password-with-special-chars-!@#$%^&*()"};
            for (String plain : plains) {
                String encrypted = CryptoUtil.encrypt(plain);
                assertEquals(plain, CryptoUtil.decrypt(encrypted));
            }
        }

        @Test
        @DisplayName("中文内容加解密正确")
        void encryptDecrypt_chineseText() {
            String plain = "中文密码测试";
            String encrypted = CryptoUtil.encrypt(plain);
            assertEquals(plain, CryptoUtil.decrypt(encrypted));
        }
    }

    @Nested
    @DisplayName("随机 IV 每次不同")
    class RandomIvTests {

        @Test
        @DisplayName("同一明文两次加密产生的密文应不同")
        void samePlaintext_shouldProduceDifferentCiphertext() {
            String plain = "samePassword";
            String encrypted1 = CryptoUtil.encrypt(plain);
            String encrypted2 = CryptoUtil.encrypt(plain);
            assertNotEquals(encrypted1, encrypted2, "由于随机IV，同一明文两次加密结果应不同");
        }

        @Test
        @DisplayName("不同密文均可正确解密")
        void differentCiphertexts_bothDecryptCorrectly() {
            String plain = "repeatable";
            String encrypted1 = CryptoUtil.encrypt(plain);
            String encrypted2 = CryptoUtil.encrypt(plain);
            assertEquals(plain, CryptoUtil.decrypt(encrypted1));
            assertEquals(plain, CryptoUtil.decrypt(encrypted2));
        }
    }

    @Nested
    @DisplayName("空值处理不崩溃")
    class NullAndEmptyTests {

        @Test
        @DisplayName("encrypt(null) 返回 null")
        void encrypt_null_returnsNull() {
            assertNull(CryptoUtil.encrypt(null));
        }

        @Test
        @DisplayName("encrypt(空字符串) 返回空字符串")
        void encrypt_empty_returnsEmpty() {
            assertEquals("", CryptoUtil.encrypt(""));
        }

        @Test
        @DisplayName("decrypt(null) 返回 null")
        void decrypt_null_returnsNull() {
            assertNull(CryptoUtil.decrypt(null));
        }

        @Test
        @DisplayName("decrypt(空字符串) 返回空字符串")
        void decrypt_empty_returnsEmpty() {
            assertEquals("", CryptoUtil.decrypt(""));
        }
    }

    @Nested
    @DisplayName("密码脱敏输出")
    class MaskTests {

        @Test
        @DisplayName("null 输入返回 ***")
        void mask_null_returnsMasked() {
            assertEquals("***", CryptoUtil.mask(null));
        }

        @Test
        @DisplayName("长度 <= 2 返回 ***")
        void mask_shortValue_returnsMasked() {
            assertEquals("***", CryptoUtil.mask("a"));
            assertEquals("***", CryptoUtil.mask("ab"));
        }

        @Test
        @DisplayName("正常长度密码首尾各保留一个字符")
        void mask_normalPassword() {
            assertEquals("a*****d", CryptoUtil.mask("android"));
            assertEquals("1**9", CryptoUtil.mask("1239"));
        }

        @Test
        @DisplayName("长密码正确脱敏")
        void mask_longPassword() {
            String result = CryptoUtil.mask("myVeryLongPassword123!");
            assertEquals('m', result.charAt(0));
            assertEquals('!', result.charAt(result.length() - 1));
            assertTrue(result.contains("*"));
            assertEquals("myVeryLongPassword123!".length(), result.length());
        }
    }

    @Nested
    @DisplayName("密钥管理")
    class KeyManagementTests {

        @Test
        @DisplayName("缓存的密钥被清除后加密应抛出异常（无环境变量时）")
        void encrypt_noCachedKeyAndNoEnv_throwsException() {
            String savedEnv = System.getenv("ENCRYPT_KEY");
            CryptoUtil.resetKey();
            if (savedEnv == null || savedEnv.isBlank()) {
                assertThrows(IllegalStateException.class, () -> CryptoUtil.encrypt("test"));
            } else {
                CryptoUtil.encrypt("test");
            }
        }

        @Test
        @DisplayName("缓存的密钥被清除后解密应抛出异常（无环境变量时）")
        void decrypt_noCachedKeyAndNoEnv_throwsException() {
            String savedEnv = System.getenv("ENCRYPT_KEY");
            CryptoUtil.resetKey();
            if (savedEnv == null || savedEnv.isBlank()) {
                assertThrows(RuntimeException.class, () -> CryptoUtil.decrypt("dGVzdA=="));
            }
        }

        @Test
        @DisplayName("setKeyForTesting 传入非16字节密钥应抛异常")
        void setKeyForTesting_invalidLength_throws() {
            assertThrows(IllegalArgumentException.class, () -> CryptoUtil.setKeyForTesting("short"));
            assertThrows(IllegalArgumentException.class, () -> CryptoUtil.setKeyForTesting("this-is-way-too-long-key"));
        }

        @Test
        @DisplayName("setKeyForTesting 传入 null 不抛异常")
        void setKeyForTesting_null_noException() {
            assertDoesNotThrow(() -> CryptoUtil.setKeyForTesting(null));
        }

        @Test
        @DisplayName("setKeyForTesting 注入密钥后加解密正常工作")
        void setKeyForTesting_validKey_encryptDecryptWorks() {
            CryptoUtil.setKeyForTesting("Another16ByteKy!");
            String encrypted = CryptoUtil.encrypt("test-data");
            assertEquals("test-data", CryptoUtil.decrypt(encrypted));
        }
    }
}
