package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SysDictDataDTO;
import com.jasic.aftersales.system.domain.query.SysDictDataQuery;
import com.jasic.aftersales.system.domain.vo.SysDictDataVO;
import com.jasic.aftersales.system.service.ISysDictDataService;
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
 * 字典数据控制器
 *
 * @author Zoro
 * @date 2026/03/19
 */
@Api(tags = "字典数据")
@RestController
@RequestMapping("/system/dict/data")
public class SysDictDataController extends BaseController {

    /**dictDataService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private ISysDictDataService dictDataService;

    /**
     * 分页查询字典数据列表。
     *
     * @param query 查询条件，包含分页、筛选和权限收口所需字段。
     * @return 业务处理结果
     */
    @SaCheckPermission("system:dictData:list")
    @GetMapping("/list")
    public Result<PageResult<SysDictDataVO>> list(SysDictDataQuery query) {
        return Result.ok(dictDataService.listPage(query));
    }

    /**
     * 根据ID查询字典数据详情。
     *
     * @return 业务处理结果
     */
    @SaCheckPermission("system:dictData:list")
    @GetMapping("/{id}")
    public Result<SysDictDataVO> getById(@PathVariable Long id) {
        return Result.ok(dictDataService.getById(id));
    }

    /**
     * 分页查询By类型列表。
     *
     * @param dictType dictType，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    @GetMapping("/type/{dictType}")
    public Result<List<SysDictDataVO>> listByType(@PathVariable String dictType) {
        return Result.ok(dictDataService.listByType(dictType));
    }

    /**
     * 新增字典数据。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     * @return 业务处理结果
     */
    @SaCheckPermission("system:dictData:add")
    @OperLog(title = "字典数据管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysDictDataDTO dto) {
        return Result.ok(dictDataService.save(dto));
    }

    /**
     * 更新字典数据。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     * @return 业务处理结果
     */
    @SaCheckPermission("system:dictData:update")
    @OperLog(title = "字典数据管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysDictDataDTO dto) {
        dictDataService.update(dto);
        return Result.ok();
    }

    /**
     * 删除字典数据。
     *
     * @return 业务处理结果
     */
    @SaCheckPermission("system:dictData:remove")
    @OperLog(title = "字典数据管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        dictDataService.remove(id);
        return Result.ok();
    }
}


