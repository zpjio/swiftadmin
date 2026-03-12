package cn.swiftadmin.system.api.service;

import cn.swiftadmin.system.api.dto.AgreementDTO;
import cn.swiftadmin.system.api.entity.SysAgreement;
import cn.swiftadmin.system.api.vo.AgreementVO;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 用户协议服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface AgreementService extends IService<SysAgreement> {

    /**
     * 获取用户协议（最新版本）
     *
     * @return 用户协议
     */
    AgreementVO getUserAgreement();

    /**
     * 获取隐私政策（最新版本）
     *
     * @return 隐私政策
     */
    AgreementVO getPrivacyPolicy();

    /**
     * 根据类型获取最新协议
     *
     * @param type 协议类型
     * @return 协议信息
     */
    AgreementVO getLatestByType(String type);

    /**
     * 获取协议历史版本列表
     *
     * @param type 协议类型
     * @return 版本列表
     */
    List<AgreementVO> listVersions(String type);

    /**
     * 获取协议详情
     *
     * @param id 协议ID
     * @return 协议详情
     */
    AgreementVO getDetail(Long id);

    /**
     * 创建协议版本
     *
     * @param dto 协议信息
     * @return 协议ID
     */
    Long create(AgreementDTO dto);

    /**
     * 更新协议
     *
     * @param dto 协议信息
     */
    void update(AgreementDTO dto);

    /**
     * 删除协议
     *
     * @param ids 协议ID列表
     */
    void delete(List<Long> ids);

    /**
     * 启用/停用协议
     *
     * @param id 协议ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 记录用户同意协议
     *
     * @param userId 用户ID
     * @param agreementType 协议类型
     * @param version 版本号
     */
    void recordAgreement(Long userId, String agreementType, String version);

    /**
     * 检查用户是否已同意协议
     *
     * @param userId 用户ID
     * @param agreementType 协议类型
     * @return true=已同意
     */
    boolean hasAgreed(Long userId, String agreementType);

}