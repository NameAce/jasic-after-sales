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
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTodoStatusEnum;
import com.jasic.aftersales.system.notify.service.NotifyMessageLogService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.notify.support.NotifyEventExecutionContext;
import com.jasic.aftersales.system.notify.support.NotifyReceiverSnapshot;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
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
 * <p>该处理器只负责解析 `WORK_ORDER_ASSIGNED` 事件自身的业务语义：
 * 1. 校验派单快照是否合法
 * 2. 转派时失效旧维修员待办
 * 3. 解析新接收维修员身份、名称和小程序 openid
 * 4. 组装统一通知执行上下文
 *
 * <p>模板渲染、目标分流、站内消息创建和外部分发表创建由统一消费编排层完成。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Component
public class WorkOrderAssignedNotifyEventHandler implements NotifyEventHandler {

    @Resource
    private NotifyMessageService notifyMessageService;

    @Resource
    private NotifyMessageLogService notifyMessageLogService;

    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(String eventType) {
        return NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode().equals(eventType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotifyEventExecutionContext buildExecutionContext(SysNotifyEvent event) {
        NotifyAssignedEventDTO payload = parseAssignedPayload(event);

        // 转派场景必须先失效旧维修员仍然有效的待办，避免同一工单出现两个可处理人。
        if (NotifyConstants.ASSIGN_TYPE_TRANSFER.equals(payload.getAssignType())) {
            invalidateTransferredTodos(event, payload);
        }

        // 这里统一查询接收维修员资料，后续无论产出站内消息、站内待办还是小程序通知，都复用同一份快照。
        SysUser receiver = sysUserMapper.selectById(payload.getNewAssignedUserId());
        String receiverName = resolveReceiverName(receiver, payload.getNewAssignedUserId());

        NotifyEventExecutionContext context = new NotifyEventExecutionContext();
        context.setSceneCode(resolveSceneCode(event));
        context.setReceiverType(NotifyReceiverTypeEnum.REPAIRER.getCode());
        context.setReceiverId(payload.getNewAssignedUserId());
        context.setReceiverCompanyId(payload.getReceiverCompanyId());
        context.setReceiverName(receiverName);
        context.setReceiverAddress(receiver == null ? null : StrUtil.trimToNull(receiver.getOpenid()));
        context.addReceiverSnapshot(NotifyReceiverSnapshot.of(
                NotifyReceiverTypeEnum.REPAIRER.getCode(),
                payload.getNewAssignedUserId(),
                payload.getReceiverCompanyId(),
                receiverName,
                receiver == null ? null : StrUtil.trimToNull(receiver.getOpenid())
        ));
        context.setTemplateVariables(buildAssignedTemplateVariables(event, payload, receiverName));
        context.setMessageExtJson(buildAssignedMessageExt(payload));
        return context;
    }

    /**
     * 解析并校验工单派单事件快照。
     *
     * <p>当前事件主表里的 `receiverId` 仍表达新维修员ID，
     * 这里同步校验事件主表和载荷快照，避免业务层传错接收对象后继续生成错误通知。</p>
     *
     * @param event 通知事件
     * @return 派单快照
     */
    private NotifyAssignedEventDTO parseAssignedPayload(SysNotifyEvent event) {
        if (StrUtil.isBlank(event.getPayloadJson())) {
            throw new ServiceException("工单派单通知事件载荷不能为空");
        }
        NotifyAssignedEventDTO payload;
        try {
            payload = JSONUtil.toBean(event.getPayloadJson(), NotifyAssignedEventDTO.class);
        } catch (Exception ex) {
            throw new ServiceException("工单派单通知事件载荷解析失败");
        }
        if (payload == null) {
            throw new ServiceException("工单派单通知事件载荷解析结果不能为空");
        }
        if (payload.getWorkOrderId() == null) {
            throw new ServiceException("工单派单通知事件缺少工单ID");
        }
        if (payload.getNewAssignedUserId() == null) {
            throw new ServiceException("工单派单通知事件缺少新维修员ID");
        }
        if (payload.getReceiverCompanyId() == null) {
            throw new ServiceException("工单派单通知事件缺少接收公司ID");
        }
        if (StrUtil.isBlank(payload.getAssignType())) {
            throw new ServiceException("工单派单通知事件缺少派单类型");
        }
        if (!Objects.equals(payload.getWorkOrderId(), event.getBizId())) {
            throw new ServiceException("工单派单通知事件工单ID与事件主表不一致");
        }
        if (!Objects.equals(payload.getNewAssignedUserId(), event.getReceiverId())) {
            throw new ServiceException("工单派单通知事件接收人与事件主表不一致");
        }
        return payload;
    }

    /**
     * 失效转派前维修员的有效待办。
     *
     * <p>该步骤只影响 `IN_APP_TODO` 产物，不影响站内消息和外部分发表创建。
     * 只有在旧维修员存在且旧新维修员不同的前提下才执行，避免普通派单误伤当前待办。</p>
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

        // 转派失效要统一写站内日志，便于后续排障解释“为什么旧维修员待办消失了”。
        LocalDateTime invalidTime = LocalDateTime.now();
        for (SysNotifyMessage message : messages) {
            if (!notifyMessageService.invalidateMessage(
                    message.getId(),
                    NotifyInvalidReasonEnum.TRANSFERRED.getCode(),
                    invalidTime)) {
                continue;
            }
            message.setTodoStatus(NotifyTodoStatusEnum.INVALID.getCode());
            message.setInvalidReason(NotifyInvalidReasonEnum.TRANSFERRED.getCode());
            message.setInvalidTime(invalidTime);
            notifyMessageLogService.createLog(buildMessageLog(
                    message,
                    NotifyActionTypeEnum.INVALID.getCode(),
                    event.getOperatorId(),
                    "工单转派，旧维修员待办失效"
            ));
        }
    }

    /**
     * 解析通知场景编码。
     *
     * <p>阶段二开始优先使用事件主表显式记录的 `sceneCode`，
     * 只有历史测试或旧数据未回填时，才回退到注册常量对应的统一场景编码。</p>
     *
     * @param event 通知事件
     * @return 场景编码
     */
    private String resolveSceneCode(SysNotifyEvent event) {
        String sceneCode = event == null ? null : StrUtil.trimToNull(event.getSceneCode());
        if (sceneCode == null) {
            throw new ServiceException("工单派单通知事件缺少场景编码");
        }
        return sceneCode;
    }

    /**
     * 解析接收维修员名称快照。
     *
     * @param receiver 用户实体
     * @param receiverId 接收人ID
     * @return 接收人名称
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
     * 组装模板变量快照。
     *
     * <p>统一把后续所有通知目标可能使用到的变量都放进快照中，
     * 避免不同目标在重试或排障时再回查实时业务数据。</p>
     *
     * @param event 通知事件
     * @param payload 派单快照
     * @param receiverName 接收维修员名称
     * @return 模板变量
     */
    private Map<String, Object> buildAssignedTemplateVariables(SysNotifyEvent event, NotifyAssignedEventDTO payload,
                                                               String receiverName) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("workOrderId", event.getBizId());
        variables.put("orderNo", event.getBizNo());
        variables.put("receiverId", payload.getNewAssignedUserId());
        variables.put("receiverName", receiverName);
        variables.put("operatorId", event.getOperatorId());
        variables.put("oldAssignedUserId", payload.getOldAssignedUserId());
        variables.put("newAssignedUserId", payload.getNewAssignedUserId());
        variables.put("assignType", payload.getAssignType());
        // 派单模板中的“用户名称、联系电话”本轮统一解释为客户信息，
        // 因此这里要把业务层已固化好的客户展示名和客户联系电话一并写入变量快照，
        // 避免 sender 渲染时再误读为工程师信息。
        variables.put("customerName", payload.getCustomerName());
        variables.put("customerMobile", payload.getCustomerMobile());
        variables.put("operationId", payload.getOperationId());
        return variables;
    }

    /**
     * 构建站内消息扩展快照。
     *
     * @param payload 派单快照
     * @return 扩展快照JSON
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
     * 构建站内消息日志。
     *
     * @param message 消息快照
     * @param actionType 动作类型
     * @param actionUserId 操作人ID
     * @param remark 日志备注
     * @return 日志实体
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
