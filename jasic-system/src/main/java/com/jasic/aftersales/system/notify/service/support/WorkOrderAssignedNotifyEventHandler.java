package com.jasic.aftersales.system.notify.service.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessage;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessageLog;
import com.jasic.aftersales.system.notify.domain.enums.NotifyActionTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyInvalidReasonEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTodoStatusEnum;
import com.jasic.aftersales.system.notify.service.NotifyMessageLogService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.service.NotifyTemplateRenderService;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工单派单通知事件处理器。
 *
 * <p>负责消费 `WORK_ORDER_ASSIGNED` 事件，并根据事件快照生成维修员站内待办。
 * 转派场景下还需要先失效旧维修员待办，避免旧处理人继续看到可操作红点。
 * 当前处理器只处理站内待办，不负责外部渠道发送。</p>
 *
 * @author Codex
 * @date 2026/05/14
 */
@Slf4j
@Component
public class WorkOrderAssignedNotifyEventHandler implements NotifyEventHandler {

    @Resource
    private NotifyMessageService notifyMessageService;

    @Resource
    private NotifyMessageLogService notifyMessageLogService;

    @Resource
    private NotifyTemplateRenderService notifyTemplateRenderService;

    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 判断是否支持工单派单事件。
     *
     * @param eventType 事件类型编码
     * @return `true` 表示支持
     */
    @Override
    public boolean supports(String eventType) {
        return NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode().equals(eventType);
    }

    /**
     * 处理工单派单通知事件。
     *
     * <p>处理流程包括：解析派单快照、转派时失效旧维修员待办、
     * 渲染站内模板并幂等创建新的待办消息。</p>
     *
     * @param event 已抢占为 `PROCESSING` 的通知事件
     */
    @Override
    public void handle(SysNotifyEvent event) {
        NotifyAssignedEventDTO payload = parseAssignedPayload(event);
        // 转派时必须先失效旧待办，避免旧维修员和新维修员同时看到可处理任务。
        if (NotifyConstants.ASSIGN_TYPE_TRANSFER.equals(payload.getAssignType())) {
            invalidateTransferredTodos(event, payload);
        }
        // 最后再按 eventId 幂等创建新待办，保证重复消费不会生成重复消息。
        createPendingMessageIfAbsent(event, payload);
    }

    /**
     * 解析并校验工单派单事件快照。
     *
     * <p>当前阶段继续兼容校验 `receiverId`，
     * 因为派单通知历史实现仍使用该字段表达新维修员身份，后续统一收口后再评估是否可以删除。</p>
     *
     * @param event 通知事件
     * @return 解析后的派单快照
     */
    private NotifyAssignedEventDTO parseAssignedPayload(SysNotifyEvent event) {
        if (StrUtil.isBlank(event.getPayloadJson())) {
            throw new ServiceException("Notify event payload cannot be blank");
        }
        NotifyAssignedEventDTO payload;
        try {
            payload = JSONUtil.toBean(event.getPayloadJson(), NotifyAssignedEventDTO.class);
        } catch (Exception ex) {
            throw new ServiceException("Notify assigned payload parse failed");
        }
        if (payload == null) {
            throw new ServiceException("Notify assigned payload parse result is null");
        }
        if (payload.getWorkOrderId() == null) {
            throw new ServiceException("Notify assigned payload missing workOrderId");
        }
        if (payload.getNewAssignedUserId() == null) {
            throw new ServiceException("Notify assigned payload missing newAssignedUserId");
        }
        if (StrUtil.isBlank(payload.getAssignType())) {
            throw new ServiceException("Notify assigned payload missing assignType");
        }
        if (!Objects.equals(payload.getWorkOrderId(), event.getBizId())) {
            throw new ServiceException("Notify assigned payload workOrderId mismatch");
        }
        if (!Objects.equals(payload.getNewAssignedUserId(), event.getReceiverId())) {
            throw new ServiceException("Notify assigned payload receiver mismatch");
        }
        if (payload.getReceiverCompanyId() == null) {
            throw new ServiceException("Notify assigned payload missing receiverCompanyId");
        }
        return payload;
    }

