package cn.swiftadmin.auth.handler;

import cn.swiftadmin.auth.api.entity.SysSocialBind;
import cn.swiftadmin.auth.api.service.SocialBindService;
import cn.swiftadmin.auth.api.vo.SocialBindVO;
import cn.swiftadmin.auth.strategy.SocialAuthStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 社交绑定处理器
 * <p>统一处理社交账号绑定/解绑操作</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialBindHandler {

    private final SocialBindService socialBindService;
    private final SocialAuthStrategy socialAuthStrategy;
    private final ObjectMapper objectMapper;

    /**
     * 绑定社交账号
     *
     * @param source 平台来源
     * @param userId 系统用户 ID
     * @param tempToken 临时 Token
     */
    public void bind(String source, Long userId, String tempToken) {
        socialAuthStrategy.bindAccount(userId, tempToken);
    }

    /**
     * 解绑社交账号
     *
     * @param source 平台来源
     * @param userId 系统用户 ID
     */
    public void unbind(String source, Long userId) {
        socialBindService.unbind(userId, source.toLowerCase());
        log.info("解绑社交账号成功：userId={}, source={}", userId, source);
    }

    /**
     * 获取用户绑定的社交账号列表
     *
     * @param userId 系统用户 ID
     * @return 绑定列表
     */
    public List<SocialBindVO> getBinds(Long userId) {
        var binds = socialBindService.getByUserId(userId);
        return binds.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取支持的社交平台列表
     */
    public List<String> getSupportedSources() {
        return socialAuthStrategy.getSupportedSources();
    }

    private SocialBindVO convertToVO(SysSocialBind bind) {
        SocialBindVO vo = SocialBindVO.builder()
                .source(bind.getSource())
                .bindTime(bind.getBindTime())
                .build();

        if (bind.getExtraInfo() != null) {
            try {
                JsonNode node = objectMapper.readTree(bind.getExtraInfo());
                vo.setNickname(node.path("nickname").asText());
                vo.setAvatar(node.path("avatar").asText());
            } catch (Exception e) {
                log.warn("解析社交绑定额外信息失败", e);
            }
        }
        return vo;
    }

}