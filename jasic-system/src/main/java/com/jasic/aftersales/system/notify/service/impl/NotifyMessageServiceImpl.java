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
     * ?????
     *
     * @param notifyMessage ??
     * @return ????
     */
    @Resource
    private SysNotifyMessageMapper sysNotifyMessageMapper;

    @Resource
    private NotifyMessageLogService notifyMessageLogService;

    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private WorkOrderPermissionService workOrderPermissionService;

    @Override
    public Long createMessage(SysNotifyMessage notifyMessage) {
        sysNotifyMessageMapper.insert(notifyMessage);
        return notifyMessage.getId();
    }

    /**
     * ??By Id?
     *
     * @param id ??ID
     * @return ????
     */
    @Override
    public SysNotifyMessage getById(Long id) {
        return sysNotifyMessageMapper.selectById(id);
    }

    /**
     * ??By Event Id?
     *
     * @param eventId event ID
     * @return ????
     */
    @Override
    public SysNotifyMessage getByEventId(Long eventId) {
        if (eventId == null) {
            return null;
        }
        // ????????????????????????
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getEventId, eventId)
                .orderByAsc(SysNotifyMessage::getId)
                .last("limit 1");
        return sysNotifyMessageMapper.selectOne(wrapper);
    }

    /**
     * ???????
     *
     * @param bizType ????
     * @param bizId ??ID
     * @param receiverId receiver ID
     * @param receiverCompanyId receiver Company ID
     * @return ????
     */
    @Override
    public List<SysNotifyMessage> listActiveTodoByBizAndReceiver(String bizType, Long bizId, Long receiverId,
                                                                 Long receiverCompanyId) {
        if (StrUtil.isBlank(bizType) || bizId == null || receiverId == null || isInvalidId(receiverCompanyId)) {
            return Collections.emptyList();
        }
        // ????????????????????????
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getBizType, bizType)
                .eq(SysNotifyMessage::getBizId, bizId)
                .eq(SysNotifyMessage::getReceiverId, receiverId)
                .eq(SysNotifyMessage::getReceiverCompanyId, receiverCompanyId)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode())
                .orderByAsc(SysNotifyMessage::getId);
        return sysNotifyMessageMapper.selectList(wrapper);
    }

    /**
     * ?? invalidateMessage ?????
     *
     * @param messageId message ID
     * @param invalidReason ??
     * @param invalidTime ??
     * @return true ??????
     */
    @Override
    public boolean invalidateMessage(Long messageId, String invalidReason, LocalDateTime invalidTime) {
        if (messageId == null) {
            return false;
        }
        // ????????????????????????
        LambdaUpdateWrapper<SysNotifyMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyMessage::getId, messageId)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode())
                .set(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.INVALID.getCode())
                .set(SysNotifyMessage::getInvalidReason, invalidReason)
                .set(SysNotifyMessage::getInvalidTime, invalidTime);
        return sysNotifyMessageMapper.update(null, wrapper) > 0;
    }

    /**
     * ?? markRead ?????
     *
     * @param id ??ID
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
        // ????????????????????????
        SysNotifyMessage message = getById(id);
        if (isInvalidId(receiverCompanyId)) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        if (message == null || !receiverId.equals(message.getReceiverId())
                || !receiverCompanyId.equals(message.getReceiverCompanyId())) {
            throw new ServiceException("消息不存在");
        }
        // ?????????????????????????????
        requireBizPermission(message.getBizType(), message.getBizId());
        if (!NotifyTodoStatusEnum.PENDING.getCode().equals(message.getTodoStatus())) {
            return;
        }
        LocalDateTime readTime = LocalDateTime.now();
        if (!markReadMessage(id, readTime)) {
            return;
        }
        message.setTodoStatus(NotifyTodoStatusEnum.READ.getCode());
        message.setReadTime(readTime);
        notifyMessageLogService.createLog(buildMessageLog(
                message,
                NotifyActionTypeEnum.READ.getCode(),
                receiverId,
                "按消息 ID 标记已读"
        ));
    }

    /**
     * ?? markReadByBiz ?????
     *
     * @param dto ????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markReadByBiz(NotifyReadByBizDTO dto) {
        // ?????????????????????????????
        validateReadByBizDTO(dto);
        requireBizPermission(dto.getBizType(), dto.getBizId());
        // ????????????????????????
        List<SysNotifyMessage> messages = listPendingTodoByBizAndReceiver(
                dto.getBizType(),
                dto.getBizId(),
                dto.getReceiverId(),
                dto.getReceiverCompanyId()
        );
        if (messages.isEmpty()) {
            return;
        }
        LocalDateTime readTime = LocalDateTime.now();
        for (SysNotifyMessage message : messages) {
            if (!markReadMessage(message.getId(), readTime)) {
                continue;
            }
            message.setTodoStatus(NotifyTodoStatusEnum.READ.getCode());
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
     * ?? completeTodoByBizAndReceiver ?????
     *
     * @param dto ????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTodoByBizAndReceiver(NotifyTodoCompleteDTO dto) {
        // ?????????????????????????????
        validateCompleteDTO(dto);
        requireBizPermission(dto.getBizType(), dto.getBizId());
        // ????????????????????????
        List<SysNotifyMessage> messages = listActiveTodoByBizAndReceiver(
                dto.getBizType(),
                dto.getBizId(),
                dto.getReceiverId(),
                dto.getReceiverCompanyId()
        );
        if (messages.isEmpty()) {
            return;
        }
        LocalDateTime doneTime = LocalDateTime.now();
        for (SysNotifyMessage message : messages) {
            if (!completeMessage(message.getId(), doneTime)) {
                continue;
            }
            message.setTodoStatus(NotifyTodoStatusEnum.DONE.getCode());
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
     * ?? invalidateTodoByBiz ?????
     *
     * @param dto ????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void invalidateTodoByBiz(NotifyTodoInvalidateDTO dto) {
        // ?????????????????????????????
        validateInvalidateDTO(dto);
        requireBizPermission(dto.getBizType(), dto.getBizId());
        // ????????????????????????
        List<SysNotifyMessage> messages = listActiveTodoByBiz(dto.getBizType(), dto.getBizId());
        if (messages.isEmpty()) {
            return;
        }
        LocalDateTime invalidTime = LocalDateTime.now();
        Long actionUserId = SecurityContext.getCurrentUserId();
        for (SysNotifyMessage message : messages) {
            if (!invalidateMessage(message.getId(), dto.getInvalidReason(), invalidTime)) {
                continue;
            }
            message.setTodoStatus(NotifyTodoStatusEnum.INVALID.getCode());
            message.setInvalidReason(dto.getInvalidReason());
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
     * ???????
     *
     * @param query ????
     * @return ????
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
        List<String> todoStatuses = resolveBoxStatuses(query.getBox());
        // ????????????????????????
        Page<SysNotifyMessage> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getReceiverId, query.getReceiverId())
                .eq(SysNotifyMessage::getReceiverCompanyId, query.getReceiverCompanyId())
                .in(SysNotifyMessage::getTodoStatus, todoStatuses)
                .orderByDesc(SysNotifyMessage::getCreateTime)
                .orderByDesc(SysNotifyMessage::getId);
        if (StrUtil.isNotBlank(query.getBizType())) {
            wrapper.eq(SysNotifyMessage::getBizType, query.getBizType().trim());
        }
        if (query.getBizId() != null) {
            wrapper.eq(SysNotifyMessage::getBizId, query.getBizId());
        }
        Page<SysNotifyMessage> result = sysNotifyMessageMapper.selectPage(page, wrapper);
        List<NotifyMessagePageVO> rows = result.getRecords().stream()
                .map(this::buildPageVO)
                .collect(Collectors.toList());
        return PageResult.of(rows, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * ???????
     *
     * @param receiverId receiver ID
     * @param receiverCompanyId receiver Company ID
     * @return ????
     */
    @Override
    public Long countTodo(Long receiverId, Long receiverCompanyId) {
        if (receiverId == null) {
            throw new ServiceException("当前登录用户不能为空");
        }
        if (isInvalidId(receiverCompanyId)) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        // ????????????????????????
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getReceiverId, receiverId)
                .eq(SysNotifyMessage::getReceiverCompanyId, receiverCompanyId)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode());
        Long count = sysNotifyMessageMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    /**
     * ???????
     *
     * @param bizType ????
     * @param bizId ??ID
     * @param receiverId receiver ID
     * @param receiverCompanyId receiver Company ID
     * @return ????
     */
    private List<SysNotifyMessage> listPendingTodoByBizAndReceiver(String bizType, Long bizId,
                                                                   Long receiverId, Long receiverCompanyId) {
        if (StrUtil.isBlank(bizType) || bizId == null || receiverId == null || isInvalidId(receiverCompanyId)) {
            return Collections.emptyList();
        }
        // ????????????????????????
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getBizType, bizType)
                .eq(SysNotifyMessage::getBizId, bizId)
                .eq(SysNotifyMessage::getReceiverId, receiverId)
                .eq(SysNotifyMessage::getReceiverCompanyId, receiverCompanyId)
                .eq(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.PENDING.getCode())
                .orderByAsc(SysNotifyMessage::getId);
        return sysNotifyMessageMapper.selectList(wrapper);
    }

    /**
     * ???????
     *
     * @param bizType ????
     * @param bizId ??ID
     * @return ????
     */
    private List<SysNotifyMessage> listActiveTodoByBiz(String bizType, Long bizId) {
        if (StrUtil.isBlank(bizType) || bizId == null) {
            return Collections.emptyList();
        }
        // ????????????????????????
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getBizType, bizType)
                .eq(SysNotifyMessage::getBizId, bizId)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode())
                .orderByAsc(SysNotifyMessage::getId);
        return sysNotifyMessageMapper.selectList(wrapper);
    }

    /**
     * ?? markReadMessage ?????
     *
     * @param messageId message ID
     * @param readTime ??
     * @return true ??????
     */
    private boolean markReadMessage(Long messageId, LocalDateTime readTime) {
        if (messageId == null) {
            return false;
        }
        // ????????????????????????
        LambdaUpdateWrapper<SysNotifyMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyMessage::getId, messageId)
                .eq(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.PENDING.getCode())
                .set(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.READ.getCode())
                .set(SysNotifyMessage::getReadTime, readTime);
        return sysNotifyMessageMapper.update(null, wrapper) > 0;
    }

    /**
     * ?? completeMessage ?????
     *
     * @param messageId message ID
     * @param doneTime ??
     * @return true ??????
     */
    private boolean completeMessage(Long messageId, LocalDateTime doneTime) {
        if (messageId == null) {
            return false;
        }
        // ????????????????????????
        LambdaUpdateWrapper<SysNotifyMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyMessage::getId, messageId)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode())
                .set(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.DONE.getCode())
                .set(SysNotifyMessage::getDoneTime, doneTime);
        return sysNotifyMessageMapper.update(null, wrapper) > 0;
    }

    /**
     * ???????
     *
     * @param message ??
     * @return ????
     */
    private NotifyMessagePageVO buildPageVO(SysNotifyMessage message) {
        // ????????????????????????
        NotifyMessagePageVO vo = new NotifyMessagePageVO();
        vo.setId(message.getId());
        vo.setTitle(message.getTitle());
        vo.setSummary(message.getSummary());
        vo.setBizType(message.getBizType());
        vo.setBizId(message.getBizId());
        vo.setBizNo(message.getBizNo());
        vo.setRouteType(message.getRouteType());
        vo.setRouteValue(message.getRouteValue());
        vo.setTodoStatus(message.getTodoStatus());
        vo.setInvalidReason(message.getInvalidReason());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    /**
     * ???????
     *
     * @param box ??
     * @return ????
     */
    private List<String> resolveBoxStatuses(String box) {
        String normalizedBox = StrUtil.trimToEmpty(box).toUpperCase();
        // ????????????????????????
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
     * ???????
     *
     * @param message ??
     * @param actionType ??
     * @param actionUserId action User ID
     * @param remark ??
     * @return ????
     */
    private SysNotifyMessageLog buildMessageLog(SysNotifyMessage message, String actionType, Long actionUserId, String remark) {
        // ????????????????????????
        SysNotifyMessageLog logEntity = new SysNotifyMessageLog();
        logEntity.setMessageId(message.getId());
        logEntity.setActionType(actionType);
        logEntity.setActionUserId(actionUserId);
        logEntity.setRemark(remark);
        logEntity.setSnapshotJson(JSONUtil.toJsonStr(message));
        return logEntity;
    }

    /**
     * ???????
     *
     * @param invalidReason ??
     * @return ?????
     */
    private String buildInvalidateRemark(String invalidReason) {
        NotifyInvalidReasonEnum reasonEnum = NotifyInvalidReasonEnum.getByCode(invalidReason);
        return reasonEnum == null ? "业务状态变化，待办失效" : String.format("%s，待办失效", reasonEnum.getDesc());
    }

    /**
     * ???????
     *
     * @param dto ????
     */
    private void validateReadByBizDTO(NotifyReadByBizDTO dto) {
        if (dto == null) {
            throw new ServiceException("已读参数不能为空");
        }
        // ?????????????????????????????
        validateBiz(dto.getBizType(), dto.getBizId());
        if (dto.getReceiverId() == null) {
            throw new ServiceException("已读参数缺少接收人");
        }
        if (isInvalidId(dto.getReceiverCompanyId())) {
            throw new ServiceException("已读参数缺少接收公司");
        }
    }

    /**
     * ???????
     *
     * @param dto ????
     */
    private void validateCompleteDTO(NotifyTodoCompleteDTO dto) {
        if (dto == null) {
            throw new ServiceException("完成参数不能为空");
        }
        // ?????????????????????????????
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
        // ????????????????????????
        if (NotifyBizTypeEnum.WORK_ORDER.getCode().equals(dto.getBizType())
                && !NotifyConstants.ACTION_TECH_ACCEPT.equals(dto.getActionCode())) {
            throw new ServiceException("当前阶段工单待办仅允许绑定 TECH_ACCEPT 完成");
        }
    }

    /**
     * ???????
     *
     * @param dto ????
     */
    private void validateInvalidateDTO(NotifyTodoInvalidateDTO dto) {
        if (dto == null) {
            throw new ServiceException("失效参数不能为空");
        }
        // ?????????????????????????????
        validateBiz(dto.getBizType(), dto.getBizId());
        if (StrUtil.isBlank(dto.getInvalidReason())) {
            throw new ServiceException("失效参数缺少失效原因");
        }
        // ????????????????????????
        if (NotifyInvalidReasonEnum.getByCode(dto.getInvalidReason()) == null) {
            throw new ServiceException("不支持的失效原因：" + dto.getInvalidReason());
        }
    }

    /**
     * ???????
     *
     * @param bizType ????
     * @param bizId ??ID
     */
    private void validateBiz(String bizType, Long bizId) {
        if (StrUtil.isBlank(bizType)) {
            throw new ServiceException("业务类型不能为空");
        }
        // ????????????????????????
        if (NotifyBizTypeEnum.getByCode(bizType) == null) {
            throw new ServiceException("不支持的业务类型：" + bizType);
        }
        if (bizId == null) {
            throw new ServiceException("业务ID不能为空");
        }
    }

    /**
     * ??????????
     *
     * @param bizType ????
     * @param bizId ??ID
     */
    private void requireBizPermission(String bizType, Long bizId) {
        // ?????????????????????????????
        validateBiz(bizType, bizId);
        // ????????????????????????
        if (!NotifyBizTypeEnum.WORK_ORDER.getCode().equals(bizType)) {
            throw new ServiceException("不支持的业务类型：" + bizType);
        }
        // ??????????????????????????
        WorkOrder workOrder = workOrderMapper.selectById(bizId);
        if (workOrder == null || !workOrderPermissionService.canView(workOrder)) {
            throw new ServiceException("无权查看该工单");
        }
    }

    /**
     * ????Invalid Id?
     *
     * @param id ??ID
     * @return true ??????
     */
    private boolean isInvalidId(Long id) {
        return id == null || id <= 0;
    }

}
