package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.query.CrmBizCompanySnapshotQuery;
import com.jasic.aftersales.system.domain.dto.SysCompanyDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.query.SysCompanyQuery;
import com.jasic.aftersales.system.domain.vo.CrmBizCompanyImportPreviewVO;
import com.jasic.aftersales.system.domain.vo.CrmBizCompanySnapshotVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySaveResultVO;
import com.jasic.aftersales.system.service.ICrmBizCompanySnapshotService;
import com.jasic.aftersales.system.service.ISysCompanyService;
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 公司管理控制器。
 *
 * <p>除常规公司增删改查外，本控制器额外提供 CRM 公司快照查询和导入预览接口，
 * 供前端“从 CRM 导入”流程调用。</p>
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Api(tags = "公司管理")
@RestController
@RequestMapping("/org/company")
public class SysCompanyController extends BaseController {

    @Resource
    private ISysCompanyService companyService;

    @Resource
    private ICrmBizCompanySnapshotService crmBizCompanySnapshotService;

    /**
     * 分页查询公司列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询公司列表")
    @SaCheckPermission("org:company:list")
    @GetMapping("/list")
    public Result<PageResult<SysCompany>> list(SysCompanyQuery query) {
        PageResult<SysCompany> page = companyService.listPage(query);
        return Result.ok(page);
    }

    @ApiOperation(value = "分页查询 CRM 公司快照")
    @SaCheckPermission("org:company:add")
    @GetMapping("/external/list")
    public Result<PageResult<CrmBizCompanySnapshotVO>> listExternalCompanies(CrmBizCompanySnapshotQuery query) {
        // 仅返回快照和本地存在性信息，不在该接口内直接创建本地公司。
        return Result.ok(crmBizCompanySnapshotService.listPage(query));
    }

    @ApiOperation(value = "查询 CRM 公司导入预览")
    @SaCheckPermission("org:company:add")
    @GetMapping("/external/{custId}/import-preview")
    public Result<CrmBizCompanyImportPreviewVO> getExternalCompanyImportPreview(@PathVariable Long custId) {
        // 前端选择 CRM 公司后，先走预览接口带入表单，再由用户确认保存。
        return Result.ok(crmBizCompanySnapshotService.getImportPreview(custId));
    }

    /**
     * 根据ID查询公司
     *
     * @param id 主键ID
     * @return 公司详情
     */
    @ApiOperation(value = "根据ID查询公司")
    @GetMapping("/{id}")
    public Result<SysCompany> getById(@PathVariable Long id) {
        SysCompany entity = companyService.getById(id);
        return Result.ok(entity);
    }

    /**
     * 新增公司
     *
     * @param dto 公司参数
     * @return 主键ID
     */
    @ApiOperation(value = "新增公司")
    @SaCheckPermission("org:company:add")
    @OperLog(title = "公司管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<SysCompanySaveResultVO> save(@Validated @RequestBody SysCompanyDTO dto) {
        Long id = companyService.save(dto);
        return Result.ok(buildSaveResult(companyService.getById(id)));
    }

    /**
     * 修改公司
     *
     * @param dto 公司参数
     * @return 操作结果
     */
    @ApiOperation(value = "修改公司")
    @SaCheckPermission("org:company:update")
    @OperLog(title = "公司管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<SysCompanySaveResultVO> update(@Validated @RequestBody SysCompanyDTO dto) {
        companyService.update(dto);
        return Result.ok(buildSaveResult(companyService.getById(dto.getId())));
    }

    /**
     * 删除公司
     *
     * @param id 主键ID
     * @return 操作结果
     */
    @ApiOperation(value = "删除公司")
    @SaCheckPermission("org:company:remove")
    @OperLog(title = "公司管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        companyService.remove(id);
        return Result.ok();
    }

    private SysCompanySaveResultVO buildSaveResult(SysCompany company) {
        SysCompanySaveResultVO vo = new SysCompanySaveResultVO();
        if (company != null) {
            vo.setId(company.getId());
            vo.setGeocodeStatus(company.getGeocodeStatus());
        }
        return vo;
    }
}
