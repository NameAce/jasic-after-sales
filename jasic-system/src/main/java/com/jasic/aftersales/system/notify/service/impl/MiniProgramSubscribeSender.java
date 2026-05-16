package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.enums.WechatMiniProgramScene;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchResultCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.service.NotifyChannelSender;
import com.jasic.aftersales.system.notify.support.NotifyChannelSendContext;
import com.jasic.aftersales.system.notify.support.NotifyChannelSendResult;
import com.jasic.aftersales.system.notify.support.NotifyDispatchPayload;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
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
 * @author Codex
 * @date 2026/04/21
 */
@Service
public class MiniProgramSubscribeSender implements NotifyChannelSender {

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
     * @param channelType 参数
     * @return 处理结果
     */
    @Override
    public boolean supports(String channelType) {
        return NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode().equals(channelType);
    }

    /**
     * 发送小程序程序订阅发送。
     *
     * @param context 参数
     * @return 处理结果
     */
    @Override
    public NotifyChannelSendResult send(NotifyChannelSendContext context) {
        if (context == null || context.getDispatch() == null || context.getPayload() == null) {
            // 说明：执行该步骤以保证业务流程正确。
            return NotifyChannelSendResult.failed(
                    NotifyDispatchResultCodeEnum.FAILED_RENDER_ERROR.getCode(),
                    "Mini program send context is incomplete"
            );
        }
        // 调用getPayload方法，复用统一能力并保证业务规则一致。
        NotifyDispatchPayload payload = context.getPayload();
        // 调用getChannelConfig方法，复用统一能力并保证业务规则一致。
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
                // 调用getFieldMapping方法，复用统一能力并保证业务规则一致。
                ? Collections.emptyList() : channelConfig.getFieldMapping();
        if (fieldMapping.isEmpty()) {
            return NotifyChannelSendResult.skipped(
                    NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(),
                    "Mini program field mapping is missing"
            );
        }
        // 阶段二开始，同一 sceneCode 可以分流多个目标，因此 sender 不能再依赖旧模板编码后缀。
        // 这里优先按接收对象类型区分 B/C 端，再用 sceneCode 作为兜底兼容判断。
        WechatMiniProgramScene scene = resolveScene(payload.getSceneCode(), context.getDispatch().getReceiverType());
        if (scene == null) {
            return NotifyChannelSendResult.skipped(
                    NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(),
                    "Mini program scene is invalid"
            );
        }
        // 调用getVariables方法，复用统一能力并保证业务规则一致。
        Map<String, Object> variables = payload.getVariables() == null ? Collections.emptyMap() : payload.getVariables();
        String pagePath;
        JSONObject data;
        try {
            // 调用getPagePathTemplate方法，复用统一能力并保证业务规则一致。
            pagePath = render(channelConfig.getPagePathTemplate(), variables);
            // 调用buildData方法，复用统一能力并保证业务规则一致。
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
            // 调用success方法，复用统一能力并保证业务规则一致。
            NotifyChannelSendResult result = NotifyChannelSendResult.success();
            // 调用setResultCode方法，复用统一能力并保证业务规则一致。
            result.setResultCode("SUCCESS");
            // 调用setResultMessage方法，复用统一能力并保证业务规则一致。
            result.setResultMessage("Message sent");
            // 调用toJsonStr方法，复用统一能力并保证业务规则一致。
            result.setChannelResponseJson(JSONUtil.toJsonStr(data));
            return result;
        } catch (ServiceException ex) {
            // 调用getMessage方法，复用统一能力并保证业务规则一致。
            String message = StrUtil.blankToDefault(ex.getMessage(), "Mini program send failed");
            if (isUserNotSubscribed(message)) {
                NotifyChannelSendResult result = NotifyChannelSendResult.skipped(
                        NotifyDispatchResultCodeEnum.SKIPPED_USER_NOT_SUBSCRIBED.getCode(),
                        message
                );
                // 调用setChannelResponseJson方法，复用统一能力并保证业务规则一致。
                result.setChannelResponseJson(message);
                return result;
            }
            NotifyChannelSendResult result = NotifyChannelSendResult.failed(
                    NotifyDispatchResultCodeEnum.FAILED_CHANNEL_REQUEST.getCode(),
                    message
            );
            // 调用setChannelResponseJson方法，复用统一能力并保证业务规则一致。
            result.setChannelResponseJson(message);
            return result;
        } catch (Exception ex) {
            NotifyChannelSendResult result = NotifyChannelSendResult.failed(
                    NotifyDispatchResultCodeEnum.FAILED_CHANNEL_REQUEST.getCode(),
                    StrUtil.blankToDefault(ex.getMessage(), "Mini program send failed")
            );
            // 调用getSimpleName方法，复用统一能力并保证业务规则一致。
            result.setChannelResponseJson(ex.getClass().getSimpleName());
            return result;
        }
    }

