package com.jasic.aftersales.customer.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderCreateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderEvaluateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderSendInfoDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderSenderVoucherDTO;
import com.jasic.aftersales.customer.domain.query.CustomerWorkOrderQuery;
import com.jasic.aftersales.customer.domain.vo.CustomerBarcodeInfoVO;
import com.jasic.aftersales.customer.domain.vo.CustomerNearbyServiceCompanyVO;
import com.jasic.aftersales.customer.domain.vo.CustomerServiceCompanyOptionVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderDetailVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderLatestSummaryVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderListVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderStatusCountVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * C端工单 Service 接口
 *
 * @author Codex
 * @date 2026/03/26
 */
public interface ICustomerWorkOrderService {

    /**
     * 创建我的工单
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    Long create(CustomerWorkOrderCreateDTO dto);

    /**
     * 查询 C 端可选服务网点列表
     *
     * @return 服务网点选项
     */
    List<CustomerServiceCompanyOptionVO> listServiceCompanyOptions();

    /**
     * 按定位查询附近服务网点
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param limit     返回条数
     * @return 服务网点选项
     */
    List<CustomerNearbyServiceCompanyVO> listNearbyServiceCompanyOptions(BigDecimal longitude, BigDecimal latitude,
                                                                         Integer limit);

    /**
     * 查询条码档案信息
     *
     * @param barcode 机器条码
     * @return 条码信息
     */
    CustomerBarcodeInfoVO getBarcodeInfo(String barcode);

    /**
     * 分页查询我的工单
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<CustomerWorkOrderListVO> listPage(CustomerWorkOrderQuery query);

    /**
     * 查询最近一条工单摘要
     *
     * @return 最近工单摘要，若不存在则返回 null
     */
    CustomerWorkOrderLatestSummaryVO getLatestSummary();

    /**
     * 查询我的工单状态计数
     *
     * @return 状态计数
     */
    CustomerWorkOrderStatusCountVO getStatusCount();

    /**
     * 查询工单详情
     *
     * @param workOrderId 工单ID
     * @return 工单详情
     */
    CustomerWorkOrderDetailVO getById(Long workOrderId);

    /**
     * 更新工单寄修信息
     *
     * @param dto 寄修信息参数
     */
    void updateSendInfo(CustomerWorkOrderSendInfoDTO dto);

    /**
     * 上传工单寄件凭证
     *
     * @param dto 寄件凭证参数
     */
    void updateSenderVoucher(CustomerWorkOrderSenderVoucherDTO dto);

    /**
     * 提交工单评价
     *
     * @param dto 评价参数
     */
    void evaluate(CustomerWorkOrderEvaluateDTO dto);
}


