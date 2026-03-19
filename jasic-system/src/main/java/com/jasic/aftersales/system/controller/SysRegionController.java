package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SysRegionDTO;
import com.jasic.aftersales.system.domain.entity.SysRegion;
import com.jasic.aftersales.system.service.ISysRegionService;
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
 * 大区管理控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@RestController
@RequestMapping("/system/region")
public class SysRegionController extends BaseController {

    @Resource
    private ISysRegionService regionService;

    /**
     * 根据公司ID查询大区列表
     *
     * @param companyId 公司ID
     * @return 大区列表
     */
    @SaCheckPermission("system:region:list")
    @GetMapping("/list")
    public Result<List<SysRegion>> listByCompanyId(@RequestParam Long companyId) {
        List<SysRegion> list = regionService.listByCompanyId(companyId);
        return Result.ok(list);
    }

    /**
     * 根据ID查询大区
     *
     * @param id 主键ID
     * @return 大区详情
     */
    @SaCheckPermission("system:region:list")
    @GetMapping("/{id}")
    public Result<SysRegion> getById(@PathVariable Long id) {
        SysRegion entity = regionService.getById(id);
        return Result.ok(entity);
    }

    /**
     * 新增大区
     *
     * @param dto 大区参数
     * @return 主键ID
     */
    @SaCheckPermission("system:region:add")
    @OperLog(title = "大区管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysRegionDTO dto) {
        Long id = regionService.save(dto);
        return Result.ok(id);
    }

    /**
     * 修改大区
     *
     * @param dto 大区参数
     * @return 操作结果
     */
    @SaCheckPermission("system:region:update")
    @OperLog(title = "大区管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysRegionDTO dto) {
        regionService.update(dto);
        return Result.ok();
    }

    /**
     * 删除大区
     *
     * @param id 主键ID
     * @return 操作结果
     */
    @SaCheckPermission("system:region:remove")
    @OperLog(title = "大区管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        regionService.remove(id);
        return Result.ok();
    }

    /**
     * 分配用户大区
     *
     * @param userId    用户ID
     * @param regionIds 大区ID列表
     * @return 操作结果
     */
    @SaCheckPermission("system:region:assign")
    @OperLog(title = "大区管理", operType = OperTypeEnum.GRANT)
    @PutMapping("/{userId}/regions")
    public Result<Void> assignUserRegions(@PathVariable Long userId, @RequestBody List<Long> regionIds) {
        regionService.assignUserRegions(userId, regionIds);
        return Result.ok();
    }
}
