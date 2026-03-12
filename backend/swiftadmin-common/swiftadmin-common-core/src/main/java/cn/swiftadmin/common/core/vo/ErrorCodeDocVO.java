package cn.swiftadmin.common.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 错误码文档 VO
 * <p>用于向前端提供错误码字典，便于错误提示国际化</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorCodeDocVO {

    /** 错误码 */
    private Integer code;

    /** 错误消息（中文） */
    private String message;

    /** 错误分类 */
    private String category;

    /** 错误级别：info/warning/error */
    private String level;

    /**
     * 根据错误码范围判断分类
     */
    public void categorize() {
        if (code == null) {
            return;
        }

        if (code == 0) {
            category = "SUCCESS";
            level = "info";
        } else if (code >= 100 && code < 1000) {
            category = "SYSTEM";
            level = "error";
        } else if (code >= 10000 && code < 100000) {
            category = "COMMON";
            level = "warning";
        } else if (code >= 100000 && code < 200000) {
            category = "USER";
            level = "error";
        } else if (code >= 1000000 && code < 1100000) {
            category = "SOCIAL_AUTH";
            level = "error";
        } else if (code >= 107000 && code < 108000) {
            category = "ACCOUNT";
            level = "warning";
        } else if (code >= 108000 && code < 109000) {
            category = "SECURITY";
            level = "error";
        } else {
            category = "OTHER";
            level = "error";
        }
    }
}
