package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateChannelDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplatePreviewDTO;
import com.jasic.aftersales.system.notify.domain.query.NotifyTemplateQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplatePreviewVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateVO;
import com.jasic.aftersales.system.notify.service.NotifyTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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

/**
 * NotifyTemplateController。
 *
 * <p>控制层组件，负责接收请求并编排业务调用。</p>
 */
@Api(tags = "通知模板")
@RestController
@RequestMapping("/system/notify/template")
public class NotifyTemplateController extends BaseController {

    @Resource
    private NotifyTemplateService notifyTemplateService;

    /**
     * 分页查询通知模板列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @ApiOperation(value = "分页查询通知模板")
    @SaCheckPermission("system:notifyTemplate:list")
    @GetMapping("/list")
    public Result<PageResult<NotifyTemplateVO>> list(NotifyTemplateQuery query) {
        return Result.ok(notifyTemplateService.listPage(query));
    }

    /**
     * 根据ID查询通知模板详情。
     *
     * @return 处理结果
     */
    @ApiOperation(value = "查询通知模板详情")
    @SaCheckPermission("system:notifyTemplate:view")
    @GetMapping("/{id}")
    public Result<NotifyTemplateVO> getById(@PathVariable Long id) {
        return Result.ok(notifyTemplateService.getById(id));
    }

    /**
     * 分页查询Channels列表。
     *
     * @param templateCode 参数
     * @return 处理结果
     */
    @ApiOperation(value = "查询通知模板渠道配置")
    @SaCheckPermission("system:notifyTemplate:view")
    @GetMapping("/{templateCode}/channels")
    public Result<List<NotifyTemplateChannelVO>> listChannels(@PathVariable String templateCode) {
        return Result.ok(notifyTemplateService.listChannelConfigs(templateCode));
    }

    /**
     * 新增自定义。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @ApiOperation(value = "新增自定义通知模板")
    @SaCheckPermission("system:notifyTemplate:add")
    @OperLog(title = "通知模板", operType = OperTypeEnum.INSERT)
    @PostMapping("/custom")
    public Result<Long> saveCustom(@Validated @RequestBody NotifyTemplateDTO dto) {
        return Result.ok(notifyTemplateService.saveCustom(dto));
    }

    /**
     * 更新自定义。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @ApiOperation(value = "修改自定义通知模板")
    @SaCheckPermission("system:notifyTemplate:update")
    @OperLog(title = "通知模板", operType = OperTypeEnum.UPDATE)
    @PutMapping("/custom")
    public Result<Void> updateCustom(@Validated @RequestBody NotifyTemplateDTO dto) {
        // 调用updateCustom方法，复用统一能力并保证业务规则一致。
        notifyTemplateService.updateCustom(dto);
        return Result.ok();
    }

    /**
     * 新增Channels。
     *
     * @param templateCode 参数
     * @param channelConfigs 参数
     * @return 处理结果
     */
    @ApiOperation(value = "保存通知模板渠道配置")
    @SaCheckPermission("system:notifyTemplate:update")
    @OperLog(title = "通知模板渠道配置", operType = OperTypeEnum.UPDATE)
    @PutMapping("/{templateCode}/channels")
    public Result<Void> saveChannels(@PathVariable String templateCode,
                                     @RequestBody List<NotifyTemplateChannelDTO> channelConfigs) {
        // 调用saveChannelConfigs方法，复用统一能力并保证业务规则一致。
        notifyTemplateService.saveChannelConfigs(templateCode, channelConfigs);
        return Result.ok();
    }

    /**
     * 删除自定义。
     *
     * @return 处理结果
     */
    @ApiOperation(value = "删除自定义通知模板")
    @SaCheckPermission("system:notifyTemplate:remove")
    @OperLog(title = "通知模板", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/custom/{id}")
    public Result<Void> removeCustom(@PathVariable Long id) {
        // 调用removeCustom方法，复用统一能力并保证业务规则一致。
        notifyTemplateService.removeCustom(id);
        return Result.ok();
    }

    /**
     * 预览通知模板内容。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @ApiOperation(value = "预览通知模板")
    @SaCheckPermission("system:notifyTemplate:preview")
    @PostMapping("/preview")
    public Result<NotifyTemplatePreviewVO> preview(@Validated @RequestBody NotifyTemplatePreviewDTO dto) {
        return Result.ok(notifyTemplateService.preview(dto));
    }

    /**
     * 刷新通知模板缓存。
     *
     * @return 处理结果
     */
    @ApiOperation(value = "刷新通知模板缓存")
    @SaCheckPermission("system:notifyTemplate:refresh")
    @OperLog(title = "通知模板", operType = OperTypeEnum.OTHER)
    @PostMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        // 调用refreshCache方法，复用统一能力并保证业务规则一致。
        notifyTemplateService.refreshCache();
        return Result.ok();
    }
}


