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

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public Long createDispatch(SysNotifyDispatch dispatch) {
        SysNotifyDispatch existing = getExistingDispatch(dispatch);
        if (existing != null) {
            return existing.getId();
        }
        sysNotifyDispatchMapper.insert(dispatch);
        return dispatch.getId();
    }

    @Override
    public SysNotifyDispatch getById(Long id) {
        return sysNotifyDispatchMapper.selectById(id);
    }

    @Override
    public List<SysNotifyDispatch> listSendableDispatches(LocalDateTime now, Integer limit) {
        LocalDateTime targetTime = now == null ? LocalDateTime.now() : now;
        LambdaQueryWrapper<SysNotifyDispatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(condition -> condition
                        .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PENDING.getCode())
                        .or(retry -> retry.eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                                .lt(SysNotifyDispatch::getRetryCount, NotifyConstants.DISPATCH_RETRY_MAX_COUNT)
                                .and(next -> next.isNull(SysNotifyDispatch::getNextRetryTime)
                                        .or()
                                        .le(SysNotifyDispatch::getNextRetryTime, targetTime))))
                .orderByAsc(SysNotifyDispatch::getId);
        if (limit != null && limit > 0) {
            wrapper.last("limit " + limit);
        }
        return sysNotifyDispatchMapper.selectList(wrapper);
    }

    @Override
    public boolean markProcessing(Long dispatchId) {
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .and(condition -> condition
                        .eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PENDING.getCode())
                        .or(retry -> retry.eq(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                                .lt(SysNotifyDispatch::getRetryCount, NotifyConstants.DISPATCH_RETRY_MAX_COUNT)))
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.PROCESSING.getCode())
                .set(SysNotifyDispatch::getNextRetryTime, null);
        return sysNotifyDispatchMapper.update(null, wrapper) > 0;
    }

    @Override
    public void markSuccess(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson) {
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.SUCCESS.getCode())
                .set(SysNotifyDispatch::getResultCode, trimValue(resultCode, 64))
                .set(SysNotifyDispatch::getResultMessage, trimValue(resultMessage, 500))
                .set(SysNotifyDispatch::getChannelResponseJson, trimText(channelResponseJson))
                .set(SysNotifyDispatch::getNextRetryTime, null)
                .set(SysNotifyDispatch::getSentTime, LocalDateTime.now());
        sysNotifyDispatchMapper.update(null, wrapper);
    }

    @Override
    public void markFailed(Long dispatchId, Integer retryCount, LocalDateTime nextRetryTime,
                           String resultCode, String resultMessage, String channelResponseJson) {
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.FAILED.getCode())
                .set(SysNotifyDispatch::getRetryCount, retryCount)
                .set(SysNotifyDispatch::getNextRetryTime, nextRetryTime)
                .set(SysNotifyDispatch::getResultCode, trimValue(resultCode, 64))
                .set(SysNotifyDispatch::getResultMessage, trimValue(resultMessage, 500))
                .set(SysNotifyDispatch::getChannelResponseJson, trimText(channelResponseJson));
        sysNotifyDispatchMapper.update(null, wrapper);
    }

    @Override
    public void markSkipped(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson) {
        LambdaUpdateWrapper<SysNotifyDispatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyDispatch::getId, dispatchId)
                .set(SysNotifyDispatch::getDispatchStatus, NotifyDispatchStatusEnum.SKIPPED.getCode())
                .set(SysNotifyDispatch::getResultCode, trimValue(resultCode, 64))
                .set(SysNotifyDispatch::getResultMessage, trimValue(resultMessage, 500))
                .set(SysNotifyDispatch::getChannelResponseJson, trimText(channelResponseJson))
                .set(SysNotifyDispatch::getNextRetryTime, null);
        sysNotifyDispatchMapper.update(null, wrapper);
    }

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
                markDispatchFailed(dispatch.getId(), ex);
            }
        }
        return successCount;
    }

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

    private NotifyChannelSender resolveSender(String channelType) {
        for (NotifyChannelSender sender : notifyChannelSenders) {
            if (sender.supports(channelType)) {
                return sender;
            }
        }
        throw new ServiceException("Unsupported notify channel sender: " + channelType);
    }

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
        LocalDateTime nextRetryTime = nextRetryCount >= NotifyConstants.DISPATCH_RETRY_MAX_COUNT
                ? null
                : LocalDateTime.now().plusMinutes(NotifyConstants.DISPATCH_RETRY_DELAY_MINUTES);
        markFailed(dispatch.getId(), nextRetryCount, nextRetryTime,
                sendResult.getResultCode(), sendResult.getResultMessage(), sendResult.getChannelResponseJson());
    }

    private void markDispatchFailed(Long dispatchId, Exception ex) {
        SysNotifyDispatch current = sysNotifyDispatchMapper.selectById(dispatchId);
        int nextRetryCount = current == null || current.getRetryCount() == null ? 1 : current.getRetryCount() + 1;
        LocalDateTime nextRetryTime = nextRetryCount >= NotifyConstants.DISPATCH_RETRY_MAX_COUNT
                ? null
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

    private SysNotifyDispatch getExistingDispatch(SysNotifyDispatch dispatch) {
        LambdaQueryWrapper<SysNotifyDispatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyDispatch::getBizType, dispatch.getBizType())
                .eq(SysNotifyDispatch::getBizId, dispatch.getBizId())
                .eq(SysNotifyDispatch::getTemplateCode, dispatch.getTemplateCode())
                .eq(SysNotifyDispatch::getChannelType, dispatch.getChannelType())
                .eq(SysNotifyDispatch::getReceiverType, dispatch.getReceiverType());
        if (dispatch.getReceiverId() != null) {
            wrapper.eq(SysNotifyDispatch::getReceiverId, dispatch.getReceiverId());
        } else {
            wrapper.isNull(SysNotifyDispatch::getReceiverId);
        }
        wrapper.last("limit 1");
        return sysNotifyDispatchMapper.selectOne(wrapper);
    }

    private String buildErrorMessage(Exception ex) {
        String message = ex == null ? null : StrUtil.trim(ex.getMessage());
        if (StrUtil.isBlank(message) && ex != null) {
            message = ex.getClass().getSimpleName();
        }
        return trimValue(message, 500);
    }

    private String trimValue(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String actual = value.trim();
        if (actual.length() <= maxLength) {
            return actual;
        }
        return StrUtil.sub(actual, 0, maxLength);
    }

    private String trimText(String value) {
        if (value == null) {
            return null;
        }
        String actual = value.trim();
        return actual.isEmpty() ? null : actual;
    }
}
