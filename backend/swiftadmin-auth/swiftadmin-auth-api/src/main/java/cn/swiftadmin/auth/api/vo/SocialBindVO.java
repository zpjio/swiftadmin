package cn.swiftadmin.auth.api.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社交绑定信息 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
public class SocialBindVO {

    private String source;

    private String nickname;

    private String avatar;

    private LocalDateTime bindTime;

}
