package com.jasic.aftersales.system.notify.service.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderEvaluatedEventDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.mapper.SysNotifyEventMapper;
import com.jasic.aftersales.system.notify.support.NotifyEventExecutionContext;
import com.jasic.aftersales.system.notify.support.NotifyReceiverSnapshot;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * B端客户评价完成通知事件处理器。
 *
 * <p>该处理器负责把客户提交评价后的业务快照解析成统一通知执行上下文，
 * 接收人固定收口为：当前责任维修员、最后一次把工单派给当前责任维修员的实际派单人、
 * 以及最终处理公司的主账号。接收人解析结果会在消费阶段固化为快照，避免后续重试时受人员关系变化影响。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@Component
public class WorkOrderEvaluatedNotifyEventHandler implements NotifyEventHandler {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysNotifyEventMapper sysNotifyEventMapper;

    @Resource
    private SysUserCompanyMapper sysUserCompanyMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(String eventType) {
        return NotifyEventTypeEnum.WORK_ORDER_EVALUATED.getCode().equals(eventType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotifyEventExecutionContext buildExecutionContext(SysNotifyEvent event) {
        NotifyWorkOrderEvaluatedEventDTO payload = parseEvaluatedPayload(event);
        List<NotifyReceiverSnapshot> receiverSnapshots = resolveReceiverSnapshots(payload);

        NotifyEventExecutionContext context = new NotifyEventExecutionContext();
        context.setSceneCode(resolveSceneCode(event));
        context.setReceiverType(NotifyReceiverTypeEnum.EVALUATED_B_USER.getCode());
        if (!receiverSnapshots.isEmpty()) {
            // 这里把第一个接收人写入兼容字段，保证旧链路或排障查看时仍能快速看到主接收快照。
            NotifyReceiverSnapshot firstSnapshot = receiverSnapshots.get(0);
            context.setReceiverId(firstSnapshot.getReceiverId());
            context.setReceiverCompanyId(firstSnapshot.getReceiverCompanyId());
            context.setReceiverName(firstSnapshot.getReceiverName());
            context.setReceiverAddress(firstSnapshot.getReceiverAddress());
        }
        context.addReceiverSnapshots(NotifyReceiverTypeEnum.EVALUATED_B_USER.getCode(), receiverSnapshots);
        context.setTemplateVariables(buildTemplateVariables(payload));
        context.setMessageExtJson(null);
        return context;
    }

    /**
     * 解析并校验 B 端客户评价完成通知事件载荷。
     *
     * @param event 通知事件
     * @return 事件载荷
     */
    private NotifyWorkOrderEvaluatedEventDTO parseEvaluatedPayload(SysNotifyEvent event) {
        if (event == null || StrUtil.isBlank(event.getPayloadJson())) {
            throw new ServiceException("B端评价提醒通知事件载荷不能为空");
        }
        NotifyWorkOrderEvaluatedEventDTO payload;
        try {
            payload = JSONUtil.toBean(event.getPayloadJson(), NotifyWorkOrderEvaluatedEventDTO.class);
        } catch (Exception ex) {
            throw new ServiceException("B端评价提醒通知事件载荷解析失败");
        }
        if (payload == null) {
            throw new ServiceException("B端评价提醒通知事件载荷解析结果不能为空");
        }
        if (payload.getWorkOrderId() == null) {
            throw new ServiceException("B端评价提醒通知事件缺少工单ID");
        }
        if (payload.getAssignedUserId() == null) {
            throw new ServiceException("B端评价提醒通知事件缺少责任维修员ID");
        }
        if (!Objects.equals(payload.getWorkOrderId(), event.getBizId())) {
            throw new ServiceException("B端评价提醒通知事件工单ID与事件主表不一致");
        }
        return payload;
    }

    /**
     * 解析最终通知接收人列表。
     *
     * <p>接收人按“责任维修员 -> 最后派单人 -> 公司主账号”的顺序收口，并按 userId 去重。
     * 这里显式只取最终责任链路，不回溯历史所有派单员或历史公司，避免评价提醒扩散成大范围广播。</p>
     *
     * @param payload 事件载荷
     * @return 接收人快照列表
     */
    private List<NotifyReceiverSnapshot> resolveReceiverSnapshots(NotifyWorkOrderEvaluatedEventDTO payload) {
        LinkedHashSet<Long> receiverUserIds = new LinkedHashSet<>();
        receiverUserIds.add(payload.getAssignedUserId());

        // 派单人必须取“最后一次把当前责任维修员派上去的人”，不能泛化成所有派单权限用户。
        Long dispatcherUserId = findLatestDispatcherUserId(payload.getWorkOrderId(), payload.getAssignedUserId());
        if (dispatcherUserId != null) {
            receiverUserIds.add(dispatcherUserId);
        }

        // 最终处理公司主账号使用新加的显式标记快速定位，不再依赖角色反推。
        Long primaryAccountUserId = payload.getCurrentAcceptCompanyId() == null
                ? null
                : sysUserCompanyMapper.selectPrimaryAccountUserIdByCompanyId(payload.getCurrentAcceptCompanyId());
        if (primaryAccountUserId != null) {
            receiverUserIds.add(primaryAccountUserId);
        }

        List<NotifyReceiverSnapshot> snapshots = new ArrayList<>();
        for (Long receiverUserId : receiverUserIds) {
            NotifyReceiverSnapshot snapshot = buildReceiverSnapshot(receiverUserId, payload.getCurrentAcceptCompanyId());
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }
        return snapshots;
    }

    /**
     * 查询最后一次把当前责任维修员派上去的实际派单人。
     *
     * @param workOrderId 工单ID
     * @param assignedUserId 当前责任维修员ID
     * @return 派单人ID；未命中时返回 {@code null}
     */
    private Long findLatestDispatcherUserId(Long workOrderId, Long assignedUserId) {
        if (workOrderId == null || assignedUserId == null) {
            return null;
        }
        LambdaQueryWrapper<SysNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyEvent::getEventType, NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode())
                .eq(SysNotifyEvent::getBizType, NotifyBizTypeEnum.WORK_ORDER.getCode())
                .eq(SysNotifyEvent::getBizId, workOrderId)
                .eq(SysNotifyEvent::getReceiverId, assignedUserId)
                // 同一工单的派单事件按自增ID倒序即可稳定命中最后一次有效责任链路。
                .orderByDesc(SysNotifyEvent::getId)
                .last("limit 1");
        SysNotifyEvent assignedEvent = sysNotifyEventMapper.selectOne(wrapper);
        return assignedEvent == null ? null : assignedEvent.getOperatorId();
    }

    /**
     * 构建接收人快照。
     *
     * <p>这里只保留已启用用户；未启用或不存在的账号不应继续收到评价提醒。
     * openid 允许为空，后续统一由分发层记为 skipped，而不是在事件消费阶段直接报错。</p>
     *
     * @param receiverUserId 接收人用户ID
     * @param receiverCompanyId 接收人归属公司ID
     * @return 接收人快照；用户不存在或已停用时返回 {@code null}
     */
    private NotifyReceiverSnapshot buildReceiverSnapshot(Long receiverUserId, Long receiverCompanyId) {
        if (receiverUserId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(receiverUserId);
        if (user == null || !Objects.equals(user.getStatus(), 1)) {
            return null;
        }
        return NotifyReceiverSnapshot.of(
                NotifyReceiverTypeEnum.EVALUATED_B_USER.getCode(),
                receiverUserId,
                receiverCompanyId,
                resolveUserName(user, receiverUserId),
                StrUtil.trimToNull(user.getOpenid())
        );
    }

    /**
     * 组装模板变量快照。
     *
     * @param payload 事件载荷
     * @return 模板变量快照
     */
    private Map<String, Object> buildTemplateVariables(NotifyWorkOrderEvaluatedEventDTO payload) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("workOrderId", payload.getWorkOrderId());
        variables.put("orderNo", payload.getOrderNo());
        variables.put("customerId", payload.getCustomerId());
        variables.put("customerName", resolveCustomerName(payload));
        variables.put("customerMobile", payload.getCustomerMobile());
        variables.put("assignedUserId", payload.getAssignedUserId());
        variables.put("assignedUserName", resolveAssignedUserName(payload));
        variables.put("currentAcceptCompanyId", payload.getCurrentAcceptCompanyId());
        return variables;
    }

    /**
     * 解析场景编码。
     *
     * @param event 通知事件
     * @return 场景编码
     */
    private String resolveSceneCode(SysNotifyEvent event) {
        return StrUtil.blankToDefault(event.getSceneCode(), NotifySceneCode.WORK_ORDER_EVALUATED.getCode());
    }

    /**
     * 解析用户展示名称。
     *
     * @param user 用户实体
     * @param userId 用户ID
     * @return 展示名称
     */
    private String resolveUserName(SysUser user, Long userId) {
        if (user == null) {
            return String.valueOf(userId);
        }
        String realName = StrUtil.trim(user.getRealName());
        if (StrUtil.isNotBlank(realName)) {
            return realName;
        }
        String username = StrUtil.trim(user.getUsername());
        return StrUtil.isNotBlank(username) ? username : String.valueOf(userId);
    }

    /**
     * 解析模板里的客户展示名称兜底。
     *
     * @param payload 事件载荷
     * @return 客户展示名称
     */
    private String resolveCustomerName(NotifyWorkOrderEvaluatedEventDTO payload) {
        String customerName = StrUtil.trim(payload.getCustomerName());
        if (StrUtil.isNotBlank(customerName)) {
            return customerName;
        }
        String customerMobile = StrUtil.trim(payload.getCustomerMobile());
        return StrUtil.isNotBlank(customerMobile) ? customerMobile : "客户";
    }

    /**
     * 解析模板里的接单人名称兜底。
     *
     * @param payload 事件载荷
     * @return 接单人展示名称
     */
    private String resolveAssignedUserName(NotifyWorkOrderEvaluatedEventDTO payload) {
        String assignedUserName = StrUtil.trim(payload.getAssignedUserName());
        return StrUtil.isNotBlank(assignedUserName)
                ? assignedUserName
                : String.valueOf(payload.getAssignedUserId());
    }
}
