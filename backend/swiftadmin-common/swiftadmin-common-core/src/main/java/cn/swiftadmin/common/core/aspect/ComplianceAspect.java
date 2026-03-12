package cn.swiftadmin.common.core.aspect;

import cn.swiftadmin.common.core.annotation.RequireAgreement;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.common.core.service.AgreementCheckService;
import cn.swiftadmin.common.core.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 合规性检查切面
 * <p>检查用户是否已同意用户协议和隐私政策</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ComplianceAspect {

    private final AgreementCheckService agreementCheckService;
    private final CurrentUserService currentUserService;

    /**
     * 定义切点：需要检查协议同意的接口
     */
    @Pointcut("@annotation(cn.swiftadmin.common.core.annotation.RequireAgreement)")
    public void requireAgreementPointcut() {
    }

    /**
     * 环绕通知：检查协议同意状态
     */
    @Around("requireAgreementPointcut()")
    public Object checkAgreementStatus(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireAgreement requireAgreement = method.getAnnotation(RequireAgreement.class);

        if (requireAgreement == null) {
            return joinPoint.proceed();
        }

        // 获取当前登录用户ID
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            // 未登录，跳过检查
            return joinPoint.proceed();
        }

        // 检查用户协议
        if (requireAgreement.checkUserAgreement() && !agreementCheckService.hasAgreed(userId, "agreement")) {
            log.warn("用户未同意用户协议：userId={}", userId);
            throw new BusinessException(ErrorCode.AGREEMENT_NOT_SIGNED)
                    .withContext("userId", userId);
        }

        // 检查隐私政策
        if (requireAgreement.checkPrivacyPolicy() && !agreementCheckService.hasAgreed(userId, "privacy")) {
            log.warn("用户未同意隐私政策：userId={}", userId);
            throw new BusinessException(ErrorCode.PRIVACY_POLICY_NOT_AGREED)
                    .withContext("userId", userId);
        }

        log.debug("用户协议检查通过：userId={}", userId);
        return joinPoint.proceed();
    }
}