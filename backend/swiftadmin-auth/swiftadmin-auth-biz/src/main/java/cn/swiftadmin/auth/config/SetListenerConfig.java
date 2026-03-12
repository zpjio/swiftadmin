package cn.swiftadmin.auth.config;

import cn.swiftadmin.auth.api.entity.SocialBindEncryptSetListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * SetListener 密钥配置
 * <p>初始化 SocialBindEncryptSetListener 的加密密钥</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Configuration
public class SetListenerConfig {

    private static final Logger log = LoggerFactory.getLogger(SetListenerConfig.class);

    @Value("${social.token-secret:swiftadmin-social-token-secret-key}")
    private String tokenSecret;

    @PostConstruct
    public void init() {
        SocialBindEncryptSetListener.setSecretKey(tokenSecret);
        log.info("SocialBindEncryptSetListener 密钥配置完成");
    }

}