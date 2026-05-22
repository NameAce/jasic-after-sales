package com.jasic.aftersales.system.notify.service;

/**
 * 通知事件消费 Service。
 *
 * @author Zoro
 * @date 2026/04/18
 */
public interface NotifyEventConsumeService {

    /**
     * 消费当前批次待处理事件。
     *
     * @return 成功消费数量
     */
    int consumePendingEvents();
}
