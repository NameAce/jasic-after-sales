package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.FirstSecondRelationDTO;
import com.jasic.aftersales.system.domain.dto.HqFirstContractDTO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 签约管理控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
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
    @SaCheckPermission("org:contract:list")
    @GetMapping("/hq-first/list")
    public Result<PageResult<HqFirstContractVO>> listHqFirstPage(HqFirstContractQuery query) {
        PageResult<HqFirstContractVO> page = contractService.listHqFirstPage(query);
        return Result.ok(page);
    }

    /**
     * 新增总部-一级签约
     *
     * @param dto 签约参数
     * @return 主键ID
     */
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
    @SaCheckPermission("org:contract:remove")
    @OperLog(title = "签约管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/hq-first/{id}")
    public Result<Void> removeHqFirst(@PathVariable Long id) {
        contractService.removeHqFirst(id);
        return Result.ok();
    }

    /**
     * 一级-二级从属分页列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @SaCheckPermission("org:contract:list")
    @GetMapping("/first-second/list")
    public Result<PageResult<FirstSecondRelationVO>> listFirstSecondPage(FirstSecondRelationQuery query) {
        PageResult<FirstSecondRelationVO> page = contractService.listFirstSecondPage(query);
        return Result.ok(page);
    }

    /**
     * 新增一级-二级从属
     *
     * @param dto 从属关系参数
     * @return 主键ID
     */
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
    @SaCheckPermission("org:contract:remove")
    @OperLog(title = "签约管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/first-second/{id}")
    public Result<Void> removeFirstSecond(@PathVariable Long id) {
        contractService.removeFirstSecond(id);
        return Result.ok();
    }
}
