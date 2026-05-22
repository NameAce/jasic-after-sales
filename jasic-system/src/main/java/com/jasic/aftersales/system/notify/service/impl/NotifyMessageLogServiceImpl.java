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
 * @author Zoro
 * @date 2026/04/18
 */
@Service
public class NotifyMessageLogServiceImpl implements NotifyMessageLogService {

    /**
     * 系统通知消息日志Mapper数据访问接口。
     *
     * @param notifyMessageLog 提示或消息文本，用于异常返回或通知内容。
     * @return 业务处理结果
     */
    @Resource
    private SysNotifyMessageLogMapper sysNotifyMessageLogMapper;

    /**
     * 执行createLog相关新增业务。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param notifyMessageLog 提示或消息文本，用于异常返回或通知内容。
     * @return 业务处理结果
     */
    @Override
    public Long createLog(SysNotifyMessageLog notifyMessageLog) {
        if (notifyMessageLog.getCreateTime() == null) {
            notifyMessageLog.setCreateTime(LocalDateTime.now());
        }
        sysNotifyMessageLogMapper.insert(notifyMessageLog);
        return notifyMessageLog.getId();
    }

    /**
     * 分页查询By查询列表。
     *
     * @param query 查询条件，包含分页、筛选和权限收口所需字段。
     * @return 业务处理结果
     */
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


