package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
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
 * 公司类型管理控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@RestController
@RequestMapping("/org/company-type")
public class SysCompanyTypeController extends BaseController {

    @Resource
    private ISysCompanyTypeService companyTypeService;

    /**
     * 查询所有公司类型（基础参考数据，仅需登录即可访问）
     *
     * @return 公司类型列表
     */
    @GetMapping("/list")
    public Result<List<SysCompanyType>> list() {
        List<SysCompanyType> list = companyTypeService.listAll();
        return Result.ok(list);
    }

    /**
     * 根据ID查询公司类型
     *
     * @param id 主键ID
     * @return 公司类型
     */
    @GetMapping("/{id}")
    public Result<SysCompanyType> getById(@PathVariable Long id) {
        SysCompanyType entity = companyTypeService.getById(id);
        return Result.ok(entity);
    }

    /**
     * 新增公司类型
     *
     * @param entity 公司类型实体
     * @return 主键ID
     */
    @SaCheckPermission("org:companyType:add")
    @OperLog(title = "公司类型管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@RequestBody SysCompanyType entity) {
        Long id = companyTypeService.save(entity);
        return Result.ok(id);
    }

    /**
     * 修改公司类型
     *
     * @param entity 公司类型实体
     * @return 操作结果
     */
    @SaCheckPermission("org:companyType:update")
    @OperLog(title = "公司类型管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@RequestBody SysCompanyType entity) {
        companyTypeService.update(entity);
        return Result.ok();
    }

    /**
     * 删除公司类型
     *
     * @param id 主键ID
     * @return 操作结果
     */
    @SaCheckPermission("org:companyType:remove")
    @OperLog(title = "公司类型管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        companyTypeService.remove(id);
        return Result.ok();
    }
}
