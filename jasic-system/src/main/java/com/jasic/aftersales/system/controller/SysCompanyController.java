package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SysCompanyDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.query.SysCompanyQuery;
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

/**
 * 公司管理控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@RestController
@RequestMapping("/org/company")
public class SysCompanyController extends BaseController {

    @Resource
    private ISysCompanyService companyService;

    /**
     * 分页查询公司列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @SaCheckPermission("org:company:list")
    @GetMapping("/list")
    public Result<PageResult<SysCompany>> list(SysCompanyQuery query) {
        PageResult<SysCompany> page = companyService.listPage(query);
        return Result.ok(page);
    }

    /**
     * 根据ID查询公司
     *
     * @param id 主键ID
     * @return 公司详情
     */
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
    @SaCheckPermission("org:company:add")
    @OperLog(title = "公司管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysCompanyDTO dto) {
        Long id = companyService.save(dto);
        return Result.ok(id);
    }

    /**
     * 修改公司
     *
     * @param dto 公司参数
     * @return 操作结果
     */
    @SaCheckPermission("org:company:update")
    @OperLog(title = "公司管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysCompanyDTO dto) {
        companyService.update(dto);
        return Result.ok();
    }

    /**
     * 删除公司
     *
     * @param id 主键ID
     * @return 操作结果
     */
    @SaCheckPermission("org:company:remove")
    @OperLog(title = "公司管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        companyService.remove(id);
        return Result.ok();
    }
}
