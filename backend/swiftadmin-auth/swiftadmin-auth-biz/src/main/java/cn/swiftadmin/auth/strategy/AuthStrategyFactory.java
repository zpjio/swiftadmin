package cn.swiftadmin.auth.strategy;

import cn.swiftadmin.auth.api.enums.LoginType;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 认证策略工厂
 * <p>管理所有认证策略，根据登录类型分发到对应策略</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Component
public class AuthStrategyFactory {

    private final Map<LoginType, AuthStrategy> strategyMap = new EnumMap<>(LoginType.class);

    /**
     * 构造方法，注入所有 AuthStrategy 实现类
     *
     * @param strategies 认证策略列表
     */
    public AuthStrategyFactory(List<AuthStrategy> strategies) {
        for (AuthStrategy strategy : strategies) {
            LoginType loginType = strategy.getLoginType();
            if (loginType != null) {
                strategyMap.put(loginType, strategy);
            }
        }
    }

    /**
     * 根据登录类型获取对应的认证策略
     *
     * @param loginType 登录类型
     * @return 认证策略实现
     */
    public AuthStrategy getStrategy(LoginType loginType) {
        AuthStrategy strategy = strategyMap.get(loginType);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.LOGIN_TYPE_NOT_SUPPORTED);
        }
        return strategy;
    }

    /**
     * 根据登录方式代码获取对应的认证策略
     *
     * @param typeCode 登录方式代码
     * @return 认证策略实现
     */
    public AuthStrategy getStrategyByCode(String typeCode) {
        LoginType loginType = LoginType.fromCode(typeCode);
        return getStrategy(loginType);
    }

}
