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

    /**
     * 系统通知消息日志Mapper数据访问接口。
     *
     * @param notifyMessageLog 参数
     * @return 处理结果
     */
    @Resource
    private SysNotifyMessageLogMapper sysNotifyMessageLogMapper;

    /**
     * 执行createLog相关新增业务。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param notifyMessageLog 参数
     * @return 处理结果
     */
    @Override
    public Long createLog(SysNotifyMessageLog notifyMessageLog) {
        // 说明：执行该步骤以保证业务流程正确。
        if (notifyMessageLog.getCreateTime() == null) {
            // 调用now方法，复用统一能力并保证业务规则一致。
            notifyMessageLog.setCreateTime(LocalDateTime.now());
        }
        // 调用insert方法，复用统一能力并保证业务规则一致。
        sysNotifyMessageLogMapper.insert(notifyMessageLog);
        return notifyMessageLog.getId();
    }

    /**
     * 分页查询By查询列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public List<SysNotifyMessageLog> listByQuery(NotifyMessageLogQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyMessageLog> wrapper = new LambdaQueryWrapper<>();
        if (query.getMessageId() != null) {
            // 调用getMessageId方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyMessageLog::getMessageId, query.getMessageId());
        }
        wrapper.orderByDesc(SysNotifyMessageLog::getCreateTime)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(SysNotifyMessageLog::getId);
        return sysNotifyMessageLogMapper.selectList(wrapper);
    }
}


