package cn.swiftadmin.system.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户协议 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class AgreementVO {

    /** 协议 ID */
    private Long id;

    /** 协议类型 */
    private String type;

    /** 版本号 */
    private String version;

    /** 协议标题 */
    private String title;

    /** 协议内容 */
    private String content;

    /** 状态 */
    private Integer status;

    /** 生效时间 */
    private LocalDateTime effectiveDate;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}