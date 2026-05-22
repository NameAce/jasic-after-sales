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
 * @author Zoro
 * @date 2026/04/18
 */
@Service
public class NotifyMessageServiceImpl implements NotifyMessageService {

    /**
     * 系统通知消息Mapper数据访问接口。
     *
     * @param notifyMessage 提示或消息文本，用于异常返回或通知内容。
     * @return 业务处理结果
     */
    @Resource
    private SysNotifyMessageMapper sysNotifyMessageMapper;

    /**notifyMessageLogService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private NotifyMessageLogService notifyMessageLogService;

    /**workOrderMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private WorkOrderMapper workOrderMapper;

    /**workOrderPermissionService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private WorkOrderPermissionService workOrderPermissionService;

    /**
     * 执行createMessage相关新增业务。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param notifyMessage 提示或消息文本，用于异常返回或通知内容。
     * @return 业务处理结果
     */
    @Override
    public Long createMessage(SysNotifyMessage notifyMessage) {
        sysNotifyMessageMapper.insert(notifyMessage);
        return notifyMessage.getId();
    }

    /**
     * 根据ID查询通知消息详情。
     *
     * @return 业务处理结果
     */
    @Override
    public SysNotifyMessage getById(Long id) {
        return sysNotifyMessageMapper.selectById(id);
    }

    /**
     * 获取By事件ID。
     *
     * @param eventId event ID
     * @return 业务处理结果
     */
    @Override
    public SysNotifyMessage getByEventId(Long eventId) {
        if (eventId == null) {
            return null;
        }
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getEventId, eventId)
                .orderByAsc(SysNotifyMessage::getId)
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
     * @param bizType bizType，当前业务处理所需的输入值。
     * @param receiverId receiver ID
     * @param receiverCompanyId receiver Company ID
     * @return 业务处理结果
     */
    @Override
    public List<SysNotifyMessage> listActiveTodoByBizAndReceiver(String bizType, Long bizId, Long receiverId,
                                                                 Long receiverCompanyId) {
        if (StrUtil.isBlank(bizType) || bizId == null || receiverId == null || isInvalidId(receiverCompanyId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getBizType, bizType)
                .eq(SysNotifyMessage::getBizId, bizId)
                .eq(SysNotifyMessage::getReceiverId, receiverId)
                .eq(SysNotifyMessage::getReceiverCompanyId, receiverCompanyId)
                .and(this::appendTodoTargetFilter)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode())
                .orderByAsc(SysNotifyMessage::getId);
        return sysNotifyMessageMapper.selectList(wrapper);
    }

