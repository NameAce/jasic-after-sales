package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.entity.SyncTask;
import com.jasic.aftersales.system.domain.entity.SyncTaskLog;
import com.jasic.aftersales.system.mapper.SyncTaskLogMapper;
import com.jasic.aftersales.system.mapper.SyncTaskMapper;
import com.jasic.aftersales.system.service.ISyncTaskExecutionService;
import com.jasic.aftersales.system.service.support.SyncTaskAsyncExecutor;
import com.jasic.aftersales.system.service.support.SyncTaskExecutionRunner;
import com.jasic.aftersales.system.service.support.SyncTaskHandler;
import com.jasic.aftersales.system.service.support.SyncTaskRunningRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 同步任务执行服务实现。
 *
 * <p>该类负责执行入口层面的编排工作，包括：</p>
 * <ul>
 *     <li>校验任务和处理器是否存在；</li>
 *     <li>创建运行中日志；</li>
 *     <li>控制单实例内同任务并发；</li>
 *     <li>将真正执行委托给异步执行器或执行运行器。</li>
 * </ul>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Slf4j
@Service
public class SyncTaskExecutionServiceImpl implements ISyncTaskExecutionService {

    private static final String LOG_STATUS_RUNNING = "RUNNING";
    private static final String LOG_STATUS_FAILED = "FAILED";
    private static final String TRIGGER_TYPE_MANUAL = "MANUAL";
    private static final String TRIGGER_TYPE_SCHEDULED = "SCHEDULED";
    private static final Long SYSTEM_TASK_TRIGGER_USER_ID = 0L;

    @Resource
    private SyncTaskMapper syncTaskMapper;

    @Resource
    private SyncTaskLogMapper syncTaskLogMapper;

    /**
     * ?? submitManualExecution ?????
     *
     * @param taskId task ID
     * @return ????
     */
    @Resource
    private List<SyncTaskHandler> syncTaskHandlers;

    @Resource
    private SyncTaskAsyncExecutor syncTaskAsyncExecutor;

    @Resource
    private SyncTaskExecutionRunner syncTaskExecutionRunner;

    @Resource
    private SyncTaskRunningRegistry syncTaskRunningRegistry;

    @Override
    public Long submitManualExecution(Long taskId) {
        SyncTask task = getRequiredTask(taskId);
        SyncTaskHandler handler = getRequiredHandler(task.getHandlerCode());
        // 手动执行与定时执行共用同一套并发保护，避免重复触发同一任务。
        syncTaskRunningRegistry.lock(task.getId(), task.getTaskName());
        SyncTaskLog logEntity = null;
        try {
            logEntity = createRunningLog(task.getId(), TRIGGER_TYPE_MANUAL, SecurityContext.getCurrentUserId());
            syncTaskAsyncExecutor.executeAsync(task.getId(), logEntity.getId());
        } catch (Exception ex) {
            syncTaskRunningRegistry.unlock(task.getId());
            if (logEntity != null) {
                failLog(logEntity.getId(), "任务提交失败：" + ex.getMessage());
            }
            throw new ServiceException("任务提交失败");
        }
        log.info("手动触发同步任务成功，taskId={}, handlerCode={}", task.getId(), handler.getCode());
        return logEntity.getId();
    }

    /**
     * ?????
     *
     * @param taskId task ID
     */
    @Override
    public void executeScheduled(Long taskId) {
        SyncTask task = getRequiredTask(taskId);
        // Quartz 触发场景直接在当前线程执行，便于让调度器感知任务执行异常。
        syncTaskRunningRegistry.lock(task.getId(), task.getTaskName());
        SyncTaskLog logEntity = null;
        try {
            logEntity = createRunningLog(task.getId(), TRIGGER_TYPE_SCHEDULED, SYSTEM_TASK_TRIGGER_USER_ID);
            syncTaskExecutionRunner.executeWithLog(task.getId(), logEntity.getId());
        } catch (Exception ex) {
            syncTaskRunningRegistry.unlock(task.getId());
            if (logEntity != null) {
                failLog(logEntity.getId(), ex.getMessage());
            }
            throw ex;
        }
    }

    SyncTask getRequiredTask(Long id) {
        SyncTask task = syncTaskMapper.selectById(id);
        if (task == null) {
            throw new ServiceException("同步任务不存在");
        }
        return task;
    }

    /**
     * ??Required Handler?
     *
     * @param handlerCode ??
     * @return ????
     */
    private SyncTaskHandler getRequiredHandler(String handlerCode) {
        SyncTaskHandler handler = buildHandlerMap().get(handlerCode);
        if (handler == null) {
            throw new ServiceException("同步任务处理器不存在");
        }
        return handler;
    }

    /**
     * ???????
     *
     * @return ????
     */
    private Map<String, SyncTaskHandler> buildHandlerMap() {
        Map<String, SyncTaskHandler> handlerMap = new LinkedHashMap<>();
        for (SyncTaskHandler handler : syncTaskHandlers) {
            handlerMap.put(handler.getCode(), handler);
        }
        return handlerMap;
    }

    /**
     * ?????
     *
     * @param taskId task ID
     * @param triggerType ??
     * @param triggerUserId trigger User ID
     * @return ????
     */
    private SyncTaskLog createRunningLog(Long taskId, String triggerType, Long triggerUserId) {
        // 执行日志先落 RUNNING 状态，后续由执行运行器统一回填结果与时间窗口。
        SyncTaskLog logEntity = new SyncTaskLog();
        logEntity.setTaskId(taskId);
        logEntity.setStatus(LOG_STATUS_RUNNING);
        logEntity.setTriggerType(triggerType);
        logEntity.setTriggerUserId(triggerUserId);
        logEntity.setStartTime(LocalDateTime.now());
        logEntity.setMessage("任务执行中");
        logEntity.setCreateTime(LocalDateTime.now());
        // ???????????????????????
        syncTaskLogMapper.insert(logEntity);
        return logEntity;
    }

    /**
     * ?? failLog ?????
     *
     * @param logId log ID
     * @param message ??
     */
    private void failLog(Long logId, String message) {
        // ??????????????????????????
        SyncTaskLog logEntity = syncTaskLogMapper.selectById(logId);
        if (logEntity == null) {
            return;
        }
        // 对外统一收口失败文案，避免页面直接暴露空消息或未经整理的异常内容。
        logEntity.setStatus(LOG_STATUS_FAILED);
        logEntity.setEndTime(LocalDateTime.now());
        logEntity.setMessage("执行失败：" + StrUtil.blankToDefault(StrUtil.trim(message), "未知错误"));
        // ???????????????????????
        syncTaskLogMapper.updateById(logEntity);
    }
}
