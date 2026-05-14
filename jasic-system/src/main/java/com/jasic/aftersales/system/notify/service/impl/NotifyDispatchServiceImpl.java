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
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Notify dispatch service implementation.
 *
 * @author Codex
 * @date 2026/04/21
 */
@Slf4j
@Service
public class NotifyDispatchServiceImpl implements NotifyDispatchService {

    @Resource
    private SysNotifyDispatchMapper sysNotifyDispatchMapper;

    @Resource
    private List<NotifyChannelSender> notifyChannelSenders = Collections.emptyList();

    /**
     * Transaction模板模板依赖。
     *
     * @param dispatch 参数
     * @return 处理结果
     */
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 执行createDispatch相关新增业务。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param dispatch 参数
     * @return 处理结果
     */
    @Override
    public Long createDispatch(SysNotifyDispatch dispatch) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyDispatch existing = getExistingDispatch(dispatch);
        if (existing != null) {
            return existing.getId();
        }
        // 调用insert方法，复用统一能力并保证业务规则一致。
        sysNotifyDispatchMapper.insert(dispatch);
        return dispatch.getId();
    }

    /**
     * 根据ID查询通知分发详情。
     *
     * @return 处理结果
     */
    @Override
    public SysNotifyDispatch getById(Long id) {
        return sysNotifyDispatchMapper.selectById(id);
    }

    /**
     * 分页查询SendableDispatches列表。
     *
     * @param now 参数
     * @param limit 参数
     * @return 处理结果
     */
    @Override
    public List<SysNotifyDispatch> listSendableDispatches(LocalDateTime now, Integer limit) {
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime targetTime = now == null ? LocalDateTime.now() : now;
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyDispatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(condition -> condition
                        .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PENDING.getCode())
                        .or(retry -> retry.eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                                .lt(SysNotifyDispatch::getRetryCount, NotifyConstants.DISPATCH_RETRY_MAX_COUNT)
                                .and(next -> next.isNull(SysNotifyDispatch::getNextRetryTime)
                                        .or()
                                        .le(SysNotifyDispatch::getNextRetryTime, targetTime))))
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysNotifyDispatch::getId);
        if (limit != null && limit > 0) {
            // 调用last方法，复用统一能力并保证业务规则一致。
            wrapper.last("limit " + limit);
        }
        return sysNotifyDispatchMapper.selectList(wrapper);
    }

    /**
     * markProcessing。
     *
     * @param dispatchId dispatch ID
     */
    @Override
    public boolean markProcessing(Long dispatchId) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .and(condition -> condition
                        .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PENDING.getCode())
                        .or(retry -> retry.eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                                .lt(SysNotifyDispatch::getRetryCount, NotifyConstants.DISPATCH_RETRY_MAX_COUNT)))
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PROCESSING.getCode())
                // 调用set方法，复用统一能力并保证业务规则一致。
                .set(SysNotifyDispatch::getNextRetryTime, null);
        return sysNotifyDispatchMapper.update(null, wrapper) > 0;
    }

    /**
     * markSuccess。
     *
     * @param dispatchId dispatch ID
     * @param resultCode 参数
     * @param resultMessage 参数
     * @param channelResponseJson 参数
     */
    @Override
    public void markSuccess(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.SUCCESS.getCode())
                .set(SysNotifyDispatch::getResultCode, trimValue(resultCode, 64))
                .set(SysNotifyDispatch::getResultMessage, trimValue(resultMessage, 500))
                .set(SysNotifyDispatch::getChannelResponseJson, trimText(channelResponseJson))
                .set(SysNotifyDispatch::getNextRetryTime, null)
                // 调用now方法，复用统一能力并保证业务规则一致。
                .set(SysNotifyDispatch::getSentTime, LocalDateTime.now());
        // 调用update方法，复用统一能力并保证业务规则一致。
        sysNotifyDispatchMapper.update(null, wrapper);
    }

    /**
     * markFailed。
     *
     * @param dispatchId dispatch ID
     * @param retryCount 参数
     * @param nextRetryTime 参数
     * @param resultCode 参数
     * @param resultMessage 参数
     * @param channelResponseJson 参数
     */
    @Override
    public void markFailed(Long dispatchId, Integer retryCount, LocalDateTime nextRetryTime,
                           String resultCode, String resultMessage, String channelResponseJson) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                .set(SysNotifyDispatch::getRetryCount, retryCount)
                .set(SysNotifyDispatch::getNextRetryTime, nextRetryTime)
                .set(SysNotifyDispatch::getResultCode, trimValue(resultCode, 64))
                .set(SysNotifyDispatch::getResultMessage, trimValue(resultMessage, 500))
                // 调用trimText方法，复用统一能力并保证业务规则一致。
                .set(SysNotifyDispatch::getChannelResponseJson, trimText(channelResponseJson));
        // 调用update方法，复用统一能力并保证业务规则一致。
        sysNotifyDispatchMapper.update(null, wrapper);
    }

    /**
     * markSkipped。
     *
     * @param dispatchId dispatch ID
     * @param resultCode 参数
     * @param resultMessage 参数
     * @param channelResponseJson 参数
     */
    @Override
    public void markSkipped(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.SKIPPED.getCode())
                .set(SysNotifyDispatch::getResultCode, trimValue(resultCode, 64))
                .set(SysNotifyDispatch::getResultMessage, trimValue(resultMessage, 500))
                .set(SysNotifyDispatch::getChannelResponseJson, trimText(channelResponseJson))
                // 调用set方法，复用统一能力并保证业务规则一致。
                .set(SysNotifyDispatch::getNextRetryTime, null);
        // 调用update方法，复用统一能力并保证业务规则一致。
        sysNotifyDispatchMapper.update(null, wrapper);
    }

    /**
     * 消费PendingDispatches。
     *
     * @return 处理结果
     */
    @Override
    public int consumePendingDispatches() {
        // 说明：执行该步骤以保证业务流程正确。
        List<SysNotifyDispatch> dispatches = listSendableDispatches(LocalDateTime.now(), NotifyConstants.DISPATCH_SEND_BATCH_SIZE);
        int successCount = 0;
        for (SysNotifyDispatch dispatch : dispatches) {
            if (!markProcessing(dispatch.getId())) {
                continue;
            }
            try {
                transactionTemplate.execute(status -> {
                    // 调用getId方法，复用统一能力并保证业务规则一致。
                    consumeSingleDispatch(dispatch.getId());
                    return null;
                });
                successCount++;
            } catch (Exception ex) {
                // 调用getId方法，复用统一能力并保证业务规则一致。
                log.error("Consume notify dispatch failed. dispatchId={}", dispatch.getId(), ex);
                // 调用getId方法，复用统一能力并保证业务规则一致。
                markDispatchFailed(dispatch.getId(), ex);
            }
        }
        return successCount;
    }

    /**
     * 消费Single分发。
     *
     * @param dispatchId dispatch ID
     */
    private void consumeSingleDispatch(Long dispatchId) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyDispatch dispatch = getRequiredProcessingDispatch(dispatchId);
        // 调用parsePayload方法，复用统一能力并保证业务规则一致。
        NotifyDispatchPayload payload = parsePayload(dispatch);
        // 调用getChannelType方法，复用统一能力并保证业务规则一致。
        NotifyChannelSender sender = resolveSender(dispatch.getChannelType());
        // 调用NotifyChannelSendContext方法，复用统一能力并保证业务规则一致。
        NotifyChannelSendContext context = new NotifyChannelSendContext();
        // 调用setDispatch方法，复用统一能力并保证业务规则一致。
        context.setDispatch(dispatch);
        // 调用setPayload方法，复用统一能力并保证业务规则一致。
        context.setPayload(payload);
        // 调用send方法，复用统一能力并保证业务规则一致。
        NotifyChannelSendResult sendResult = sender.send(context);
        // 调用applySendResult方法，复用统一能力并保证业务规则一致。
        applySendResult(dispatch, sendResult);
    }

    /**
     * 获取RequiredProcessing分发。
     *
     * @param dispatchId dispatch ID
     * @return 处理结果
     */
    private SysNotifyDispatch getRequiredProcessingDispatch(Long dispatchId) {
        // 说明：执行该步骤以保证业务流程正确。
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
     * parsePayload。
     *
     * @param dispatch 参数
     * @return 处理结果
     */
    private NotifyDispatchPayload parsePayload(SysNotifyDispatch dispatch) {
        if (StrUtil.isBlank(dispatch.getPayloadJson())) {
            // 说明：执行该步骤以保证业务流程正确。
            throw new ServiceException("Notify dispatch payload cannot be blank");
        }
        try {
            // 调用getPayloadJson方法，复用统一能力并保证业务规则一致。
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
     * 解析发送人。
     *
     * @param channelType 参数
     * @return 处理结果
     */
    private NotifyChannelSender resolveSender(String channelType) {
        // 说明：执行该步骤以保证业务流程正确。
        for (NotifyChannelSender sender : notifyChannelSenders) {
            if (sender.supports(channelType)) {
                return sender;
            }
        }
        throw new ServiceException("Unsupported notify channel sender: " + channelType);
    }

    /**
     * apply发送结果。
     *
     * @param dispatch 参数
     * @param sendResult 参数
     */
    private void applySendResult(SysNotifyDispatch dispatch, NotifyChannelSendResult sendResult) {
        if (sendResult == null) {
            // 说明：执行该步骤以保证业务流程正确。
            throw new ServiceException("Notify channel sender returned null result");
        }
        // 调用getDispatchStatus方法，复用统一能力并保证业务规则一致。
        String status = sendResult.getDispatchStatus();
        if (NotifyDispatchStatusEnum.SUCCESS.getCode().equals(status)) {
            markSuccess(dispatch.getId(), sendResult.getResultCode(), sendResult.getResultMessage(),
                    // 调用getChannelResponseJson方法，复用统一能力并保证业务规则一致。
                    sendResult.getChannelResponseJson());
            return;
        }
        if (NotifyDispatchStatusEnum.SKIPPED.getCode().equals(status)) {
            markSkipped(dispatch.getId(), sendResult.getResultCode(), sendResult.getResultMessage(),
                    // 调用getChannelResponseJson方法，复用统一能力并保证业务规则一致。
                    sendResult.getChannelResponseJson());
            return;
        }
        if (!NotifyDispatchStatusEnum.FAILED.getCode().equals(status)) {
            throw new ServiceException("Unsupported dispatch status returned by sender: " + status);
        }
        // 调用getRetryCount方法，复用统一能力并保证业务规则一致。
        int nextRetryCount = dispatch.getRetryCount() == null ? 1 : dispatch.getRetryCount() + 1;
        LocalDateTime nextRetryTime = nextRetryCount >= NotifyConstants.DISPATCH_RETRY_MAX_COUNT
                ? null
                // 调用plusMinutes方法，复用统一能力并保证业务规则一致。
                : LocalDateTime.now().plusMinutes(NotifyConstants.DISPATCH_RETRY_DELAY_MINUTES);
        markFailed(dispatch.getId(), nextRetryCount, nextRetryTime,
                // 调用getChannelResponseJson方法，复用统一能力并保证业务规则一致。
                sendResult.getResultCode(), sendResult.getResultMessage(), sendResult.getChannelResponseJson());
    }

    /**
     * mark分发Failed。
     *
     * @param dispatchId dispatch ID
     * @param ex 参数
     */
    private void markDispatchFailed(Long dispatchId, Exception ex) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyDispatch current = sysNotifyDispatchMapper.selectById(dispatchId);
        // 调用getRetryCount方法，复用统一能力并保证业务规则一致。
        int nextRetryCount = current == null || current.getRetryCount() == null ? 1 : current.getRetryCount() + 1;
        LocalDateTime nextRetryTime = nextRetryCount >= NotifyConstants.DISPATCH_RETRY_MAX_COUNT
                ? null
                // 调用plusMinutes方法，复用统一能力并保证业务规则一致。
                : LocalDateTime.now().plusMinutes(NotifyConstants.DISPATCH_RETRY_DELAY_MINUTES);
        markFailed(
                dispatchId,
                nextRetryCount,
                nextRetryTime,
                NotifyDispatchResultCodeEnum.FAILED_CHANNEL_REQUEST.getCode(),
                buildErrorMessage(ex),
                null
        );
    }

    /**
     * 获取Existing分发。
     *
     * @param dispatch 参数
     * @return 处理结果
     */
    private SysNotifyDispatch getExistingDispatch(SysNotifyDispatch dispatch) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyDispatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyDispatch::getBizType, dispatch.getBizType())
                .eq(SysNotifyDispatch::getBizId, dispatch.getBizId())
                .eq(SysNotifyDispatch::getTemplateCode, dispatch.getTemplateCode())
                .eq(SysNotifyDispatch::getChannelType, dispatch.getChannelType())
                // 调用getReceiverType方法，复用统一能力并保证业务规则一致。
                .eq(SysNotifyDispatch::getReceiverType, dispatch.getReceiverType());
        if (dispatch.getReceiverId() != null) {
            // 调用getReceiverId方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyDispatch::getReceiverId, dispatch.getReceiverId());
        } else {
            // 调用isNull方法，复用统一能力并保证业务规则一致。
            wrapper.isNull(SysNotifyDispatch::getReceiverId);
        }
        // 调用last方法，复用统一能力并保证业务规则一致。
        wrapper.last("limit 1");
        return sysNotifyDispatchMapper.selectOne(wrapper);
    }

    /**
     * 构建Error消息。
     *
     * @param ex 参数
     * @return 处理结果
     */
    private String buildErrorMessage(Exception ex) {
        // 调用getMessage方法，复用统一能力并保证业务规则一致。
        String message = ex == null ? null : StrUtil.trim(ex.getMessage());
        if (StrUtil.isBlank(message) && ex != null) {
            // 调用getSimpleName方法，复用统一能力并保证业务规则一致。
            message = ex.getClass().getSimpleName();
        }
        return trimValue(message, 500);
    }

    /**
     * trim值。
     *
     * @param value 参数
     * @param maxLength 参数
     * @return 处理结果
     */
    private String trimValue(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String actual = value.trim();
        if (actual.length() <= maxLength) {
            return actual;
        }
        return StrUtil.sub(actual, 0, maxLength);
    }

    /**
     * trimText。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String trimText(String value) {
        if (value == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String actual = value.trim();
        return actual.isEmpty() ? null : actual;
    }
}