    /**
     * 作废消息。
     *
     * @param messageId message ID
     * @param invalidReason invalidReason，当前业务处理所需的输入值。
     * @param invalidTime 时间值，用于业务节点记录或时效判断。
     */
    @Override
    public boolean invalidateMessage(Long messageId, String invalidReason, LocalDateTime invalidTime) {
        if (messageId == null) {
            return false;
        }
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
        SysNotifyMessage message = getById(id);
        if (isInvalidId(receiverCompanyId)) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        if (message == null || !receiverId.equals(message.getReceiverId())
                || !receiverCompanyId.equals(message.getReceiverCompanyId())) {
            throw new ServiceException("消息不存在");
        }
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
     * mark读取By业务。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markReadByBiz(NotifyReadByBizDTO dto) {
        validateReadByBizDTO(dto);
        requireBizPermission(dto.getBizType(), dto.getBizId());
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
     * 完成待办By业务And接收人。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTodoByBizAndReceiver(NotifyTodoCompleteDTO dto) {
        validateCompleteDTO(dto);
        requireBizPermission(dto.getBizType(), dto.getBizId());
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
     * 作废待办By业务。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void invalidateTodoByBiz(NotifyTodoInvalidateDTO dto) {
        validateInvalidateDTO(dto);
        requireBizPermission(dto.getBizType(), dto.getBizId());
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
     * 分页查询通知消息列表。
     *
     * @param query 查询条件，包含分页、筛选和权限收口所需字段。
     * @return 业务处理结果
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
     * count待办。
     *
     * @param receiverId receiver ID
     * @param receiverCompanyId receiver Company ID
     * @return 业务处理结果
     */
    @Override
    public Long countTodo(Long receiverId, Long receiverCompanyId) {
        if (receiverId == null) {
            throw new ServiceException("当前登录用户不能为空");
        }
        if (isInvalidId(receiverCompanyId)) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getReceiverId, receiverId)
                .eq(SysNotifyMessage::getReceiverCompanyId, receiverCompanyId)
                .and(this::appendTodoTargetFilter)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode());
        Long count = sysNotifyMessageMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    /**
     * 分页查询Pending待办By业务And接收人列表。
     *
     * @param bizType bizType，当前业务处理所需的输入值。
     * @param receiverId receiver ID
     * @param receiverCompanyId receiver Company ID
     * @return 业务处理结果
     */
    private List<SysNotifyMessage> listPendingTodoByBizAndReceiver(String bizType, Long bizId,
                                                                   Long receiverId, Long receiverCompanyId) {
        if (StrUtil.isBlank(bizType) || bizId == null || receiverId == null || isInvalidId(receiverCompanyId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getBizType, bizType)
                .eq(SysNotifyMessage::getBizId, bizId)
                .eq(SysNotifyMessage::getReceiverId, receiverId)
                .eq(SysNotifyMessage::getReceiverCompanyId, receiverCompanyId)
                .and(this::appendTodoTargetFilter)
                .eq(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.PENDING.getCode())
                .orderByAsc(SysNotifyMessage::getId);
        return sysNotifyMessageMapper.selectList(wrapper);
    }

    /**
     * 分页查询Active待办By业务列表。
     *
     * @param bizType bizType，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private List<SysNotifyMessage> listActiveTodoByBiz(String bizType, Long bizId) {
        if (StrUtil.isBlank(bizType) || bizId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getBizType, bizType)
                .eq(SysNotifyMessage::getBizId, bizId)
                .and(this::appendTodoTargetFilter)
                .in(SysNotifyMessage::getTodoStatus,
                        NotifyTodoStatusEnum.PENDING.getCode(),
                        NotifyTodoStatusEnum.READ.getCode())
                .orderByAsc(SysNotifyMessage::getId);
        return sysNotifyMessageMapper.selectList(wrapper);
    }

    /**
     * mark读取消息。
     *
     * @param messageId message ID
     * @param readTime 时间值，用于业务节点记录或时效判断。
     */
    private boolean markReadMessage(Long messageId, LocalDateTime readTime) {
        if (messageId == null) {
            return false;
        }
        LambdaUpdateWrapper<SysNotifyMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyMessage::getId, messageId)
                .eq(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.PENDING.getCode())
                .set(SysNotifyMessage::getTodoStatus, NotifyTodoStatusEnum.READ.getCode())
                .set(SysNotifyMessage::getReadTime, readTime);
        return sysNotifyMessageMapper.update(null, wrapper) > 0;
    }

    /**
     * 完成消息。
     *
     * @param messageId message ID
     * @param doneTime 时间值，用于业务节点记录或时效判断。
     */
    private boolean completeMessage(Long messageId, LocalDateTime doneTime) {
        if (messageId == null) {
            return false;
        }
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
     * @param message 提示或消息文本，用于异常返回或通知内容。
     * @return 业务处理结果
     */
    private NotifyMessagePageVO buildPageVO(SysNotifyMessage message) {
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
     * 解析BoxStatuses。
     *
     * @param box box，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private List<String> resolveBoxStatuses(String box) {
        String normalizedBox = StrUtil.trimToEmpty(box).toUpperCase();
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
     * @param message 提示或消息文本，用于异常返回或通知内容。
     * @param actionType actionType，当前业务处理所需的输入值。
     * @param actionUserId action User ID
     * @param remark remark，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private SysNotifyMessageLog buildMessageLog(SysNotifyMessage message, String actionType, Long actionUserId, String remark) {
        SysNotifyMessageLog logEntity = new SysNotifyMessageLog();
        logEntity.setMessageId(message.getId());
        logEntity.setActionType(actionType);
        logEntity.setActionUserId(actionUserId);
        logEntity.setRemark(remark);
        logEntity.setSnapshotJson(JSONUtil.toJsonStr(message));
        return logEntity;
    }

    /**
     * 构建作废Remark。
     *
     * @param invalidReason invalidReason，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private String buildInvalidateRemark(String invalidReason) {
        NotifyInvalidReasonEnum reasonEnum = NotifyInvalidReasonEnum.getByCode(invalidReason);
        return reasonEnum == null ? "业务状态变化，待办失效" : String.format("%s，待办失效", reasonEnum.getDesc());
    }

    /**
     * 校验读取By业务DTO。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    private void validateReadByBizDTO(NotifyReadByBizDTO dto) {
        if (dto == null) {
            throw new ServiceException("已读参数不能为空");
        }
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
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    private void validateCompleteDTO(NotifyTodoCompleteDTO dto) {
        if (dto == null) {
            throw new ServiceException("完成参数不能为空");
        }
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
        if (NotifyBizTypeEnum.WORK_ORDER.getCode().equals(dto.getBizType())
                && !NotifyConstants.ACTION_TECH_ACCEPT.equals(dto.getActionCode())) {
            throw new ServiceException("当前阶段工单待办仅允许绑定 TECH_ACCEPT 完成");
        }
    }

    /**
     * 校验作废DTO。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    private void validateInvalidateDTO(NotifyTodoInvalidateDTO dto) {
        if (dto == null) {
            throw new ServiceException("失效参数不能为空");
        }
        validateBiz(dto.getBizType(), dto.getBizId());
        if (StrUtil.isBlank(dto.getInvalidReason())) {
            throw new ServiceException("失效参数缺少失效原因");
        }
        if (NotifyInvalidReasonEnum.getByCode(dto.getInvalidReason()) == null) {
            throw new ServiceException("不支持的失效原因：" + dto.getInvalidReason());
        }
    }

    /**
     * 校验业务。
     *
     * @param bizType bizType，当前业务处理所需的输入值。
     */
    private void validateBiz(String bizType, Long bizId) {
        if (StrUtil.isBlank(bizType)) {
            throw new ServiceException("业务类型不能为空");
        }
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
     * @param bizType bizType，当前业务处理所需的输入值。
     */
    private void requireBizPermission(String bizType, Long bizId) {
        validateBiz(bizType, bizId);
        if (!NotifyBizTypeEnum.WORK_ORDER.getCode().equals(bizType)) {
            throw new ServiceException("不支持的业务类型：" + bizType);
        }
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



