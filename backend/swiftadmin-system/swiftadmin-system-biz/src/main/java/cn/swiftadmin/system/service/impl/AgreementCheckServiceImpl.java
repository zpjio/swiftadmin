package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.service.AgreementCheckService;
import cn.swiftadmin.system.api.service.AgreementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 协议检查服务实现
 * <p>委托给 AgreementService 实现</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementCheckServiceImpl implements AgreementCheckService {

    private final AgreementService agreementService;

    @Override
    public boolean hasAgreed(Long userId, String agreementType) {
        return agreementService.hasAgreed(userId, agreementType);
    }

    @Override
    public void recordAgreement(Long userId, String agreementType, String version) {
        agreementService.recordAgreement(userId, agreementType, version);
    }
}