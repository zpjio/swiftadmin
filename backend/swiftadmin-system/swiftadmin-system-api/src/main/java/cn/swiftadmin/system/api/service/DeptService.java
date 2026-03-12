package cn.swiftadmin.system.api.service;

import cn.swiftadmin.system.api.dto.DeptDTO;
import cn.swiftadmin.system.api.entity.SysDept;
import cn.swiftadmin.system.api.vo.DeptVO;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 部门服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface DeptService extends IService<SysDept> {

    /**
     * 获取部门树
     *
     * @param status 状态过滤（可选）
     * @return 部门树
     */
    List<DeptVO> getTree(Integer status);

    /**
     * 获取部门详情
     *
     * @param id 部门 ID
     * @return 部门详情
     */
    DeptVO getDetail(Long id);

    /**
     * 创建部门
     *
     * @param dto 部门信息
     * @return 部门 ID
     */
    Long create(DeptDTO dto);

    /**
     * 更新部门
     *
     * @param dto 部门信息
     */
    void update(DeptDTO dto);

    /**
     * 删除部门
     *
     * @param id 部门 ID
     */
    void delete(Long id);

    /**
     * 获取子部门 ID 列表（包含自身）
     *
     * @param deptId 部门 ID
     * @return 部门 ID 列表
     */
    List<Long> getChildrenIds(Long deptId);

    /**
     * 检查部门名称是否存在
     *
     * @param name 部门名称
     * @param parentId 父部门 ID
     * @param excludeId 排除的部门 ID
     * @return true=存在
     */
    boolean existsByName(String name, Long parentId, Long excludeId);

    /**
     * 检查部门是否存在子部门
     *
     * @param deptId 部门 ID
     * @return true=存在
     */
    boolean hasChildren(Long deptId);

    /**
     * 检查部门是否存在用户
     *
     * @param deptId 部门 ID
     * @return true=存在
     */
    boolean hasUsers(Long deptId);

}