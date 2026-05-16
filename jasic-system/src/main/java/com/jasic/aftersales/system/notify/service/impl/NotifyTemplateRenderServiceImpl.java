package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.entity.NotifySceneTarget;
import com.jasic.aftersales.system.notify.mapper.NotifySceneTargetMapper;
import com.jasic.aftersales.system.notify.service.NotifyTemplateRenderService;
import com.jasic.aftersales.system.notify.support.NotifySceneMeta;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
import com.jasic.aftersales.system.notify.support.NotifySceneTargetMeta;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知模板运行时渲染服务实现。
 *
 * <p>该实现服务于现有运行时链路，但底层数据源已经切换为 `notify_scene_target`。
 * 也就是说，事件消费方仍然可以调用“按场景编码渲染”的旧入口，
 * 但真正命中的已经是该场景下默认目标类型对应的目标配置。
 * 阶段二完成后，运行时将逐步切换为显式传入 `targetType`。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Slf4j
@Service
public class NotifyTemplateRenderServiceImpl implements NotifyTemplateRenderService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    @Resource
    private NotifySceneTargetMapper notifySceneTargetMapper;

    @Resource
    private NotifySceneRegistry notifySceneRegistry;

    /**
     * 按场景编码渲染默认通知目标。
     *
     * @param sceneCode 通知场景编码
     * @param variables 模板变量
     * @return 渲染结果
     */
    @Override
    public NotifyTemplateRenderResult render(String sceneCode, Map<String, Object> variables) {
        NotifySceneMeta sceneMeta = getRequiredScene(sceneCode);
        return render(sceneMeta.getSceneCode(), sceneMeta.getDefaultTargetType(), variables);
    }

    /**
     * 按场景编码和通知目标类型渲染指定目标。
     *
     * @param sceneCode 通知场景编码
     * @param targetType 通知目标类型
     * @param variables 模板变量
     * @return 渲染结果
     */
    @Override
    public NotifyTemplateRenderResult render(String sceneCode, String targetType, Map<String, Object> variables) {
        String normalizedSceneCode = normalizeRequiredField(sceneCode, "通知场景编码不能为空");
        String normalizedTargetType = normalizeRequiredField(targetType, "通知目标类型不能为空");
        NotifySceneMeta sceneMeta = notifySceneRegistry.getRequiredScene(normalizedSceneCode);
        NotifySceneTargetMeta targetMeta = notifySceneRegistry.getRequiredTargetMeta(normalizedSceneCode, normalizedTargetType);

        NotifyTemplateRenderResult result = new NotifyTemplateRenderResult();
        result.setSceneCode(sceneMeta.getSceneCode());
        result.setSceneName(sceneMeta.getSceneName());
        // 当前排障和运行时链路仍把 templateCode 视为“命中的业务场景编码”。
        // 阶段一先保持该字段继续存 sceneCode，避免扩大 trace、dispatch 等模块的改动面。
        result.setTemplateCode(sceneMeta.getSceneCode());

        NotifySceneTarget target = getActiveTargetConfig(normalizedSceneCode, normalizedTargetType);
        if (target == null) {
            result.setNotifyEnabled(false);
            result.addError("未找到启用通知目标配置，sceneCode=" + normalizedSceneCode + ", targetType=" + normalizedTargetType);
            return result;
        }

        Map<String, Object> actualVariables = variables == null ? Collections.emptyMap() : variables;
        result.setNotifyEnabled(true);
        result.setTemplateName(resolveTemplateName(targetMeta));
        result.setTitle(renderText(target.getTitleTemplate(), actualVariables));
        result.setSummary(renderText(target.getContentTemplate(), actualVariables));
        result.setRouteType(target.getRouteType());
        result.setRouteValue(renderText(target.getRouteValueTemplate(), actualVariables));
        return result;
    }

    /**
     * 查询某个场景目标下启用中的配置。
     *
     * @param sceneCode 场景编码
     * @param targetType 通知目标类型
     * @return 启用中的目标配置；不存在时返回 {@code null}
     */
    private NotifySceneTarget getActiveTargetConfig(String sceneCode, String targetType) {
        LambdaQueryWrapper<NotifySceneTarget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifySceneTarget::getSceneCode, sceneCode)
                .eq(NotifySceneTarget::getTargetType, targetType)
                .eq(NotifySceneTarget::getEnabled, 1)
                .orderByDesc(NotifySceneTarget::getId)
                .last("limit 2");

        List<NotifySceneTarget> targets = notifySceneTargetMapper.selectList(wrapper);
        if (targets == null || targets.isEmpty()) {
            return null;
        }
        if (targets.size() > 1) {
            log.warn("Detected multiple active notify scene targets, sceneCode={}, targetType={}", sceneCode, targetType);
        }
        return targets.get(0);
    }

    /**
     * 解析运行时模板名称。
     *
     * @param targetMeta 目标元数据
     * @return 模板名称
     */
    private String resolveTemplateName(NotifySceneTargetMeta targetMeta) {
        return targetMeta == null ? null : targetMeta.getDefaultTemplateName();
    }

    /**
     * 渲染文本模板。
     *
     * <p>运行时缺变量时统一替换为空串，避免单个变量缺失直接打断整个通知事件消费。</p>
     *
     * @param template 模板文本
     * @param variables 模板变量
     * @return 渲染后的文本
     */
    private String renderText(String template, Map<String, Object> variables) {
        if (StrUtil.isBlank(template)) {
            return null;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object variableValue = variables.get(variableName);
            String replacement = variableValue == null ? "" : String.valueOf(variableValue);
            matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    /**
     * 按场景编码读取注册表场景并做必填校验。
     *
     * @param sceneCode 场景编码
     * @return 场景元数据
     */
    private NotifySceneMeta getRequiredScene(String sceneCode) {
        return notifySceneRegistry.getRequiredScene(normalizeRequiredField(sceneCode, "通知场景编码不能为空"));
    }

    /**
     * 规范化必填字符串。
     *
     * @param value 原始值
     * @param emptyMessage 为空时的异常文案
     * @return 规范化后的字符串
     */
    private String normalizeRequiredField(String value, String emptyMessage) {
        String normalizedValue = normalizeNullableField(value);
        if (normalizedValue == null) {
            throw new ServiceException(emptyMessage);
        }
        return normalizedValue;
    }

    /**
     * 规范化可空字符串。
     *
     * @param value 原始值
     * @return 去掉首尾空白后的值；为空时返回 {@code null}
     */
    private String normalizeNullableField(String value) {
        return StrUtil.trimToNull(value);
    }
}
