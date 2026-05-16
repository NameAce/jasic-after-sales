package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyTemplate;
import com.jasic.aftersales.system.notify.mapper.SysNotifyTemplateMapper;
import com.jasic.aftersales.system.notify.service.NotifyTemplateRenderService;
import com.jasic.aftersales.system.notify.support.NotifySceneMeta;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
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
 * <p>该实现只服务事件消费链路，根据 `sceneCode` 命中当前启用模板并完成文本渲染。
 * 它不负责后台模板维护、渠道配置和消息落库，也不再保留历史组合字段查询逻辑。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Slf4j
@Service
public class NotifyTemplateRenderServiceImpl implements NotifyTemplateRenderService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    @Resource
    private SysNotifyTemplateMapper sysNotifyTemplateMapper;

    @Resource
    private NotifySceneRegistry notifySceneRegistry;

    /**
     * 按通知场景渲染当前启用模板。
     *
     * @param sceneCode 通知场景编码
     * @param variables 模板变量
     * @return 渲染结果
     */
    @Override
    public NotifyTemplateRenderResult render(String sceneCode, Map<String, Object> variables) {
        String normalizedSceneCode = normalizeRequiredField(sceneCode, "通知场景编码不能为空");
        NotifySceneMeta sceneMeta = notifySceneRegistry.getRequiredScene(normalizedSceneCode);
        NotifyTemplateRenderResult result = new NotifyTemplateRenderResult();
        // 先固化场景元数据，即使模板缺失或停用，后续跳过记录也能展示业务场景名称。
        result.setSceneCode(sceneMeta.getSceneCode());
        result.setSceneName(sceneMeta.getSceneName());
        // 运行时结果对象当前仍沿用 templateCode 字段对外传递模板身份，这里先回填 sceneCode，
        // 避免模板缺失时后续日志和分发链路完全丢失场景定位信息。
        result.setTemplateCode(normalizedSceneCode);

        SysNotifyTemplate template = getActiveTemplateBySceneCode(normalizedSceneCode);
        if (template == null) {
            result.setNotifyEnabled(false);
            result.addError("未找到启用通知模板，sceneCode=" + normalizedSceneCode);
            return result;
        }

        Map<String, Object> actualVariables = variables == null ? Collections.emptyMap() : variables;
        result.setNotifyEnabled(true);
        result.setTemplateCode(template.getSceneCode());
        result.setTemplateName(template.getTemplateName());
        result.setTitle(renderText(template.getTitleTemplate(), actualVariables));
        // 当前消息中心主体链路仍使用 summary 承载内容模板，因此这里继续复用旧字段名返回渲染内容。
        result.setSummary(renderText(template.getContentTemplate(), actualVariables));
        result.setRouteType(template.getRouteType());
        result.setRouteValue(renderText(template.getRouteValueTemplate(), actualVariables));
        return result;
    }

    /**
     * 按通知场景查询启用模板。
     *
     * <p>模板表理论上已通过唯一键保证 `sceneCode` 唯一，
     * 这里仍读取两条并记录告警，避免脏数据直接卡死事件消费链路。</p>
     *
     * @param sceneCode 通知场景编码
     * @return 启用模板；不存在时返回 {@code null}
     */
    private SysNotifyTemplate getActiveTemplateBySceneCode(String sceneCode) {
        LambdaQueryWrapper<SysNotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyTemplate::getSceneCode, sceneCode)
                .eq(SysNotifyTemplate::getStatus, 1)
                .orderByDesc(SysNotifyTemplate::getId)
                .last("limit 2");

        List<SysNotifyTemplate> templates = sysNotifyTemplateMapper.selectList(wrapper);
        if (templates == null || templates.isEmpty()) {
            return null;
        }
        if (templates.size() > 1) {
            log.warn("Detected multiple active notify templates for sceneCode={}", sceneCode);
        }
        return templates.get(0);
    }

    /**
     * 渲染文本模板。
     *
     * <p>运行时缺变量时统一回写空串，避免单个变量缺失直接打断整个通知事件的消费。
     * 缺失变量的具体影响仍可通过渲染结果和最终通知内容排查。</p>
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
     * 规范化必填字符串。
     *
     * @param value 原值
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
     * @param value 原值
     * @return 去空白后的值；为空时返回 {@code null}
     */
    private String normalizeNullableField(String value) {
        return StrUtil.trimToNull(value);
    }
}
