package com.jasic.aftersales.customer.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderEvaluateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderSendInfoDTO;
import com.jasic.aftersales.customer.domain.query.CustomerWorkOrderQuery;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderDetailVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderListVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderStatusCountVO;

/**
 * C端工单 Service 接口
 *
 * @author Codex
 * @date 2026/03/26
 */
public interface ICustomerWorkOrderService {

    /**
     * 分页查询我的工单
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<CustomerWorkOrderListVO> listPage(CustomerWorkOrderQuery query);

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
     * 提交工单评价
     *
     * @param dto 评价参数
     */
    void evaluate(CustomerWorkOrderEvaluateDTO dto);
}
