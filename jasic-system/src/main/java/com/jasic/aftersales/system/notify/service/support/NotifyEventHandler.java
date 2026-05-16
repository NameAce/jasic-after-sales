package com.jasic.aftersales.system.notify.service.support;

import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.support.NotifyEventExecutionContext;

/**
 * 通知事件处理器。
 *
 * <p>阶段二开始，事件处理器只负责解析自己支持的业务事件，
 * 把接收对象、模板变量、扩展快照等信息整理为统一执行上下文。
 * 真正的模板渲染、目标分流、站内落库和外部分发创建，
 * 统一由事件消费编排层负责，避免不同业务事件重复实现同一套通知目标分流逻辑。</p>
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
     * 构建已经被抢占为处理中状态的执行上下文。
     *
     * @param event 已抢占为 `PROCESSING` 的通知事件
     * @return 事件执行上下文
     */
    NotifyEventExecutionContext buildExecutionContext(SysNotifyEvent event);
}
