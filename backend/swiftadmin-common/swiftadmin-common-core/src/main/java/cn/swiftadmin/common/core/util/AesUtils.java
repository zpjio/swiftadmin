package cn.swiftadmin.common.core.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES 加密工具类
 * <p>使用 AES-256-GCM 算法进行加密解密，提供认证加密保证</p>
 * <p>适用于敏感数据如 Token、密码等的安全存储</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
public final class AesUtils {

    private AesUtils() {
    }

    /** 加密算法 */
    private static final String ALGORITHM = "AES";

    /** 加密模式和填充方式 */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** GCM 认证标签长度（位） */
    private static final int GCM_TAG_LENGTH = 128;

    /** IV 长度（字节），GCM 推荐使用 12 字节 */
    private static final int IV_LENGTH = 12;

    /** 密钥长度（字节），AES-256 需要 32 字节 */
    private static final int KEY_LENGTH = 32;

    /** 安全随机数生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * AES-256-GCM 加密
     *
     * @param plainText 明文
     * @param secretKey 密钥（至少 32 字节）
     * @return Base64 编码的密文（包含 IV + 密文 + Tag）
     * @throws IllegalArgumentException 如果密钥长度不足或加密失败
     */
    public static String encrypt(String plainText, String secretKey) {
        if (!StringUtils.hasLength(plainText)) {
            return null;
        }

        validateKey(secretKey);

        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            SecretKeySpec keySpec = new SecretKeySpec(
                    normalizeKey(secretKey).getBytes(StandardCharsets.UTF_8),
                    ALGORITHM
            );

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES 加密失败：{}", e.getMessage(), e);
            throw new IllegalArgumentException("加密失败：" + e.getMessage(), e);
        }
    }

    /**
     * AES-256-GCM 解密
     *
     * @param cipherText Base64 编码的密文（包含 IV + 密文 + Tag）
     * @param secretKey 密钥（至少 32 字节）
     * @return 明文
     * @throws IllegalArgumentException 如果密钥长度不足或解密失败
     */
    public static String decrypt(String cipherText, String secretKey) {
        if (!StringUtils.hasLength(cipherText)) {
            return null;
        }

        validateKey(secretKey);

        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            if (combined.length < IV_LENGTH + GCM_TAG_LENGTH / 8) {
                throw new IllegalArgumentException("密文格式无效，长度不足");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKeySpec keySpec = new SecretKeySpec(
                    normalizeKey(secretKey).getBytes(StandardCharsets.UTF_8),
                    ALGORITHM
            );

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainText = cipher.doFinal(encrypted);

            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 解密失败：{}", e.getMessage(), e);
            throw new IllegalArgumentException("解密失败：" + e.getMessage(), e);
        }
    }

    /**
     * 验证密钥
     */
    private static void validateKey(String secretKey) {
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalArgumentException("密钥不能为空");
        }
    }

    /**
     * 标准化密钥至 32 字节
     * <p>如果密钥长度不足 32 字节，使用重复填充</p>
     * <p>如果密钥长度超过 32 字节，截取前 32 字节</p>
     */
    private static String normalizeKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        
        if (keyBytes.length >= KEY_LENGTH) {
            return new String(keyBytes, 0, KEY_LENGTH, StandardCharsets.UTF_8);
        }

        byte[] padded = new byte[KEY_LENGTH];
        int repeatCount = KEY_LENGTH / keyBytes.length;
        int remainder = KEY_LENGTH % keyBytes.length;
        
        for (int i = 0; i < repeatCount; i++) {
            System.arraycopy(keyBytes, 0, padded, i * keyBytes.length, keyBytes.length);
        }
        
        if (remainder > 0) {
            System.arraycopy(keyBytes, 0, padded, KEY_LENGTH - remainder, remainder);
        }

        return new String(padded, StandardCharsets.UTF_8);
    }

    /**
     * 验证密钥格式
     *
     * @param secretKey 密钥
     * @return true=密钥格式正确
     */
    public static boolean isValidKey(String secretKey) {
        if (!StringUtils.hasText(secretKey)) {
            return false;
        }
        
        try {
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            return keyBytes.length > 0 && keyBytes.length <= 64;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成随机密钥
     *
     * @return 32 字节的 Base64 编码密钥
     */
    public static String generateKey() {
        byte[] key = new byte[KEY_LENGTH];
        SECURE_RANDOM.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /**
     * 生成指定长度的随机密钥
     *
     * @param length 密钥长度（字节）
     * @return Base64 编码的密钥
     */
    public static String generateKey(int length) {
        if (length < 16 || length > 64) {
            throw new IllegalArgumentException("密钥长度应在 16-64 字节之间");
        }
        byte[] key = new byte[length];
        SECURE_RANDOM.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
