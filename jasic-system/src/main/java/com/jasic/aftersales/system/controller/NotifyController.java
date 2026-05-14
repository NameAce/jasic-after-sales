package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO;
import com.jasic.aftersales.system.notify.domain.query.NotifyMessageQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyMessagePageResultVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyMessagePageVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTodoCountVO;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
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
 * 消息通知控制器。
 *
 * @author Codex
 * @date 2026/04/18
 */
@Api(tags = "消息通知")
@RestController
@SaCheckLogin
@RequestMapping("/system/notify")
public class NotifyController {

    @Resource
    private NotifyMessageService notifyMessageService;

    /**
     * 获取当前登录用户有效待办数。
     *
     * @return 待办统计
     */
    @ApiOperation(value = "获取当前登录用户有效待办数")
    @GetMapping("/todo/count")
    public Result<NotifyTodoCountVO> todoCount() {
        // ????????????????????????
        NotifyTodoCountVO vo = new NotifyTodoCountVO();
        vo.setCount(notifyMessageService.countTodo(
                SecurityContext.getCurrentUserId(),
                SecurityContext.getCurrentCompanyId()
        ));
        return Result.ok(vo);
    }

    /**
     * 分页查询当前登录用户消息。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询当前登录用户消息")
    @GetMapping("/todo/page")
    public Result<NotifyMessagePageResultVO> todoPage(NotifyMessageQuery query) {
        query.setReceiverId(SecurityContext.getCurrentUserId());
        query.setReceiverCompanyId(SecurityContext.getCurrentCompanyId());
        // ????????????????????????
        PageResult<NotifyMessagePageVO> pageResult = notifyMessageService.listPage(query);
        return Result.ok(NotifyMessagePageResultVO.of(pageResult.getTotal(), pageResult.getRecords()));
    }

    /**
     * 按消息 ID 标记已读。
     *
     * @param id 消息 ID
     * @return 操作结果
     */
    @ApiOperation(value = "按消息 ID 标记已读")
    @PostMapping("/message/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        // ????????????????????????
        notifyMessageService.markRead(
                id,
                SecurityContext.getCurrentUserId(),
                SecurityContext.getCurrentCompanyId()
        );
        return Result.ok();
    }

    /**
     * 按业务对象标记已读。
     *
     * @param dto 请求参数
     * @return 操作结果
     */
    @ApiOperation(value = "按业务对象标记已读")
    @PostMapping("/message/read-by-biz")
    public Result<Void> readByBiz(@Validated @RequestBody NotifyReadByBizDTO dto) {
        dto.setReceiverId(SecurityContext.getCurrentUserId());
        dto.setReceiverCompanyId(SecurityContext.getCurrentCompanyId());
        // ????????????????????????
        notifyMessageService.markReadByBiz(dto);
        return Result.ok();
    }
}
