package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.notify.domain.dto.NotifySceneConfigSaveDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyScenePreviewDTO;
import com.jasic.aftersales.system.notify.domain.query.NotifySceneConfigQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneConfigDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneConfigOptionsVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneConfigPageVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyScenePreviewVO;
import com.jasic.aftersales.system.notify.service.NotifySceneTargetConfigService;
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

/**
 * 通知场景配置控制器。
 *
 * <p>该控制器是后台“通知场景配置”入口的唯一后端编排层。
 * 它统一围绕“一个通知场景下维护多个通知目标”提供元数据、列表、详情、保存和预览接口，
 * 不再暴露旧“模板管理/渠道配置”割裂入口。</p>
 *
 * @author Zoro
 * @date 2026/05/16
 */
@Api(tags = "通知场景配置")
@RestController
@RequestMapping("/system/notify/scene")
public class NotifySceneController extends BaseController {

    /**notifySceneTargetConfigService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private NotifySceneTargetConfigService notifySceneTargetConfigService;

    /**
     * 查询通知场景配置页元数据。
     *
     * @return 场景配置页元数据
     */
    @ApiOperation(value = "查询通知场景配置页元数据")
    @SaCheckPermission("system:notifyScene:view")
    @GetMapping("/options")
    public Result<NotifySceneConfigOptionsVO> options() {
        return Result.ok(notifySceneTargetConfigService.getOptions());
    }

    /**
     * 分页查询通知场景配置。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询通知场景配置")
    @SaCheckPermission("system:notifyScene:list")
    @GetMapping("/list")
    public Result<PageResult<NotifySceneConfigPageVO>> list(NotifySceneConfigQuery query) {
        return Result.ok(notifySceneTargetConfigService.listPage(query));
    }

    /**
     * 查询单个通知场景配置详情。
     *
     * @param sceneCode 场景编码
     * @return 场景配置详情
     */
    @ApiOperation(value = "查询单个通知场景配置详情")
    @SaCheckPermission("system:notifyScene:view")
    @GetMapping("/{sceneCode}")
    public Result<NotifySceneConfigDetailVO> getDetail(@PathVariable String sceneCode) {
        return Result.ok(notifySceneTargetConfigService.getDetail(sceneCode));
    }

    /**
     * 保存整个通知场景下的全部目标配置。
     *
     * @param sceneCode 场景编码
     * @param dto 场景保存参数
     * @return 业务处理结果
     */
    @ApiOperation(value = "保存整个通知场景下的全部目标配置")
    @SaCheckPermission("system:notifyScene:update")
    @OperLog(title = "通知场景配置", operType = OperTypeEnum.UPDATE)
    @PutMapping("/{sceneCode}")
    public Result<Void> save(@PathVariable String sceneCode,
                             @Validated @RequestBody NotifySceneConfigSaveDTO dto) {
        notifySceneTargetConfigService.saveSceneConfig(sceneCode, dto);
        return Result.ok();
    }

    /**
     * 预览指定场景目标的渲染结果。
     *
     * @param dto 预览参数
     * @return 预览结果
     */
    @ApiOperation(value = "预览指定场景目标的渲染结果")
    @SaCheckPermission("system:notifyScene:preview")
    @PostMapping("/preview")
    public Result<NotifyScenePreviewVO> preview(@Validated @RequestBody NotifyScenePreviewDTO dto) {
        return Result.ok(notifySceneTargetConfigService.preview(dto));
    }
}