    /**
     * 将转派前维修员的有效待办失效。
     *
     * <p>只有明确存在旧维修员且旧新维修员不同，才需要执行失效逻辑。
     * 失效后同步写入消息日志，保证后续排查可以解释“为什么待办消失了”。</p>
     *
     * @param event 通知事件
     * @param payload 派单快照
     */
    private void invalidateTransferredTodos(SysNotifyEvent event, NotifyAssignedEventDTO payload) {
        if (payload.getOldAssignedUserId() == null
                || Objects.equals(payload.getOldAssignedUserId(), payload.getNewAssignedUserId())) {
            return;
        }
        List<SysNotifyMessage> messages = notifyMessageService.listActiveTodoByBizAndReceiver(
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                payload.getWorkOrderId(),
                payload.getOldAssignedUserId(),
                payload.getReceiverCompanyId()
        );
        if (messages.isEmpty()) {
            return;
        }
        LocalDateTime invalidTime = LocalDateTime.now();
        for (SysNotifyMessage message : messages) {
            if (!notifyMessageService.invalidateMessage(
                    message.getId(),
                    NotifyInvalidReasonEnum.TRANSFERRED.getCode(),
                    invalidTime)) {
                continue;
            }
            // 回填运行时对象快照，保证日志中能看到最终失效状态。
            message.setTodoStatus(NotifyTodoStatusEnum.INVALID.getCode());
            message.setInvalidReason(NotifyInvalidReasonEnum.TRANSFERRED.getCode());
            message.setInvalidTime(invalidTime);
            notifyMessageLogService.createLog(buildMessageLog(
                    message,
                    NotifyActionTypeEnum.INVALID.getCode(),
                    event.getOperatorId(),
                    "Invalidate transferred todo for previous assignee"
            ));
        }
    }

    /**
     * 幂等创建新维修员待办消息。
     *
     * <p>当前按 `eventId` 做幂等保护，避免重复消费时重复生成站内待办。
     * 如果模板被停用则直接跳过，不影响工单主流程。</p>
     *
     * @param event 通知事件
     * @param payload 派单快照
     */
    private void createPendingMessageIfAbsent(SysNotifyEvent event, NotifyAssignedEventDTO payload) {
        if (notifyMessageService.getByEventId(event.getId()) != null) {
            return;
        }
        SysUser receiver = sysUserMapper.selectById(event.getReceiverId());
        String receiverName = resolveReceiverName(receiver, event.getReceiverId());
        NotifyTemplateRenderResult renderResult = notifyTemplateRenderService.render(
                NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(),
                buildAssignedTemplateVariables(event, payload, receiverName)
        );
        if (!renderResult.isNotifyEnabled()) {
            // 模板缺失或停用时只跳过通知侧，不回滚工单派单主流程；原因通过日志和渲染错误保留给排障使用。
            log.warn("跳过工单派单站内待办：未找到启用通知模板或模板已停用。eventId={}, sceneCode={}, errors={}",
                    event.getId(),
                    NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(),
                    renderResult.getErrors());
            return;
        }
        SysNotifyMessage message = new SysNotifyMessage();
        message.setEventId(event.getId());
        message.setMessageType(NotifyConstants.MESSAGE_TYPE_TODO);
        message.setEventType(event.getEventType());
        // 站内待办必须落实际命中的 template_code，保证后续排障能准确追到当前启用模板版本。
        message.setTemplateCode(renderResult.getTemplateCode());
        message.setBizType(event.getBizType());
        message.setBizId(event.getBizId());
        message.setBizNo(event.getBizNo());
        message.setReceiverId(event.getReceiverId());
        message.setReceiverCompanyId(payload.getReceiverCompanyId());
        message.setReceiverName(receiverName);
        message.setTitle(renderResult.getTitle());
        message.setSummary(renderResult.getSummary());
        message.setRouteType(renderResult.getRouteType());
        message.setRouteValue(renderResult.getRouteValue());
        message.setTodoStatus(NotifyTodoStatusEnum.PENDING.getCode());
        message.setExtJson(buildAssignedMessageExt(payload));
        Long messageId = notifyMessageService.createMessage(message);
        message.setId(messageId);
        notifyMessageLogService.createLog(buildMessageLog(
                message,
                NotifyActionTypeEnum.CREATE.getCode(),
                event.getOperatorId(),
                NotifyConstants.ASSIGN_TYPE_TRANSFER.equals(payload.getAssignType())
                        ? "Create todo for transfer target"
                        : "Create todo for assigned technician"
        ));
    }

