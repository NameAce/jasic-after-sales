package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.entity.NotifySceneTarget;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessage;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessageLog;
import com.jasic.aftersales.system.notify.domain.enums.NotifyActionTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchResultCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTodoStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTypeEnum;
import com.jasic.aftersales.system.notify.mapper.NotifySceneTargetMapper;
import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
import com.jasic.aftersales.system.notify.service.NotifyEventConsumeService;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.NotifyMessageLogService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.service.NotifyTemplateRenderService;
import com.jasic.aftersales.system.notify.service.support.NotifyEventHandler;
import com.jasic.aftersales.system.notify.service.support.NotifyEventHandlerRegistry;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.notify.support.NotifyDispatchPayload;
import com.jasic.aftersales.system.notify.support.NotifyEventExecutionContext;
import com.jasic.aftersales.system.notify.support.NotifyReceiverSnapshot;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
import com.jasic.aftersales.system.notify.support.NotifySceneTargetMeta;
import com.jasic.aftersales.system.notify.support.NotifyTemplateChannelConfig;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 通知事件消费服务。
 *
 * <p>该服务负责把单条通知事件展开成多个通知目标。
 * 站内目标直接落消息表，外部目标写入分发表，真实发送仍由 dispatch 链路异步执行。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Slf4j
@Service
public class NotifyEventConsumeServiceImpl implements NotifyEventConsumeService {

    /**
     * 超过最大重试次数后的死信说明。
     */
    private static final String DEAD_RETRY_EXCEEDED_MESSAGE = "通知事件超过最大重试次数，已转入死信";

    @Resource
    private NotifyEventService notifyEventService;

    @Resource
    private NotifyEventHandlerRegistry notifyEventHandlerRegistry;

    @Resource
    private NotifySceneTargetMapper notifySceneTargetMapper;

    @Resource
    private NotifySceneRegistry notifySceneRegistry;

    @Resource
    private NotifyTemplateRenderService notifyTemplateRenderService;

    @Resource
    private NotifyMessageService notifyMessageService;

    @Resource
    private NotifyMessageLogService notifyMessageLogService;

    @Resource
    private NotifyDispatchService notifyDispatchService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Value("${jasic.notify.event-retry-max-count:" + NotifyConstants.EVENT_RETRY_MAX_COUNT + "}")
    private int eventRetryMaxCount = NotifyConstants.EVENT_RETRY_MAX_COUNT;

    @Value("${jasic.notify.event-retry-delay-minutes:" + NotifyConstants.EVENT_RETRY_DELAY_MINUTES + "}")
    private long eventRetryDelayMinutes = NotifyConstants.EVENT_RETRY_DELAY_MINUTES;

    /**
     * {@inheritDoc}
     */
    @Override
    public int consumePendingEvents() {
        List<SysNotifyEvent> events = notifyEventService.listConsumableEvents(
                LocalDateTime.now(),
                NotifyConstants.EVENT_CONSUME_BATCH_SIZE
        );
        int successCount = 0;
        for (SysNotifyEvent event : events) {
            if (!notifyEventService.markProcessing(event.getId())) {
                continue;
            }
            try {
                transactionTemplate.execute(status -> {
                    consumeSingleEvent(event.getId());
                    return null;
                });
                successCount++;
            } catch (Exception ex) {
                log.error("消费通知事件失败。eventId={}, eventKey={}", event.getId(), event.getEventKey(), ex);
                markEventFailed(event.getId(), ex);
            }
        }
        return successCount;
    }

    /**
     * 消费单条已抢占事件。
     *
     * @param eventId 事件ID
     */
    private void consumeSingleEvent(Long eventId) {
        SysNotifyEvent event = getRequiredProcessingEvent(eventId);
        NotifyEventHandler handler = notifyEventHandlerRegistry.getRequiredHandler(event.getEventType());
        NotifyEventExecutionContext context = handler.buildExecutionContext(event);
        executeTargets(event, context);
        notifyEventService.markSuccess(event.getId());
    }

