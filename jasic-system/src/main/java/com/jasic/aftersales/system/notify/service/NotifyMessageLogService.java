package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessageLog;
import com.jasic.aftersales.system.notify.domain.query.NotifyMessageLogQuery;

import java.util.List;

/**
 * 通知消息日志 Service。
 *
 * @author Codex
 * @date 2026/04/18
 */
public interface NotifyMessageLogService {

    /**
     * 创建通知消息日志。
     *
     * @param notifyMessageLog 通知消息日志
     * @return 主键ID
     */
    Long createLog(SysNotifyMessageLog notifyMessageLog);

    /**
     * 按条件查询通知消息日志。
     *
     * @param query 查询参数
     * @return 日志列表
     */
    List<SysNotifyMessageLog> listByQuery(NotifyMessageLogQuery query);
}
