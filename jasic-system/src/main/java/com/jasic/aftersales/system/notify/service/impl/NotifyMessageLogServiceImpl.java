package com.jasic.aftersales.system.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessageLog;
import com.jasic.aftersales.system.notify.domain.query.NotifyMessageLogQuery;
import com.jasic.aftersales.system.notify.mapper.SysNotifyMessageLogMapper;
import com.jasic.aftersales.system.notify.service.NotifyMessageLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 通知消息日志 Service 实现。
 *
 * @author Codex
 * @date 2026/04/18
 */
@Service
public class NotifyMessageLogServiceImpl implements NotifyMessageLogService {

    @Resource
    private SysNotifyMessageLogMapper sysNotifyMessageLogMapper;

    @Override
    public Long createLog(SysNotifyMessageLog notifyMessageLog) {
        if (notifyMessageLog.getCreateTime() == null) {
            notifyMessageLog.setCreateTime(LocalDateTime.now());
        }
        sysNotifyMessageLogMapper.insert(notifyMessageLog);
        return notifyMessageLog.getId();
    }

    @Override
    public List<SysNotifyMessageLog> listByQuery(NotifyMessageLogQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysNotifyMessageLog> wrapper = new LambdaQueryWrapper<>();
        if (query.getMessageId() != null) {
            wrapper.eq(SysNotifyMessageLog::getMessageId, query.getMessageId());
        }
        wrapper.orderByDesc(SysNotifyMessageLog::getCreateTime)
                .orderByDesc(SysNotifyMessageLog::getId);
        return sysNotifyMessageLogMapper.selectList(wrapper);
    }
}
