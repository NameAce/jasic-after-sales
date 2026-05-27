package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.enums.WechatMiniProgramScene;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelSceneEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchResultCodeEnum;
import com.jasic.aftersales.system.notify.service.NotifyChannelSender;
import com.jasic.aftersales.system.notify.support.NotifyChannelSendContext;
import com.jasic.aftersales.system.notify.support.NotifyChannelSendResult;
import com.jasic.aftersales.system.notify.support.NotifyDispatchPayload;
import com.jasic.aftersales.system.notify.support.NotifyTemplateChannelConfig;
import com.jasic.aftersales.system.service.WechatMiniProgramService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 小程序订阅消息发送器。
 *
 * <p>当前发送器负责把通知中心生成的模板变量渲染成微信订阅消息 payload，
 * 并统一处理小程序场景选择、模板字段格式兼容和微信发送异常转换。</p>
 *
 * @author Zoro
 * @date 2026/04/21
 */
@Service
public class MiniProgramSubscribeSender implements NotifyChannelSender {

    /** 模板占位符格式，统一解析 `${variable}` 形式的变量引用。 */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");
    /** 微信 time 类字段统一命名为 `time + 序号`，发送前需要做专项格式兼容。 */
    private static final Pattern WECHAT_TIME_FIELD_PATTERN = Pattern.compile("^time\\d+$");
    /** 企业微信截图已确认目标格式为 `yyyy-MM-dd HH:mm:ss`，这里统一按该格式输出。 */
    private static final DateTimeFormatter WECHAT_TIME_OUTPUT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 兼容数据库常见的“年月日 时:分:秒”字符串。 */
    private static final DateTimeFormatter COMMON_DATE_TIME_SECONDS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 兼容数据库常见的“年月日 时:分”字符串；秒数缺失时默认补 `00`。 */
    private static final DateTimeFormatter COMMON_DATE_TIME_MINUTES_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 微信小程序服务，负责真正调用微信订阅消息发送接口。 */
    @Resource
    private WechatMiniProgramService wechatMiniProgramService;

    /**
     * 判断当前 sender 是否支持指定渠道。
     *
     * @param channelType 渠道类型编码
     * @return 支持返回 true，否则返回 false
     */
    @Override
    public boolean supports(String channelType) {
        return NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode().equals(channelType);
    }

    /**
     * 发送小程序订阅消息。
     *
     * @param context 发送上下文，包含派发记录、渠道配置和模板变量
     * @return 发送结果
     */
    @Override
    public NotifyChannelSendResult send(NotifyChannelSendContext context) {
        if (context == null || context.getDispatch() == null || context.getPayload() == null) {
            return NotifyChannelSendResult.failed(
                    NotifyDispatchResultCodeEnum.FAILED_RENDER_ERROR.getCode(),
                    "Mini program send context is incomplete"
            );
        }
        NotifyDispatchPayload payload = context.getPayload();
        NotifyTemplateChannelConfig channelConfig = payload.getChannelConfig();
        if (channelConfig == null) {
            return NotifyChannelSendResult.skipped(
                    NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(),
                    "Mini program channel config is missing"
            );
        }
        if (StrUtil.isBlank(context.getDispatch().getReceiverAddress())) {
            return NotifyChannelSendResult.skipped(
                    NotifyDispatchResultCodeEnum.SKIPPED_OPENID_MISSING.getCode(),
                    "Customer openid is missing"
            );
        }
        if (StrUtil.isBlank(channelConfig.getTemplateId())) {
            return NotifyChannelSendResult.skipped(
                    NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(),
                    "Mini program templateId is missing"
            );
        }
        if (StrUtil.isBlank(channelConfig.getPagePathTemplate())) {
            return NotifyChannelSendResult.skipped(
                    NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(),
                    "Mini program pagePathTemplate is missing"
            );
        }
        List<NotifyChannelFieldMappingDTO> fieldMapping = channelConfig.getFieldMapping() == null
                ? Collections.emptyList() : channelConfig.getFieldMapping();
        if (fieldMapping.isEmpty()) {
            return NotifyChannelSendResult.skipped(
                    NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(),
                    "Mini program field mapping is missing"
            );
        }

        // 小程序归属端必须来自场景目标配置，避免再根据接收对象类型或 sceneCode 后缀猜测 B/C 端。
        WechatMiniProgramScene scene = resolveScene(channelConfig.getChannelScene());
        if (scene == null) {
            return NotifyChannelSendResult.skipped(
                    NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(),
                    "Mini program scene is invalid"
            );
        }

        Map<String, Object> variables = payload.getVariables() == null ? Collections.emptyMap() : payload.getVariables();
        String pagePath;
        JSONObject data;
        try {
            pagePath = render(channelConfig.getPagePathTemplate(), variables);
            data = buildData(fieldMapping, variables);
        } catch (Exception ex) {
            return NotifyChannelSendResult.failed(
                    NotifyDispatchResultCodeEnum.FAILED_RENDER_ERROR.getCode(),
                    ex.getMessage()
            );
        }

        try {
            wechatMiniProgramService.sendSubscribeMessage(
                    scene,
                    context.getDispatch().getReceiverAddress(),
                    channelConfig.getTemplateId(),
                    pagePath,
                    data
            );
            NotifyChannelSendResult result = NotifyChannelSendResult.success();
            result.setResultCode("SUCCESS");
            result.setResultMessage("Message sent");
            result.setChannelResponseJson(JSONUtil.toJsonStr(data));
            return result;
        } catch (ServiceException ex) {
            String message = StrUtil.blankToDefault(ex.getMessage(), "Mini program send failed");
            if (isUserNotSubscribed(message)) {
                NotifyChannelSendResult result = NotifyChannelSendResult.skipped(
                        NotifyDispatchResultCodeEnum.SKIPPED_USER_NOT_SUBSCRIBED.getCode(),
                        message
                );
                result.setChannelResponseJson(message);
                return result;
            }
            NotifyChannelSendResult result = NotifyChannelSendResult.failed(
                    NotifyDispatchResultCodeEnum.FAILED_CHANNEL_REQUEST.getCode(),
                    message
            );
            result.setChannelResponseJson(message);
            return result;
        } catch (Exception ex) {
            NotifyChannelSendResult result = NotifyChannelSendResult.failed(
                    NotifyDispatchResultCodeEnum.FAILED_CHANNEL_REQUEST.getCode(),
                    StrUtil.blankToDefault(ex.getMessage(), "Mini program send failed")
            );
            result.setChannelResponseJson(ex.getClass().getSimpleName());
            return result;
        }
    }

