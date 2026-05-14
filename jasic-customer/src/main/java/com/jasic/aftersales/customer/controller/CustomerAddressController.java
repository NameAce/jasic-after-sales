package com.jasic.aftersales.customer.controller;

import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.customer.domain.dto.CustomerAddressCreateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerAddressUpdateDTO;
import com.jasic.aftersales.customer.domain.vo.CustomerAddressVO;
import com.jasic.aftersales.customer.service.ICustomerAddressService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * C端客户地址控制器
 *
 * @author Codex
 * @date 2026/04/08
 */
@Api(tags = "C端客户地址簿")
@RestController
@RequestMapping("/customer/address")
public class CustomerAddressController {

    @Resource
    private ICustomerAddressService customerAddressService;

    /**
     * 查询当前客户地址列表
     *
     * @return 地址列表
     */
    @ApiOperation(value = "查询当前客户地址列表")
    @GetMapping("/list")
    public Result<List<CustomerAddressVO>> list() {
        return Result.ok(customerAddressService.list());
    }

    /**
     * 查询地址详情
     *
     * @param addressId 地址ID
     * @return 地址详情
     */
    @ApiOperation(value = "查询地址详情")
    @GetMapping("/{addressId}")
    public Result<CustomerAddressVO> getById(@PathVariable Long addressId) {
        return Result.ok(customerAddressService.getById(addressId));
    }

    /**
     * 新增地址
     *
     * @param dto 新增参数
     * @return 地址ID
     */
    @ApiOperation(value = "新增地址")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody CustomerAddressCreateDTO dto) {
        return Result.ok(customerAddressService.create(dto));
    }

    /**
     * 修改地址
     *
     * @param dto 修改参数
     * @return 操作结果
     */
    @ApiOperation(value = "修改地址")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody CustomerAddressUpdateDTO dto) {
        // 调用update方法，复用统一能力并保证业务规则一致。
        customerAddressService.update(dto);
        return Result.ok();
    }

    /**
     * 删除地址
     *
     * @param addressId 地址ID
     * @return 操作结果
     */
    @ApiOperation(value = "删除地址")
    @DeleteMapping("/{addressId}")
    public Result<Void> delete(@PathVariable Long addressId) {
        // 调用delete方法，复用统一能力并保证业务规则一致。
        customerAddressService.delete(addressId);
        return Result.ok();
    }

    /**
     * 设为默认地址
     *
     * @param addressId 地址ID
     * @return 操作结果
     */
    @ApiOperation(value = "设为默认地址")
    @PutMapping("/{addressId}/default")
    public Result<Void> setDefault(@PathVariable Long addressId) {
        // 调用setDefault方法，复用统一能力并保证业务规则一致。
        customerAddressService.setDefault(addressId);
        return Result.ok();
    }
}
