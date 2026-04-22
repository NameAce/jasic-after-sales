package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notify dispatch service.
 *
 * @author Codex
 * @date 2026/04/21
 */
public interface NotifyDispatchService {

    Long createDispatch(SysNotifyDispatch dispatch);

    SysNotifyDispatch getById(Long id);

    List<SysNotifyDispatch> listSendableDispatches(LocalDateTime now, Integer limit);

    boolean markProcessing(Long dispatchId);

    void markSuccess(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson);

    void markFailed(Long dispatchId, Integer retryCount, LocalDateTime nextRetryTime,
                    String resultCode, String resultMessage, String channelResponseJson);

    void markSkipped(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson);

    int consumePendingDispatches();
}
