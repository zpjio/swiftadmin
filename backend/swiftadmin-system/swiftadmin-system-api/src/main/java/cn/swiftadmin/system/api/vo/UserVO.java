package cn.swiftadmin.system.api.vo;

import cn.swiftadmin.common.core.enums.CommonStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class UserVO {

    /** 部门名称字段名常量（用于 SQL 别名映射） */
    public static final String FIELD_DEPT_NAME = "deptName";

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 URL */
    private String avatar;

    /** 状态：1=正常，0=禁用 */
    private CommonStatus status;

    /** 部门 ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 角色 ID 列表 */
    private List<Long> roleIds;

    /** 角色名称列表 */
    private List<String> roleNames;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}