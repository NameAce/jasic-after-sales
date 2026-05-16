package com.jasic.aftersales.system.notify.service.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyEvaluationInviteEventDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchResultCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.service.NotifyChannelConfigService;
import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
import com.jasic.aftersales.system.notify.service.NotifyTemplateRenderService;
import com.jasic.aftersales.system.notify.support.NotifyDispatchPayload;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import com.jasic.aftersales.system.notify.support.NotifyTemplateChannelConfig;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 客户评价邀请通知事件处理器。
 *
 * <p>负责消费 `WORK_ORDER_EVALUATION_INVITE` 事件，并根据模板和渠道配置生成小程序分发任务。
 * 当前处理器只负责写入 `sys_notify_dispatch`，不直接调用微信发送，
 * 真实发送仍由后续分发发送任务统一处理。</p>
 *
 * @author Codex
 * @date 2026/05/14
 */
@Component
public class WorkOrderEvaluationInviteNotifyEventHandler implements NotifyEventHandler {

    @Resource
    private NotifyTemplateRenderService notifyTemplateRenderService;

    @Resource
    private NotifyChannelConfigService notifyChannelConfigService;

    @Resource
    private NotifyDispatchService notifyDispatchService;

    /**
     * 判断是否支持客户评价邀请事件。
     *
     * @param eventType 事件类型编码
     * @return `true` 表示支持
     */
    @Override
    public boolean supports(String eventType) {
        return NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode().equals(eventType);
    }

    /**
     * 处理客户评价邀请通知事件。
     *
     * <p>处理流程包括：解析评价快照、读取模板开关与渠道配置，
     * 然后按渠道生成待发送或跳过的分发任务。</p>
     *
     * @param event 已抢占为 `PROCESSING` 的通知事件
     */
    @Override
    public void handle(SysNotifyEvent event) {
        NotifyEvaluationInviteEventDTO payload = parseEvaluationInvitePayload(event);
        Map<String, Object> templateVariables = buildEvaluationVariables(payload);
        NotifyTemplateRenderResult renderResult = notifyTemplateRenderService.render(
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode(),
                templateVariables
        );

        // 模板停用时保留一条可解释的 SKIPPED 记录，避免事件消费成功但排障时完全看不到原因。
        if (!renderResult.isNotifyEnabled()) {
            createEvaluationDispatch(event, payload, null, renderResult, templateVariables,
                    NotifyDispatchStatusEnum.SKIPPED.getCode(),
                    NotifyDispatchResultCodeEnum.SKIPPED_TEMPLATE_DISABLED.getCode(),
                    "未找到启用通知模板，sceneCode="
                            + NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode());
            return;
        }

        // 渠道侧改为直接按 sceneCode 读取，使 dispatch 与模板配置页引用同一条场景配置。
        List<NotifyTemplateChannelVO> channels = notifyChannelConfigService.listRuntimeChannelConfigs(
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode()
        );
        if (channels.isEmpty()) {
            boolean hasConfiguredChannels = notifyChannelConfigService.hasRuntimeChannelConfigs(
                    NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode()
            );
            // 渠道停用时尽量带上一条停用渠道快照，排障页才能同时展示渠道状态和跳过原因。
            NotifyTemplateChannelVO skippedChannel = hasConfiguredChannels ? resolveFirstConfiguredChannel() : null;
            createEvaluationDispatch(event, payload, skippedChannel, renderResult, templateVariables,
                    NotifyDispatchStatusEnum.SKIPPED.getCode(),
                    hasConfiguredChannels
                            ? NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_DISABLED.getCode()
                            : NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(),
                    hasConfiguredChannels
                            ? "模板已启用，但小程序渠道均已停用"
                            : "模板已启用，但未配置可用小程序渠道");
            return;
        }
        // 运行时渠道服务已经只返回启用记录，handler 这里只负责为每个可发送渠道生成 dispatch。
        for (NotifyTemplateChannelVO channel : channels) {
            createEvaluationDispatch(event, payload, channel, renderResult, templateVariables,
                    null, null, null);
        }
    }

    /**
     * 查询评价邀请场景的第一条渠道配置快照。
     *
     * <p>该方法只在运行时没有可发送渠道时使用，用于区分渠道停用和渠道缺失。
     * 查询失败或没有记录时返回 {@code null}，不影响跳过记录的落库。</p>
     *
     * @return 渠道配置快照；不存在时返回 {@code null}
     */
    private NotifyTemplateChannelVO resolveFirstConfiguredChannel() {
        List<NotifyTemplateChannelVO> channelConfigs = notifyChannelConfigService.listChannelConfigs(
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode()
        );
        return channelConfigs == null || channelConfigs.isEmpty() ? null : channelConfigs.get(0);
    }

