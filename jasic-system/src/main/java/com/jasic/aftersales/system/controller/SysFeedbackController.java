package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SysFeedbackAcceptDTO;
import com.jasic.aftersales.system.domain.dto.SysFeedbackCreateDTO;
import com.jasic.aftersales.system.domain.query.SysFeedbackManageQuery;
import com.jasic.aftersales.system.domain.query.SysFeedbackMyQuery;
import com.jasic.aftersales.system.domain.vo.SysFeedbackVO;
import com.jasic.aftersales.system.service.ISysFeedbackService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 平台反馈单系统端控制器。
 *
 * <p>该控制器同时承载：
 * 网点用户提交反馈、查询自己的反馈列表与详情；
 * 总部后台查询管理列表、查看详情、首次受理和修改受理。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
@Api(tags = "平台反馈单")
@RestController
@RequestMapping("/system/feedback")
public class SysFeedbackController {

    /** 平台反馈单 Service */
    @Resource
    private ISysFeedbackService sysFeedbackService;

    /**
     * 网点用户提交反馈。
     *
     * @param dto 提交参数
     * @return 新建反馈 ID
     */
    @ApiOperation(value = "网点用户提交反馈")
    @SaCheckPermission("feedback:add")
    @OperLog(title = "反馈管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> create(@Validated @RequestBody SysFeedbackCreateDTO dto) {
        return Result.ok(sysFeedbackService.createForCurrentServiceUser(dto));
    }

    /**
     * 查询当前网点用户自己的反馈列表。
     *
     * @param query 分页参数
     * @return 分页结果
     */
    @ApiOperation(value = "查询当前网点用户自己的反馈列表")
    @SaCheckPermission("feedback:list")
    @GetMapping("/my-list")
    public Result<PageResult<SysFeedbackVO>> myList(SysFeedbackMyQuery query) {
        return Result.ok(sysFeedbackService.listCurrentServiceUserPage(query));
    }

    /**
     * 查询当前网点用户自己的反馈详情。
     *
     * @param id 反馈 ID
     * @return 反馈详情
     */
    @ApiOperation(value = "查询当前网点用户自己的反馈详情")
    @SaCheckPermission("feedback:list")
    @GetMapping("/my/{id}")
    public Result<SysFeedbackVO> myDetail(@PathVariable Long id) {
        return Result.ok(sysFeedbackService.getCurrentServiceUserDetail(id));
    }

    /**
     * 查询总部后台反馈管理列表。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "查询总部后台反馈管理列表")
    @SaCheckPermission("feedback:list")
    @GetMapping("/manage/list")
    public Result<PageResult<SysFeedbackVO>> manageList(SysFeedbackManageQuery query) {
        return Result.ok(sysFeedbackService.listManagePage(query));
    }

    /**
     * 查询总部后台反馈详情。
     *
     * @param id 反馈 ID
     * @return 反馈详情
     */
    @ApiOperation(value = "查询总部后台反馈详情")
    @SaCheckPermission("feedback:list")
    @GetMapping("/manage/{id}")
    public Result<SysFeedbackVO> manageDetail(@PathVariable Long id) {
        return Result.ok(sysFeedbackService.getManageDetail(id));
    }

    /**
     * 总部后台首次受理反馈。
     *
     * @param dto 受理参数
     * @return 操作结果
     */
    @ApiOperation(value = "总部后台受理反馈")
    @SaCheckPermission("feedback:accept")
    @OperLog(title = "反馈管理", operType = OperTypeEnum.UPDATE)
    @PostMapping("/manage/accept")
    public Result<Void> accept(@Validated @RequestBody SysFeedbackAcceptDTO dto) {
        sysFeedbackService.accept(dto);
        return Result.ok();
    }

    /**
     * 总部后台修改已受理反馈的受理结果。
     *
     * @param dto 修改参数
     * @return 操作结果
     */
    @ApiOperation(value = "总部后台修改受理")
    @SaCheckPermission("feedback:updateAccept")
    @OperLog(title = "反馈管理", operType = OperTypeEnum.UPDATE)
    @PostMapping("/manage/update-accept")
    public Result<Void> updateAccept(@Validated @RequestBody SysFeedbackAcceptDTO dto) {
        sysFeedbackService.updateAccept(dto);
        return Result.ok();
    }
}
