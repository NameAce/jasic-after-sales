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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mini program subscribe message sender.
 *
 * @author Zoro
 * @date 2026/04/21
 */
@Service
public class MiniProgramSubscribeSender implements NotifyChannelSender {

    /**PLACEHOLDER_PATTERN 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    /**
     * 微信小程序程序服务依赖。
     */
    @Resource
    private WechatMiniProgramService wechatMiniProgramService;

    /**
     * 处理supports业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param channelType channelType，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    @Override
    public boolean supports(String channelType) {
        return NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode().equals(channelType);
    }

    /**
     * 发送小程序程序订阅发送。
     *
     * @param context 上下文对象，承载当前操作人、公司和数据范围。
     * @return 业务处理结果
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
        // 小程序归属端必须来自场景目标配置，避免再次用接收人类型或旧 sceneCode 后缀猜测 B/C 端。
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
     * 解析场景。
     *
     * @param channelScene 配置中的小程序场景
     * @return 业务处理结果
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
     * 构建数据。
     *
     * @param fieldMapping 业务映射数据，用于批量组装或快速查找。
     * @param variables variables，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private JSONObject buildData(List<NotifyChannelFieldMappingDTO> fieldMapping, Map<String, Object> variables) {
        JSONObject data = JSONUtil.createObj();
        for (NotifyChannelFieldMappingDTO item : fieldMapping) {
            if (item == null || StrUtil.isBlank(item.getField()) || StrUtil.isBlank(item.getValue())) {
                continue;
            }
            data.set(item.getField().trim(), render(item.getValue(), variables));
        }
        return data;
    }

    /**
     * 渲染小程序程序订阅发送。
     *
     * @param template template，当前业务处理所需的输入值。
     * @param variables variables，当前业务处理所需的输入值。
     * @return 业务处理结果
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
     * 判断是否用户NotSubscribed。
     *
     * @param message 提示或消息文本，用于异常返回或通知内容。
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




