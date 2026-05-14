package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.CrmFirstSecondRelationImportDTO;
import com.jasic.aftersales.system.domain.dto.CrmHqFirstContractImportDTO;
import com.jasic.aftersales.system.domain.dto.FirstSecondRelationDTO;
import com.jasic.aftersales.system.domain.dto.HqFirstContractDTO;
import com.jasic.aftersales.system.domain.query.CrmFirstSecondRelationImportQuery;
import com.jasic.aftersales.system.domain.query.CrmHqFirstContractImportQuery;
import com.jasic.aftersales.system.domain.vo.CrmFirstSecondRelationImportResultVO;
import com.jasic.aftersales.system.domain.vo.CrmFirstSecondRelationImportVO;
import com.jasic.aftersales.system.domain.vo.CrmHqFirstContractImportResultVO;
import com.jasic.aftersales.system.domain.vo.CrmHqFirstContractImportVO;
import com.jasic.aftersales.system.domain.vo.FirstSecondRelationVO;
import com.jasic.aftersales.system.domain.vo.HqFirstContractVO;
import com.jasic.aftersales.system.domain.query.FirstSecondRelationQuery;
import com.jasic.aftersales.system.domain.query.HqFirstContractQuery;
import com.jasic.aftersales.system.service.ISysContractService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 签约管理控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Api(tags = "签约管理")
@RestController
@RequestMapping("/org/contract")
public class SysContractController extends BaseController {

    @Resource
    private ISysContractService contractService;

    /**
     * 总部-一级签约分页列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "总部-一级签约分页列表")
    @SaCheckPermission("org:contract:list")
    @GetMapping("/hq-first/list")
    public Result<PageResult<HqFirstContractVO>> listHqFirstPage(HqFirstContractQuery query) {
        PageResult<HqFirstContractVO> page = contractService.listHqFirstPage(query);
        return Result.ok(page);
    }

    /**
     * CRM 签约导入分页列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "CRM签约导入分页列表")
    @SaCheckPermission("org:contract:add")
    @GetMapping("/hq-first/crm-import/list")
    public Result<PageResult<CrmHqFirstContractImportVO>> listCrmHqFirstImportPage(CrmHqFirstContractImportQuery query) {
        PageResult<CrmHqFirstContractImportVO> page = contractService.listCrmHqFirstImportPage(query);
        return Result.ok(page);
    }

    /**
     * 新增总部-一级签约
     *
     * @param dto 签约参数
     * @return 主键ID
     */
    @ApiOperation(value = "新增总部-一级签约")
    @SaCheckPermission("org:contract:add")
    @OperLog(title = "签约管理", operType = OperTypeEnum.INSERT)
    @PostMapping("/hq-first")
    public Result<Long> saveHqFirst(@Validated @RequestBody HqFirstContractDTO dto) {
        Long id = contractService.saveHqFirst(dto);
        return Result.ok(id);
    }

    /**
     * 修改总部-一级签约
     *
     * @param dto 签约参数
     * @return 操作结果
     */
    @ApiOperation(value = "修改总部-一级签约")
    @SaCheckPermission("org:contract:update")
    @OperLog(title = "签约管理", operType = OperTypeEnum.UPDATE)
    @PutMapping("/hq-first")
    public Result<Void> updateHqFirst(@Validated @RequestBody HqFirstContractDTO dto) {
        contractService.updateHqFirst(dto);
        return Result.ok();
    }

    /**
     * 删除总部-一级签约
     *
     * @param id 主键ID
     * @return 操作结果
     */
    @ApiOperation(value = "删除总部-一级签约")
    @SaCheckPermission("org:contract:remove")
    @OperLog(title = "签约管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/hq-first/{id}")
    public Result<Void> removeHqFirst(@PathVariable Long id,
                                      @RequestParam(required = false) Long targetCompanyId) {
        contractService.removeHqFirst(id, targetCompanyId);
        return Result.ok();
    }

    /**
     * 从 CRM 快照导入总部-一级签约
     *
     * @param dto 导入参数
     * @return 导入结果
     */
    @ApiOperation(value = "从CRM快照导入总部-一级签约")
    @SaCheckPermission("org:contract:add")
    @OperLog(title = "签约管理", operType = OperTypeEnum.INSERT)
    @PostMapping("/hq-first/crm-import")
    public Result<CrmHqFirstContractImportResultVO> importHqFirstFromCrm(@Validated @RequestBody CrmHqFirstContractImportDTO dto) {
        CrmHqFirstContractImportResultVO result = contractService.importHqFirstFromCrm(dto);
        return Result.ok(result);
    }

    /**
     * 一级-二级从属分页列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "一级-二级从属分页列表")
    @SaCheckPermission("org:contract:list")
    @GetMapping("/first-second/list")
    public Result<PageResult<FirstSecondRelationVO>> listFirstSecondPage(FirstSecondRelationQuery query) {
        PageResult<FirstSecondRelationVO> page = contractService.listFirstSecondPage(query);
        return Result.ok(page);
    }

    /**
     * ???????
     *
     * @param query ????
     * @return ????
     */
    @ApiOperation(value = "CRM一级二级关系导入分页列表")
    @SaCheckPermission("org:contract:add")
    @GetMapping("/first-second/crm-import/list")
    public Result<PageResult<CrmFirstSecondRelationImportVO>> listCrmFirstSecondImportPage(CrmFirstSecondRelationImportQuery query) {
        PageResult<CrmFirstSecondRelationImportVO> page = contractService.listCrmFirstSecondImportPage(query);
        return Result.ok(page);
    }

    /**
     * ?? importFirstSecondFromCrm ?????
     *
     * @param dto ????
     * @return ??????
     */
    @ApiOperation(value = "从CRM来源快照导入一级二级关系")
    @SaCheckPermission("org:contract:add")
    @OperLog(title = "绛剧害绠＄悊", operType = OperTypeEnum.INSERT)
    @PostMapping("/first-second/crm-import")
    public Result<CrmFirstSecondRelationImportResultVO> importFirstSecondFromCrm(@Validated @RequestBody CrmFirstSecondRelationImportDTO dto) {
        CrmFirstSecondRelationImportResultVO result = contractService.importFirstSecondFromCrm(dto);
        return Result.ok(result);
    }

    /**
     * 新增一级-二级从属
     *
     * @param dto 从属关系参数
     * @return 主键ID
     */
    @ApiOperation(value = "新增一级-二级从属")
    @SaCheckPermission("org:contract:add")
    @OperLog(title = "签约管理", operType = OperTypeEnum.INSERT)
    @PostMapping("/first-second")
    public Result<Long> saveFirstSecond(@Validated @RequestBody FirstSecondRelationDTO dto) {
        Long id = contractService.saveFirstSecond(dto);
        return Result.ok(id);
    }

    /**
     * 删除一级-二级从属
     *
     * @param id 主键ID
     * @return 操作结果
     */
    @ApiOperation(value = "删除一级-二级从属")
    @SaCheckPermission("org:contract:remove")
    @OperLog(title = "签约管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/first-second/{id}")
    public Result<Void> removeFirstSecond(@PathVariable Long id,
                                          @RequestParam(required = false) Long targetCompanyId) {
        contractService.removeFirstSecond(id, targetCompanyId);
        return Result.ok();
    }
}
