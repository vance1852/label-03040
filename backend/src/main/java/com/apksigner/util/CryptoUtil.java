package com.apksigner.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 加解密工具，用于敏感字段（密码）的存储加密。
 * 密钥必须通过环境变量 ENCRYPT_KEY 注入，长度必须为 16 字节（AES-128）。
 * 不提供任何硬编码默认值，启动时若未配置将抛出异常阻止服务运行。
 */
public class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final String ENV_KEY_NAME = "ENCRYPT_KEY";

    private static volatile String cachedKey;

    /**
     * 获取加密密钥，强制从环境变量读取，无默认值
     */
    public static String getKey() {
        if (cachedKey != null) {
            return cachedKey;
        }
        String envKey = System.getenv(ENV_KEY_NAME);
        if (envKey == null || envKey.isBlank()) {
            throw new IllegalStateException(
                    "环境变量 " + ENV_KEY_NAME + " 未配置。请设置一个 16 字节的 AES 密钥。");
        }
        if (envKey.length() != 16) {
            throw new IllegalStateException(
                    "环境变量 " + ENV_KEY_NAME + " 长度必须为 16 字节（当前: " + envKey.length() + "）");
        }
        cachedKey = envKey;
        return cachedKey;
    }

    /**
     * 仅用于测试：允许注入密钥，避免测试依赖环境变量
     */
    public static void setKeyForTesting(String key) {
        if (key != null && key.length() != 16) {
            throw new IllegalArgumentException("测试密钥长度必须为 16 字节");
        }
        cachedKey = key;
    }

    /**
     * 重置缓存密钥（测试清理用）
     */
    public static void resetKey() {
        cachedKey = null;
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    getKey().getBytes(StandardCharsets.UTF_8), "AES");
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(IV_LENGTH + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (IllegalStateException e) {
            throw e; // 密钥未配置异常直接抛出
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            SecretKeySpec keySpec = new SecretKeySpec(
                    getKey().getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 脱敏显示：只保留首尾各一个字符，中间用 * 替代
     */
    public static String mask(String value) {
        if (value == null || value.length() <= 2) {
            return "***";
        }
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }
}
