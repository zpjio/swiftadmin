package cn.swiftadmin.auth.service;

import cn.swiftadmin.auth.api.event.LoginEvent;
import cn.swiftadmin.auth.context.LoginContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 登录事件服务
 * <p>封装登录事件的发布逻辑</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginEventService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布登录成功事件
     *
     * @param context 登录上下文
     */
    public void publishSuccess(LoginContext context) {
        eventPublisher.publishEvent(LoginEvent.success(
                context.getUsername(),
                context.getIp(),
                context.getUserAgent(),
                context.getSource()
        ));
    }

    /**
     * 发布登录失败事件
     *
     * @param context 登录上下文
     * @param reason 失败原因
     */
    public void publishFailure(LoginContext context, String reason) {
        eventPublisher.publishEvent(LoginEvent.failure(
                context.getUsername(),
                reason,
                context.getIp(),
                context.getUserAgent(),
                context.getSource()
        ));
    }

    /**
     * 发布登录成功事件（自定义用户名）
     *
     * @param username 用户名
     * @param context 登录上下文
     */
    public void publishSuccess(String username, LoginContext context) {
        eventPublisher.publishEvent(LoginEvent.success(
                username,
                context.getIp(),
                context.getUserAgent(),
                context.getSource()
        ));
    }

    /**
     * 发布登录失败事件（自定义用户名）
     *
     * @param username 用户名
     * @param reason 失败原因
     * @param context 登录上下文
     */
    public void publishFailure(String username, String reason, LoginContext context) {
        eventPublisher.publishEvent(LoginEvent.failure(
                username,
                reason,
                context.getIp(),
                context.getUserAgent(),
                context.getSource()
        ));
    }
}