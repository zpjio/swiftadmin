package cn.swiftadmin.common.core.service;

/**
 * 协议检查服务接口
 * <p>定义协议检查的最小接口，供 AOP 切面使用</p>
 * <p>实现在 system-biz 模块，委托给 AgreementService</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface AgreementCheckService {

    /**
     * 检查用户是否已同意协议
     *
     * @param userId 用户 ID
     * @param agreementType 协议类型：agreement/privacy
     * @return true=已同意
     */
    boolean hasAgreed(Long userId, String agreementType);

    /**
     * 记录用户同意协议
     *
     * @param userId 用户 ID
     * @param agreementType 协议类型：agreement/privacy
     * @param version 协议版本
     */
    void recordAgreement(Long userId, String agreementType, String version);
}