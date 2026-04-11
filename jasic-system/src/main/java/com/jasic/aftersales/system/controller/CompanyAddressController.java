package com.jasic.aftersales.system.controller;

import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.CompanyAddressCreateDTO;
import com.jasic.aftersales.system.domain.dto.CompanyAddressUpdateDTO;
import com.jasic.aftersales.system.domain.vo.CompanyAddressVO;
import com.jasic.aftersales.system.service.ICompanyAddressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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

/**
 * 公司地址簿控制器。
 *
 * @author Codex
 * @date 2026/04/11
 */
@Api(tags = "公司地址簿")
@RestController
@RequestMapping("/system/company-address")
public class CompanyAddressController {

    @Resource
    private ICompanyAddressService companyAddressService;

    /**
     * 查询当前公司地址列表。
     *
     * @return 地址列表
     */
    @ApiOperation(value = "查询当前公司地址列表")
    @GetMapping("/list")
    public Result<List<CompanyAddressVO>> list() {
        return Result.ok(companyAddressService.list());
    }

    /**
     * 查询地址详情。
     *
     * @param addressId 地址ID
     * @return 地址详情
     */
    @ApiOperation(value = "查询公司地址详情")
    @GetMapping("/{addressId}")
    public Result<CompanyAddressVO> getById(@PathVariable Long addressId) {
        return Result.ok(companyAddressService.getById(addressId));
    }

    /**
     * 新增地址。
     *
     * @param dto 新增参数
     * @return 地址ID
     */
    @ApiOperation(value = "新增公司地址")
    @OperLog(title = "公司地址簿", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> create(@Validated @RequestBody CompanyAddressCreateDTO dto) {
        return Result.ok(companyAddressService.create(dto));
    }

    /**
     * 修改地址。
     *
     * @param dto 修改参数
     * @return 操作结果
     */
    @ApiOperation(value = "修改公司地址")
    @OperLog(title = "公司地址簿", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody CompanyAddressUpdateDTO dto) {
        companyAddressService.update(dto);
        return Result.ok();
    }

    /**
     * 删除地址。
     *
     * @param addressId 地址ID
     * @return 操作结果
     */
    @ApiOperation(value = "删除公司地址")
    @OperLog(title = "公司地址簿", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{addressId}")
    public Result<Void> delete(@PathVariable Long addressId) {
        companyAddressService.delete(addressId);
        return Result.ok();
    }

    /**
     * 设为默认地址。
     *
     * @param addressId 地址ID
     * @return 操作结果
     */
    @ApiOperation(value = "设为默认公司地址")
    @OperLog(title = "公司地址簿", operType = OperTypeEnum.UPDATE)
    @PutMapping("/{addressId}/default")
    public Result<Void> setDefault(@PathVariable Long addressId) {
        companyAddressService.setDefault(addressId);
        return Result.ok();
    }
}
