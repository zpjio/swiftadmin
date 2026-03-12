package cn.swiftadmin.auth.api.entity;

import cn.swiftadmin.common.mybatis.encrypt.AesEncryptSetListener;

import java.util.Set;

/**
 * SysSocialBind 实体的加密 SetListener
 * <p>用于加密 accessToken 和 refreshToken 字段</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
public class SocialBindEncryptSetListener extends AesEncryptSetListener {

    /** 需要加密的字段 */
    private static final Set<String> ENCRYPT_FIELDS = Set.of("accessToken", "refreshToken");

    /** 加密密钥，从环境变量或默认值获取 */
    private static String SECRET_KEY = System.getProperty("social.token-secret",
            System.getenv("SOCIAL_TOKEN_SECRET") != null
                    ? System.getenv("SOCIAL_TOKEN_SECRET")
                    : "swiftadmin-social-token-secret-key");

    /**
     * 设置加密密钥（由 Spring 容器在启动时调用）
     *
     * @param key 加密密钥
     */
    public static void setSecretKey(String key) {
        SECRET_KEY = key;
    }

    @Override
    protected Set<String> getEncryptFields() {
        return ENCRYPT_FIELDS;
    }

    @Override
    protected String getSecretKey() {
        return SECRET_KEY;
    }

}