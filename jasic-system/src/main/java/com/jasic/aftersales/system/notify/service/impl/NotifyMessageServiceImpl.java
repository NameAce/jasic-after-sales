package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.service.WorkOrderPermissionService;
import com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoCompleteDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoInvalidateDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessage;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessageLog;
import com.jasic.aftersales.system.notify.domain.enums.NotifyActionTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyInvalidReasonEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTodoStatusEnum;
import com.jasic.aftersales.system.notify.domain.query.NotifyMessageQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyMessagePageVO;
import com.jasic.aftersales.system.notify.mapper.SysNotifyMessageMapper;
import com.jasic.aftersales.system.notify.service.NotifyMessageLogService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知消息 Service 实现。
 *
 * @author Codex
 * @date 2026/04/18
 */
@Service
public class NotifyMessageServiceImpl implements NotifyMessageService {

    /**
     * 系统通知消息Mapper数据访问接口。
     *
     * @param notifyMessage 参数
     * @return 处理结果
     */
    @Resource
    private SysNotifyMessageMapper sysNotifyMessageMapper;

    @Resource
    private NotifyMessageLogService notifyMessageLogService;

    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private WorkOrderPermissionService workOrderPermissionService;

    /**
     * 执行createMessage相关新增业务。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param notifyMessage 参数
     * @return 处理结果
     */
    @Override
    public Long createMessage(SysNotifyMessage notifyMessage) {
        // 调用insert方法，复用统一能力并保证业务规则一致。
        sysNotifyMessageMapper.insert(notifyMessage);
        return notifyMessage.getId();
    }

    /**
     * 根据ID查询通知消息详情。
     *
     * @return 处理结果
     */
    @Override
    public SysNotifyMessage getById(Long id) {
        return sysNotifyMessageMapper.selectById(id);
    }

