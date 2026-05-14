package com.jasic.aftersales.customer.controller;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
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
import com.jasic.aftersales.customer.service.ICustomerWorkOrderService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * C端工单控制器
 *
 * @author Codex
 * @date 2026/03/26
 */
@Api(tags = "C端工单")
@RestController
@RequestMapping("/customer/work-order")
public class CustomerWorkOrderController {

    @Resource
    private ICustomerWorkOrderService customerWorkOrderService;

    /**
     * 创建我的工单
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    @ApiOperation(value = "创建我的工单")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody CustomerWorkOrderCreateDTO dto) {
        return Result.ok(customerWorkOrderService.create(dto));
    }

    /**
     * 查询可选服务网点
     *
     * @return 服务网点选项
     */
    @ApiOperation(value = "查询可选服务网点")
    @GetMapping("/service-company-options")
    public Result<List<CustomerServiceCompanyOptionVO>> listServiceCompanyOptions() {
        return Result.ok(customerWorkOrderService.listServiceCompanyOptions());
    }

    /**
     * 按定位查询附近服务网点
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param limit     返回条数
     * @return 服务网点选项
     */
    @ApiOperation(value = "按定位查询附近服务网点")
    @GetMapping("/nearby-service-company-options")
    public Result<List<CustomerNearbyServiceCompanyVO>> listNearbyServiceCompanyOptions(
            @RequestParam BigDecimal longitude,
            @RequestParam BigDecimal latitude,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(customerWorkOrderService.listNearbyServiceCompanyOptions(longitude, latitude, limit));
    }

    /**
     * 查询条码档案信息
     *
     * @param barcode 机器条码
     * @return 条码信息
     */
    @ApiOperation(value = "查询条码档案信息")
    @GetMapping("/barcode-info")
    public Result<CustomerBarcodeInfoVO> getBarcodeInfo(@RequestParam String barcode) {
        return Result.ok(customerWorkOrderService.getBarcodeInfo(barcode));
    }

    /**
     * 分页查询我的工单
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询我的工单")
    @GetMapping("/list")
    public Result<PageResult<CustomerWorkOrderListVO>> list(CustomerWorkOrderQuery query) {
        return Result.ok(customerWorkOrderService.listPage(query));
    }

    /**
     * 查询最近一条工单摘要
     *
     * @return 最近工单摘要
     */
    @ApiOperation(value = "查询最近一条工单摘要")
    @GetMapping("/latest-summary")
    public Result<CustomerWorkOrderLatestSummaryVO> getLatestSummary() {
        return Result.ok(customerWorkOrderService.getLatestSummary());
    }

    /**
     * 查询我的工单状态计数
     *
     * @return 状态计数
     */
    @ApiOperation(value = "查询我的工单状态计数")
    @GetMapping("/status-count")
    public Result<CustomerWorkOrderStatusCountVO> getStatusCount() {
        return Result.ok(customerWorkOrderService.getStatusCount());
    }

    /**
     * 查询工单详情
     *
     * @param workOrderId 工单ID
     * @return 工单详情
     */
    @ApiOperation(value = "查询工单详情")
    @GetMapping("/{workOrderId}")
    public Result<CustomerWorkOrderDetailVO> getById(@PathVariable Long workOrderId) {
        return Result.ok(customerWorkOrderService.getById(workOrderId));
    }

    /**
     * 更新寄修信息
     *
     * @param dto 寄修信息参数
     * @return 操作结果
     */
    @ApiOperation(value = "更新寄修信息")
    @PutMapping("/send-info")
    public Result<Void> updateSendInfo(@Validated @RequestBody CustomerWorkOrderSendInfoDTO dto) {
        // 调用updateSendInfo方法，复用统一能力并保证业务规则一致。
        customerWorkOrderService.updateSendInfo(dto);
        return Result.ok();
    }

    /**
     * 上传寄件凭证
     *
     * @param dto 寄件凭证参数
     * @return 操作结果
     */
    @ApiOperation(value = "上传寄件凭证")
    @PutMapping("/sender-voucher")
    public Result<Void> updateSenderVoucher(@Validated @RequestBody CustomerWorkOrderSenderVoucherDTO dto) {
        // 调用updateSenderVoucher方法，复用统一能力并保证业务规则一致。
        customerWorkOrderService.updateSenderVoucher(dto);
        return Result.ok();
    }

    /**
     * 提交工单评价
     *
     * @param dto 评价参数
     * @return 操作结果
     */
    @ApiOperation(value = "提交工单评价")
    @PostMapping("/evaluate")
    public Result<Void> evaluate(@Validated @RequestBody CustomerWorkOrderEvaluateDTO dto) {
        // 调用evaluate方法，复用统一能力并保证业务规则一致。
        customerWorkOrderService.evaluate(dto);
        return Result.ok();
    }
}


