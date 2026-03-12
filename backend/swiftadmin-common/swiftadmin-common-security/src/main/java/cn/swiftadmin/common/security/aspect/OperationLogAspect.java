package cn.swiftadmin.common.security.aspect;

import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.event.OperationLogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 操作日志切面
 * <p>自动记录标注 @OperationLog 的方法操作日志</p>
 * <p>通过 Spring 事件机制解耦，避免循环依赖</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /** 请求开始时间 */
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    /** 当前操作的日志数据 */
    private static final ThreadLocal<Map<String, Object>> CURRENT_LOG = new ThreadLocal<>();

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * 切点：所有标注 @OperationLog 的方法
     */
    @Pointcut("@annotation(cn.swiftadmin.common.security.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

    /**
     * 方法执行前记录开始时间
     */
    @org.aspectj.lang.annotation.Before("operationLogPointcut()")
    public void doBefore(JoinPoint joinPoint) {
        START_TIME.set(System.currentTimeMillis());

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        OperationLog operationLog = signature.getMethod().getAnnotation(OperationLog.class);

        // 初始化日志数据
        Map<String, Object> logData = new HashMap<>();
        logData.put("title", operationLog.title());
        logData.put("businessType", operationLog.businessType().getCode());
        logData.put("operatorType", operationLog.operatorType().getCode());
        logData.put("operTime", LocalDateTime.now());
        logData.put("method", joinPoint.getTarget().getClass().getName() + "." + signature.getName());

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logData.put("operUrl", request.getRequestURI());
            logData.put("operIp", getClientIp(request));
            logData.put("requestMethod", request.getMethod());

            // 保存请求参数
            if (operationLog.isSaveRequestData()) {
                String params = getRequestParams(joinPoint, operationLog.excludeParamNames());
                logData.put("operParam", truncate(params, 2000));
            }
        }

        // 获取当前用户
        LoginUser loginUser = getLoginUser();
        if (loginUser != null) {
            logData.put("operName", loginUser.getUsername());
            logData.put("deptName", loginUser.getNickname());
        }

        CURRENT_LOG.set(logData);
    }

    /**
     * 方法正常返回后记录日志
     */
    @AfterReturning(pointcut = "operationLogPointcut()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        handleLog(joinPoint, null, result);
    }

    /**
     * 方法抛出异常后记录日志
     */
    @AfterThrowing(pointcut = "operationLogPointcut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    /**
     * 处理日志记录
     */
    private void handleLog(JoinPoint joinPoint, Exception e, Object result) {
        try {
            Map<String, Object> logData = CURRENT_LOG.get();
            if (logData == null) {
                return;
            }

            // 计算耗时
            Long startTime = START_TIME.get();
            if (startTime != null) {
                logData.put("costTime", System.currentTimeMillis() - startTime);
            }

            // 设置状态
            if (e != null) {
                logData.put("status", 1);
                logData.put("errorMsg", truncate(e.getMessage(), 2000));
            } else {
                logData.put("status", 0);
                // 保存响应结果
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                OperationLog operationLog = signature.getMethod().getAnnotation(OperationLog.class);
                if (operationLog.isSaveResponseData() && result != null) {
                    logData.put("jsonResult", truncate(objectMapper.writeValueAsString(result), 2000));
                }
            }

            // 发布事件
            eventPublisher.publishEvent(new OperationLogEvent(logData));
        } catch (Exception ex) {
            log.error("记录操作日志异常", ex);
        } finally {
            START_TIME.remove();
            CURRENT_LOG.remove();
        }
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(JoinPoint joinPoint, String[] excludeParamNames) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            if (paramNames == null || args == null) {
                return "";
            }

            Set<String> excludeSet = new HashSet<>(Arrays.asList(excludeParamNames));
            Map<String, Object> params = new LinkedHashMap<>();

            for (int i = 0; i < paramNames.length; i++) {
                String paramName = paramNames[i];
                Object paramValue = args[i];

                // 排除指定参数
                if (excludeSet.contains(paramName)) {
                    continue;
                }

                // 排除不需要记录的参数类型
                if (paramValue instanceof HttpServletRequest
                        || paramValue instanceof HttpServletResponse
                        || paramValue instanceof MultipartFile) {
                    continue;
                }

                params.put(paramName, paramValue);
            }

            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("获取请求参数失败", e);
            return "";
        }
    }

    /**
     * 获取当前登录用户
     */
    private LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

}