package cn.swiftadmin.common.mybatis.encrypt;

import cn.swiftadmin.common.core.util.AesUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * AES-GCM 加密 TypeHandler
 * <p>用于自动加密/解密数据库字段</p>
 * <p>使用方式：@Column(typeHandler = AesGcmTypeHandler.class)</p>
 * <p>需要在配置中设置加密密钥：AesGcmTypeHandler.setSecretKey(key)</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@MappedTypes(String.class)
public class AesGcmTypeHandler extends BaseTypeHandler<String> {

    /** 加密密钥，需要在应用启动时设置 */
    private static String secretKey;

    /**
     * 设置加密密钥
     *
     * @param key 加密密钥（至少 32 字节）
     */
    public static void setSecretKey(String key) {
        secretKey = key;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        // 写入数据库时加密
        ps.setString(i, encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // 从数据库读取时解密
        return decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        // 从数据库读取时解密
        return decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        // 从存储过程读取时解密
        return decrypt(cs.getString(columnIndex));
    }

    private String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        if (secretKey == null) {
            throw new IllegalStateException("AES 密钥未设置，请先调用 AesGcmTypeHandler.setSecretKey()");
        }
        return AesUtils.encrypt(plaintext, secretKey);
    }

    private String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        if (secretKey == null) {
            throw new IllegalStateException("AES 密钥未设置，请先调用 AesGcmTypeHandler.setSecretKey()");
        }
        return AesUtils.decrypt(ciphertext, secretKey);
    }

}