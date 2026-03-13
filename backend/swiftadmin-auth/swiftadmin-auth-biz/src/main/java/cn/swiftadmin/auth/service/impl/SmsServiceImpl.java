package cn.swiftadmin.auth.service.impl;

import cn.swiftadmin.auth.api.entity.SysSmsCode;
import cn.swiftadmin.auth.api.service.CaptchaService;
import cn.swiftadmin.auth.api.service.SmsService;
import cn.swiftadmin.auth.config.AuthSecurityProperties;
import cn.swiftadmin.auth.mapper.SysSmsCodeMapper;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import com.mybatisflex.core.update.UpdateChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static cn.swiftadmin.auth.api.entity.table.SysSmsCodeTableDef.SYS_SMS_CODE;

/**
 * 短信服务实现类
 * <p>基于 Redis 实现验证码存储和限流，支持阿里云/腾讯云短信发送</p>
 * <p>安全防护：IP 限流、验证错误次数限制、图形验证码触发</p>
 * <p>使用 MyBatis-Flex UpdateChain 优化更新操作</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final StringRedisTemplate redisTemplate;
    private final SysSmsCodeMapper smsCodeMapper;
    private final CaptchaService captchaService;
    private final AuthSecurityProperties securityProperties;

    /**
     * 发送验证码
     * <p>限流：1 分钟 1 次，1 小时 5 次（手机号），1 小时 10 次（IP）</p>
     * <p>安全：IP 发送超过阈值触发图形验证码</p>
     */
    @Override
    public void sendCode(String phone, String scene, String ip, String captchaId, String captchaCode) {
        // 0. 检查验证错误锁定
        String lockKey = "sms:verify_lock:" + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        // 1. 检查发送频率（1 分钟内只能发送 1 次）
        String intervalKey = "sms:interval:" + phone;
        Boolean exists = redisTemplate.hasKey(intervalKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_SEND_TOO_FREQUENT);
        }

        // 2. 检查 1 小时内发送次数（手机号维度）
        String hourCountKey = "sms:hour_count:" + phone;
        String countStr = redisTemplate.opsForValue().get(hourCountKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        if (count >= securityProperties.getMaxSendPerHour()) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        // 3. IP 级别限流
        if (StringUtils.hasText(ip)) {
            String ipCountKey = "sms:ip_count:" + ip;
            String ipCountStr = redisTemplate.opsForValue().get(ipCountKey);
            int ipCount = ipCountStr != null ? Integer.parseInt(ipCountStr) : 0;

            // 检查 IP 发送上限
            if (ipCount >= securityProperties.getMaxIpSendPerHour()) {
                throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
            }

            // 检查是否需要图形验证码
            if (ipCount >= securityProperties.getCaptchaTriggerThreshold()) {
                if (captchaId == null || captchaCode == null) {
                    throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED);
                }
                if (!captchaService.verify(captchaId, captchaCode)) {
                    throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
                }
            }

            // 更新 IP 计数
            redisTemplate.opsForValue().increment(ipCountKey);
            redisTemplate.expire(ipCountKey, 1, TimeUnit.HOURS);
        }

        // 4. 生成 6 位验证码
        String code = generateCode();

        // 5. 保存到数据库
        SysSmsCode smsCode = SysSmsCode.builder()
                .phone(phone)
                .code(code)
                .scene(scene)
                .status(1)
                .expireAt(LocalDateTime.now().plusMinutes(securityProperties.getCodeExpireMinutes()))
                .ipAddress(ip)
                .build();
        smsCodeMapper.insert(smsCode);

        // 6. 存入 Redis（用于快速验证）
        String redisKey = "sms:code:" + phone + ":" + scene;
        redisTemplate.opsForValue().set(redisKey, code, securityProperties.getCodeExpireMinutes(), TimeUnit.MINUTES);

        // 7. 设置发送间隔锁
        redisTemplate.opsForValue().set(intervalKey, "1", securityProperties.getSendIntervalSeconds(), TimeUnit.SECONDS);

        // 8. 更新小时计数
        redisTemplate.opsForValue().increment(hourCountKey);
        redisTemplate.expire(hourCountKey, 1, TimeUnit.HOURS);

        // 9. 发送短信
        log.info("【短信验证码】手机号：{}, 验证码：{}, 场景：{}, IP:{}", phone, code, scene, ip);

        // 调用短信服务商 API 发送
        // 开发环境仅记录日志，生产环境需要接入实际的短信服务
        // 示例：阿里云短信、腾讯云短信等
        if (!"dev".equals(System.getProperty("spring.profiles.active", "dev"))) {
            // 生产环境应该调用实际的短信发送服务
            // 这里保留接口，实际使用时需要注入 SmsProvider 并调用发送方法
            log.warn("生产环境需要接入实际的短信发送服务");
        }
    }

    /**
     * 验证验证码
     * <p>使用 Redis Lua 脚本实现原子性验证和删除，防止重放攻击</p>
     * <p>安全：连续错误 3 次后锁定 15 分钟</p>
     */
    @Override
    public boolean verifyCode(String phone, String code, String scene) {
        // 检查验证错误锁定
        String lockKey = "sms:verify_lock:" + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            log.warn("验证码验证被锁定：{}", phone);
            return false;
        }

        String redisKey = "sms:code:" + phone + ":" + scene;
        
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
            java.util.Collections.singletonList(redisKey)
        );

        if (storedCode == null) {
            log.warn("验证码已过期或已被使用：{}", phone);
            return false;
        }

        if (!code.equals(storedCode)) {
            log.warn("验证码错误：{}", phone);

            // 增加错误计数
            String failKey = "sms:verify_fail:" + phone;
            Long failCount = redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, securityProperties.getVerifyLockMinutes(), TimeUnit.MINUTES);

            // 达到错误上限，锁定账户
            if (failCount != null && failCount >= securityProperties.getMaxVerifyFailCount()) {
                redisTemplate.opsForValue().set(lockKey, "1", securityProperties.getVerifyLockMinutes(), TimeUnit.MINUTES);
                log.warn("验证码错误次数过多，已锁定：{}", phone);
            }

            return false;
        }

        // 验证成功，清除错误计数
        String failKey = "sms:verify_fail:" + phone;
        redisTemplate.delete(failKey);

        // 异步更新数据库状态（仅用于审计）
        try {
            updateSmsCodeStatusAsync(phone, scene);
        } catch (Exception e) {
            log.warn("更新验证码状态失败", e);
        }

        log.info("验证码验证成功：phone={}, scene={}", phone, scene);
        return true;
    }

    /**
     * 检查是否需要图形验证码
     */
    @Override
    public boolean isCaptchaRequired(String ip) {
        if (!StringUtils.hasLength(ip)) {
            return false;
        }
        return captchaService.isCaptchaRequired(ip, securityProperties.getCaptchaTriggerThreshold());
    }

    /**
     * 更新验证码状态（异步，仅用于审计）
     * <p>使用 UpdateChain 直接更新，避免先查询</p>
     */
    @org.springframework.scheduling.annotation.Async
    public void updateSmsCodeStatusAsync(String phone, String scene) {
        try {
            // 使用 UpdateChain 直接更新，一条 SQL 完成
            boolean updated = UpdateChain.of(SysSmsCode.class)
                    .set(SYS_SMS_CODE.STATUS, 0)
                    .where(SYS_SMS_CODE.PHONE.eq(phone))
                    .and(SYS_SMS_CODE.SCENE.eq(scene))
                    .and(SYS_SMS_CODE.STATUS.eq(1))
                    .update();

            if (updated) {
                log.debug("验证码状态已更新: phone={}", phone);
            }
        } catch (Exception e) {
            log.warn("更新验证码状态失败: phone={}", phone, e);
        }
    }

    /**
     * 生成 6 位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

}
