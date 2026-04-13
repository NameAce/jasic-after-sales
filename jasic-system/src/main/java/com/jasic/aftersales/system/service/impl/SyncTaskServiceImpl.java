package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SyncTaskDTO;
import com.jasic.aftersales.system.domain.entity.SyncTask;
import com.jasic.aftersales.system.domain.entity.SyncTaskLog;
import com.jasic.aftersales.system.domain.query.SyncTaskLogQuery;
import com.jasic.aftersales.system.domain.query.SyncTaskQuery;
import com.jasic.aftersales.system.domain.vo.SyncTaskHandlerOptionVO;
import com.jasic.aftersales.system.domain.vo.SyncTaskLogVO;
import com.jasic.aftersales.system.domain.vo.SyncTaskVO;
import com.jasic.aftersales.system.mapper.SyncTaskLogMapper;
import com.jasic.aftersales.system.mapper.SyncTaskMapper;
import com.jasic.aftersales.system.service.ISyncTaskExecutionService;
import com.jasic.aftersales.system.service.ISyncTaskService;
import com.jasic.aftersales.system.service.support.SyncTaskHandler;
import com.jasic.aftersales.system.service.support.SyncTaskQuartzJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 同步任务服务实现。
 *
 * <p>负责同步任务的配置管理、日志查询和 Quartz 调度刷新。业务处理逻辑本身由
 * {@link SyncTaskHandler} 实现类承载。</p>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Slf4j
@Service
public class SyncTaskServiceImpl implements ISyncTaskService {

    private static final Integer STATUS_ENABLED = 1;
    private static final Integer STATUS_DISABLED = 0;
    private static final String LOG_STATUS_RUNNING = "RUNNING";
    private static final String LOG_STATUS_FAILED = "FAILED";
    private static final String QUARTZ_GROUP = "SYNC_TASK";
    private static final String DEFAULT_MACHINE_BARCODE_HANDLER = "machineBarcodeSync";

    @Resource
    private SyncTaskMapper syncTaskMapper;

    @Resource
    private SyncTaskLogMapper syncTaskLogMapper;

    @Resource
    private List<SyncTaskHandler> syncTaskHandlers;

    @Resource
    private Scheduler scheduler;

    @Resource
    private ISyncTaskExecutionService syncTaskExecutionService;

