package cn.swiftadmin.common.core.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 敏感数据加密工具类
 * <p>使用 AES-256-GCM 加密算法，提供安全的加密和解密功能</p>
 * <p>适用于手机号、身份证号、银行卡号等敏感信息的加密存储</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
public class EncryptionUtil {

    @Value("${encryption.aes-key:}")
    private String aesKey;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static SecretKey secretKey;
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /**
     * 初始化密钥
     */
    @PostConstruct
    public void init() {
        if (INITIALIZED.get()) {
            return;
        }
        
        try {
            if (aesKey == null || aesKey.trim().isEmpty()) {
                log.warn("未配置加密密钥，将生成随机密钥（重启后失效）");
                KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
                keyGen.init(KEY_SIZE, SecureRandom.getInstanceStrong());
                secretKey = keyGen.generateKey();
            } else {
                byte[] keyBytes = validateAndDecodeKey(aesKey);
                secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
                log.info("加密密钥初始化成功");
            }
            INITIALIZED.set(true);
        } catch (NoSuchAlgorithmException e) {
            log.error("初始化加密密钥失败", e);
            throw new RuntimeException("初始化加密密钥失败", e);
        }
    }

    /**
     * 验证并解码密钥
     *
     * @param key Base64 编码的密钥
     * @return 解码后的密钥字节
     */
    private static byte[] validateAndDecodeKey(String key) {
        if (Objects.isNull(key) || key.trim().isEmpty()) {
            throw new IllegalArgumentException("密钥不能为空");
        }
        
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key.trim());
            if (keyBytes.length != KEY_SIZE / 8) {
                log.warn("密钥长度不正确，应为 {} 字节，实际为 {} 字节", KEY_SIZE / 8, keyBytes.length);
            }
            return keyBytes;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("密钥格式错误，应为 Base64 编码", e);
        }
    }

    /**
     * 检查是否已初始化
     *
     * @return true=已初始化
     */
    public static boolean isInitialized() {
        return INITIALIZED.get();
    }

    /**
     * 加密字符串
     *
     * @param plaintext 明文
     * @return 密文（Base64 编码）
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        if (!INITIALIZED.get()) {
            throw new IllegalStateException("加密组件未初始化");
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = SecureRandom.getInstanceStrong();
            random.nextBytes(iv);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("加密失败：{}", e.getMessage(), e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密字符串
     *
     * @param ciphertext 密文（Base64 编码）
     * @return 明文
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }

        if (!INITIALIZED.get()) {
            throw new IllegalStateException("加密组件未初始化");
        }

        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            ByteBuffer byteBuffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] ciphertextBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertextBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintextBytes = cipher.doFinal(ciphertextBytes);
            return new String(plaintextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败：{}", e.getMessage(), e);
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 加密手机号（仅加密后 4 位）
     *
     * @param phone 手机号
     * @return 加密后的手机号
     */
    public String encryptPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        String prefix = phone.substring(0, phone.length() - 4);
        String suffix = phone.substring(phone.length() - 4);
        return prefix + encrypt(suffix);
    }

    /**
     * 解密手机号
     *
     * @param encryptedPhone 加密的手机号
     * @return 明文手机号
     */
    public String decryptPhone(String encryptedPhone) {
        if (encryptedPhone == null || encryptedPhone.length() < 4) {
            return encryptedPhone;
        }
        int encryptStart = encryptedPhone.length() - 4;
        String prefix = encryptedPhone.substring(0, encryptStart);
        String encrypted = encryptedPhone.substring(encryptStart);
        return prefix + decrypt(encrypted);
    }

    /**
     * 加密身份证号（仅加密后 4 位）
     *
     * @param idCard 身份证号
     * @return 加密后的身份证号
     */
    public String encryptIdCard(String idCard) {
        if (idCard == null || idCard.length() < 4) {
            return idCard;
        }
        String prefix = idCard.substring(0, idCard.length() - 4);
        String suffix = idCard.substring(idCard.length() - 4);
        return prefix + encrypt(suffix);
    }

    /**
     * 解密身份证号
     *
     * @param encryptedIdCard 加密的身份证号
     * @return 明文身份证号
     */
    public String decryptIdCard(String encryptedIdCard) {
        if (encryptedIdCard == null || encryptedIdCard.length() < 4) {
            return encryptedIdCard;
        }
        int encryptStart = encryptedIdCard.length() - 4;
        String prefix = encryptedIdCard.substring(0, encryptStart);
        String encrypted = encryptedIdCard.substring(encryptStart);
        return prefix + decrypt(encrypted);
    }

    /**
     * 脱敏手机号（显示前 3 后 4）
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 脱敏身份证号（显示前 3 后 4）
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "**********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 脱敏姓名（只显示姓）
     *
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 生成 AES 密钥（Base64 编码）
     * <p>可用于生成配置文件中的 encryption.aes-key</p>
     *
     * @return Base64 编码的密钥
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE, SecureRandom.getInstanceStrong());
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("生成密钥失败", e);
        }
    }
}
