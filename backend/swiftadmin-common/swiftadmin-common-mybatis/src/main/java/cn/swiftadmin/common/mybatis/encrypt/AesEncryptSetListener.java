package cn.swiftadmin.common.mybatis.encrypt;

import cn.swiftadmin.common.core.util.AesUtils;
import com.mybatisflex.annotation.SetListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * AES 加密 SetListener 基类
 * <p>在设置字段值时自动加密，支持实体操作</p>
 * <p>子类需要实现 getEncryptFields() 和 getSecretKey() 方法</p>
 *
 * <p>使用方式：</p>
 * <pre>
 * public class SysSocialBindSetListener extends AesEncryptSetListener {
 *     public Set&lt;String&gt; getEncryptFields() {
 *         return Set.of("accessToken", "refreshToken");
 *     }
 *     public String getSecretKey() {
 *         return "your-secret-key";
 *     }
 * }
 *
 * &#64;SetListener(SysSocialBindSetListener.class)
 * &#64;Table("sys_social_bind")
 * public class SysSocialBind { ... }
 * </pre>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Slf4j
public abstract class AesEncryptSetListener implements SetListener {

    /**
     * 获取需要加密的字段名集合
     *
     * @return 字段名集合
     */
    protected abstract Set<String> getEncryptFields();

    /**
     * 获取加密密钥
     *
     * @return 加密密钥
     */
    protected abstract String getSecretKey();

    @Override
    public Object onSet(Object entity, String property, Object value) {
        // 如果不是需要加密的字段，直接返回原值
        if (!getEncryptFields().contains(property)) {
            return value;
        }

        // 如果值为空，直接返回
        if (value == null) {
            return null;
        }

        String valueStr = value.toString();
        if (valueStr.isEmpty()) {
            return valueStr;
        }

        // 检查是否已经是加密后的值
        if (isEncrypted(valueStr)) {
            log.debug("字段 {} 已经是加密值，跳过加密", property);
            return value;
        }

        try {
            String encrypted = AesUtils.encrypt(valueStr, getSecretKey());
            log.debug("字段 {} 加密成功", property);
            return encrypted;
        } catch (Exception e) {
            log.error("字段 {} 加密失败", property, e);
            throw new RuntimeException("加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 判断值是否已经加密
     */
    private boolean isEncrypted(String value) {
        try {
            AesUtils.decrypt(value, getSecretKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}