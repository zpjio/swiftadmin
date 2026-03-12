package cn.swiftadmin.auth.event;

import cn.swiftadmin.auth.api.event.LoginEvent;
import cn.swiftadmin.auth.api.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 登录事件监听器
 * <p>异步记录登录日志，解耦认证逻辑与日志记录</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogListener {

    private final LoginLogService loginLogService;

    /**
     * 处理登录事件
     * <p>异步记录登录日志，不阻塞认证流程</p>
     */
    @Async
    @EventListener
    public void onLoginEvent(LoginEvent event) {
        try {
            int status = event.success() ? 1 : 0;
            loginLogService.record(
                    event.username(),
                    event.ipAddress(),
                    event.userAgent(),
                    status,
                    event.message()
            );
            log.debug("登录日志记录成功：username={}, success={}", event.username(), event.success());
        } catch (Exception e) {
            log.error("登录日志记录失败：username={}", event.username(), e);
        }
    }

}