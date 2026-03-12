package cn.swiftadmin.auth.exception;

import cn.swiftadmin.auth.api.exception.SocialAuthException;
import cn.swiftadmin.common.core.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 社交认证异常处理器
 * <p>专门处理社交登录相关的异常</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SocialAuthExceptionHandler {

    /**
     * 处理社交认证异常
     */
    @ExceptionHandler(SocialAuthException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleSocialAuthException(SocialAuthException e) {
        log.warn("社交认证异常：{} - {}", e.getError().getCode(), e.getMessage());
        return R.fail(Integer.parseInt(e.getErrorCode()), e.getMessage());
    }

}