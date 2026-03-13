package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.enums.CommonStatus;
import cn.swiftadmin.system.api.dto.DeptDTO;
import cn.swiftadmin.system.api.entity.SysDept;
import cn.swiftadmin.system.api.service.DeptService;
import cn.swiftadmin.system.api.vo.DeptVO;
import cn.swiftadmin.system.mapper.SysDeptMapper;
import cn.swiftadmin.system.mapper.SysUserMapper;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.swiftadmin.system.api.entity.table.SysDeptTableDef.SYS_DEPT;
import static cn.swiftadmin.system.api.entity.table.SysUserTableDef.SYS_USER;

/**
 * 部门服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements DeptService {

    private final SysUserMapper userMapper;

    @Override
    public List<DeptVO> getTree(Integer status) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_DEPT.STATUS.eq(status, status != null))
                .orderBy(SYS_DEPT.SORT_ORDER.asc(), SYS_DEPT.CREATE_TIME.asc());

        return buildTree(list(queryWrapper), 0L);
    }

    @Override
    public DeptVO getDetail(Long id) {
        SysDept dept = getById(id);
        return dept != null ? toDeptVO(dept) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DeptDTO dto) {
        Long parentId = dto.getParentId() != null ? dto.getParentId() : 0L;

        if (existsByName(dto.getName(), parentId, null)) {
            throw new BusinessException(400, "同级部门名称已存在");
        }

        // 计算祖级路径
        String ancestors = calculateAncestors(parentId);

        SysDept dept = SysDept.builder()
                .parentId(parentId)
                .name(dto.getName())
                .ancestors(ancestors)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .leader(dto.getLeader())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .status(dto.getStatus() != null ? CommonStatus.fromCode(dto.getStatus()) : CommonStatus.ENABLED)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();

        save(dept);
        return dept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DeptDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "部门 ID 不能为空");
        }

        SysDept existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(404, "部门不存在");
        }

        Long parentId = dto.getParentId() != null ? dto.getParentId() : 0L;

        if (parentId.equals(dto.getId())) {
            throw new BusinessException(400, "父部门不能是自己");
        }

        // 检查是否移动到了自己的子部门
        if (isChildOf(parentId, dto.getId())) {
            throw new BusinessException(400, "不能移动到自己的子部门");
        }

        if (!existing.getName().equals(dto.getName()) && existsByName(dto.getName(), parentId, dto.getId())) {
            throw new BusinessException(400, "同级部门名称已存在");
        }

        // 计算新的祖级路径
        String newAncestors = calculateAncestors(parentId);

        UpdateChain.of(SysDept.class)
                .set(SYS_DEPT.PARENT_ID, parentId)
                .set(SYS_DEPT.NAME, dto.getName())
                .set(SYS_DEPT.ANCESTORS, newAncestors)
                .set(SYS_DEPT.SORT_ORDER, dto.getSortOrder())
                .set(SYS_DEPT.LEADER, dto.getLeader())
                .set(SYS_DEPT.PHONE, dto.getPhone())
                .set(SYS_DEPT.EMAIL, dto.getEmail())
                .set(SYS_DEPT.STATUS, dto.getStatus() != null ? CommonStatus.fromCode(dto.getStatus()) : null)
                .set(SYS_DEPT.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_DEPT.ID.eq(dto.getId()))
                .update();

        // 如果父部门变更，需要更新所有子部门的祖级路径
        if (!parentId.equals(existing.getParentId())) {
            updateChildrenAncestors(dto.getId(), newAncestors);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (hasChildren(id)) {
            throw new BusinessException(400, "存在子部门，不能删除");
        }

        if (hasUsers(id)) {
            throw new BusinessException(400, "部门存在用户，不能删除");
        }

        removeById(id);
    }

    @Override
    public List<Long> getChildrenIds(Long deptId) {
        List<Long> ids = new ArrayList<>();
        ids.add(deptId);

        // 使用 ancestors 字段查询子部门（更高效）
        SysDept dept = getById(deptId);
        if (dept != null && dept.getAncestors() != null) {
            String ancestorPath = dept.getAncestors() + "," + deptId;
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .select(SYS_DEPT.ID)
                    .where(SYS_DEPT.ANCESTORS.like(ancestorPath + "%"))
                    .or(SYS_DEPT.ANCESTORS.eq(ancestorPath));
            List<SysDept> children = list(queryWrapper);
            ids.addAll(children.stream().map(SysDept::getId).collect(Collectors.toList()));
        } else {
            // 降级为使用 Map 优化的查询
            collectChildrenIdsOptimized(list(), deptId, ids);
        }

        return ids;
    }

    @Override
    public boolean existsByName(String name, Long parentId, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_DEPT.NAME.eq(name))
                .and(SYS_DEPT.PARENT_ID.eq(parentId != null ? parentId : 0L));

        if (excludeId != null) {
            queryWrapper.and(SYS_DEPT.ID.ne(excludeId));
        }

        return count(queryWrapper) > 0;
    }

    @Override
    public boolean hasChildren(Long deptId) {
        return count(QueryWrapper.create().where(SYS_DEPT.PARENT_ID.eq(deptId))) > 0;
    }

    @Override
    public boolean hasUsers(Long deptId) {
        return userMapper.selectCountByQuery(QueryWrapper.create().where(SYS_USER.DEPT_ID.eq(deptId))) > 0;
    }

    /**
     * 收集子部门 ID（优化版：使用 Map 分组，O(n) 时间复杂度）
     */
    private void collectChildrenIdsOptimized(List<SysDept> allDepts, Long rootId, List<Long> ids) {
        if (CollectionUtils.isEmpty(allDepts)) {
            return;
        }

        // 按 parentId 分组
        Map<Long, List<SysDept>> parentMap = allDepts.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() != null ? d.getParentId() : 0L));

        // 从根节点开始收集
        collectIdsByMap(parentMap, rootId, ids);
    }

    private void collectIdsByMap(Map<Long, List<SysDept>> parentMap, Long parentId, List<Long> ids) {
        List<SysDept> children = parentMap.get(parentId);
        if (children != null) {
            for (SysDept child : children) {
                ids.add(child.getId());
                collectIdsByMap(parentMap, child.getId(), ids);
            }
        }
    }

    /**
     * 构建部门树（优化版：O(n) 时间复杂度）
     */
    private List<DeptVO> buildTree(List<SysDept> depts, Long rootParentId) {
        if (CollectionUtils.isEmpty(depts)) {
            return Collections.emptyList();
        }

        // 批量转换为 VO
        List<DeptVO> allVos = BeanCopyUtils.copyList(depts, DeptVO::new);

        // 按 parentId 分组
        Map<Long, List<DeptVO>> parentMap = allVos.stream()
                .collect(Collectors.groupingBy(vo -> vo.getParentId() != null ? vo.getParentId() : 0L));

        // 设置每个节点的 children
        allVos.forEach(vo -> vo.setChildren(parentMap.get(vo.getId())));

        // 返回根节点列表
        return parentMap.getOrDefault(rootParentId, Collections.emptyList());
    }

    private DeptVO toDeptVO(SysDept dept) {
        return BeanCopyUtils.copy(dept, DeptVO::new);
    }

    /**
     * 计算祖级路径
     * <p>格式：父部门的祖级路径 + "," + 父部门ID</p>
     * <p>例如：父部门ID=101，其ancestors="0,100"，则新部门的ancestors="0,100,101"</p>
     *
     * @param parentId 父部门ID
     * @return 祖级路径
     */
    private String calculateAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        SysDept parent = getById(parentId);
        if (parent == null) {
            return "0";
        }
        String parentAncestors = parent.getAncestors();
        if (!StringUtils.hasLength(parentAncestors)) {
            parentAncestors = "0";
        }
        return parentAncestors + "," + parentId;
    }

    /**
     * 检查目标部门是否是指定部门的子部门
     *
     * @param targetId 目标部门ID
     * @param parentId 父部门ID
     * @return true=是子部门
     */
    private boolean isChildOf(Long targetId, Long parentId) {
        if (targetId == null || parentId == null) {
            return false;
        }
        SysDept target = getById(targetId);
        if (target == null) {
            return false;
        }
        // 检查目标部门的祖级路径是否包含父部门
        String ancestors = target.getAncestors();
        if (!StringUtils.hasLength(ancestors)) {
            return false;
        }
        return ancestors.contains(parentId.toString());
    }

    /**
     * 递归更新子部门的祖级路径
     *
     * @param parentId      父部门ID
     * @param parentAncestors 父部门的祖级路径
     */
    private void updateChildrenAncestors(Long parentId, String parentAncestors) {
        List<SysDept> children = list(QueryWrapper.create().where(SYS_DEPT.PARENT_ID.eq(parentId)));
        for (SysDept child : children) {
            String newAncestors = parentAncestors + "," + parentId;
            UpdateChain.of(SysDept.class)
                    .set(SYS_DEPT.ANCESTORS, newAncestors)
                    .set(SYS_DEPT.UPDATE_TIME, LocalDateTime.now())
                    .where(SYS_DEPT.ID.eq(child.getId()))
                    .update();
            // 递归更新子部门的子部门
            updateChildrenAncestors(child.getId(), newAncestors);
        }
    }

}