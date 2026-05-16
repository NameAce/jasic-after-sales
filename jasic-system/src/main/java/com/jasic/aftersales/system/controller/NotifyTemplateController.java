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
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateStatusDTO;
import com.jasic.aftersales.system.notify.domain.query.NotifyTemplateQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateOptionsVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplatePreviewVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateVO;
import com.jasic.aftersales.system.notify.service.NotifyChannelConfigService;
import com.jasic.aftersales.system.notify.service.NotifyTemplateAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
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
 * 通知模板配置控制器。
 *
 * <p>该控制器负责后台“通知模板配置”菜单下的模板维护接口编排，
 * 包括列表、详情、新增、修改、启停、预览和渠道配置查询。
 * 本次精简重构后模板和渠道配置都按 `sceneCode` 维护，不再接受历史组合字段。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Api(tags = "通知模板配置")
@RestController
@RequestMapping("/system/notify/template")
public class NotifyTemplateController extends BaseController {

    @Resource
    private NotifyTemplateAdminService notifyTemplateAdminService;

    @Resource
    private NotifyChannelConfigService notifyChannelConfigService;

    /**
     * 查询模板配置页元数据。
     *
     * <p>新增模板前必须先读取后端场景注册表，
     * 避免前端绕过基线直接提交未知通知场景或伪造变量清单。</p>
     *
     * @return 模板配置页元数据
     */
    @ApiOperation(value = "查询通知模板配置页元数据")
    @SaCheckPermission("system:notifyTemplate:view")
    @GetMapping("/options")
    public Result<NotifyTemplateOptionsVO> options() {
        return Result.ok(notifyTemplateAdminService.getOptions());
    }

    /**
     * 分页查询通知模板列表。
     *
     * @param query 查询条件
     * @return 模板分页结果
     */
    @ApiOperation(value = "分页查询通知模板")
    @SaCheckPermission("system:notifyTemplate:list")
    @GetMapping("/list")
    public Result<PageResult<NotifyTemplateVO>> list(NotifyTemplateQuery query) {
        return Result.ok(notifyTemplateAdminService.listPage(query));
    }

    /**
     * 查询通知模板详情。
     *
     * @param id 模板主键
     * @return 模板详情
     */
    @ApiOperation(value = "查询通知模板详情")
    @SaCheckPermission("system:notifyTemplate:view")
    @GetMapping("/{id}")
    public Result<NotifyTemplateVO> getById(@PathVariable Long id) {
        return Result.ok(notifyTemplateAdminService.getById(id));
    }

    /**
     * 新增通知模板。
     *
     * @param dto 模板参数
     * @return 新增后的主键
     */
    @ApiOperation(value = "新增通知模板")
    @SaCheckPermission("system:notifyTemplate:add")
    @OperLog(title = "通知模板配置", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> create(@Validated @RequestBody NotifyTemplateDTO dto) {
        return Result.ok(notifyTemplateAdminService.createTemplate(dto));
    }

    /**
     * 修改通知模板。
     *
     * @param dto 模板参数
     * @return 处理结果
     */
    @ApiOperation(value = "修改通知模板")
    @SaCheckPermission("system:notifyTemplate:update")
    @OperLog(title = "通知模板配置", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody NotifyTemplateDTO dto) {
        notifyTemplateAdminService.updateTemplate(dto);
        return Result.ok();
    }

    /**
     * 启用或停用通知模板。
     *
     * <p>这里继续复用 `system:notifyTemplate:remove` 权限，
     * 但语义已经改为模板启停，不暴露物理删除能力。</p>
     *
     * @param id 模板主键
     * @param dto 状态参数
     * @return 处理结果
     */
    @ApiOperation(value = "启用或停用通知模板")
    @SaCheckPermission("system:notifyTemplate:remove")
    @OperLog(title = "通知模板配置", operType = OperTypeEnum.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Validated @RequestBody NotifyTemplateStatusDTO dto) {
        notifyTemplateAdminService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    /**
     * 预览通知模板内容。
     *
     * @param dto 预览参数
     * @return 预览结果
     */
    @ApiOperation(value = "预览通知模板")
    @SaCheckPermission("system:notifyTemplate:preview")
    @PostMapping("/preview")
    public Result<NotifyTemplatePreviewVO> preview(@Validated @RequestBody NotifyTemplatePreviewDTO dto) {
        return Result.ok(notifyTemplateAdminService.preview(dto));
    }

    /**
     * 查询指定通知场景的渠道配置。
     *
     * @param sceneCode 通知场景编码
     * @return 渠道配置列表
     */
    @ApiOperation(value = "查询通知模板渠道配置")
    @SaCheckPermission("system:notifyTemplate:view")
    @GetMapping("/{sceneCode}/channels")
    public Result<List<NotifyTemplateChannelVO>> listChannels(@PathVariable String sceneCode) {
        return Result.ok(notifyChannelConfigService.listChannelConfigs(sceneCode));
    }

    /**
     * 保存指定通知场景的渠道配置。
     *
     * @param sceneCode 通知场景编码
     * @param channelConfigs 渠道配置列表
     * @return 处理结果
     */
    @ApiOperation(value = "保存通知模板渠道配置")
    @SaCheckPermission("system:notifyTemplate:update")
    @OperLog(title = "通知模板渠道配置", operType = OperTypeEnum.UPDATE)
    @PutMapping("/{sceneCode}/channels")
    public Result<Void> saveChannels(@PathVariable String sceneCode,
                                     @RequestBody List<NotifyTemplateChannelDTO> channelConfigs) {
        notifyChannelConfigService.saveChannelConfigs(sceneCode, channelConfigs);
        return Result.ok();
    }
}
