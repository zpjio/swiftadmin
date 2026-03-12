package cn.swiftadmin.auth.strategy;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.dto.OAuthStateDTO;
import cn.swiftadmin.auth.api.dto.SocialUserInfo;
import cn.swiftadmin.auth.api.entity.SysSocialBind;
import cn.swiftadmin.auth.api.event.LoginEvent;
import cn.swiftadmin.auth.api.service.SocialBindService;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.strategy.social.SocialAuthHandler;
import cn.swiftadmin.auth.util.TokenManager;
import cn.swiftadmin.auth.api.exception.SocialAuthError;
import cn.swiftadmin.auth.api.exception.SocialAuthException;
import cn.swiftadmin.auth.api.enums.LoginType;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.UserDetailsService;
import cn.swiftadmin.common.security.service.UserSessionService;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 社交登录策略
 * <p>统一处理所有社交登录平台，通过注入 SocialAuthHandler 实现扩展</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
public class SocialAuthStrategy implements AuthStrategy {

    private final SocialBindService socialBindService;
    private final TokenManager tokenManager;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, SocialAuthHandler> handlerMap;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest request;
    private final UserDetailsService userDetailsService;
    private final UserSessionService userSessionService;

    /** 临时 Token 有效期（分钟） */
    private static final int TEMP_TOKEN_EXPIRE_MINUTES = 10;

    /** Lua 脚本：原子性获取并删除 */
    private static final String GET_AND_DELETE_SCRIPT =
            "local v = redis.call('GET', KEYS[1]); " +
            "if v then redis.call('DEL', KEYS[1]); end; " +
            "return v;";

