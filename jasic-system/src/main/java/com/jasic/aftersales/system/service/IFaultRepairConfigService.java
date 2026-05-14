package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.FaultRepairConfigDTO;
import com.jasic.aftersales.system.domain.query.FaultRepairConfigQuery;
import com.jasic.aftersales.system.domain.vo.FaultRepairConfigVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;

import java.util.List;

/**
 * 故障与维修配置 Service 接口
 *
 * @author Codex
 * @date 2026/04/01
 */
public interface IFaultRepairConfigService {

    /**
     * 分页查询配置
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<FaultRepairConfigVO> listPage(FaultRepairConfigQuery query);

    /**
     * 查询配置详情
     *
     * @param id 配置ID
     * @return 配置详情
     */
    FaultRepairConfigVO getById(Long id, Long ownerHqId);

    /**
     * 新增配置
     *
     * @param dto 配置参数
     * @return 配置ID
     */
    Long save(FaultRepairConfigDTO dto);

    /**
     * 修改配置
     *
     * @param dto 配置参数
     */
    void update(FaultRepairConfigDTO dto);

    /**
     * 查询归属总部选项
     *
     * @return 公司选项
     */
    List<SysCompanySimpleVO> listCompanyOptions();

    /**
     * 按产品查询维修故障选项
     *
     * @param resolvedHqCompanyId 后端解析出的归属总部ID
     * @param productCode  物料编码
     * @param productModel 产品型号
     * @return 故障与维修说明选项
     */
    List<WorkOrderRepairFaultOptionVO> listRepairFaultOptionsForResolvedHq(Long resolvedHqCompanyId, String productCode,
                                                                            String productModel);

    /**
     * 按已绑定配置ID查询维修故障选项。
     *
     * @param configId 配置ID
     * @return 故障与维修说明选项
     */
    List<WorkOrderRepairFaultOptionVO> listRepairFaultOptionsByConfigId(Long configId);

    /**
     * 按总部和产品信息匹配当前应绑定的启用配置ID。
     *
     * @param resolvedHqCompanyId 后端解析出的归属总部ID
     * @param productCode  物料编码
     * @param productModel 产品型号
     * @return 配置ID
     */
    Long findEnabledConfigIdForResolvedHq(Long resolvedHqCompanyId, String productCode, String productModel);

    /**
     * 查询指定总部下启用状态的产品型号选项。
     *
     * @param resolvedHqCompanyId 后端解析出的归属总部ID
     * @param keyword 产品型号关键字
     * @return 产品型号选项
     */
    List<String> listEnabledProductModelsForResolvedHq(Long resolvedHqCompanyId, String keyword);
}
