package cn.swiftadmin.system.api.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_oper_log")
public class SysOperLog {

    /** 日志ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 操作标题 */
    @Column
    private String title;

    /** 业务类型：0=其它，1=新增，2=修改，3=删除，4=授权，5=导出，6=导入，7=强退，8=生成代码，9=清空数据 */
    @Column
    private Integer businessType;

    /** 方法名称 */
    @Column
    private String method;

    /** 请求方式 */
    @Column
    private String requestMethod;

    /** 操作类别：0=其它，1=后台用户，2=手机端用户 */
    @Column
    private Integer operatorType;

    /** 操作人员 */
    @Column
    private String operName;

    /** 部门名称 */
    @Column
    private String deptName;

    /** 请求URL */
    @Column
    private String operUrl;

    /** 主机地址 */
    @Column
    private String operIp;

    /** 操作地点 */
    @Column
    private String operLocation;

    /** 请求参数 */
    @Column
    private String operParam;

    /** 返回参数 */
    @Column
    private String jsonResult;

    /** 操作状态：0=正常，1=异常 */
    @Column
    private Integer status;

    /** 错误消息 */
    @Column
    private String errorMsg;

    /** 操作时间 */
    @Column
    private LocalDateTime operTime;

    /** 消耗时间(毫秒) */
    @Column
    private Long costTime;

}