    /**
     * 统一执行当前事件下的多个通知目标。
     *
     * @param event 通知事件
     * @param context 事件执行上下文
     */
    private void executeTargets(SysNotifyEvent event, NotifyEventExecutionContext context) {
        if (context == null) {
            throw new ServiceException("通知事件执行上下文不能为空");
        }
        String sceneCode = normalizeRequiredField(resolveSceneCode(event, context), "通知场景编码不能为空");

        List<NotifySceneTarget> enabledTargets = listEnabledTargets(sceneCode);
        if (enabledTargets.isEmpty()) {
            log.info("通知事件未命中任何启用目标，直接结束消费。eventId={}, sceneCode={}", event.getId(), sceneCode);
            return;
        }

        for (NotifySceneTarget target : enabledTargets) {
            String targetType = normalizeRequiredField(target.getTargetType(), "通知目标类型不能为空");
            NotifySceneTargetMeta targetMeta = notifySceneRegistry.getRequiredTargetMeta(sceneCode, targetType);
            NotifyTemplateRenderResult renderResult = notifyTemplateRenderService.render(
                    sceneCode,
                    targetType,
                    context.getTemplateVariables()
            );

            if (!renderResult.isNotifyEnabled()) {
                log.warn("通知目标渲染被跳过。eventId={}, sceneCode={}, targetType={}, errors={}",
                        event.getId(), sceneCode, targetType, renderResult.getErrors());
                continue;
            }

            NotifyTypeEnum targetTypeEnum = NotifyTypeEnum.getByCode(targetType);
            if (targetTypeEnum == null) {
                throw new ServiceException("不支持的通知目标类型：" + targetType);
            }

            NotifyReceiverSnapshot receiverSnapshot = resolveReceiverSnapshot(context, targetMeta);
            if (targetTypeEnum.isInAppTarget()) {
                createInAppMessage(event, targetTypeEnum, receiverSnapshot, context, renderResult);
                continue;
            }

            if (targetMeta.isExternalTarget() || targetTypeEnum.isMiniProgramSubscribeTarget()) {
                createDispatch(event, targetMeta, target, receiverSnapshot, context, renderResult);
                continue;
            }

            throw new ServiceException("当前消费链路尚未覆盖该通知目标类型：" + targetType);
        }
    }

    /**
     * 创建站内消息或站内待办。
     *
     * @param event 通知事件
     * @param targetTypeEnum 目标类型
     * @param receiverSnapshot 接收人快照
     * @param context 事件执行上下文
     * @param renderResult 模板渲染结果
     */
    private void createInAppMessage(SysNotifyEvent event, NotifyTypeEnum targetTypeEnum,
                                    NotifyReceiverSnapshot receiverSnapshot, NotifyEventExecutionContext context,
                                    NotifyTemplateRenderResult renderResult) {
        if (notifyMessageService.getByEventIdAndTargetType(event.getId(), targetTypeEnum.getCode()) != null) {
            return;
        }
        if (receiverSnapshot == null) {
            throw new ServiceException("站内通知缺少接收人快照，targetType=" + targetTypeEnum.getCode());
        }
        if (receiverSnapshot.getReceiverId() == null) {
            throw new ServiceException("站内通知缺少接收人ID");
        }
        if (receiverSnapshot.getReceiverCompanyId() == null) {
            throw new ServiceException("站内通知缺少接收公司ID");
        }

        SysNotifyMessage message = new SysNotifyMessage();
        message.setEventId(event.getId());
        message.setSceneCode(renderResult.getSceneCode());
        message.setTargetType(targetTypeEnum.getCode());
        message.setMessageType(targetTypeEnum.getCode());
        message.setEventType(event.getEventType());
        message.setTemplateCode(renderResult.getTemplateCode());
        message.setBizType(event.getBizType());
        message.setBizId(event.getBizId());
        message.setBizNo(event.getBizNo());
        message.setReceiverId(receiverSnapshot.getReceiverId());
        message.setReceiverCompanyId(receiverSnapshot.getReceiverCompanyId());
        message.setReceiverName(receiverSnapshot.getReceiverName());
        message.setTitle(renderResult.getTitle());
        message.setSummary(renderResult.getSummary());
        message.setRouteType(renderResult.getRouteType());
        message.setRouteValue(renderResult.getRouteValue());
        message.setTodoStatus(NotifyTodoStatusEnum.PENDING.getCode());
        message.setExtJson(context.getMessageExtJson());

        Long messageId = notifyMessageService.createMessage(message);
        message.setId(messageId);
        notifyMessageLogService.createLog(buildCreateMessageLog(
                message,
                event.getOperatorId(),
                targetTypeEnum == NotifyTypeEnum.IN_APP_TODO ? "创建站内待办" : "创建站内消息"
        ));
    }

