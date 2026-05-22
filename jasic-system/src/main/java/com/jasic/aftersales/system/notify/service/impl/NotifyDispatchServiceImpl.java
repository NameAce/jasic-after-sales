package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchResultCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import com.jasic.aftersales.system.notify.mapper.SysNotifyDispatchMapper;
import com.jasic.aftersales.system.notify.service.NotifyChannelSender;
import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
import com.jasic.aftersales.system.notify.support.NotifyChannelSendContext;
import com.jasic.aftersales.system.notify.support.NotifyChannelSendResult;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.notify.support.NotifyDispatchPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 通知分发服务实现。
 *
 * <p>负责外部分发任务的状态机维护，包括自动发送、失败重试、超时恢复和死信处理。
 * 渠道发送器只返回发送结果，由本类统一决定如何更新分发状态。</p>
 *
 * @author Zoro
 * @date 2026/04/21
 */
@Slf4j
@Service
public class NotifyDispatchServiceImpl implements NotifyDispatchService {

    /**sysNotifyDispatchMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysNotifyDispatchMapper sysNotifyDispatchMapper;

    /**notifyChannelSenders 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private List<NotifyChannelSender> notifyChannelSenders = Collections.emptyList();

    /**transactionTemplate 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private TransactionTemplate transactionTemplate;

    /**dispatchRetryMaxCount 字段，用于当前类内部业务处理。*/
    @Value("${jasic.notify.dispatch-retry-max-count:" + NotifyConstants.DISPATCH_RETRY_MAX_COUNT + "}")
    private int dispatchRetryMaxCount = NotifyConstants.DISPATCH_RETRY_MAX_COUNT;

    /**dispatchRetryDelayMinutes 字段，用于当前类内部业务处理。*/
    @Value("${jasic.notify.dispatch-retry-delay-minutes:" + NotifyConstants.DISPATCH_RETRY_DELAY_MINUTES + "}")
    private long dispatchRetryDelayMinutes = NotifyConstants.DISPATCH_RETRY_DELAY_MINUTES;

    /**
     * 超时恢复结果文案。
     */
    private static final String TIMEOUT_RECOVER_RESULT_MESSAGE = "通知分发处理超时，系统已恢复为待重试";

    /**
     * 自动重试耗尽后的兜底文案。
     */
    private static final String DEAD_RETRY_EXCEEDED_MESSAGE = "通知分发超过最大重试次数，已转入死信";

    /**
     * 人工标记死信时的默认结果码。
     */
    private static final String DEFAULT_MANUAL_DEAD_RESULT_CODE =
            NotifyDispatchResultCodeEnum.DEAD_MANUAL_CLOSED.getCode();

    /**
     * 人工标记死信时的默认说明。
     */
    private static final String DEFAULT_MANUAL_DEAD_RESULT_MESSAGE = "通知分发已标记为死信，等待人工处理";