    /**
     * 解析并校验评价邀请事件快照。
     *
     * @param event 通知事件
     * @return 解析后的评价邀请快照
     */
    private NotifyEvaluationInviteEventDTO parseEvaluationInvitePayload(SysNotifyEvent event) {
        if (StrUtil.isBlank(event.getPayloadJson())) {
            throw new ServiceException("Notify event payload cannot be blank");
        }
        NotifyEvaluationInviteEventDTO payload;
        try {
            payload = JSONUtil.toBean(event.getPayloadJson(), NotifyEvaluationInviteEventDTO.class);
        } catch (Exception ex) {
            throw new ServiceException("Notify evaluation invite payload parse failed");
        }
        if (payload == null) {
            throw new ServiceException("Notify evaluation invite payload parse result is null");
        }
        if (payload.getWorkOrderId() == null) {
            throw new ServiceException("Notify evaluation invite payload missing workOrderId");
        }
        if (!Objects.equals(payload.getWorkOrderId(), event.getBizId())) {
            throw new ServiceException("Notify evaluation invite payload workOrderId mismatch");
        }
        return payload;
    }

    /**
     * 创建评价邀请分发任务。
     *
     * <p>该方法只负责落库分发任务，不负责真实发送。
     * 如果模板停用、渠道停用、渠道配置不完整或客户缺少 openid，
     * 都要明确落一条 `SKIPPED` 记录，便于后续排障解释。</p>
     *
     * @param event 通知事件
     * @param payload 评价邀请快照
     * @param channel 渠道配置
     * @param renderResult 模板渲染结果；模板停用时仍携带 sceneCode 和场景名用于排障
     * @param templateVariables 事件快照转出的模板变量
     * @param forcedStatus 强制状态
     * @param forcedResultCode 强制结果码
     * @param forcedResultMessage 强制结果说明
     */
    private void createEvaluationDispatch(SysNotifyEvent event, NotifyEvaluationInviteEventDTO payload,
                                          NotifyTemplateChannelVO channel, NotifyTemplateRenderResult renderResult,
                                          Map<String, Object> templateVariables,
                                          String forcedStatus, String forcedResultCode, String forcedResultMessage) {
        String channelType = channel == null ? NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode() : channel.getChannelType();
        SysNotifyDispatch dispatch = buildEvaluationDispatch(event, payload, channel, channelType, renderResult,
                templateVariables);
        // 所有外部分发记录都固化 payload，包括跳过记录，保证排障页能看到实际 sceneCode、模板和渠道快照。
        dispatch.setPayloadJson(buildEvaluationDispatchPayload(renderResult, channelType, buildChannelConfig(channel),
                channel, templateVariables));
        if (forcedStatus != null) {
            dispatch.setDispatchStatus(forcedStatus);
            dispatch.setResultCode(forcedResultCode);
            dispatch.setResultMessage(forcedResultMessage);
            notifyDispatchService.createDispatch(dispatch);
            return;
        }
        NotifyTemplateChannelConfig config = buildChannelConfig(channel);
        // 发送载荷在消费阶段提前固化，保证后续重试时仍按事件发生时的快照发送。
        dispatch.setPayloadJson(buildEvaluationDispatchPayload(renderResult, channelType, config, channel,
                templateVariables));
        if (StrUtil.isBlank(dispatch.getReceiverAddress())) {
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_OPENID_MISSING.getCode());
            dispatch.setResultMessage("客户缺少 openid，无法发送评价邀请");
        } else if (config == null
                || StrUtil.isBlank(config.getTemplateId())
                || StrUtil.isBlank(config.getPagePathTemplate())
                || config.getFieldMapping() == null
                || config.getFieldMapping().isEmpty()) {
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode());
            dispatch.setResultMessage("小程序渠道配置不完整，无法构建订阅消息载荷");
        } else {
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.PENDING.getCode());
        }
        notifyDispatchService.createDispatch(dispatch);
    }

    /**
     * 构建评价邀请分发实体。
     *
     * @param event 通知事件
     * @param payload 评价邀请快照
     * @param channel 渠道配置
     * @param channelType 渠道类型
     * @param renderResult 模板渲染结果
     * @param templateVariables 事件快照转出的模板变量
     * @return 分发实体
     */
    private SysNotifyDispatch buildEvaluationDispatch(SysNotifyEvent event, NotifyEvaluationInviteEventDTO payload,
                                                      NotifyTemplateChannelVO channel, String channelType,
                                                      NotifyTemplateRenderResult renderResult,
                                                      Map<String, Object> templateVariables) {
        SysNotifyDispatch dispatch = new SysNotifyDispatch();
        dispatch.setEventId(event.getId());
        // 当前阶段运行时主体表仍沿用 template_code 字段名，这里先把实际命中的 sceneCode 写入该字段，
        // 保证后续发送、重试和排障都能追到具体通知场景。
        dispatch.setTemplateCode(resolveSceneCode(renderResult, channel));
        dispatch.setChannelType(channelType);
        dispatch.setReceiverType(NotifyReceiverTypeEnum.CUSTOMER.getCode());
        dispatch.setReceiverId(payload.getCustomerId());
        dispatch.setReceiverAddress(StrUtil.trimToNull(payload.getCustomerOpenid()));
        dispatch.setBizType(event.getBizType());
        dispatch.setBizId(event.getBizId());
        dispatch.setBizNo(event.getBizNo());
        dispatch.setRetryCount(0);
        // 兼容现有行为：有渠道配置时先写入一次载荷，后续如需按最新 config 固化会在上层覆盖。
        if (channel != null) {
            dispatch.setPayloadJson(buildEvaluationDispatchPayload(
                    renderResult,
                    channelType,
                    buildChannelConfig(channel),
                    channel,
                    templateVariables
            ));
        }
        return dispatch;
    }

    /**
     * 解析本次分发实际使用的通知场景编码。
     *
     * <p>Phase 2 后外部分发不再使用旧模板组合字段，优先从渲染结果读取 sceneCode。
     * 测试替身或历史调用未回填 sceneCode 时，才回退到 templateCode 或渠道上的场景编码，避免空值写入。</p>
     *
     * @param renderResult 模板渲染结果
     * @param channel 渠道配置
     * @return 实际通知场景编码
     */
    private String resolveSceneCode(NotifyTemplateRenderResult renderResult, NotifyTemplateChannelVO channel) {
        if (renderResult != null && StrUtil.isNotBlank(renderResult.getSceneCode())) {
            return renderResult.getSceneCode();
        }
        if (renderResult != null && StrUtil.isNotBlank(renderResult.getTemplateCode())) {
            return renderResult.getTemplateCode();
        }
        return channel == null ? NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode() : channel.getSceneCode();
    }

    /**
     * 将渠道视图对象转成发送配置快照。
     *
     * @param channel 渠道配置视图
     * @return 发送配置快照
     */
    private NotifyTemplateChannelConfig buildChannelConfig(NotifyTemplateChannelVO channel) {
        if (channel == null) {
            return null;
        }
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setTemplateId(channel.getTemplateId());
        config.setPagePathTemplate(channel.getPagePathTemplate());
        config.setFieldMapping(channel.getFieldMapping());
        return config;
    }

    /**
     * 构建评价邀请发送载荷。
     *
     * @param renderResult 模板渲染结果
     * @param channelType 渠道类型
     * @param channelConfig 渠道配置快照
     * @param channel 渠道配置视图
     * @param templateVariables 评价邀请模板变量
     * @return 分发载荷 JSON
     */
    private String buildEvaluationDispatchPayload(NotifyTemplateRenderResult renderResult, String channelType,
                                                  NotifyTemplateChannelConfig channelConfig,
                                                  NotifyTemplateChannelVO channel,
                                                  Map<String, Object> templateVariables) {
        NotifyDispatchPayload dispatchPayload = new NotifyDispatchPayload();
        dispatchPayload.setSceneCode(resolveSceneCode(renderResult, channel));
        dispatchPayload.setSceneName(renderResult == null ? null : renderResult.getSceneName());
        dispatchPayload.setTemplateCode(resolveSceneCode(renderResult, channel));
        dispatchPayload.setTemplateName(renderResult == null ? null : renderResult.getTemplateName());
        dispatchPayload.setTitle(renderResult == null ? null : renderResult.getTitle());
        dispatchPayload.setContent(renderResult == null ? null : renderResult.getSummary());
        dispatchPayload.setRouteType(renderResult == null ? null : renderResult.getRouteType());
        dispatchPayload.setRouteValue(renderResult == null ? null : renderResult.getRouteValue());
        dispatchPayload.setChannelType(channelType);
        dispatchPayload.setChannelEnabled(channel == null ? null : channel.getChannelEnabled());
        dispatchPayload.setChannelConfig(channelConfig);
        dispatchPayload.setVariables(templateVariables);
        return JSONUtil.toJsonStr(dispatchPayload);
    }

    /**
     * 组装评价邀请模板变量。
     *
     * @param payload 评价邀请快照
     * @return 模板变量
     */
    private Map<String, Object> buildEvaluationVariables(NotifyEvaluationInviteEventDTO payload) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("workOrderId", payload.getWorkOrderId());
        variables.put("orderNo", payload.getOrderNo());
        variables.put("customerId", payload.getCustomerId());
        variables.put("customerMobile", payload.getCustomerMobile());
        variables.put("customerOpenid", payload.getCustomerOpenid());
        variables.put("companyId", payload.getCompanyId());
        variables.put("companyName", payload.getCompanyName());
        variables.put("closedTime", payload.getClosedTime());
        return variables;
    }
}

