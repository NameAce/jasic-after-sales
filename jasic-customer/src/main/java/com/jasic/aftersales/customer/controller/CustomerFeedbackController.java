package com.jasic.aftersales.customer.controller;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.customer.service.ICustomerFeedbackService;
import com.jasic.aftersales.system.domain.dto.SysFeedbackCreateDTO;
import com.jasic.aftersales.system.domain.query.SysFeedbackMyQuery;
import com.jasic.aftersales.system.domain.vo.SysFeedbackVO;
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
 * C端反馈单控制器。
 *
 * <p>该控制器只面向终端用户开放，负责提交反馈、查看自己的反馈列表和查看自己的反馈详情。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
@Api(tags = "C端反馈单")
@RestController
@RequestMapping("/customer/feedback")
public class CustomerFeedbackController {

    /** C端反馈Service */
    @Resource
    private ICustomerFeedbackService customerFeedbackService;

    /**
     * 终端用户提交反馈。
     *
     * @param dto 提交参数
     * @return 新建反馈ID
     */
    @ApiOperation(value = "终端用户提交反馈")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody SysFeedbackCreateDTO dto) {
        return Result.ok(customerFeedbackService.create(dto));
    }

    /**
     * 分页查询终端用户自己的反馈列表。
     *
     * @param query 分页参数
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询终端用户自己的反馈列表")
    @GetMapping("/list")
    public Result<PageResult<SysFeedbackVO>> list(SysFeedbackMyQuery query) {
        return Result.ok(customerFeedbackService.listPage(query));
    }

    /**
     * 查询终端用户自己的反馈详情。
     *
     * @param id 反馈ID
     * @return 反馈详情
     */
    @ApiOperation(value = "查询终端用户自己的反馈详情")
    @GetMapping("/{id}")
    public Result<SysFeedbackVO> getById(@PathVariable Long id) {
        return Result.ok(customerFeedbackService.getById(id));
    }
}
