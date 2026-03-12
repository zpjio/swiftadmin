package cn.swiftadmin.common.core.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Servlet 工具类
 * <p>提供获取客户端 IP、User-Agent、请求 URL 等常用方法</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Slf4j
public final class ServletUtils {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String SEPARATOR = ",";

    /** IP 代理头优先级列表 */
    private static final List<String> IP_HEADER_CANDIDATES = Arrays.asList(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    );

    /** 私有 IP 地址正则 */
    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
            "^(127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|" +
            "(10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|" +
            "(172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3})|" +
            "(192\\.168\\.\\d{1,3}\\.\\d{1,3})|" +
            "(0\\.0\\.0\\.0)$"
    );

    /** IP 缓存（ThreadLocal） */
    private static final ThreadLocal<Map<String, String>> IP_CACHE = ThreadLocal.withInitial(HashMap::new);

    private ServletUtils() {
    }

    /**
     * 获取当前请求
     *
     * @return 当前请求，不存在返回 null
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端真实 IP（自动获取当前请求）
     *
     * @return 客户端 IP
     */
    public static String getClientIp() {
        HttpServletRequest request = getRequest();
        return request != null ? getClientIp(request) : null;
    }

    /**
     * 获取客户端真实 IP
     * <p>依次检查代理头，使用优先级列表获取第一个有效 IP</p>
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        if (Objects.isNull(request)) {
            return null;
        }

        String cacheKey = request.getRequestId();
        String cachedIp = IP_CACHE.get().get(cacheKey);
        if (cachedIp != null) {
            return cachedIp;
        }

        String ip = null;
        for (String header : IP_HEADER_CANDIDATES) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }

        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        ip = normalizeIp(ip);

        if (ip != null && ip.contains(SEPARATOR)) {
            ip = ip.split(SEPARATOR)[0].trim();
        }

        IP_CACHE.get().put(cacheKey, ip);
        return ip;
    }

    /**
     * 清除 IP 缓存
     */
    public static void clearIpCache() {
        IP_CACHE.remove();
    }

    /**
     * 验证 IP 是否有效
     */
    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * 标准化 IP 地址
     */
    private static String normalizeIp(String ip) {
        if (ip == null) {
            return null;
        }

        if (LOCALHOST_IPV6.equals(ip)) {
            return LOCALHOST_IPV4;
        }

        return ip;
    }

    /**
     * 获取 User-Agent（自动获取当前请求）
     *
     * @return User-Agent
     */
    public static String getUserAgent() {
        HttpServletRequest request = getRequest();
        return request != null ? getUserAgent(request) : null;
    }

    /**
     * 获取 User-Agent
     *
     * @param request HTTP 请求
     * @return User-Agent
     */
    public static String getUserAgent(HttpServletRequest request) {
        return request != null ? request.getHeader("User-Agent") : null;
    }

    /**
     * 获取请求 URL
     *
     * @param request HTTP 请求
     * @return 请求 URL
     */
    public static String getRequestUrl(HttpServletRequest request) {
        return request != null ? request.getRequestURL().toString() : null;
    }

    /**
     * 获取请求 URI
     *
     * @param request HTTP 请求
     * @return 请求 URI
     */
    public static String getRequestUri(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : null;
    }

    /**
     * 判断是否为 Ajax 请求
     *
     * @param request HTTP 请求
     * @return true=Ajax 请求
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String xRequestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(xRequestedWith);
    }

    /**
     * 判断是否为 JSON 请求
     *
     * @param request HTTP 请求
     * @return true=JSON 请求
     */
    public static boolean isJsonRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String contentType = request.getContentType();
        return contentType != null && contentType.contains("application/json");
    }

    /**
     * 获取请求头
     *
     * @param name 请求头名称
     * @return 请求头值
     */
    public static String getHeader(String name) {
        HttpServletRequest request = getRequest();
        return request != null ? request.getHeader(name) : null;
    }

    /**
     * 获取所有请求头
     *
     * @param request HTTP 请求
     * @return 请求头 Map
     */
    public static Map<String, String> getAllHeaders(HttpServletRequest request) {
        if (request == null) {
            return Collections.emptyMap();
        }

        Map<String, String> headers = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(name -> 
            headers.put(name, request.getHeader(name))
        );
        return headers;
    }

    /**
     * 判断 IP 是否为私有地址
     *
     * @param ip IP 地址
     * @return true=私有地址
     */
    public static boolean isPrivateIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return PRIVATE_IP_PATTERN.matcher(ip).matches();
    }

    /**
     * 验证 IP 地址格式
     *
     * @param ip IP 地址
     * @return true=格式正确
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        try {
            InetAddress address = InetAddress.getByName(ip);
            return address != null;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * 获取请求协议
     *
     * @param request HTTP 请求
     * @return 协议（http/https）
     */
    public static String getScheme(HttpServletRequest request) {
        return request != null ? request.getScheme() : null;
    }

    /**
     * 获取请求主机
     *
     * @param request HTTP 请求
     * @return 主机名
     */
    public static String getHost(HttpServletRequest request) {
        return request != null ? request.getServerName() : null;
    }

    /**
     * 获取请求端口
     *
     * @param request HTTP 请求
     * @return 端口号
     */
    public static int getPort(HttpServletRequest request) {
        return request != null ? request.getServerPort() : -1;
    }

    /**
     * 获取完整请求 URL（包含协议、主机、端口、URI）
     *
     * @param request HTTP 请求
     * @return 完整 URL
     */
    public static String getFullUrl(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme()).append("://");
        url.append(request.getServerName());
        
        int port = request.getServerPort();
        if ((!"http".equals(request.getScheme()) || port != 80) &&
            (!"https".equals(request.getScheme()) || port != 443)) {
            url.append(":").append(port);
        }
        
        url.append(request.getRequestURI());
        
        String queryString = request.getQueryString();
        if (queryString != null) {
            url.append("?").append(queryString);
        }
        
        return url.toString();
    }
}
