package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.WorkOrderAssignDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderCloseDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderProxyCreateDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderQuoteDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderRepairDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderSendExpressDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTechAcceptDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderReviewDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTransferDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderUpstreamCreateDTO;
import com.jasic.aftersales.system.domain.query.WorkOrderQuery;
import com.jasic.aftersales.system.domain.vo.WorkOrderCreateBarcodeInfoVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderDetailVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderListVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderStatusCountVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderUserOptionVO;

import java.util.List;

/**
 * 工单查询 Service 接口
 *
 * @author Codex
 * @date 2026/03/26
 */
public interface IWorkOrderService {

    /**
     * 分页查询工单列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<WorkOrderListVO> listPage(WorkOrderQuery query);

    /**
     * 按状态统计工单数量
     *
     * @param query 查询参数
     * @return 状态统计结果
     */
    List<WorkOrderStatusCountVO> countByStatus(WorkOrderQuery query);

    /**
     * 查询工单详情
     *
     * @param workOrderId 工单ID
     * @return 工单详情
     */
    WorkOrderDetailVO getById(Long workOrderId);

    /**
     * 查询代客户填写条码信息
     *
     * @param barcode 机器条码
     * @return 条码信息
     */
    WorkOrderCreateBarcodeInfoVO getProxyCreateBarcodeInfo(String barcode);

    /**
     * 查询二级报修一级条码信息
     *
     * @param barcode 机器条码
     * @return 条码信息
     */
    WorkOrderCreateBarcodeInfoVO getUpstreamFirstCreateBarcodeInfo(String barcode);

    /**
     * 查询二级无码报修一级可选目标公司。
     *
     * @return 公司选项
     */
    List<SysCompanySimpleVO> listUpstreamFirstCreateTargetOptions();

    /**
     * 查询一级报修佳士条码信息
     *
     * @param barcode 机器条码
     * @return 条码信息
     */
    WorkOrderCreateBarcodeInfoVO getUpstreamHqCreateBarcodeInfo(String barcode, Long targetCompanyId);

    /**
     * 代客户填写创建工单
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    Long createProxy(WorkOrderProxyCreateDTO dto);

    /**
     * 二级报修一级创建工单
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    Long createUpstreamFirst(WorkOrderUpstreamCreateDTO dto);

    /**
     * 一级报修佳士创建工单
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    Long createUpstreamHq(WorkOrderUpstreamCreateDTO dto);

    /**
     * 派单
     *
     * @param dto 派单参数
     */
    void assign(WorkOrderAssignDTO dto);

    /**
     * 维修员接单
     *
     * @param dto 接单参数
     */
    void techAccept(WorkOrderTechAcceptDTO dto);

    /**
     * 转单
     *
     * @param dto 转单参数
     */
    void transfer(WorkOrderTransferDTO dto);

    /**
     * 保存报价
     *
     * @param dto 报价参数
     */
    void saveQuote(WorkOrderQuoteDTO dto);

    /**
     * 保存维修登记
     *
     * @param dto 维修参数
     */
    void saveRepair(WorkOrderRepairDTO dto);

    /**
     * 保存复检记录
     *
     * @param dto 复检参数
     */
    void saveReview(WorkOrderReviewDTO dto);

    /**
     * 上传寄件快递单号
     *
     * @param dto 寄件快递单号参数
     */
    void updateSendExpress(WorkOrderSendExpressDTO dto);

    /**
     * 关闭工单
     *
     * @param dto 关闭参数
     */
    void close(WorkOrderCloseDTO dto);

    /**
     * 查询建单可选归属总部
     *
     * @return 公司选项
     */
    List<SysCompanySimpleVO> listCreateHqOptions();

    /**
     * 查询工单可派单人员
     *
     * @param workOrderId 工单ID
     * @return 人员选项
     */
    List<WorkOrderUserOptionVO> listAssignUserOptions(Long workOrderId);

    /**
     * 查询工单可转单目标
     *
     * @param workOrderId 工单ID
     * @return 公司选项
     */
    List<SysCompanySimpleVO> listTransferTargetOptions(Long workOrderId);

    /**
     * 查询维修登记可选故障与维修说明
     *
     * @param workOrderId 工单ID
     * @return 故障与维修说明选项
     */
    List<WorkOrderRepairFaultOptionVO> listRepairFaultOptions(Long workOrderId);
}
