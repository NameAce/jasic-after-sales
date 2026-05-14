package com.jasic.aftersales.system.service.support;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.datapermission.CompanyDataAccessContext;
import com.jasic.aftersales.system.domain.entity.SyncTask;
import com.jasic.aftersales.system.domain.entity.SyncTaskLog;
import com.jasic.aftersales.system.mapper.SyncTaskLogMapper;
import com.jasic.aftersales.system.mapper.SyncTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 同步任务实际执行器。
 *
 * <p>该类负责衔接“任务中心”和“具体处理器”：读取最近一次成功时间，组装执行上下文，
 * 调用处理器，并将结果统一回写到日志表。</p>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Slf4j
@Component
public class SyncTaskExecutionRunner {

    private static final String LOG_STATUS_SUCCESS = "SUCCESS";
    private static final String LOG_STATUS_FAILED = "FAILED";

    @Resource
    private SyncTaskMapper syncTaskMapper;

    /**
     * 同步任务日志Mapper数据访问接口。
     *
     * @param taskId task ID
     * @param logId log ID
     */
    @Resource
    private SyncTaskLogMapper syncTaskLogMapper;

    @Resource
    private List<SyncTaskHandler> syncTaskHandlers;

    @Resource
    private SyncTaskRunningRegistry syncTaskRunningRegistry;

    @Resource
    private CompanyDataAccessContext companyDataAccessContext;

    /**
     * 处理executeWithLog业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param taskId 参数
     * @param logId 参数
     */
    public void executeWithLog(Long taskId, Long logId) {
        try {
            // 同步任务按任务参数和系统配置确定数据范围，执行前清理请求线程遗留的目标公司上下文。
            companyDataAccessContext.clear();
            // 调用getRequiredTask方法，复用统一能力并保证业务规则一致。
            SyncTask task = getRequiredTask(taskId);
            // 调用getHandlerCode方法，复用统一能力并保证业务规则一致。
            SyncTaskHandler handler = getRequiredHandler(task.getHandlerCode());
            // 最近一次成功结束时间由任务中心统一提供给处理器，用于推导增量窗口。
            LocalDateTime lastSuccessEndTime = getLastSuccessEndTime(taskId);
            SyncTaskExecutionContext context = SyncTaskExecutionContext.builder()
                    .executionTime(LocalDateTime.now())
                    .lastSuccessEndTime(lastSuccessEndTime)
                    // 调用build方法，复用统一能力并保证业务规则一致。
                    .build();
            // 调用execute方法，复用统一能力并保证业务规则一致。
            SyncTaskExecutionResult result = handler.execute(task, context);
            // 说明：执行该步骤以保证业务流程正确。
            SyncTaskLog logEntity = syncTaskLogMapper.selectById(logId);
            if (logEntity != null) {
                // 无论具体处理器实现细节如何，日志结果统一在这里收口。
                logEntity.setStatus(LOG_STATUS_SUCCESS);
                // 调用now方法，复用统一能力并保证业务规则一致。
                logEntity.setEndTime(LocalDateTime.now());
                // 调用getDataStartTime方法，复用统一能力并保证业务规则一致。
                logEntity.setDataStartTime(result == null ? null : result.getDataStartTime());
                // 调用getDataEndTime方法，复用统一能力并保证业务规则一致。
                logEntity.setDataEndTime(result == null ? null : result.getDataEndTime());
                // 调用getMessage方法，复用统一能力并保证业务规则一致。
                logEntity.setMessage(result == null ? "执行成功" : result.getMessage());
                // 说明：执行该步骤以保证业务流程正确。
                syncTaskLogMapper.updateById(logEntity);
            }
        } catch (Exception ex) {
            // 调用error方法，复用统一能力并保证业务规则一致。
            log.error("执行同步任务失败，taskId={}, logId={}", taskId, logId, ex);
            // 调用getMessage方法，复用统一能力并保证业务规则一致。
            failLog(logId, ex.getMessage());
        } finally {
            // 运行态必须在 finally 中释放，避免异常后任务永久锁死。
            companyDataAccessContext.clear();
            // 调用unlock方法，复用统一能力并保证业务规则一致。
            syncTaskRunningRegistry.unlock(taskId);
        }
    }

    /**
     * 获取Required任务。
     *
     * @return 处理结果
     */
    private SyncTask getRequiredTask(Long id) {
        // 说明：执行该步骤以保证业务流程正确。
        SyncTask task = syncTaskMapper.selectById(id);
        if (task == null) {
            throw new ServiceException("同步任务不存在");
        }
        return task;
    }

    /**
     * 获取Required处理。
     *
     * @param handlerCode 参数
     * @return 处理结果
     */
    private SyncTaskHandler getRequiredHandler(String handlerCode) {
        // 调用get方法，复用统一能力并保证业务规则一致。
        SyncTaskHandler handler = buildHandlerMap().get(handlerCode);
        if (handler == null) {
            throw new ServiceException("同步任务处理器不存在");
        }
        return handler;
    }

    /**
     * 构建处理Map。
     *
     * @return 处理结果
     */
    private Map<String, SyncTaskHandler> buildHandlerMap() {
        Map<String, SyncTaskHandler> handlerMap = new LinkedHashMap<>();
        for (SyncTaskHandler handler : syncTaskHandlers) {
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            handlerMap.put(handler.getCode(), handler);
        }
        return handlerMap;
    }

    /**
     * 获取LastSuccessEndTime。
     *
     * @param taskId task ID
     * @return 处理结果
     */
    private LocalDateTime getLastSuccessEndTime(Long taskId) {
        LambdaQueryWrapper<SyncTaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncTaskLog::getTaskId, taskId)
                .eq(SyncTaskLog::getStatus, LOG_STATUS_SUCCESS)
                .isNotNull(SyncTaskLog::getEndTime)
                .orderByDesc(SyncTaskLog::getEndTime)
                // 调用last方法，复用统一能力并保证业务规则一致。
                .last("LIMIT 1");
        // 说明：执行该步骤以保证业务流程正确。
        SyncTaskLog lastSuccessLog = syncTaskLogMapper.selectOne(wrapper);
        return lastSuccessLog == null ? null : lastSuccessLog.getEndTime();
    }

    /**
     * fail日志。
     *
     * @param logId log ID
     * @param message 参数
     */
    private void failLog(Long logId, String message) {
        // 说明：执行该步骤以保证业务流程正确。
        SyncTaskLog logEntity = syncTaskLogMapper.selectById(logId);
        if (logEntity == null) {
            return;
        }
        // 失败日志同样统一格式化，便于前端和日志列表稳定展示。
        logEntity.setStatus(LOG_STATUS_FAILED);
        // 调用now方法，复用统一能力并保证业务规则一致。
        logEntity.setEndTime(LocalDateTime.now());
        // 调用trim方法，复用统一能力并保证业务规则一致。
        logEntity.setMessage("执行失败：" + StrUtil.blankToDefault(StrUtil.trim(message), "未知错误"));
        // 说明：执行该步骤以保证业务流程正确。
        syncTaskLogMapper.updateById(logEntity);
    }
}