    @Override
    public PageResult<SyncTaskVO> listPage(SyncTaskQuery query) {
        Page<SyncTask> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getTaskCode())) {
            wrapper.like(SyncTask::getTaskCode, query.getTaskCode().trim());
        }
        if (StrUtil.isNotBlank(query.getTaskName())) {
            wrapper.like(SyncTask::getTaskName, query.getTaskName().trim());
        }
        if (StrUtil.isNotBlank(query.getHandlerCode())) {
            wrapper.eq(SyncTask::getHandlerCode, query.getHandlerCode().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SyncTask::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SyncTask::getId);
        Page<SyncTask> result = syncTaskMapper.selectPage(page, wrapper);

        List<SyncTask> records = result.getRecords();
        Map<Long, SyncTaskLog> latestLogMap = buildLatestLogMap(records.stream()
                .map(SyncTask::getId)
                .collect(Collectors.toList()));
        Map<String, SyncTaskHandler> handlerMap = buildHandlerMap();

        List<SyncTaskVO> voList = new ArrayList<>();
        for (SyncTask record : records) {
            SyncTaskVO vo = BeanUtil.copyProperties(record, SyncTaskVO.class);
            // 页面展示需要直接看到处理器名称、最近执行结果和下一次触发时间，这里统一组装。
            SyncTaskHandler handler = handlerMap.get(record.getHandlerCode());
            vo.setHandlerName(handler == null ? null : handler.getName());
            SyncTaskLog latestLog = latestLogMap.get(record.getId());
            if (latestLog != null) {
                vo.setLastStatus(latestLog.getStatus());
                vo.setLastStartTime(latestLog.getStartTime());
                vo.setLastEndTime(latestLog.getEndTime());
                vo.setLastMessage(latestLog.getMessage());
            }
            vo.setNextFireTime(getNextFireTime(record.getId()));
            voList.add(vo);
        }
        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public SyncTaskVO getById(Long id) {
        SyncTask task = getRequiredTask(id);
        SyncTaskVO vo = BeanUtil.copyProperties(task, SyncTaskVO.class);
        SyncTaskHandler handler = buildHandlerMap().get(task.getHandlerCode());
        vo.setHandlerName(handler == null ? null : handler.getName());
        SyncTaskLog latestLog = getLatestLog(task.getId());
        if (latestLog != null) {
            vo.setLastStatus(latestLog.getStatus());
            vo.setLastStartTime(latestLog.getStartTime());
            vo.setLastEndTime(latestLog.getEndTime());
            vo.setLastMessage(latestLog.getMessage());
        }
        vo.setNextFireTime(getNextFireTime(task.getId()));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(SyncTaskDTO dto) {
        SyncTask entity = BeanUtil.copyProperties(dto, SyncTask.class);
        normalizeTask(entity);
        validateTask(entity, null);
        syncTaskMapper.insert(entity);
        refreshSchedules();
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(SyncTaskDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("任务ID不能为空");
        }
        SyncTask entity = syncTaskMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("同步任务不存在");
        }
        BeanUtil.copyProperties(dto, entity);
        normalizeTask(entity);
        validateTask(entity, entity.getId());
        syncTaskMapper.updateById(entity);
        refreshSchedules();
    }

    @Override
    public PageResult<SyncTaskLogVO> listLogPage(SyncTaskLogQuery query) {
        Page<SyncTaskLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SyncTaskLog> wrapper = new LambdaQueryWrapper<>();
        if (query.getTaskId() != null) {
            wrapper.eq(SyncTaskLog::getTaskId, query.getTaskId());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(SyncTaskLog::getStatus, query.getStatus().trim());
        }
        wrapper.orderByDesc(SyncTaskLog::getId);
        Page<SyncTaskLog> result = syncTaskLogMapper.selectPage(page, wrapper);

        Map<Long, String> taskNameMap = buildTaskNameMap(result.getRecords().stream()
                .map(SyncTaskLog::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        List<SyncTaskLogVO> records = result.getRecords().stream()
                .map(item -> {
                    SyncTaskLogVO vo = BeanUtil.copyProperties(item, SyncTaskLogVO.class);
                    vo.setTaskName(taskNameMap.get(item.getTaskId()));
                    return vo;
                })
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public List<SyncTaskHandlerOptionVO> listHandlerOptions() {
        return syncTaskHandlers.stream()
                .sorted(Comparator.comparing(SyncTaskHandler::getCode))
                .map(handler -> {
                    SyncTaskHandlerOptionVO vo = new SyncTaskHandlerOptionVO();
                    vo.setHandlerCode(handler.getCode());
                    vo.setHandlerName(handler.getName());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Long execute(Long id) {
        return syncTaskExecutionService.submitManualExecution(id);
    }

    @Override
    public Long executeDefaultMachineBarcodeTask() {
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncTask::getHandlerCode, DEFAULT_MACHINE_BARCODE_HANDLER)
                .orderByAsc(SyncTask::getId)
                .last("LIMIT 1");
        SyncTask task = syncTaskMapper.selectOne(wrapper);
        if (task == null) {
            throw new ServiceException("未配置条码同步任务，请先在同步任务中新增 machineBarcodeSync 处理器任务");
        }
        return execute(task.getId());
    }

    @Override
    public void refreshSchedules() {
        try {
            List<SyncTask> tasks = syncTaskMapper.selectList(new LambdaQueryWrapper<>());
            // 先计算数据库中应当存在的启用任务集合，再清理 Quartz 中多余的历史任务。
            Set<String> activeJobKeys = tasks.stream()
                    .filter(task -> Objects.equals(task.getStatus(), STATUS_ENABLED))
                    .map(task -> buildJobKey(task.getId()).getName())
                    .collect(Collectors.toSet());

            for (String jobName : scheduler.getJobKeys(org.quartz.impl.matchers.GroupMatcher.jobGroupEquals(QUARTZ_GROUP))
                    .stream()
                    .map(JobKey::getName)
                    .collect(Collectors.toSet())) {
                if (!activeJobKeys.contains(jobName)) {
                    scheduler.deleteJob(new JobKey(jobName, QUARTZ_GROUP));
                }
            }

            for (SyncTask task : tasks) {
                if (Objects.equals(task.getStatus(), STATUS_ENABLED)) {
                    scheduleTask(task);
                } else {
                    // 停用任务需要同步从 Quartz 中移除，避免配置与实际调度状态不一致。
                    unscheduleTask(task.getId());
                }
            }
        } catch (SchedulerException ex) {
            throw new ServiceException("刷新同步任务调度失败");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initSchedules() {
        // 应用启动后先恢复上次异常中断的运行日志，再重建所有调度任务。
        recoverRunningLogs();
        refreshSchedules();
    }

    private void recoverRunningLogs() {
        LambdaQueryWrapper<SyncTaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncTaskLog::getStatus, LOG_STATUS_RUNNING);
        List<SyncTaskLog> runningLogs = syncTaskLogMapper.selectList(wrapper);
        if (runningLogs.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (SyncTaskLog runningLog : runningLogs) {
            // 应用重启后，上一轮仍处于 RUNNING 的日志已无法继续，统一标记为失败。
            runningLog.setStatus(LOG_STATUS_FAILED);
            runningLog.setEndTime(now);
            runningLog.setMessage("应用重启，上一轮执行已中断");
            syncTaskLogMapper.updateById(runningLog);
        }
    }

    private void scheduleTask(SyncTask task) throws SchedulerException {
        validateCronExpression(task.getCronExpression());
        JobKey jobKey = buildJobKey(task.getId());
        TriggerKey triggerKey = buildTriggerKey(task.getId());

        JobDetail jobDetail = JobBuilder.newJob(SyncTaskQuartzJob.class)
                .withIdentity(jobKey)
                .usingJobData("taskId", task.getId())
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression())
                        // 错过执行窗口时不补跑，避免长时间停机后集中触发大量历史同步。
                        .withMisfireHandlingInstructionDoNothing())
                .build();

        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
        scheduler.scheduleJob(jobDetail, trigger);
    }

    private void unscheduleTask(Long taskId) throws SchedulerException {
        JobKey jobKey = buildJobKey(taskId);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

    private LocalDateTime getNextFireTime(Long taskId) {
        try {
            Trigger trigger = scheduler.getTrigger(buildTriggerKey(taskId));
            if (trigger == null || trigger.getNextFireTime() == null) {
                return null;
            }
            return LocalDateTime.ofInstant(trigger.getNextFireTime().toInstant(), ZoneId.systemDefault());
        } catch (SchedulerException ex) {
            log.warn("获取同步任务下一次触发时间失败，taskId={}", taskId, ex);
            return null;
        }
    }

    private SyncTask getRequiredTask(Long id) {
        SyncTask task = syncTaskMapper.selectById(id);
        if (task == null) {
            throw new ServiceException("同步任务不存在");
        }
        return task;
    }

    private Map<String, SyncTaskHandler> buildHandlerMap() {
        Map<String, SyncTaskHandler> handlerMap = new LinkedHashMap<>();
        for (SyncTaskHandler handler : syncTaskHandlers) {
            handlerMap.put(handler.getCode(), handler);
        }
        return handlerMap;
    }

    private Map<Long, SyncTaskLog> buildLatestLogMap(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SyncTaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SyncTaskLog::getTaskId, taskIds).orderByDesc(SyncTaskLog::getId);
        List<SyncTaskLog> logs = syncTaskLogMapper.selectList(wrapper);
        Map<Long, SyncTaskLog> result = new LinkedHashMap<>();
        for (SyncTaskLog logEntity : logs) {
            result.putIfAbsent(logEntity.getTaskId(), logEntity);
        }
        return result;
    }

    private SyncTaskLog getLatestLog(Long taskId) {
        LambdaQueryWrapper<SyncTaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncTaskLog::getTaskId, taskId)
                .orderByDesc(SyncTaskLog::getId)
                .last("LIMIT 1");
        return syncTaskLogMapper.selectOne(wrapper);
    }

    private Map<Long, String> buildTaskNameMap(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SyncTask::getId, taskIds);
        return syncTaskMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(SyncTask::getId, SyncTask::getTaskName, (a, b) -> a));
    }

    private JobKey buildJobKey(Long taskId) {
        return new JobKey("sync-task-" + taskId, QUARTZ_GROUP);
    }

    private TriggerKey buildTriggerKey(Long taskId) {
        return new TriggerKey("sync-trigger-" + taskId, QUARTZ_GROUP);
    }

    private void validateTask(SyncTask entity, Long currentId) {
        if (StrUtil.isBlank(entity.getTaskCode())) {
            throw new ServiceException("任务编码不能为空");
        }
        if (StrUtil.isBlank(entity.getTaskName())) {
            throw new ServiceException("任务名称不能为空");
        }
        if (StrUtil.isBlank(entity.getHandlerCode())) {
            throw new ServiceException("处理器不能为空");
        }
        validateCronExpression(entity.getCronExpression());
        validateStatus(entity.getStatus());
        if (!buildHandlerMap().containsKey(entity.getHandlerCode())) {
            throw new ServiceException("处理器不存在");
        }
        // 当前设计要求一个处理器只绑定一个任务，避免同类内置同步被重复调度。
        checkUniqueTaskCode(entity.getTaskCode(), currentId);
        checkUniqueHandlerCode(entity.getHandlerCode(), currentId);
    }

    private void checkUniqueTaskCode(String taskCode, Long currentId) {
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncTask::getTaskCode, taskCode).last("LIMIT 1");
        SyncTask existing = syncTaskMapper.selectOne(wrapper);
        if (existing != null && !Objects.equals(existing.getId(), currentId)) {
            throw new ServiceException("任务编码已存在");
        }
    }

    private void checkUniqueHandlerCode(String handlerCode, Long currentId) {
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncTask::getHandlerCode, handlerCode).last("LIMIT 1");
        SyncTask existing = syncTaskMapper.selectOne(wrapper);
        if (existing != null && !Objects.equals(existing.getId(), currentId)) {
            throw new ServiceException("处理器已绑定其他同步任务");
        }
    }

    private void validateCronExpression(String cronExpression) {
        if (StrUtil.isBlank(cronExpression) || !CronExpression.isValidExpression(cronExpression.trim())) {
            throw new ServiceException("Cron表达式不合法");
        }
    }

    private void validateStatus(Integer status) {
        if (!Objects.equals(status, STATUS_ENABLED) && !Objects.equals(status, STATUS_DISABLED)) {
            throw new ServiceException("任务状态不合法");
        }
    }

    private void normalizeTask(SyncTask entity) {
        // 所有文本字段入库前统一 trim，避免因首尾空格导致唯一性校验和页面展示异常。
        entity.setTaskCode(normalizeRequiredText(entity.getTaskCode()));
        entity.setTaskName(normalizeRequiredText(entity.getTaskName()));
        entity.setHandlerCode(normalizeRequiredText(entity.getHandlerCode()));
        entity.setCronExpression(normalizeRequiredText(entity.getCronExpression()));
        entity.setRemark(normalizeOptionalText(entity.getRemark()));
    }

    private String normalizeRequiredText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private String normalizeOptionalText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }
}
