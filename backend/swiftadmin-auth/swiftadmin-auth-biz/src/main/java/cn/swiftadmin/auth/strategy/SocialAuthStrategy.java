package cn.swiftadmin.auth.strategy;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.dto.OAuthStateDTO;
import cn.swiftadmin.auth.api.dto.SocialUserInfo;
import cn.swiftadmin.auth.api.entity.SysSocialBind;
import cn.swiftadmin.auth.api.enums.LoginType;
import cn.swiftadmin.auth.api.service.SocialBindService;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.context.LoginContext;
import cn.swiftadmin.auth.service.LoginEventService;
import cn.swiftadmin.auth.service.LoginSessionService;
import cn.swiftadmin.auth.strategy.social.SocialAuthHandler;
import cn.swiftadmin.auth.api.exception.SocialAuthError;
import cn.swiftadmin.auth.api.exception.SocialAuthException;
import cn.swiftadmin.common.core.constants.RedisKeyConstants;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.UserService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 社交登录策略
 * <p>统一处理所有社交登录平台，通过注入 SocialAuthHandler 实现扩展</p>
 * <p>重构版：依赖注入从 9 个减少到 8 个</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
public class SocialAuthStrategy implements AuthStrategy {

    private final SocialBindService socialBindService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, SocialAuthHandler> handlerMap;
    private final LoginSessionService sessionService;
    private final LoginEventService eventService;
    private final HttpServletRequest request;

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
            UserService userService,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            LoginSessionService sessionService,
            LoginEventService eventService,
            HttpServletRequest request,
            List<SocialAuthHandler> handlers) {
        this.socialBindService = socialBindService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.sessionService = sessionService;
        this.eventService = eventService;
        this.request = request;
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(SocialAuthHandler::getSource, Function.identity()));
        log.debug("社交登录策略初始化完成，已注册平台：{}", handlerMap.keySet());
    }

    /**
     * 获取登录类型
     * <p>社交登录不走 AuthStrategyFactory，直接由 SocialAuthController 调用</p>
     * <p>因此返回 null，不参与策略工厂的路由</p>
     *
     * @return null
     */
    @Override
    public LoginType getLoginType() {
        return null;
    }

    @Override
    public String getSource() {
        return "social";
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
     */
    public LoginVO authenticateWithState(LoginDTO dto, OAuthStateDTO stateDTO) {
        SocialAuthHandler handler = getHandler(dto.getSource());
        SocialUserInfo userInfo = handler.getUserInfo(dto.getAuthCode());
        String openId = handler.extractOpenId(userInfo);

        String action = stateDTO.getAction();
        if (OAuthStateDTO.ACTION_BIND.equals(action)) {
            return handleSocialBind(dto.getSource(), openId, userInfo, stateDTO.getBindUserId());
        } else {
            return handleSocialLogin(dto.getSource(), openId, userInfo);
        }
    }

    @Override
    public String getAuthUrl(String state) {
        throw new UnsupportedOperationException("请使用 getAuthUrl(source, state) 方法");
    }

    /**
     * 获取社交登录授权 URL
     */
    public SocialUrlInfo getAuthUrl(String source, String sessionId) {
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
     */
    public SocialUrlInfo getBindUrl(String source, Long userId) {
        OAuthStateDTO stateDTO = OAuthStateDTO.builder()
                .source(source.toLowerCase())
                .timestamp(System.currentTimeMillis())
                .nonce(UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .action(OAuthStateDTO.ACTION_BIND)
                .bindUserId(userId)
                .build();

        return buildAuthUrl(source, stateDTO);
    }

    private SocialUrlInfo buildAuthUrl(String source, OAuthStateDTO stateDTO) {
        String state = encodeState(stateDTO);
        String url = getHandler(source).getAuthUrl(state);

        String stateKey = RedisKeyConstants.oauthState(state);
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

        String stateKey = RedisKeyConstants.oauthState(state);
        String stateJson = redisTemplate.opsForValue().get(stateKey);

        OAuthStateDTO stateDTO;
        if (stateJson != null) {
            try {
                stateDTO = objectMapper.readValue(stateJson, OAuthStateDTO.class);
            } catch (Exception e) {
                log.warn("解析 State 失败：state={}", state, e);
                throw new SocialAuthException(SocialAuthError.STATE_INVALID);
            }
            redisTemplate.delete(stateKey);
        } else {
            stateDTO = decodeState(state);
            if (stateDTO == null) {
                throw new SocialAuthException(SocialAuthError.STATE_EXPIRED);
            }
        }

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
        String cacheKey = RedisKeyConstants.oauthTempToken(tempToken);

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

    private LoginContext buildContext(String source) {
        String ip = ServletUtils.getClientIp(request);
        String userAgent = ServletUtils.getUserAgent(request);
        return LoginContext.ofSocial(ip, userAgent, source);
    }

    private LoginVO handleSocialLogin(String source, String openId, SocialUserInfo userInfo) {
        LoginContext context = buildContext(source);
        SysSocialBind bind = socialBindService.getBySourceAndOpenId(source, openId);

        if (bind != null) {
            // 已绑定：直接登录
            SysUser user = userService.getById(bind.getUserId());
            if (user == null) {
                eventService.publishFailure("unknown", "用户不存在", context);
                throw new SocialAuthException(SocialAuthError.BIND_NOT_FOUND, "用户不存在");
            }
            if (!user.getStatus().isEnabled()) {
                eventService.publishFailure(user.getUsername(), "用户已被禁用", context);
                throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "用户已被禁用");
            }

            // 更新 Token
            Map<String, Object> tokenInfo = userInfo.toMap();
            socialBindService.updateToken(bind.getId(), tokenInfo);

            log.info("社交账号登录成功：source={}, openId={}, userId={}", source, openId, user.getId());
            eventService.publishSuccess(user.getUsername(), context);
            return sessionService.createSession(user.getUsername(), context);
        } else {
            // 未绑定：返回临时 Token
            log.info("社交账号未绑定：source={}, openId={}", source, openId);
            String tempToken = generateTempToken(openId, source, userInfo);
            return buildNeedBindVO(tempToken, userInfo, source);
        }
    }

    private LoginVO handleSocialBind(String source, String openId, SocialUserInfo userInfo, Long bindUserId) {
        if (bindUserId == null) {
            throw new SocialAuthException(SocialAuthError.STATE_INVALID, "绑定用户ID缺失");
        }

        SysUser user = userService.getById(bindUserId);
        if (user == null) {
            throw new SocialAuthException(SocialAuthError.BIND_NOT_FOUND, "用户不存在");
        }

        SysSocialBind existingBind = socialBindService.getBySourceAndOpenId(source, openId);
        if (existingBind != null) {
            if (existingBind.getUserId().equals(bindUserId)) {
                log.info("社交账号已绑定到当前用户：source={}, openId={}, userId={}", source, openId, bindUserId);
                return buildBindSuccessVO(source);
            } else {
                log.warn("社交账号已被其他用户绑定：source={}, openId={}, boundUserId={}", source, openId, existingBind.getUserId());
                throw new SocialAuthException(SocialAuthError.BIND_ALREADY_EXISTS, "该社交账号已被其他用户绑定");
            }
        }

        SysSocialBind userBind = socialBindService.getByUserIdAndSource(bindUserId, source);
        if (userBind != null) {
            throw new SocialAuthException(SocialAuthError.USER_ALREADY_BOUND, "您已绑定该类型的社交账号");
        }

        Map<String, Object> userInfoMap = userInfo.toMap();
        socialBindService.bind(bindUserId, source, openId, userInfoMap);
        updateUserInfo(bindUserId, userInfoMap);

        log.info("用户绑定社交账号成功：userId={}, source={}, openId={}", bindUserId, source, openId);
        return buildBindSuccessVO(source);
    }

    private LoginVO buildBindSuccessVO(String source) {
        return LoginVO.builder()
                .needBind(false)
                .build();
    }

    private String generateTempToken(String openId, String source, SocialUserInfo userInfo) {
        String tempToken = UUID.randomUUID().toString().replace("-", "");
        String cacheKey = RedisKeyConstants.oauthTempToken(tempToken);

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