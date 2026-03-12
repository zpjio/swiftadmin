package cn.swiftadmin.auth.api.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 社交授权 URL VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
public class SocialUrlVO {

    private String url;
    private String state;

}
