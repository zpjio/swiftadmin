package cn.swiftadmin.common.core.util;

import cn.swiftadmin.common.core.result.R;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 统一错误响应写入器
 * <p>封装 HTTP 错误响应的写入逻辑，避免代码重复</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    /**
     * 写入错误响应
     *
     * @param response HTTP 响应
     * @param status 状态码
     * @param message 错误消息
     * @throws IOException IO 异常
     */
    public void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(status, message)));
    }

    /**
     * 写入错误响应（带数据）
     *
     * @param response HTTP 响应
     * @param status 状态码
     * @param message 错误消息
     * @param data 错误数据
     * @throws IOException IO 异常
     */
    public <T> void writeError(HttpServletResponse response, int status, String message, T data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(status, message)));
    }

}