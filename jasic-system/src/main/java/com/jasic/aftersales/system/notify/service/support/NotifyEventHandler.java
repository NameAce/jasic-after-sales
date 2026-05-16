package com.jasic.aftersales.system.notify.service.support;

import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;

/**
 * 通知事件处理器。
 *
 * <p>每个通知场景对应一个独立处理器，只负责消费自己支持的事件类型。
 * 处理器内部可以解析事件快照、解析接收对象、生成站内待办或外部分发任务，
 * 但不负责事件抢占、统一失败回写和批量调度。</p>
 *
 * @author Codex
 * @date 2026/05/14
 */
public interface NotifyEventHandler {

    /**
     * 判断当前处理器是否支持指定事件类型。
     *
     * @param eventType 事件类型编码
     * @return `true` 表示由当前处理器消费
     */
    boolean supports(String eventType);

    /**
     * 处理已经被抢占为处理中状态的通知事件。
     *
     * @param event 已抢占为 `PROCESSING` 的通知事件
     */
    void handle(SysNotifyEvent event);
}