    /**
     * 构造方法，自动注入所有 SocialAuthHandler
     */
    public SocialAuthStrategy(
            SocialBindService socialBindService,
            TokenManager tokenManager,
            UserService userService,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            ApplicationEventPublisher eventPublisher,
            HttpServletRequest request,
            UserDetailsService userDetailsService,
            UserSessionService userSessionService,
            List<SocialAuthHandler> handlers) {
        this.socialBindService = socialBindService;
        this.tokenManager = tokenManager;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.request = request;
        this.userDetailsService = userDetailsService;
        this.userSessionService = userSessionService;
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(SocialAuthHandler::getSource, Function.identity()));
        log.info("社交登录策略初始化完成，已注册平台：{}", handlerMap.keySet());
    }

    @Override
    public LoginType getLoginType() {
        return null; // 社交登录策略不绑定单一类型
    }

    @Override
    public String getSource() {
        return "social"; // 通用标识
    }

    @Override
    public boolean supportsBind() {
        return true;
    }

    @Override
    public LoginVO authenticate(LoginDTO dto) {
        SocialAuthHandler handler = getHandler(dto.getSource());
        SocialUserInfo userInfo = handler.getUserInfo(dto.getAuthCode());
        String openId = handler.extractOpenId(userInfo);
        return handleSocialLogin(dto.getSource(), openId, userInfo);
    }

    /**
     * 带 State 的认证方法（区分登录/绑定场景）
     *
     * @param dto 登录参数
     * @param stateDTO State 参数
     * @return 登录结果
     */
    public LoginVO authenticateWithState(LoginDTO dto, OAuthStateDTO stateDTO) {
        SocialAuthHandler handler = getHandler(dto.getSource());
        SocialUserInfo userInfo = handler.getUserInfo(dto.getAuthCode());
        String openId = handler.extractOpenId(userInfo);

        // 根据 action 区分处理
        String action = stateDTO.getAction();
        if (OAuthStateDTO.ACTION_BIND.equals(action)) {
            return handleSocialBind(dto.getSource(), openId, userInfo, stateDTO.getBindUserId());
        } else {
            // 默认为登录场景
            return handleSocialLogin(dto.getSource(), openId, userInfo);
        }
    }

    @Override
    public String getAuthUrl(String state) {
        throw new UnsupportedOperationException("请使用 getAuthUrl(source, state) 方法");
    }

    /**
     * 获取社交登录授权 URL
     *
     * @param source 平台来源
     * @param sessionId 会话 ID（可选）
     * @return 授权 URL 和 State
     */
    public SocialUrlInfo getAuthUrl(String source, String sessionId) {
        // 构建登录场景的 State 参数
        OAuthStateDTO stateDTO = OAuthStateDTO.builder()
                .source(source.toLowerCase())
                .sessionId(sessionId)
                .timestamp(System.currentTimeMillis())
                .nonce(UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .action(OAuthStateDTO.ACTION_LOGIN)
                .build();

        return buildAuthUrl(source, stateDTO);
    }

    /**
     * 获取绑定第三方账号的授权 URL
     * <p>已登录用户主动绑定第三方账号时使用</p>
     *
     * @param source 平台来源
     * @param userId 系统用户 ID
     * @return 授权 URL 和 State
     */
    public SocialUrlInfo getBindUrl(String source, Long userId) {
        // 构建绑定场景的 State 参数
        OAuthStateDTO stateDTO = OAuthStateDTO.builder()
                .source(source.toLowerCase())
                .timestamp(System.currentTimeMillis())
                .nonce(UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .action(OAuthStateDTO.ACTION_BIND)
                .bindUserId(userId)
                .build();

        return buildAuthUrl(source, stateDTO);
    }

    /**
     * 构建授权 URL（内部方法）
     */
    private SocialUrlInfo buildAuthUrl(String source, OAuthStateDTO stateDTO) {
        String state = encodeState(stateDTO);
        String url = getHandler(source).getAuthUrl(state);

        // 存储 State 用于验证（保留兼容性）
        String stateKey = "oauth:state:" + state;
        try {
            String stateJson = objectMapper.writeValueAsString(stateDTO);
            redisTemplate.opsForValue().set(stateKey, stateJson, OAuthStateDTO.VALID_PERIOD, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("存储 State 失败", e);
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "生成授权链接失败");
        }

        return new SocialUrlInfo(url, state);
    }

    /**
     * 验证 State 参数
     */
    public OAuthStateDTO validateState(String state) {
        if (state == null || state.isEmpty()) {
            throw new SocialAuthException(SocialAuthError.STATE_INVALID);
        }

        // 先从 Redis 获取
        String stateKey = "oauth:state:" + state;
        String stateJson = redisTemplate.opsForValue().get(stateKey);

        OAuthStateDTO stateDTO;
        if (stateJson != null) {
            // Redis 中存在，使用存储的值
            try {
                stateDTO = objectMapper.readValue(stateJson, OAuthStateDTO.class);
            } catch (Exception e) {
                log.warn("解析 State 失败：state={}", state, e);
                throw new SocialAuthException(SocialAuthError.STATE_INVALID);
            }
            // 删除已使用的 State
            redisTemplate.delete(stateKey);
        } else {
            // 尝试直接解析 State（Base64）
            stateDTO = decodeState(state);
            if (stateDTO == null) {
                throw new SocialAuthException(SocialAuthError.STATE_EXPIRED);
            }
        }

        // 检查是否过期
        if (stateDTO.isExpired()) {
            throw new SocialAuthException(SocialAuthError.STATE_EXPIRED);
        }

        return stateDTO;
    }

    /**
     * 获取支持的社交平台列表
     */
    public List<String> getSupportedSources() {
        return List.copyOf(handlerMap.keySet());
    }

    /**
     * 绑定社交账号
     */
    @Override
    public void bindAccount(Long userId, String tempToken) {
        String cacheKey = "social:temp:" + tempToken;

        // 使用 Lua 脚本原子性获取并删除（确保单次使用）
        DefaultRedisScript<String> script = new DefaultRedisScript<>(GET_AND_DELETE_SCRIPT, String.class);
        String userInfoJson = redisTemplate.execute(script, Collections.singletonList(cacheKey));

        if (userInfoJson == null) {
            throw new SocialAuthException(SocialAuthError.TEMP_TOKEN_EXPIRED);
        }

        Map<String, Object> userInfo;
        try {
            userInfo = objectMapper.readValue(userInfoJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("解析临时 Token 数据失败", e);
            throw new SocialAuthException(SocialAuthError.TEMP_TOKEN_EXPIRED);
        }

        String openId = (String) userInfo.get("openId");
        String source = (String) userInfo.get("source");
        if (openId == null || source == null) {
            throw new SocialAuthException(SocialAuthError.TEMP_TOKEN_EXPIRED, "数据不完整");
        }

        socialBindService.bind(userId, source, openId, userInfo);
        updateUserInfo(userId, userInfo);

        log.info("用户绑定社交账号成功：userId={}, source={}", userId, source);
    }

    // ========== 私有方法 ==========

    private SocialAuthHandler getHandler(String source) {
        SocialAuthHandler handler = handlerMap.get(source.toLowerCase());
        if (handler == null) {
            throw new SocialAuthException(SocialAuthError.UNSUPPORTED_PLATFORM, source);
        }
        return handler;
    }

    private LoginVO handleSocialLogin(String source, String openId, SocialUserInfo userInfo) {
        String ip = ServletUtils.getClientIp(request);
        String userAgent = ServletUtils.getUserAgent(request);

        SysSocialBind bind = socialBindService.getBySourceAndOpenId(source, openId);

        if (bind != null) {
            // 已绑定：直接登录
            SysUser user = userService.getById(bind.getUserId());
            if (user == null) {
                eventPublisher.publishEvent(LoginEvent.failure("unknown", "用户不存在", ip, userAgent, source));
                throw new SocialAuthException(SocialAuthError.BIND_NOT_FOUND, "用户不存在");
            }
            if (!user.getStatus().isEnabled()) {
                eventPublisher.publishEvent(LoginEvent.failure(user.getUsername(), "用户已被禁用", ip, userAgent, source));
                throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "用户已被禁用");
            }

            // 更新 Token
            Map<String, Object> tokenInfo = userInfo.toMap();
            socialBindService.updateToken(bind.getId(), tokenInfo);

            log.info("社交账号登录成功：source={}, openId={}, userId={}", source, openId, user.getId());
            eventPublisher.publishEvent(LoginEvent.success(user.getUsername(), ip, userAgent, source));
            return buildLoginVO(user);
        } else {
            // 未绑定：返回临时 Token
            log.info("社交账号未绑定：source={}, openId={}", source, openId);
            String tempToken = generateTempToken(openId, source, userInfo);
            return buildNeedBindVO(tempToken, userInfo, source);
        }
    }

    /**
     * 处理绑定场景（已登录用户主动绑定）
     */
    private LoginVO handleSocialBind(String source, String openId, SocialUserInfo userInfo, Long bindUserId) {
        if (bindUserId == null) {
            throw new SocialAuthException(SocialAuthError.STATE_INVALID, "绑定用户ID缺失");
        }

        // 检查用户是否存在
        SysUser user = userService.getById(bindUserId);
        if (user == null) {
            throw new SocialAuthException(SocialAuthError.BIND_NOT_FOUND, "用户不存在");
        }

        // 检查该社交账号是否已被其他用户绑定
        SysSocialBind existingBind = socialBindService.getBySourceAndOpenId(source, openId);
        if (existingBind != null) {
            if (existingBind.getUserId().equals(bindUserId)) {
                // 已经绑定到当前用户，直接返回成功
                log.info("社交账号已绑定到当前用户：source={}, openId={}, userId={}", source, openId, bindUserId);
                return buildBindSuccessVO(source);
            } else {
                // 已被其他用户绑定
                log.warn("社交账号已被其他用户绑定：source={}, openId={}, boundUserId={}", source, openId, existingBind.getUserId());
                throw new SocialAuthException(SocialAuthError.BIND_ALREADY_EXISTS, "该社交账号已被其他用户绑定");
            }
        }

        // 检查当前用户是否已绑定同类型社交账号
        SysSocialBind userBind = socialBindService.getByUserIdAndSource(bindUserId, source);
        if (userBind != null) {
            throw new SocialAuthException(SocialAuthError.USER_ALREADY_BOUND, "您已绑定该类型的社交账号");
        }

        // 执行绑定
        Map<String, Object> userInfoMap = userInfo.toMap();
        socialBindService.bind(bindUserId, source, openId, userInfoMap);
        updateUserInfo(bindUserId, userInfoMap);

        log.info("用户绑定社交账号成功：userId={}, source={}, openId={}", bindUserId, source, openId);
        return buildBindSuccessVO(source);
    }

    /**
     * 构建绑定成功响应
     */
    private LoginVO buildBindSuccessVO(String source) {
        return LoginVO.builder()
                .needBind(false)
                .build();
    }

    private String generateTempToken(String openId, String source, SocialUserInfo userInfo) {
        String tempToken = UUID.randomUUID().toString().replace("-", "");
        String cacheKey = "social:temp:" + tempToken;

        Map<String, Object> tempInfo = userInfo.toMap();
        tempInfo.put("openId", openId);
        tempInfo.put("source", source);

        try {
            String userInfoJson = objectMapper.writeValueAsString(tempInfo);
            redisTemplate.opsForValue().set(cacheKey, userInfoJson, TEMP_TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("生成临时 Token 失败", e);
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "生成临时 Token 失败");
        }

        return tempToken;
    }

    private LoginVO buildLoginVO(SysUser user) {
        LoginVO loginVO = tokenManager.generateLoginTokens(user.getUsername());
        loginVO.setNeedBind(false);

        String sessionId = tokenManager.getJtiFromToken(loginVO.getAccessToken());
        LoginUser loginUser = userDetailsService.loadUserByUsername(user.getUsername());
        if (loginUser != null) {
            String ip = ServletUtils.getClientIp(request);
            String userAgent = ServletUtils.getUserAgent(request);
            userSessionService.createSession(
                    sessionId,
                    loginUser,
                    ip,
                    userAgent,
                    tokenManager.getAccessTokenExpiration()
            );
        }

        return loginVO;
    }

    private LoginVO buildNeedBindVO(String tempToken, SocialUserInfo userInfo, String source) {
        LoginVO.UserInfo userInfoVO = LoginVO.UserInfo.builder()
                .nickname(userInfo.getNickname())
                .avatar(userInfo.getAvatar())
                .source(source)
                .build();

        return LoginVO.builder()
                .tempToken(tempToken)
                .needBind(true)
                .userInfo(userInfoVO)
                .build();
    }

    private void updateUserInfo(Long userId, Map<String, Object> userInfo) {
        String nickname = (String) userInfo.get("nickname");
        String avatar = (String) userInfo.get("avatar");

        if (nickname != null || avatar != null) {
            userService.updateSocialInfo(userId, nickname, avatar);
        }
    }

    /**
     * 编码 State 为 Base64
     */
    private String encodeState(OAuthStateDTO stateDTO) {
        try {
            String json = objectMapper.writeValueAsString(stateDTO);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("编码 State 失败", e);
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "生成授权参数失败");
        }
    }

    /**
     * 解码 Base64 State
     */
    private OAuthStateDTO decodeState(String state) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(state);
            String json = new String(bytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, OAuthStateDTO.class);
        } catch (Exception e) {
            log.debug("解码 State 失败：state={}", state);
            return null;
        }
    }

    /**
     * 授权 URL 信息
     */
    public record SocialUrlInfo(String url, String state) {}

}