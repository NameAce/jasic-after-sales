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
    FaultRepairConfigVO getById(Long id);

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
     * 删除配置
     *
     * @param id 配置ID
     */
    void remove(Long id);

    /**
     * 查询归属总部选项
     *
     * @return 公司选项
     */
    List<SysCompanySimpleVO> listCompanyOptions();

    /**
     * 按产品查询维修故障选项
     *
     * @param companyId    归属总部ID
     * @param productCode  物料编码
     * @param productModel 产品型号
     * @return 故障与维修说明选项
     */
    List<WorkOrderRepairFaultOptionVO> listRepairFaultOptions(Long companyId, String productCode, String productModel);
}