    /**
     * 获取By事件ID。
     *
     * @param eventId event ID
     * @return 处理结果
     */
    @Override
    public SysNotifyMessage getByEventId(Long eventId) {
        if (eventId == null) {
            return null;
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getEventId, eventId)
                .orderByAsc(SysNotifyMessage::getId)
                // 调用last方法，复用统一能力并保证业务规则一致。
                .last("limit 1");
        return sysNotifyMessageMapper.selectOne(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysNotifyMessage getByEventIdAndTargetType(Long eventId, String targetType) {
        if (eventId == null || StrUtil.isBlank(targetType)) {
            return null;
        }
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getEventId, eventId)
                .and(condition -> condition
                        .eq(SysNotifyMessage::getTargetType, targetType)
                        .or()
                        .eq(SysNotifyMessage::getMessageType, targetType))
                .orderByAsc(SysNotifyMessage::getId)
                .last("limit 1");
        return sysNotifyMessageMapper.selectOne(wrapper);
    }

    /**
     * 分页查询Active待办By业务And接收人列表。
     *
     * @param bizType 参数
     * @param receiverId receiver ID
     * @param receiverCompanyId receiver Company ID
     * @return 处理结果
     */
    @Override
    public List<SysNotifyMessage> listActiveTodoByBizAndReceiver(String bizType, Long bizId, Long receiverId,
                                                                 Long receiverCompanyId) {
        if (StrUtil.isBlank(bizType) || bizId == null || receiverId == null || isInvalidId(receiverCompanyId)) {
            return Collections.emptyList();
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getBizType, bizType)
                .eq(SysNotifyMessage::getBizId, bizId)
                .eq(SysNotifyMessage::getReceiverId, receiverId)
                .eq(SysNotifyMessage::getReceiverCompanyId, receiverCompanyId)
                .and(this::appendTodoTargetFilter)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode())
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysNotifyMessage::getId);
        return sysNotifyMessageMapper.selectList(wrapper);
    }

    /**
     * 作废消息。
     *
     * @param messageId message ID
     * @param invalidReason 参数
     * @param invalidTime 参数
     */
    @Override
    public boolean invalidateMessage(Long messageId, String invalidReason, LocalDateTime invalidTime) {
        if (messageId == null) {
            return false;
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaUpdateWrapper<SysNotifyMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyMessage::getId, messageId)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode())
                .set(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.INVALID.getCode())
                .set(SysNotifyMessage::getInvalidReason, invalidReason)
                // 调用set方法，复用统一能力并保证业务规则一致。
                .set(SysNotifyMessage::getInvalidTime, invalidTime);
        return sysNotifyMessageMapper.update(null, wrapper) > 0;
    }

    /**
     * mark读取。
     *
     * @param receiverId receiver ID
     * @param receiverCompanyId receiver Company ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id, Long receiverId, Long receiverCompanyId) {
        if (id == null) {
            throw new ServiceException("消息 ID 不能为空");
        }
        if (receiverId == null) {
            throw new ServiceException("当前登录用户不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyMessage message = getById(id);
        if (isInvalidId(receiverCompanyId)) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        if (message == null || !receiverId.equals(message.getReceiverId())
                || !receiverCompanyId.equals(message.getReceiverCompanyId())) {
            throw new ServiceException("消息不存在");
        }
        // 说明：执行该步骤以保证业务流程正确。
        requireBizPermission(message.getBizType(), message.getBizId());
        if (!NotifyTodoStatusEnum.PENDING.getCode().equals(message.getTodoStatus())) {
            return;
        }
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime readTime = LocalDateTime.now();
        if (!markReadMessage(id, readTime)) {
            return;
        }
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        message.setTodoStatus(NotifyTodoStatusEnum.READ.getCode());
        // 调用setReadTime方法，复用统一能力并保证业务规则一致。
        message.setReadTime(readTime);
        notifyMessageLogService.createLog(buildMessageLog(
                message,
                NotifyActionTypeEnum.READ.getCode(),
                receiverId,
                "按消息 ID 标记已读"
        ));
    }

    /**
     * mark读取By业务。
     *
     * @param dto 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markReadByBiz(NotifyReadByBizDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        validateReadByBizDTO(dto);
        // 调用getBizId方法，复用统一能力并保证业务规则一致。
        requireBizPermission(dto.getBizType(), dto.getBizId());
        // 说明：执行该步骤以保证业务流程正确。
        List<SysNotifyMessage> messages = listPendingTodoByBizAndReceiver(
                dto.getBizType(),
                dto.getBizId(),
                dto.getReceiverId(),
                dto.getReceiverCompanyId()
        );
        if (messages.isEmpty()) {
            return;
        }
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime readTime = LocalDateTime.now();
        for (SysNotifyMessage message : messages) {
            if (!markReadMessage(message.getId(), readTime)) {
                continue;
            }
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            message.setTodoStatus(NotifyTodoStatusEnum.READ.getCode());
            // 调用setReadTime方法，复用统一能力并保证业务规则一致。
            message.setReadTime(readTime);
            notifyMessageLogService.createLog(buildMessageLog(
                    message,
                    NotifyActionTypeEnum.READ.getCode(),
                    dto.getReceiverId(),
                    "进入业务详情页，待办标记已读"
            ));
        }
    }

    /**
     * 完成待办By业务And接收人。
     *
     * @param dto 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTodoByBizAndReceiver(NotifyTodoCompleteDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        validateCompleteDTO(dto);
        // 调用getBizId方法，复用统一能力并保证业务规则一致。
        requireBizPermission(dto.getBizType(), dto.getBizId());
        // 说明：执行该步骤以保证业务流程正确。
        List<SysNotifyMessage> messages = listActiveTodoByBizAndReceiver(
                dto.getBizType(),
                dto.getBizId(),
                dto.getReceiverId(),
                dto.getReceiverCompanyId()
        );
        if (messages.isEmpty()) {
            return;
        }
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime doneTime = LocalDateTime.now();
        for (SysNotifyMessage message : messages) {
            if (!completeMessage(message.getId(), doneTime)) {
                continue;
            }
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            message.setTodoStatus(NotifyTodoStatusEnum.DONE.getCode());
            // 调用setDoneTime方法，复用统一能力并保证业务规则一致。
            message.setDoneTime(doneTime);
            notifyMessageLogService.createLog(buildMessageLog(
                    message,
                    NotifyActionTypeEnum.DONE.getCode(),
                    dto.getReceiverId(),
                    String.format("业务动作 %s 完成待办", dto.getActionCode())
            ));
        }
    }

    /**
     * 作废待办By业务。
     *
     * @param dto 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void invalidateTodoByBiz(NotifyTodoInvalidateDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        validateInvalidateDTO(dto);
        // 调用getBizId方法，复用统一能力并保证业务规则一致。
        requireBizPermission(dto.getBizType(), dto.getBizId());
        // 说明：执行该步骤以保证业务流程正确。
        List<SysNotifyMessage> messages = listActiveTodoByBiz(dto.getBizType(), dto.getBizId());
        if (messages.isEmpty()) {
            return;
        }
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime invalidTime = LocalDateTime.now();
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        Long actionUserId = SecurityContext.getCurrentUserId();
        for (SysNotifyMessage message : messages) {
            if (!invalidateMessage(message.getId(), dto.getInvalidReason(), invalidTime)) {
                continue;
            }
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            message.setTodoStatus(NotifyTodoStatusEnum.INVALID.getCode());
            // 调用getInvalidReason方法，复用统一能力并保证业务规则一致。
            message.setInvalidReason(dto.getInvalidReason());
            // 调用setInvalidTime方法，复用统一能力并保证业务规则一致。
            message.setInvalidTime(invalidTime);
            notifyMessageLogService.createLog(buildMessageLog(
                    message,
                    NotifyActionTypeEnum.INVALID.getCode(),
                    actionUserId,
                    buildInvalidateRemark(dto.getInvalidReason())
            ));
        }
    }

    /**
     * 分页查询通知消息列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public PageResult<NotifyMessagePageVO> listPage(NotifyMessageQuery query) {
        if (query == null) {
            return PageResult.of(Collections.emptyList(), 0L, 1, 10);
        }
        if (query.getReceiverId() == null) {
            throw new ServiceException("当前登录用户不能为空");
        }
        if (isInvalidId(query.getReceiverCompanyId())) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        // 调用getBox方法，复用统一能力并保证业务规则一致。
        List<String> todoStatuses = resolveBoxStatuses(query.getBox());
        // 说明：执行该步骤以保证业务流程正确。
        Page<SysNotifyMessage> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getReceiverId, query.getReceiverId())
                .eq(SysNotifyMessage::getReceiverCompanyId, query.getReceiverCompanyId())
                .in(SysNotifyMessage::getTodoStatus, todoStatuses)
                .orderByDesc(SysNotifyMessage::getCreateTime)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(SysNotifyMessage::getId);
        if (StrUtil.isNotBlank(query.getBizType())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyMessage::getBizType, query.getBizType().trim());
        }
        if (query.getBizId() != null) {
            // 调用getBizId方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyMessage::getBizId, query.getBizId());
        }
        // 调用selectPage方法，复用统一能力并保证业务规则一致。
        Page<SysNotifyMessage> result = sysNotifyMessageMapper.selectPage(page, wrapper);
        List<NotifyMessagePageVO> rows = result.getRecords().stream()
                .map(this::buildPageVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        return PageResult.of(rows, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * count待办。
     *
     * @param receiverId receiver ID
     * @param receiverCompanyId receiver Company ID
     * @return 处理结果
     */
    @Override
    public Long countTodo(Long receiverId, Long receiverCompanyId) {
        if (receiverId == null) {
            throw new ServiceException("当前登录用户不能为空");
        }
        if (isInvalidId(receiverCompanyId)) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getReceiverId, receiverId)
                .eq(SysNotifyMessage::getReceiverCompanyId, receiverCompanyId)
                .and(this::appendTodoTargetFilter)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        // 调用getCode方法，复用统一能力并保证业务规则一致。
                        NotifyTodoStatusEnum.READ.getCode());
        // 调用selectCount方法，复用统一能力并保证业务规则一致。
        Long count = sysNotifyMessageMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    /**
     * 分页查询Pending待办By业务And接收人列表。
     *
     * @param bizType 参数
     * @param receiverId receiver ID
     * @param receiverCompanyId receiver Company ID
     * @return 处理结果
     */
    private List<SysNotifyMessage> listPendingTodoByBizAndReceiver(String bizType, Long bizId,
                                                                   Long receiverId, Long receiverCompanyId) {
        if (StrUtil.isBlank(bizType) || bizId == null || receiverId == null || isInvalidId(receiverCompanyId)) {
            return Collections.emptyList();
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getBizType, bizType)
                .eq(SysNotifyMessage::getBizId, bizId)
                .eq(SysNotifyMessage::getReceiverId, receiverId)
                .eq(SysNotifyMessage::getReceiverCompanyId, receiverCompanyId)
                .and(this::appendTodoTargetFilter)
                .eq(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.PENDING.getCode())
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysNotifyMessage::getId);
        return sysNotifyMessageMapper.selectList(wrapper);
    }

    /**
     * 分页查询Active待办By业务列表。
     *
     * @param bizType 参数
     * @return 处理结果
     */
    private List<SysNotifyMessage> listActiveTodoByBiz(String bizType, Long bizId) {
        if (StrUtil.isBlank(bizType) || bizId == null) {
            return Collections.emptyList();
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getBizType, bizType)
                .eq(SysNotifyMessage::getBizId, bizId)
                .and(this::appendTodoTargetFilter)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode())
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysNotifyMessage::getId);
        return sysNotifyMessageMapper.selectList(wrapper);
    }

    /**
     * mark读取消息。
     *
     * @param messageId message ID
     * @param readTime 参数
     */
    private boolean markReadMessage(Long messageId, LocalDateTime readTime) {
        if (messageId == null) {
            return false;
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaUpdateWrapper<SysNotifyMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyMessage::getId, messageId)
                .eq(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.PENDING.getCode())
                .set(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.READ.getCode())
                // 调用set方法，复用统一能力并保证业务规则一致。
                .set(SysNotifyMessage::getReadTime, readTime);
        return sysNotifyMessageMapper.update(null, wrapper) > 0;
    }

    /**
     * 完成消息。
     *
     * @param messageId message ID
     * @param doneTime 参数
     */
    private boolean completeMessage(Long messageId, LocalDateTime doneTime) {
        if (messageId == null) {
            return false;
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaUpdateWrapper<SysNotifyMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyMessage::getId, messageId)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode())
                .set(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.DONE.getCode())
                // 调用set方法，复用统一能力并保证业务规则一致。
                .set(SysNotifyMessage::getDoneTime, doneTime);
        return sysNotifyMessageMapper.update(null, wrapper) > 0;
    }

    /**
     * 追加“仅站内待办”过滤条件。
     *
     * <p>阶段二开始，站内消息和站内待办共存于同一张表。
     * 因此所有待办计数、完成、失效和按业务批量已读逻辑，都必须明确只命中 `IN_APP_TODO`，
     * 同时兼容历史仍写成 `TODO` 的旧消息类型。</p>
     *
     * @param wrapper 查询包装器
     */
    private void appendTodoTargetFilter(LambdaQueryWrapper<SysNotifyMessage> wrapper) {
        wrapper.eq(SysNotifyMessage::getTargetType, NotifyTypeEnum.IN_APP_TODO.getCode())
                .or()
                .eq(SysNotifyMessage::getMessageType, NotifyTypeEnum.IN_APP_TODO.getCode())
                .or()
                .eq(SysNotifyMessage::getMessageType, NotifyConstants.MESSAGE_TYPE_TODO);
    }

    /**
     * 构建分页视图。
     *
     * @param message 参数
     * @return 处理结果
     */
    private NotifyMessagePageVO buildPageVO(SysNotifyMessage message) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyMessagePageVO vo = new NotifyMessagePageVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setId(message.getId());
        // 调用getTitle方法，复用统一能力并保证业务规则一致。
        vo.setTitle(message.getTitle());
        // 调用getSummary方法，复用统一能力并保证业务规则一致。
        vo.setSummary(message.getSummary());
        // 调用getBizType方法，复用统一能力并保证业务规则一致。
        vo.setBizType(message.getBizType());
        // 调用getBizId方法，复用统一能力并保证业务规则一致。
        vo.setBizId(message.getBizId());
        // 调用getBizNo方法，复用统一能力并保证业务规则一致。
        vo.setBizNo(message.getBizNo());
        // 调用getRouteType方法，复用统一能力并保证业务规则一致。
        vo.setRouteType(message.getRouteType());
        // 调用getRouteValue方法，复用统一能力并保证业务规则一致。
        vo.setRouteValue(message.getRouteValue());
        // 调用getTodoStatus方法，复用统一能力并保证业务规则一致。
        vo.setTodoStatus(message.getTodoStatus());
        // 调用getInvalidReason方法，复用统一能力并保证业务规则一致。
        vo.setInvalidReason(message.getInvalidReason());
        // 调用getCreateTime方法，复用统一能力并保证业务规则一致。
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    /**
     * 解析BoxStatuses。
     *
     * @param box 参数
     * @return 处理结果
     */
    private List<String> resolveBoxStatuses(String box) {
        // 调用toUpperCase方法，复用统一能力并保证业务规则一致。
        String normalizedBox = StrUtil.trimToEmpty(box).toUpperCase();
        // 说明：执行该步骤以保证业务流程正确。
        if (NotifyConstants.BOX_TODO.equals(normalizedBox)) {
            return Arrays.asList(
                    NotifyTodoStatusEnum.PENDING.getCode(),
                    NotifyTodoStatusEnum.READ.getCode()
            );
        }
        if (NotifyConstants.BOX_HISTORY.equals(normalizedBox)) {
            return Arrays.asList(
                    NotifyTodoStatusEnum.DONE.getCode(),
                    NotifyTodoStatusEnum.INVALID.getCode()
            );
        }
        throw new ServiceException("消息盒子仅支持 TODO 或 HISTORY");
    }

    /**
     * 构建消息日志。
     *
     * @param message 参数
     * @param actionType 参数
     * @param actionUserId action User ID
     * @param remark 参数
     * @return 处理结果
     */
    private SysNotifyMessageLog buildMessageLog(SysNotifyMessage message, String actionType, Long actionUserId, String remark) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyMessageLog logEntity = new SysNotifyMessageLog();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        logEntity.setMessageId(message.getId());
        // 调用setActionType方法，复用统一能力并保证业务规则一致。
        logEntity.setActionType(actionType);
        // 调用setActionUserId方法，复用统一能力并保证业务规则一致。
        logEntity.setActionUserId(actionUserId);
        // 调用setRemark方法，复用统一能力并保证业务规则一致。
        logEntity.setRemark(remark);
        // 调用toJsonStr方法，复用统一能力并保证业务规则一致。
        logEntity.setSnapshotJson(JSONUtil.toJsonStr(message));
        return logEntity;
    }

    /**
     * 构建作废Remark。
     *
     * @param invalidReason 参数
     * @return 处理结果
     */
    private String buildInvalidateRemark(String invalidReason) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyInvalidReasonEnum reasonEnum = NotifyInvalidReasonEnum.getByCode(invalidReason);
        return reasonEnum == null ? "业务状态变化，待办失效" : String.format("%s，待办失效", reasonEnum.getDesc());
    }

    /**
     * 校验读取By业务DTO。
     *
     * @param dto 参数
     */
    private void validateReadByBizDTO(NotifyReadByBizDTO dto) {
        if (dto == null) {
            throw new ServiceException("已读参数不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        validateBiz(dto.getBizType(), dto.getBizId());
        if (dto.getReceiverId() == null) {
            throw new ServiceException("已读参数缺少接收人");
        }
        if (isInvalidId(dto.getReceiverCompanyId())) {
            throw new ServiceException("已读参数缺少接收公司");
        }
    }

    /**
     * 校验完成DTO。
     *
     * @param dto 参数
     */
    private void validateCompleteDTO(NotifyTodoCompleteDTO dto) {
        if (dto == null) {
            throw new ServiceException("完成参数不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        validateBiz(dto.getBizType(), dto.getBizId());
        if (dto.getReceiverId() == null) {
            throw new ServiceException("完成参数缺少接收人");
        }
        if (isInvalidId(dto.getReceiverCompanyId())) {
            throw new ServiceException("完成参数缺少接收公司");
        }
        if (StrUtil.isBlank(dto.getActionCode())) {
            throw new ServiceException("完成参数缺少业务动作编码");
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (NotifyBizTypeEnum.WORK_ORDER.getCode().equals(dto.getBizType())
                && !NotifyConstants.ACTION_TECH_ACCEPT.equals(dto.getActionCode())) {
            throw new ServiceException("当前阶段工单待办仅允许绑定 TECH_ACCEPT 完成");
        }
    }

    /**
     * 校验作废DTO。
     *
     * @param dto 参数
     */
    private void validateInvalidateDTO(NotifyTodoInvalidateDTO dto) {
        if (dto == null) {
            throw new ServiceException("失效参数不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        validateBiz(dto.getBizType(), dto.getBizId());
        if (StrUtil.isBlank(dto.getInvalidReason())) {
            throw new ServiceException("失效参数缺少失效原因");
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (NotifyInvalidReasonEnum.getByCode(dto.getInvalidReason()) == null) {
            throw new ServiceException("不支持的失效原因：" + dto.getInvalidReason());
        }
    }

    /**
     * 校验业务。
     *
     * @param bizType 参数
     */
    private void validateBiz(String bizType, Long bizId) {
        if (StrUtil.isBlank(bizType)) {
            throw new ServiceException("业务类型不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (NotifyBizTypeEnum.getByCode(bizType) == null) {
            throw new ServiceException("不支持的业务类型：" + bizType);
        }
        if (bizId == null) {
            throw new ServiceException("业务ID不能为空");
        }
    }

    /**
     * require业务权限。
     *
     * @param bizType 参数
     */
    private void requireBizPermission(String bizType, Long bizId) {
        // 说明：执行该步骤以保证业务流程正确。
        validateBiz(bizType, bizId);
        // 说明：执行该步骤以保证业务流程正确。
        if (!NotifyBizTypeEnum.WORK_ORDER.getCode().equals(bizType)) {
            throw new ServiceException("不支持的业务类型：" + bizType);
        }
        // 说明：执行该步骤以保证业务流程正确。
        WorkOrder workOrder = workOrderMapper.selectById(bizId);
        if (workOrder == null || !workOrderPermissionService.canView(workOrder)) {
            throw new ServiceException("无权查看该工单");
        }
    }

    /**
     * 判断是否无效ID。
     */
    private boolean isInvalidId(Long id) {
        return id == null || id <= 0;
    }

}



