package cn.swiftadmin.system.service.impl;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.UserSessionService;
import cn.swiftadmin.system.api.service.OnlineUserService;
import cn.swiftadmin.system.api.vo.OnlineUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在线用户服务实现类
 * <p>通过 Redis 管理在线用户信息</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {

    private final UserSessionService userSessionService;

    @Override
    public Page<OnlineUserVO> page(String username, String ip, Integer page, Integer pageSize) {
        List<OnlineUserVO> allUsers = listAll();

        List<OnlineUserVO> filtered = allUsers.stream()
                .filter(u -> !StringUtils.hasText(username) || u.getUsername().contains(username))
                .filter(u -> !StringUtils.hasText(ip) || (u.getIp() != null && u.getIp().contains(ip)))
                .collect(Collectors.toList());

        int total = filtered.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        List<OnlineUserVO> pageData = new ArrayList<>();
        if (start < total) {
            pageData = filtered.subList(start, end);
        }

        Page<OnlineUserVO> result = new Page<>();
        result.setRecords(pageData);
        result.setTotalRow(total);
        result.setPageNumber(page);
        result.setPageSize(pageSize);
        return result;
    }

    @Override
    public List<OnlineUserVO> listAll() {
        List<OnlineUserVO> onlineUsers = new ArrayList<>();

        List<String> sessionIds = userSessionService.getAllSessionIds();
        if (CollectionUtils.isEmpty(sessionIds)) {
            return onlineUsers;
        }

        for (String sessionId : sessionIds) {
            LoginUser loginUser = userSessionService.getSession(sessionId);
            if (loginUser != null) {
                OnlineUserVO vo = convertToVO(loginUser, sessionId);
                onlineUsers.add(vo);
            }
        }

        return onlineUsers;
    }

    @Override
    public boolean forceLogout(String sessionId) {
        boolean removed = userSessionService.removeSession(sessionId);
        if (removed) {
            log.info("强制用户下线: sessionId={}", sessionId);
        }
        return removed;
    }

    @Override
    public int forceLogoutBatch(List<String> sessionIds) {
        int count = 0;
        for (String sessionId : sessionIds) {
            if (forceLogout(sessionId)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public long count() {
        return userSessionService.count();
    }

    @Override
    public boolean isOnline(Long userId) {
        List<String> sessionIds = userSessionService.getUserSessionIds(userId);
        return !CollectionUtils.isEmpty(sessionIds);
    }

    private OnlineUserVO convertToVO(LoginUser loginUser, String sessionId) {
        OnlineUserVO vo = new OnlineUserVO();
        vo.setSessionId(sessionId);
        vo.setUserId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setNickname(loginUser.getNickname());
        vo.setDeptName(loginUser.getDeptName());

        UserSessionService.SessionExtra extra = userSessionService.getSessionExtra(sessionId);
        if (extra != null) {
            vo.setIp(extra.ip());
            vo.setLocation(extra.location());
            vo.setBrowser(extra.browser());
            vo.setOs(extra.os());
            vo.setLoginTime(extra.loginTime());
            vo.setExpireTime(extra.expireTime());
        }

        return vo;
    }

}
