package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.CompanyAddressCreateDTO;
import com.jasic.aftersales.system.domain.dto.CompanyAddressUpdateDTO;
import com.jasic.aftersales.system.domain.vo.CompanyAddressVO;
import com.jasic.aftersales.system.service.CompanyDataAccessService;
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
import org.springframework.web.bind.annotation.RequestParam;
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

    @Resource
    private CompanyDataAccessService companyDataAccessService;

    /**
     * 查询当前公司地址列表。
     *
     * @return 地址列表
     */
    @ApiOperation(value = "查询当前公司地址列表")
    @GetMapping("/list")
    @SaCheckPermission("companyAddress:list")
    public Result<List<CompanyAddressVO>> list(@RequestParam(required = false) Long targetCompanyId) {
        // 调用公司上下文执行器，确保在目标公司数据范围内查询地址列表。
        return Result.ok(companyDataAccessService.runWithCurrentCompanyTarget(
                targetCompanyId,
                () -> companyAddressService.list()
        ));
    }

    /**
     * 查询地址详情。
     *
     * @param addressId 地址ID
     * @return 地址详情
     */
    @ApiOperation(value = "查询公司地址详情")
    @GetMapping("/{addressId}")
    @SaCheckPermission("companyAddress:list")
    public Result<CompanyAddressVO> getById(@PathVariable Long addressId,
                                            @RequestParam(required = false) Long targetCompanyId) {
        // 调用公司上下文执行器，确保在目标公司数据范围内查询地址详情。
        return Result.ok(companyDataAccessService.runWithCurrentCompanyTarget(
                targetCompanyId,
                () -> companyAddressService.getById(addressId)
        ));
    }

    /**
     * 新增地址。
     *
     * @param dto 新增参数
     * @return 地址ID
     */
    @ApiOperation(value = "新增公司地址")
    @SaCheckPermission("companyAddress:manage")
    @OperLog(title = "公司地址簿", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> create(@Validated @RequestBody CompanyAddressCreateDTO dto) {
        // 调用公司上下文执行器，确保在目标公司数据范围内创建地址数据。
        return Result.ok(companyDataAccessService.runWithCurrentCompanyTarget(
                dto.getTargetCompanyId(),
                () -> companyAddressService.create(dto)
        ));
    }

    /**
     * 修改地址。
     *
     * @param dto 修改参数
     * @return 操作结果
     */
    @ApiOperation(value = "修改公司地址")
    @SaCheckPermission("companyAddress:manage")
    @OperLog(title = "公司地址簿", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody CompanyAddressUpdateDTO dto) {
        companyDataAccessService.runWithCurrentCompanyTarget(
                dto.getTargetCompanyId(),
                () -> companyAddressService.update(dto)
        );
        return Result.ok();
    }

    /**
     * 删除地址。
     *
     * @param addressId 地址ID
     * @return 操作结果
     */
    @ApiOperation(value = "删除公司地址")
    @SaCheckPermission("companyAddress:manage")
    @OperLog(title = "公司地址簿", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{addressId}")
    public Result<Void> delete(@PathVariable Long addressId,
                               @RequestParam(required = false) Long targetCompanyId) {
        companyDataAccessService.runWithCurrentCompanyTarget(
                targetCompanyId,
                () -> companyAddressService.delete(addressId)
        );
        return Result.ok();
    }

    /**
     * 设为默认地址。
     *
     * @param addressId 地址ID
     * @return 操作结果
     */
    @ApiOperation(value = "设为默认公司地址")
    @SaCheckPermission("companyAddress:manage")
    @OperLog(title = "公司地址簿", operType = OperTypeEnum.UPDATE)
    @PutMapping("/{addressId}/default")
    public Result<Void> setDefault(@PathVariable Long addressId,
                                   @RequestParam(required = false) Long targetCompanyId) {
        companyDataAccessService.runWithCurrentCompanyTarget(
                targetCompanyId,
                () -> companyAddressService.setDefault(addressId)
        );
        return Result.ok();
    }
}
