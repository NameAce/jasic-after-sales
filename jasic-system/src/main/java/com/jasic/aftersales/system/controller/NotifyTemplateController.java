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

@Api(tags = "通知模板")
@RestController
@RequestMapping("/system/notify/template")
public class NotifyTemplateController extends BaseController {

    @Resource
    private NotifyTemplateService notifyTemplateService;

    /**
     * ???????
     *
     * @param query ????
     * @return ????
     */
    @ApiOperation(value = "分页查询通知模板")
    @SaCheckPermission("system:notifyTemplate:list")
    @GetMapping("/list")
    public Result<PageResult<NotifyTemplateVO>> list(NotifyTemplateQuery query) {
        return Result.ok(notifyTemplateService.listPage(query));
    }

    /**
     * ??By Id?
     *
     * @param id ??ID
     * @return ??????
     */
    @ApiOperation(value = "查询通知模板详情")
    @SaCheckPermission("system:notifyTemplate:view")
    @GetMapping("/{id}")
    public Result<NotifyTemplateVO> getById(@PathVariable Long id) {
        return Result.ok(notifyTemplateService.getById(id));
    }

    /**
     * ???????
     *
     * @param templateCode ??
     * @return ????
     */
    @ApiOperation(value = "查询通知模板渠道配置")
    @SaCheckPermission("system:notifyTemplate:view")
    @GetMapping("/{templateCode}/channels")
    public Result<List<NotifyTemplateChannelVO>> listChannels(@PathVariable String templateCode) {
        return Result.ok(notifyTemplateService.listChannelConfigs(templateCode));
    }

    /**
     * ?????
     *
     * @param dto ????
     * @return ??????
     */
    @ApiOperation(value = "新增自定义通知模板")
    @SaCheckPermission("system:notifyTemplate:add")
    @OperLog(title = "通知模板", operType = OperTypeEnum.INSERT)
    @PostMapping("/custom")
    public Result<Long> saveCustom(@Validated @RequestBody NotifyTemplateDTO dto) {
        return Result.ok(notifyTemplateService.saveCustom(dto));
    }

    /**
     * ?????
     *
     * @param dto ????
     * @return ??????
     */
    @ApiOperation(value = "修改自定义通知模板")
    @SaCheckPermission("system:notifyTemplate:update")
    @OperLog(title = "通知模板", operType = OperTypeEnum.UPDATE)
    @PutMapping("/custom")
    public Result<Void> updateCustom(@Validated @RequestBody NotifyTemplateDTO dto) {
        notifyTemplateService.updateCustom(dto);
        return Result.ok();
    }

    /**
     * ?????
     *
     * @param templateCode ??
     * @param channelConfigs ??
     * @return ??????
     */
    @ApiOperation(value = "保存通知模板渠道配置")
    @SaCheckPermission("system:notifyTemplate:update")
    @OperLog(title = "通知模板渠道配置", operType = OperTypeEnum.UPDATE)
    @PutMapping("/{templateCode}/channels")
    public Result<Void> saveChannels(@PathVariable String templateCode,
                                     @RequestBody List<NotifyTemplateChannelDTO> channelConfigs) {
        notifyTemplateService.saveChannelConfigs(templateCode, channelConfigs);
        return Result.ok();
    }

    /**
     * ?????
     *
     * @param id ??ID
     * @return ??????
     */
    @ApiOperation(value = "删除自定义通知模板")
    @SaCheckPermission("system:notifyTemplate:remove")
    @OperLog(title = "通知模板", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/custom/{id}")
    public Result<Void> removeCustom(@PathVariable Long id) {
        notifyTemplateService.removeCustom(id);
        return Result.ok();
    }

    /**
     * ?? preview ?????
     *
     * @param dto ????
     * @return ??????
     */
    @ApiOperation(value = "预览通知模板")
    @SaCheckPermission("system:notifyTemplate:preview")
    @PostMapping("/preview")
    public Result<NotifyTemplatePreviewVO> preview(@Validated @RequestBody NotifyTemplatePreviewDTO dto) {
        return Result.ok(notifyTemplateService.preview(dto));
    }

    /**
     * ?? refreshCache ?????
     *
     * @return ??????
     */
    @ApiOperation(value = "刷新通知模板缓存")
    @SaCheckPermission("system:notifyTemplate:refresh")
    @OperLog(title = "通知模板", operType = OperTypeEnum.OTHER)
    @PostMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        notifyTemplateService.refreshCache();
        return Result.ok();
    }
}