    /**
     * 解析小程序归属场景。
     *
     * @param channelScene 配置中的小程序场景编码
     * @return 解析后的微信小程序场景；无法识别时返回 null
     */
    private WechatMiniProgramScene resolveScene(String channelScene) {
        NotifyChannelSceneEnum configuredScene = NotifyChannelSceneEnum.getByCode(channelScene);
        if (configuredScene != null) {
            return NotifyChannelSceneEnum.B.equals(configuredScene)
                    ? WechatMiniProgramScene.B
                    : WechatMiniProgramScene.C;
        }
        return null;
    }

    /**
     * 组装微信订阅消息 data 数据。
     *
     * @param fieldMapping 字段映射配置
     * @param variables 模板变量
     * @return 微信订阅消息 data
     */
    private JSONObject buildData(List<NotifyChannelFieldMappingDTO> fieldMapping, Map<String, Object> variables) {
        JSONObject data = JSONUtil.createObj();
        for (NotifyChannelFieldMappingDTO item : fieldMapping) {
            if (item == null || StrUtil.isBlank(item.getField()) || StrUtil.isBlank(item.getValue())) {
                continue;
            }
            String fieldName = item.getField().trim();
            String renderedValue = render(item.getValue(), variables);
            // 微信会按模板字段类型严格校验内容格式，这里在下发前集中做 time 字段兼容，
            // 避免 LocalDateTime 默认字符串 `2026-05-27T14:30:45` 被微信判定为非法值。
            data.set(fieldName, normalizeWechatFieldValue(fieldName, renderedValue));
        }
        return data;
    }

    /**
     * 按模板字段类型兜底格式，避免每个通知场景重复处理相同的微信格式约束。
     *
     * @param fieldName 微信模板字段名
     * @param renderedValue 模板渲染后的值
     * @return 兜底格式化后的字段值
     */
    private String normalizeWechatFieldValue(String fieldName, String renderedValue) {
        if (StrUtil.isBlank(fieldName) || StrUtil.isBlank(renderedValue)) {
            return renderedValue;
        }
        if (WECHAT_TIME_FIELD_PATTERN.matcher(fieldName).matches()) {
            return normalizeWechatTimeValue(renderedValue);
        }
        return renderedValue;
    }

    /**
     * 把项目里常见时间输出转换成微信侧确认可接受的完整时间格式。
     *
     * @param value 原始时间字符串
     * @return 转换后的时间字符串；无法识别时保留原值，避免误伤其它文本
     */
    private String normalizeWechatTimeValue(String value) {
        String trimmedValue = StrUtil.trim(value);
        if (StrUtil.isBlank(trimmedValue)) {
            return trimmedValue;
        }
        if (trimmedValue.matches("^\\d{1,2}:\\d{2}(~\\d{1,2}:\\d{2})?$")) {
            return trimmedValue;
        }
        LocalDateTime parsedValue = parseLocalDateTime(trimmedValue);
        if (parsedValue == null) {
            return trimmedValue;
        }
        return parsedValue.format(WECHAT_TIME_OUTPUT_FORMATTER);
    }

    /**
     * 兼容当前通知链路最常见的时间字符串格式。
     *
     * @param value 时间字符串
     * @return 解析成功返回时间对象，否则返回 null
     */
    private LocalDateTime parseLocalDateTime(String value) {
        DateTimeFormatter[] supportedFormatters = new DateTimeFormatter[]{
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                COMMON_DATE_TIME_SECONDS_FORMATTER,
                COMMON_DATE_TIME_MINUTES_FORMATTER
        };
        for (DateTimeFormatter formatter : supportedFormatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 继续尝试后续兼容格式，避免单一格式不匹配直接放弃时间兜底。
            }
        }
        return null;
    }

    /**
     * 渲染模板字符串。
     *
     * @param template 模板文本
     * @param variables 模板变量
     * @return 渲染后的文本
     */
    private String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value).trim()));
        }
        matcher.appendTail(buffer);
        return buffer.toString().trim();
    }

    /**
     * 判断是否属于用户未订阅场景。
     *
     * @param message 异常消息
     * @return 用户未订阅返回 true，否则返回 false
     */
    private boolean isUserNotSubscribed(String message) {
        if (StrUtil.isBlank(message)) {
            return false;
        }
        return StrUtil.containsIgnoreCase(message, "43101")
                || StrUtil.containsIgnoreCase(message, "require subscribe")
                || StrUtil.containsIgnoreCase(message, "accept the msg")
                || StrUtil.contains(message, "用户拒绝接受消息")
                || StrUtil.contains(message, "用户未订阅");
    }
}