    /**
     * {@inheritDoc}
     */
    @Override
    public Long createDispatch(SysNotifyDispatch dispatch) {
        SysNotifyDispatch existing = getExistingDispatch(dispatch);
        if (existing != null) {
            return existing.getId();
        }
        sysNotifyDispatchMapper.insert(dispatch);
        return dispatch.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysNotifyDispatch getById(Long id) {
        return sysNotifyDispatchMapper.selectById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysNotifyDispatch> listSendableDispatches(LocalDateTime now, Integer limit) {
        LocalDateTime targetTime = now == null ? LocalDateTime.now() : now;
        LambdaQueryWrapper<SysNotifyDispatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(condition -> condition
                        .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PENDING.getCode())
                        .or(failed -> failed.eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                                .lt(SysNotifyDispatch::getRetryCount, dispatchRetryMaxCount)
                                .and(next -> next.isNull(SysNotifyDispatch::getNextRetryTime)
                                        .or()
                                        .le(SysNotifyDispatch::getNextRetryTime, targetTime))))
                .orderByAsc(SysNotifyDispatch::getId);
        if (limit != null && limit > 0) {
            wrapper.last("limit " + limit);
        }
        return sysNotifyDispatchMapper.selectList(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysNotifyDispatch> listTimeoutProcessingDispatches(LocalDateTime timeoutBefore, Integer limit) {
        if (timeoutBefore == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysNotifyDispatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PROCESSING.getCode())
                .le(SysNotifyDispatch::getProcessingTime, timeoutBefore)
                .orderByAsc(SysNotifyDispatch::getId);
        if (limit != null && limit > 0) {
            wrapper.last("limit " + limit);
        }
        return sysNotifyDispatchMapper.selectList(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean markProcessing(Long dispatchId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .and(condition -> condition
                        .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PENDING.getCode())
                        .or(failed -> failed.eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                                .lt(SysNotifyDispatch::getRetryCount, dispatchRetryMaxCount)
                                .and(next -> next.isNull(SysNotifyDispatch::getNextRetryTime)
                                        .or()
                                        .le(SysNotifyDispatch::getNextRetryTime, now))))
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PROCESSING.getCode())
                // 记录发送开始时间，便于 Quartz 在宕机或线程异常后识别卡死任务。
                .set(SysNotifyDispatch::getProcessingTime, now)
                .set(SysNotifyDispatch::getNextRetryTime, null);
        return sysNotifyDispatchMapper.update(null, wrapper) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markSuccess(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson) {
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PROCESSING.getCode())
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.SUCCESS.getCode())
                .set(SysNotifyDispatch::getProcessingTime, null)
                .set(SysNotifyDispatch::getResultCode, trimValue(resultCode, 64))
                .set(SysNotifyDispatch::getResultMessage, trimValue(resultMessage, 500))
                .set(SysNotifyDispatch::getChannelResponseJson, trimText(channelResponseJson))
                .set(SysNotifyDispatch::getNextRetryTime, null)
                .set(SysNotifyDispatch::getSentTime, LocalDateTime.now());
        sysNotifyDispatchMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markFailed(Long dispatchId, Integer retryCount, LocalDateTime nextRetryTime,
                           String resultCode, String resultMessage, String channelResponseJson) {
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PROCESSING.getCode())
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                .set(SysNotifyDispatch::getRetryCount, retryCount)
                .set(SysNotifyDispatch::getProcessingTime, null)
                .set(SysNotifyDispatch::getNextRetryTime, nextRetryTime)
                .set(SysNotifyDispatch::getResultCode, trimValue(resultCode, 64))
                .set(SysNotifyDispatch::getResultMessage, trimValue(resultMessage, 500))
                .set(SysNotifyDispatch::getChannelResponseJson, trimText(channelResponseJson));
        sysNotifyDispatchMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markSkipped(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson) {
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PROCESSING.getCode())
                // 业务不可发送属于终态，不允许再进入自动重试队列。
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.SKIPPED.getCode())
                .set(SysNotifyDispatch::getProcessingTime, null)
                .set(SysNotifyDispatch::getResultCode, trimValue(resultCode, 64))
                .set(SysNotifyDispatch::getResultMessage, trimValue(resultMessage, 500))
                .set(SysNotifyDispatch::getChannelResponseJson, trimText(channelResponseJson))
                .set(SysNotifyDispatch::getNextRetryTime, null);
        sysNotifyDispatchMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int recoverTimeoutProcessingDispatches(LocalDateTime timeoutBefore) {
        if (timeoutBefore == null) {
            return 0;
        }
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PROCESSING.getCode())
                .le(SysNotifyDispatch::getProcessingTime, timeoutBefore)
                // 超时恢复后不直接改成 PENDING，而是先回 FAILED，让排障页面能看出这是恢复而非首发。
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                .set(SysNotifyDispatch::getProcessingTime, null)
                .set(SysNotifyDispatch::getNextRetryTime, null)
                .set(SysNotifyDispatch::getResultCode, NotifyDispatchResultCodeEnum.FAILED_UNKNOWN.getCode())
                .set(SysNotifyDispatch::getResultMessage, TIMEOUT_RECOVER_RESULT_MESSAGE);
        return sysNotifyDispatchMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markDead(Long dispatchId, String resultCode, String resultMessage) {
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .and(condition -> condition
                        .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PENDING.getCode())
                        .or()
                        .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                        .or()
                        .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PROCESSING.getCode()))
                // 死信是外部通知的人工接管态，自动任务必须完全停止继续发送。
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.DEAD.getCode())
                .set(SysNotifyDispatch::getProcessingTime, null)
                .set(SysNotifyDispatch::getNextRetryTime, null)
                .set(SysNotifyDispatch::getResultCode, trimValue(resolveManualDeadResultCode(resultCode), 64))
                .set(SysNotifyDispatch::getResultMessage, trimValue(resolveManualDeadResultMessage(resultMessage), 500));
        sysNotifyDispatchMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetForRetry(Long dispatchId) {
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .and(condition -> condition
                        .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                        .or()
                        .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.DEAD.getCode()))
                // 人工重试必须清空旧失败上下文，避免后台再次查看时混淆这一轮与上一轮的结果。
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PENDING.getCode())
                .set(SysNotifyDispatch::getRetryCount, 0)
                .set(SysNotifyDispatch::getProcessingTime, null)
                .set(SysNotifyDispatch::getNextRetryTime, null)
                .set(SysNotifyDispatch::getResultCode, null)
                .set(SysNotifyDispatch::getResultMessage, null)
                .set(SysNotifyDispatch::getChannelResponseJson, null)
                .set(SysNotifyDispatch::getSentTime, null);
        sysNotifyDispatchMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int consumePendingDispatches() {
        List<SysNotifyDispatch> dispatches = listSendableDispatches(LocalDateTime.now(), NotifyConstants.DISPATCH_SEND_BATCH_SIZE);
        int successCount = 0;
        for (SysNotifyDispatch dispatch : dispatches) {
            if (!markProcessing(dispatch.getId())) {
                continue;
            }
            try {
                transactionTemplate.execute(status -> {
                    consumeSingleDispatch(dispatch.getId());
                    return null;
                });
                successCount++;
            } catch (Exception ex) {
                log.error("Consume notify dispatch failed. dispatchId={}", dispatch.getId(), ex);
                // 渠道异常统一走这里，避免不同发送器各自决定失败状态而导致重试策略分裂。
                markDispatchFailed(dispatch.getId(), ex);
            }
        }
        return successCount;
    }

    /**
     * 消费单条已抢占分发任务。
     *
     * @param dispatchId 分发任务ID
     */
    private void consumeSingleDispatch(Long dispatchId) {
        SysNotifyDispatch dispatch = getRequiredProcessingDispatch(dispatchId);
        NotifyDispatchPayload payload = parsePayload(dispatch);
        NotifyChannelSender sender = resolveSender(dispatch.getChannelType());
        NotifyChannelSendContext context = new NotifyChannelSendContext();
        context.setDispatch(dispatch);
        context.setPayload(payload);
        NotifyChannelSendResult sendResult = sender.send(context);
        applySendResult(dispatch, sendResult);
    }

    /**
     * 查询并校验处理中任务。
     *
     * @param dispatchId 分发任务ID
     * @return 处理中任务
     */
    private SysNotifyDispatch getRequiredProcessingDispatch(Long dispatchId) {
        SysNotifyDispatch dispatch = sysNotifyDispatchMapper.selectById(dispatchId);
        if (dispatch == null) {
            throw new ServiceException("Notify dispatch not found");
        }
        if (!NotifyDispatchStatusEnum.PROCESSING.getCode().equals(dispatch.getDispatchStatus())) {
            throw new ServiceException("Notify dispatch is not processing");
        }
        return dispatch;
    }

    /**
     * 解析分发载荷。
     *
     * @param dispatch 分发任务
     * @return 分发载荷
     */
    private NotifyDispatchPayload parsePayload(SysNotifyDispatch dispatch) {
        if (StrUtil.isBlank(dispatch.getPayloadJson())) {
            throw new ServiceException("Notify dispatch payload cannot be blank");
        }
        try {
            NotifyDispatchPayload payload = JSONUtil.toBean(dispatch.getPayloadJson(), NotifyDispatchPayload.class);
            if (payload == null) {
                throw new ServiceException("Notify dispatch payload parse result is null");
            }
            return payload;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("Notify dispatch payload parse failed");
        }
    }

    /**
     * 解析渠道发送器。
     *
     * @param channelType 渠道类型
     * @return 渠道发送器
     */
    private NotifyChannelSender resolveSender(String channelType) {
        for (NotifyChannelSender sender : notifyChannelSenders) {
            if (sender.supports(channelType)) {
                return sender;
            }
        }
        throw new ServiceException("Unsupported notify channel sender: " + channelType);
    }

    /**
     * 按发送结果推进分发状态机。
     *
     * @param dispatch 分发任务
     * @param sendResult 发送结果
     */
    private void applySendResult(SysNotifyDispatch dispatch, NotifyChannelSendResult sendResult) {
        if (sendResult == null) {
            throw new ServiceException("Notify channel sender returned null result");
        }
        String status = sendResult.getDispatchStatus();
        if (NotifyDispatchStatusEnum.SUCCESS.getCode().equals(status)) {
            markSuccess(dispatch.getId(), sendResult.getResultCode(), sendResult.getResultMessage(),
                    sendResult.getChannelResponseJson());
            return;
        }
        if (NotifyDispatchStatusEnum.SKIPPED.getCode().equals(status)) {
            markSkipped(dispatch.getId(), sendResult.getResultCode(), sendResult.getResultMessage(),
                    sendResult.getChannelResponseJson());
            return;
        }
        if (!NotifyDispatchStatusEnum.FAILED.getCode().equals(status)) {
            throw new ServiceException("Unsupported dispatch status returned by sender: " + status);
        }
        int nextRetryCount = dispatch.getRetryCount() == null ? 1 : dispatch.getRetryCount() + 1;
        if (nextRetryCount >= dispatchRetryMaxCount) {
            // 自动重试预算耗尽后直接进入死信，避免 Quartz 永久循环消费同一条失败任务。
            markDispatchDead(dispatch.getId(), nextRetryCount, sendResult.getResultMessage(),
                    sendResult.getChannelResponseJson());
            return;
        }
        markFailed(dispatch.getId(), nextRetryCount, LocalDateTime.now().plusMinutes(dispatchRetryDelayMinutes),
                sendResult.getResultCode(), sendResult.getResultMessage(), sendResult.getChannelResponseJson());
    }

    /**
     * 统一回写分发异常。
     *
     * @param dispatchId 分发任务ID
     * @param ex 发送异常
     */
    private void markDispatchFailed(Long dispatchId, Exception ex) {
        SysNotifyDispatch current = sysNotifyDispatchMapper.selectById(dispatchId);
        int nextRetryCount = current == null || current.getRetryCount() == null ? 1 : current.getRetryCount() + 1;
        if (nextRetryCount >= dispatchRetryMaxCount) {
            markDispatchDead(dispatchId, nextRetryCount, buildErrorMessage(ex), null);
            return;
        }
        markFailed(
                dispatchId,
                nextRetryCount,
                LocalDateTime.now().plusMinutes(dispatchRetryDelayMinutes),
                NotifyDispatchResultCodeEnum.FAILED_CHANNEL_REQUEST.getCode(),
                buildErrorMessage(ex),
                null
        );
    }

    /**
     * 标记自动重试耗尽的死信任务。
     *
     * @param dispatchId 分发任务ID
     * @param retryCount 最终重试次数
     * @param resultMessage 渠道失败说明
     * @param channelResponseJson 渠道响应快照
     */
    private void markDispatchDead(Long dispatchId, Integer retryCount, String resultMessage, String channelResponseJson) {
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PROCESSING.getCode())
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.DEAD.getCode())
                .set(SysNotifyDispatch::getRetryCount, retryCount)
                .set(SysNotifyDispatch::getProcessingTime, null)
                .set(SysNotifyDispatch::getNextRetryTime, null)
                .set(SysNotifyDispatch::getResultCode, NotifyDispatchResultCodeEnum.DEAD_RETRY_EXCEEDED.getCode())
                .set(SysNotifyDispatch::getResultMessage, trimValue(buildDeadResultMessage(resultMessage), 500))
                .set(SysNotifyDispatch::getChannelResponseJson, trimText(channelResponseJson));
        sysNotifyDispatchMapper.update(null, wrapper);
    }

    /**
     * 生成自动进入死信的结果说明。
     *
     * @param resultMessage 最近一次失败说明
     * @return 死信说明
     */
    private String buildDeadResultMessage(String resultMessage) {
        if (StrUtil.isBlank(resultMessage)) {
            return DEAD_RETRY_EXCEEDED_MESSAGE;
        }
        String message = DEAD_RETRY_EXCEEDED_MESSAGE + "：" + resultMessage;
        return message.length() > 500 ? StrUtil.sub(message, 0, 500) : message;
    }

    /**
     * 查询是否已存在同一业务接收人的分发任务。
     *
     * @param dispatch 分发任务
     * @return 已存在任务
     */
    private SysNotifyDispatch getExistingDispatch(SysNotifyDispatch dispatch) {
        LambdaQueryWrapper<SysNotifyDispatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyDispatch::getBizType, dispatch.getBizType())
                .eq(SysNotifyDispatch::getBizId, dispatch.getBizId())
                .eq(SysNotifyDispatch::getTemplateCode, dispatch.getTemplateCode())
                .eq(SysNotifyDispatch::getChannelType, dispatch.getChannelType())
                .eq(SysNotifyDispatch::getReceiverType, dispatch.getReceiverType());
        if (StrUtil.isNotBlank(dispatch.getSceneCode())) {
            wrapper.eq(SysNotifyDispatch::getSceneCode, dispatch.getSceneCode());
        }
        if (StrUtil.isNotBlank(dispatch.getTargetType())) {
            wrapper.eq(SysNotifyDispatch::getTargetType, dispatch.getTargetType());
        }
        if (dispatch.getReceiverId() != null) {
            wrapper.eq(SysNotifyDispatch::getReceiverId, dispatch.getReceiverId());
        } else {
            wrapper.isNull(SysNotifyDispatch::getReceiverId);
        }
        wrapper.last("limit 1");
        return sysNotifyDispatchMapper.selectOne(wrapper);
    }

    /**
     * 裁剪异常文本。
     *
     * @param ex 发送异常
     * @return 裁剪后的异常文本
     */
    private String buildErrorMessage(Exception ex) {
        String message = ex == null ? null : StrUtil.trim(ex.getMessage());
        if (StrUtil.isBlank(message) && ex != null) {
            message = ex.getClass().getSimpleName();
        }
        return trimValue(message, 500);
    }

    /**
     * 解析人工死信结果码。
     *
     * @param resultCode 传入结果码
     * @return 最终结果码
     */
    private String resolveManualDeadResultCode(String resultCode) {
        if (StrUtil.isBlank(resultCode)) {
            return DEFAULT_MANUAL_DEAD_RESULT_CODE;
        }
        return resultCode;
    }

    /**
     * 解析人工死信说明。
     *
     * @param resultMessage 传入说明
     * @return 最终说明
     */
    private String resolveManualDeadResultMessage(String resultMessage) {
        if (StrUtil.isBlank(resultMessage)) {
            return DEFAULT_MANUAL_DEAD_RESULT_MESSAGE;
        }
        return resultMessage;
    }

    /**
     * 裁剪定长字段，避免超长文本导致更新失败。
     *
     * @param value 原始值
     * @param maxLength 最大长度
     * @return 裁剪后的值
     */
    private String trimValue(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String actual = value.trim();
        if (actual.isEmpty()) {
            return null;
        }
        if (actual.length() <= maxLength) {
            return actual;
        }
        return StrUtil.sub(actual, 0, maxLength);
    }

    /**
     * 裁剪长文本快照。
     *
     * @param value 原始文本
     * @return 裁剪后的文本
     */
    private String trimText(String value) {
        if (value == null) {
            return null;
        }
        String actual = value.trim();
        return actual.isEmpty() ? null : actual;
    }
}
