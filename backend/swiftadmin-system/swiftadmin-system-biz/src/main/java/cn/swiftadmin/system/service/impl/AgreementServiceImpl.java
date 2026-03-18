package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.system.api.dto.AgreementDTO;
import cn.swiftadmin.system.api.entity.SysAgreement;
import cn.swiftadmin.system.api.entity.SysUserAgreement;
import cn.swiftadmin.system.api.service.AgreementService;
import cn.swiftadmin.system.api.vo.AgreementVO;
import cn.swiftadmin.system.mapper.SysAgreementMapper;
import cn.swiftadmin.system.mapper.SysUserAgreementMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static cn.swiftadmin.system.api.entity.table.SysAgreementTableDef.SYS_AGREEMENT;
import static cn.swiftadmin.system.api.entity.table.SysUserAgreementTableDef.SYS_USER_AGREEMENT;

/**
 * 用户协议服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementServiceImpl extends ServiceImpl<SysAgreementMapper, SysAgreement> implements AgreementService {

    private final SysUserAgreementMapper userAgreementMapper;

    @Override
    public AgreementVO getUserAgreement() {
        return getLatestByType("agreement");
    }

    @Override
    public AgreementVO getPrivacyPolicy() {
        return getLatestByType("privacy");
    }

    @Override
    public AgreementVO getLatestByType(String type) {
        SysAgreement agreement = getOne(QueryWrapper.create()
                .where(SYS_AGREEMENT.TYPE.eq(type))
                .and(SYS_AGREEMENT.STATUS.eq(1))
                .orderBy(SYS_AGREEMENT.VERSION.desc())
                .limit(1));
        return agreement != null ? BeanCopyUtils.copy(agreement, AgreementVO::new) : null;
    }

    @Override
    public List<AgreementVO> listVersions(String type) {
        return BeanCopyUtils.copyList(
                list(QueryWrapper.create()
                        .where(SYS_AGREEMENT.TYPE.eq(type))
                        .orderBy(SYS_AGREEMENT.VERSION.desc())),
                AgreementVO::new);
    }

    @Override
    public AgreementVO getDetail(Long id) {
        SysAgreement agreement = getById(id);
        return agreement != null ? BeanCopyUtils.copy(agreement, AgreementVO::new) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AgreementDTO dto) {
        // 检查版本号是否已存在
        if (existsByVersion(dto.getType(), dto.getVersion(), null)) {
            throw new BusinessException(400, "该版本号已存在");
        }

        SysAgreement agreement = SysAgreement.builder()
                .type(dto.getType())
                .version(dto.getVersion())
                .title(dto.getTitle())
                .content(dto.getContent())
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .effectiveDate(dto.getEffectiveDate())
                .build();

        save(agreement);
        log.info("创建协议版本: type={}, version={}", dto.getType(), dto.getVersion());
        return agreement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AgreementDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "协议 ID 不能为空");
        }

        SysAgreement existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(404, "协议不存在");
        }

        // 检查版本号是否已存在
        if (!existing.getVersion().equals(dto.getVersion())
                && existsByVersion(dto.getType(), dto.getVersion(), dto.getId())) {
            throw new BusinessException(400, "该版本号已存在");
        }

        UpdateChain.of(SysAgreement.class)
                .set(SYS_AGREEMENT.VERSION, dto.getVersion())
                .set(SYS_AGREEMENT.TITLE, dto.getTitle())
                .set(SYS_AGREEMENT.CONTENT, dto.getContent())
                .set(SYS_AGREEMENT.STATUS, dto.getStatus())
                .set(SYS_AGREEMENT.EFFECTIVE_DATE, dto.getEffectiveDate())
                .set(SYS_AGREEMENT.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_AGREEMENT.ID.eq(dto.getId()))
                .update();

        log.info("更新协议版本: id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            mapper.deleteBatchByIds(ids);
            log.info("删除协议版本: ids={}", ids);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        UpdateChain.of(SysAgreement.class)
                .set(SYS_AGREEMENT.STATUS, status)
                .set(SYS_AGREEMENT.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_AGREEMENT.ID.eq(id))
                .update();
        log.info("更新协议状态: id={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordAgreement(Long userId, String agreementType, String version) {
        // 检查是否已同意
        SysUserAgreement existing = userAgreementMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(SYS_USER_AGREEMENT.USER_ID.eq(userId))
                        .and(SYS_USER_AGREEMENT.AGREEMENT_TYPE.eq(agreementType)));

        if (existing != null) {
            // 更新同意记录
            UpdateChain.of(SysUserAgreement.class)
                    .set(SYS_USER_AGREEMENT.AGREED_VERSION, version)
                    .set(SYS_USER_AGREEMENT.AGREED_TIME, LocalDateTime.now())
                    .where(SYS_USER_AGREEMENT.ID.eq(existing.getId()))
                    .update();
        } else {
            // 新增同意记录
            SysUserAgreement record = SysUserAgreement.builder()
                    .userId(userId)
                    .agreementType(agreementType)
                    .agreedVersion(version)
                    .agreedTime(LocalDateTime.now())
                    .build();
            userAgreementMapper.insert(record);
        }
        log.info("记录用户同意协议: userId={}, type={}, version={}", userId, agreementType, version);
    }

    @Override
    public boolean hasAgreed(Long userId, String agreementType) {
        return userAgreementMapper.selectCountByQuery(
                QueryWrapper.create()
                        .where(SYS_USER_AGREEMENT.USER_ID.eq(userId))
                        .and(SYS_USER_AGREEMENT.AGREEMENT_TYPE.eq(agreementType))) > 0;
    }

    /**
     * 检查版本号是否存在
     */
    private boolean existsByVersion(String type, String version, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_AGREEMENT.TYPE.eq(type))
                .and(SYS_AGREEMENT.VERSION.eq(version));

        if (excludeId != null) {
            queryWrapper.and(SYS_AGREEMENT.ID.ne(excludeId));
        }

        return mapper.selectCountByQuery(queryWrapper) > 0;
    }

}