    /**
     * 解析待办接收人名称。
     *
     * <p>优先展示真实姓名，其次回退用户名，再回退接收人ID，
     * 避免用户基础资料不完整时待办消息无法生成。</p>
     *
     * @param receiver 用户实体
     * @param receiverId 接收人ID
     * @return 接收人名称快照
     */
    private String resolveReceiverName(SysUser receiver, Long receiverId) {
        if (receiver == null) {
            return String.valueOf(receiverId);
        }
        String realName = StrUtil.trim(receiver.getRealName());
        if (StrUtil.isNotBlank(realName)) {
            return realName;
        }
        String username = StrUtil.trim(receiver.getUsername());
        return StrUtil.isNotBlank(username) ? username : String.valueOf(receiverId);
    }

    /**
     * 组装派单模板变量。
     *
     * @param event 通知事件
     * @param payload 派单快照
     * @param receiverName 接收人名称
     * @return 模板变量
     */
    private Map<String, Object> buildAssignedTemplateVariables(SysNotifyEvent event, NotifyAssignedEventDTO payload,
                                                               String receiverName) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("workOrderId", event.getBizId());
        variables.put("orderNo", event.getBizNo());
        variables.put("receiverId", event.getReceiverId());
        variables.put("receiverName", receiverName);
        variables.put("operatorId", event.getOperatorId());
        variables.put("oldAssignedUserId", payload.getOldAssignedUserId());
        variables.put("newAssignedUserId", payload.getNewAssignedUserId());
        variables.put("assignType", payload.getAssignType());
        variables.put("operationId", payload.getOperationId());
        return variables;
    }

    /**
     * 组装派单消息扩展字段。
     *
     * <p>扩展字段继续保留转派相关快照，
     * 方便消息中心后续判断历史来源；该兼容快照可待后续统一排障页面稳定后再评估是否精简。</p>
     *
     * @param payload 派单快照
     * @return 扩展字段 JSON
     */
    private String buildAssignedMessageExt(NotifyAssignedEventDTO payload) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("assignType", payload.getAssignType());
        ext.put("operationId", payload.getOperationId());
        ext.put("oldAssignedUserId", payload.getOldAssignedUserId());
        ext.put("newAssignedUserId", payload.getNewAssignedUserId());
        return JSONUtil.toJsonStr(ext);
    }

    /**
     * 构建站内消息日志快照。
     *
     * @param message 通知消息
     * @param actionType 动作类型
     * @param actionUserId 操作人ID
     * @param remark 日志备注
     * @return 消息日志实体
     */
    private SysNotifyMessageLog buildMessageLog(SysNotifyMessage message, String actionType,
                                                Long actionUserId, String remark) {
        SysNotifyMessageLog logEntity = new SysNotifyMessageLog();
        logEntity.setMessageId(message.getId());
        logEntity.setActionType(actionType);
        logEntity.setActionUserId(actionUserId);
        logEntity.setRemark(remark);
        logEntity.setSnapshotJson(JSONUtil.toJsonStr(message));
        return logEntity;
    }
}

