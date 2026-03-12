package cn.swiftadmin.system.api.dto;

import lombok.Data;

/**
 * 用户查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class UserQueryDTO {

    /** 当前页码 */
    private Integer page = 1;

    /** 每页大小 */
    private Integer pageSize = 10;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 状态：1=正常，0=禁用 */
    private Integer status;

    /** 部门 ID */
    private Long deptId;

    /** 创建时间开始 */
    private String createTimeBegin;

    /** 创建时间结束 */
    private String createTimeEnd;

}