    /**
     * 创建外部分发任务。
     *
     * @param event 通知事件
     * @param targetMeta 目标元数据
     * @param target 目标配置
     * @param receiverSnapshot 接收人快照
     * @param context 事件执行上下文
     * @param renderResult 模板渲染结果
     */
    private void createDispatch(SysNotifyEvent event, NotifySceneTargetMeta targetMeta, NotifySceneTarget target,
                                NotifyReceiverSnapshot receiverSnapshot, NotifyEventExecutionContext context,
                                NotifyTemplateRenderResult renderResult) {
        if (receiverSnapshot == null) {
            throw new ServiceException("外部通知缺少接收人快照，targetType=" + target.getTargetType());
        }
        NotifyTemplateChannelConfig channelConfig = parseChannelConfig(target.getConfigJson());

        SysNotifyDispatch dispatch = new SysNotifyDispatch();
        dispatch.setEventId(event.getId());
        dispatch.setSceneCode(renderResult.getSceneCode());
        dispatch.setTargetType(target.getTargetType());
        dispatch.setTemplateCode(renderResult.getTemplateCode());
        dispatch.setChannelType(targetMeta.getChannelType());
        dispatch.setReceiverType(receiverSnapshot.getReceiverType());
        dispatch.setReceiverId(receiverSnapshot.getReceiverId());
        dispatch.setReceiverAddress(StrUtil.trimToNull(receiverSnapshot.getReceiverAddress()));
        dispatch.setBizType(event.getBizType());
        dispatch.setBizId(event.getBizId());
        dispatch.setBizNo(event.getBizNo());
        dispatch.setRetryCount(0);
        dispatch.setPayloadJson(buildDispatchPayload(targetMeta, target, renderResult, channelConfig, context));

        if (StrUtil.isBlank(dispatch.getReceiverAddress())) {
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_OPENID_MISSING.getCode());
            dispatch.setResultMessage("接收人缺少小程序 openid，无法创建订阅消息发送任务");
        } else if (!isValidMpChannelConfig(channelConfig)) {
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode());
            dispatch.setResultMessage("小程序通知目标配置不完整，无法创建订阅消息发送任务");
        } else {
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.PENDING.getCode());
        }

        notifyDispatchService.createDispatch(dispatch);
    }

    /**
     * 构建外部分发快照。
     *
     * @param targetMeta 目标元数据
     * @param target 目标配置
     * @param renderResult 模板渲染结果
     * @param channelConfig 渠道配置快照
     * @param context 事件执行上下文
     * @return 分发快照JSON
     */
    private String buildDispatchPayload(NotifySceneTargetMeta targetMeta, NotifySceneTarget target,
                                        NotifyTemplateRenderResult renderResult, NotifyTemplateChannelConfig channelConfig,
                                        NotifyEventExecutionContext context) {
        NotifyDispatchPayload payload = new NotifyDispatchPayload();
        payload.setSceneCode(renderResult.getSceneCode());
        payload.setSceneName(renderResult.getSceneName());
        payload.setTargetType(target.getTargetType());
        payload.setTemplateCode(renderResult.getTemplateCode());
        payload.setTemplateName(renderResult.getTemplateName());
        payload.setTitle(renderResult.getTitle());
        payload.setContent(renderResult.getSummary());
        payload.setRouteType(renderResult.getRouteType());
        payload.setRouteValue(renderResult.getRouteValue());
        payload.setChannelType(targetMeta.getChannelType());
        payload.setChannelEnabled(target.getEnabled());
        payload.setChannelConfig(channelConfig);
        payload.setVariables(context.getTemplateVariables() == null
                ? Collections.emptyMap()
                : context.getTemplateVariables());
        return JSONUtil.toJsonStr(payload);
    }

    /**
     * 查询指定场景下启用中的目标配置。
     *
     * @param sceneCode 场景编码
     * @return 启用目标列表
     */
    private List<NotifySceneTarget> listEnabledTargets(String sceneCode) {
        LambdaQueryWrapper<NotifySceneTarget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifySceneTarget::getSceneCode, sceneCode)
                .eq(NotifySceneTarget::getEnabled, 1)
                .orderByAsc(NotifySceneTarget::getId);
        return notifySceneTargetMapper.selectList(wrapper);
    }

    /**
     * 解析小程序通知目标配置。
     *
     * @param configJson 目标配置JSON
     * @return 渠道配置快照
     */
    private NotifyTemplateChannelConfig parseChannelConfig(String configJson) {
        if (StrUtil.isBlank(configJson)) {
            return null;
        }
        try {
            return JSONUtil.toBean(configJson, NotifyTemplateChannelConfig.class);
        } catch (Exception ex) {
            throw new ServiceException("小程序通知目标配置解析失败");
        }
    }

    /**
     * 校验小程序渠道配置是否完整。
     *
     * @param channelConfig 渠道配置快照
     * @return `true` 表示配置可用于发送
     */
    private boolean isValidMpChannelConfig(NotifyTemplateChannelConfig channelConfig) {
        return channelConfig != null
                && StrUtil.isNotBlank(channelConfig.getTemplateId())
                && StrUtil.isNotBlank(channelConfig.getPagePathTemplate())
                && channelConfig.getFieldMapping() != null
                && !channelConfig.getFieldMapping().isEmpty();
    }

    /**
     * 查询并校验处理中事件。
     *
     * @param eventId 事件ID
     * @return 处理中事件
     */
    private SysNotifyEvent getRequiredProcessingEvent(Long eventId) {
        SysNotifyEvent event = notifyEventService.getById(eventId);
        if (event == null) {
            throw new ServiceException("通知事件不存在");
        }
        if (!NotifyEventStatusEnum.PROCESSING.getCode().equals(event.getStatus())) {
            throw new ServiceException("通知事件当前不是处理中状态");
        }
        return event;
    }

    /**
     * 优先解析执行上下文中的场景编码。
     *
     * @param event 通知事件
     * @param context 执行上下文
     * @return 场景编码
     */
    private String resolveSceneCode(SysNotifyEvent event, NotifyEventExecutionContext context) {
        if (context != null && StrUtil.isNotBlank(context.getSceneCode())) {
            return context.getSceneCode();
        }
        return event == null ? null : event.getSceneCode();
    }

    /**
     * 按通知目标读取对应接收人快照。
     *
     * <p>同一场景下不同目标可能指向不同接收对象，
     * 消费层必须按 `targetMeta.receiverType` 精确选择。</p>
     *
     * @param context 事件执行上下文
     * @param targetMeta 目标元数据
     * @return 接收人快照；未命中时返回 {@code null}
     */
    private NotifyReceiverSnapshot resolveReceiverSnapshot(NotifyEventExecutionContext context,
                                                           NotifySceneTargetMeta targetMeta) {
        if (context == null || targetMeta == null) {
            return null;
        }
        return context.getReceiverSnapshot(targetMeta.getReceiverType());
    }

    /**
     * 统一回写事件失败结果。
     *
     * @param eventId 事件ID
     * @param ex 消费异常
     */
    private void markEventFailed(Long eventId, Exception ex) {
        SysNotifyEvent current = notifyEventService.getById(eventId);
        int currentRetryCount = current == null || current.getRetryCount() == null ? 0 : current.getRetryCount();
        int nextRetryCount = currentRetryCount + 1;
        String errorMessage = buildErrorMessage(ex);
        if (nextRetryCount >= eventRetryMaxCount) {
            notifyEventService.updateRetryInfo(eventId, nextRetryCount, null, errorMessage);
            notifyEventService.markDead(eventId, buildDeadErrorMessage(errorMessage));
            return;
        }
        notifyEventService.markFailed(
                eventId,
                nextRetryCount,
                LocalDateTime.now().plusMinutes(eventRetryDelayMinutes),
                errorMessage
        );
    }

    /**
     * 构建创建消息日志。
     *
     * @param message 消息快照
     * @param actionUserId 操作人ID
     * @param remark 日志备注
     * @return 消息日志
     */
    private SysNotifyMessageLog buildCreateMessageLog(SysNotifyMessage message, Long actionUserId, String remark) {
        SysNotifyMessageLog logEntity = new SysNotifyMessageLog();
        logEntity.setMessageId(message.getId());
        logEntity.setActionType(NotifyActionTypeEnum.CREATE.getCode());
        logEntity.setActionUserId(actionUserId);
        logEntity.setRemark(remark);
        logEntity.setSnapshotJson(JSONUtil.toJsonStr(message));
        return logEntity;
    }

    /**
     * 构建死信原因。
     *
     * @param errorMessage 最近一次失败原因
     * @return 死信原因
     */
    private String buildDeadErrorMessage(String errorMessage) {
        if (StrUtil.isBlank(errorMessage)) {
            return DEAD_RETRY_EXCEEDED_MESSAGE;
        }
        String message = DEAD_RETRY_EXCEEDED_MESSAGE + "：" + errorMessage;
        return message.length() > 500 ? StrUtil.sub(message, 0, 500) : message;
    }

    /**
     * 构建失败原因文本。
     *
     * @param ex 消费异常
     * @return 裁剪后的失败原因
     */
    private String buildErrorMessage(Exception ex) {
        String message = ex == null ? null : StrUtil.trim(ex.getMessage());
        if (StrUtil.isBlank(message) && ex != null) {
            message = ex.getClass().getSimpleName();
        }
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? StrUtil.sub(message, 0, 500) : message;
    }

    /**
     * 规范化必填字符串。
     *
     * @param value 原始值
     * @param emptyMessage 为空时的异常文案
     * @return 规范化后的字符串
     */
    private String normalizeRequiredField(String value, String emptyMessage) {
        String normalizedValue = StrUtil.trimToNull(value);
        if (normalizedValue == null) {
            throw new ServiceException(emptyMessage);
        }
        return normalizedValue;
    }
}
