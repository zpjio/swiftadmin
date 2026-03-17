package cn.swiftadmin.auth.service.impl;

import cn.swiftadmin.auth.api.service.CaptchaService;
import cn.swiftadmin.auth.api.vo.CaptchaVO;
import cn.swiftadmin.auth.captcha.CaptchaGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务实现
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final StringRedisTemplate redisTemplate;

    /** 验证码 Redis Key 前缀 */
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /** 验证码有效期（分钟） */
    private static final int EXPIRE_MINUTES = 5;

    @Override
    public CaptchaVO generate() {
        // 生成验证码 ID
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        // 生成验证码文本
        String code = CaptchaGenerator.generateCode();

        // 生成图片
        String image = CaptchaGenerator.generateImage(code);

        // 存储到 Redis（验证时使用小写比较，不区分大小写）
        String key = CAPTCHA_KEY_PREFIX + captchaId;
        redisTemplate.opsForValue().set(key, code.toLowerCase(), EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.debug("生成验证码：id={}, code={}", captchaId, code);

        return CaptchaVO.builder()
                .captchaId(captchaId)
                .image(image)
                .build();
    }

    @Override
    public boolean verify(String captchaId, String code) {
        if (captchaId == null || code == null) {
            return false;
        }

        String key = CAPTCHA_KEY_PREFIX + captchaId;

        // 使用 Lua 脚本原子性地获取并删除验证码
        String verifyAndDeleteScript =
            "local code = redis.call('GET', KEYS[1]); " +
            "if code then " +
            "  redis.call('DEL', KEYS[1]); " +
            "  return code; " +
            "end " +
            "return nil;";

        String storedCode = redisTemplate.execute(
            new DefaultRedisScript<>(verifyAndDeleteScript, String.class),
            Collections.singletonList(key)
        );

        if (storedCode == null) {
            log.warn("验证码已过期或不存在：id={}", captchaId);
            return false;
        }

        // 不区分大小写比较
        boolean result = storedCode.equalsIgnoreCase(code.trim());
        if (!result) {
            log.warn("验证码错误：id={}, expected={}, actual={}", captchaId, storedCode, code);
        }

        return result;
    }

    @Override
    public boolean isCaptchaRequired(String ip, int threshold) {
        String key = "sms:ip_count:" + ip;
        String countStr = redisTemplate.opsForValue().get(key);
        if (countStr == null) {
            return false;
        }
        int count = Integer.parseInt(countStr);
        return count >= threshold;
    }
}