    /**
     * 解析场景。
     *
     * @param sceneCode 通知场景编码
     * @param receiverType 接收对象类型
     * @return 处理结果
     */
    private WechatMiniProgramScene resolveScene(String sceneCode, String receiverType) {
        if (NotifyReceiverTypeEnum.REPAIRER.getCode().equals(receiverType)) {
            return WechatMiniProgramScene.B;
        }
        if (NotifyReceiverTypeEnum.CUSTOMER.getCode().equals(receiverType)) {
            return WechatMiniProgramScene.C;
        }
        if (NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode().equals(sceneCode)
                || StrUtil.endWithIgnoreCase(sceneCode, "_MP_C")) {
            return WechatMiniProgramScene.C;
        }
        if (StrUtil.endWithIgnoreCase(sceneCode, "_MP_B")) {
            return WechatMiniProgramScene.B;
        }
        return null;
    }

    /**
     * 构建数据。
     *
     * @param fieldMapping 参数
     * @param variables 参数
     * @return 处理结果
     */
    private JSONObject buildData(List<NotifyChannelFieldMappingDTO> fieldMapping, Map<String, Object> variables) {
        // 调用createObj方法，复用统一能力并保证业务规则一致。
        JSONObject data = JSONUtil.createObj();
        // 说明：执行该步骤以保证业务流程正确。
        for (NotifyChannelFieldMappingDTO item : fieldMapping) {
            if (item == null || StrUtil.isBlank(item.getField()) || StrUtil.isBlank(item.getValue())) {
                continue;
            }
            // 调用getValue方法，复用统一能力并保证业务规则一致。
            data.set(item.getField().trim(), render(item.getValue(), variables));
        }
        return data;
    }

    /**
     * 渲染小程序程序订阅发送。
     *
     * @param template 参数
     * @param variables 参数
     * @return 处理结果
     */
    private String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        // 调用matcher方法，复用统一能力并保证业务规则一致。
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        // 调用StringBuffer方法，复用统一能力并保证业务规则一致。
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            // 调用group方法，复用统一能力并保证业务规则一致。
            String variableName = matcher.group(1);
            // 调用get方法，复用统一能力并保证业务规则一致。
            Object value = variables.get(variableName);
            // 调用trim方法，复用统一能力并保证业务规则一致。
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value).trim()));
        }
        // 调用appendTail方法，复用统一能力并保证业务规则一致。
        matcher.appendTail(buffer);
        return buffer.toString().trim();
    }

    /**
     * 判断是否用户NotSubscribed。
     *
     * @param message 参数
     */
    private boolean isUserNotSubscribed(String message) {
        if (StrUtil.isBlank(message)) {
            return false;
        }
        return StrUtil.containsIgnoreCase(message, "43101")
                || StrUtil.containsIgnoreCase(message, "require subscribe")
                || StrUtil.containsIgnoreCase(message, "accept the msg")
                || StrUtil.contains(message, "用户拒绝接受消息")
                // 调用contains方法，复用统一能力并保证业务规则一致。
                || StrUtil.contains(message, "用户未订阅");
    }
}




