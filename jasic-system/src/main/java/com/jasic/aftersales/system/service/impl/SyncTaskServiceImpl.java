package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
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
    private static final String PERMISSION_SYNC_TASK_EXECUTE = "system:syncTask:execute";
    private static final String PERMISSION_MACHINE_BARCODE_SYNC = "system:machineBarcode:sync";

    @Resource
    private SyncTaskMapper syncTaskMapper;

    /**
     * 同步任务日志Mapper数据访问接口。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Resource
    private SyncTaskLogMapper syncTaskLogMapper;

    @Resource
    private List<SyncTaskHandler> syncTaskHandlers;

    @Resource
    private Scheduler scheduler;

    @Resource
    private ISyncTaskExecutionService syncTaskExecutionService;

    /**
     * 查询listPage相关业务数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public PageResult<SyncTaskVO> listPage(SyncTaskQuery query) {
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<SyncTask> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getTaskCode())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(SyncTask::getTaskCode, query.getTaskCode().trim());
        }
        if (StrUtil.isNotBlank(query.getTaskName())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(SyncTask::getTaskName, query.getTaskName().trim());
        }
        if (StrUtil.isNotBlank(query.getHandlerCode())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SyncTask::getHandlerCode, query.getHandlerCode().trim());
        }
        if (query.getStatus() != null) {
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SyncTask::getStatus, query.getStatus());
        }
        // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByDesc(SyncTask::getId);
        // 说明：执行该步骤以保证业务流程正确。
        Page<SyncTask> result = syncTaskMapper.selectPage(page, wrapper);

        // 调用getRecords方法，复用统一能力并保证业务规则一致。
        List<SyncTask> records = result.getRecords();
        Map<Long, SyncTaskLog> latestLogMap = buildLatestLogMap(records.stream()
                .map(SyncTask::getId)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList()));
        // 调用buildHandlerMap方法，复用统一能力并保证业务规则一致。
        Map<String, SyncTaskHandler> handlerMap = buildHandlerMap();

        List<SyncTaskVO> voList = new ArrayList<>();
        for (SyncTask record : records) {
            // 调用copyProperties方法，复用统一能力并保证业务规则一致。
            SyncTaskVO vo = BeanUtil.copyProperties(record, SyncTaskVO.class);
            // 页面展示需要直接看到处理器名称、最近执行结果和下一次触发时间，这里统一组装。
            SyncTaskHandler handler = handlerMap.get(record.getHandlerCode());
            // 调用getName方法，复用统一能力并保证业务规则一致。
            vo.setHandlerName(handler == null ? null : handler.getName());
            // 调用getId方法，复用统一能力并保证业务规则一致。
            SyncTaskLog latestLog = latestLogMap.get(record.getId());
            if (latestLog != null) {
                // 调用getStatus方法，复用统一能力并保证业务规则一致。
                vo.setLastStatus(latestLog.getStatus());
                // 调用getStartTime方法，复用统一能力并保证业务规则一致。
                vo.setLastStartTime(latestLog.getStartTime());
                // 调用getEndTime方法，复用统一能力并保证业务规则一致。
                vo.setLastEndTime(latestLog.getEndTime());
                // 调用getMessage方法，复用统一能力并保证业务规则一致。
                vo.setLastMessage(latestLog.getMessage());
            }
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setNextFireTime(getNextFireTime(record.getId()));
            // 调用add方法，复用统一能力并保证业务规则一致。
            voList.add(vo);
        }
        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 根据ID查询同步任务详情。
     *
     * @return 处理结果
     */
    @Override
    public SyncTaskVO getById(Long id) {
        // 调用getRequiredTask方法，复用统一能力并保证业务规则一致。
        SyncTask task = getRequiredTask(id);
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        SyncTaskVO vo = BeanUtil.copyProperties(task, SyncTaskVO.class);
        // 调用getHandlerCode方法，复用统一能力并保证业务规则一致。
        SyncTaskHandler handler = buildHandlerMap().get(task.getHandlerCode());
        // 调用getName方法，复用统一能力并保证业务规则一致。
        vo.setHandlerName(handler == null ? null : handler.getName());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        SyncTaskLog latestLog = getLatestLog(task.getId());
        if (latestLog != null) {
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            vo.setLastStatus(latestLog.getStatus());
            // 调用getStartTime方法，复用统一能力并保证业务规则一致。
            vo.setLastStartTime(latestLog.getStartTime());
            // 调用getEndTime方法，复用统一能力并保证业务规则一致。
            vo.setLastEndTime(latestLog.getEndTime());
            // 调用getMessage方法，复用统一能力并保证业务规则一致。
            vo.setLastMessage(latestLog.getMessage());
        }
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setNextFireTime(getNextFireTime(task.getId()));
        return vo;
    }

    /**
     * 新增同步任务。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(SyncTaskDTO dto) {
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        SyncTask entity = BeanUtil.copyProperties(dto, SyncTask.class);
        // 调用normalizeTask方法，复用统一能力并保证业务规则一致。
        normalizeTask(entity);
        // 说明：执行该步骤以保证业务流程正确。
        validateTask(entity, null);
        // 说明：执行该步骤以保证业务流程正确。
        syncTaskMapper.insert(entity);
        // 调用refreshSchedules方法，复用统一能力并保证业务规则一致。
        refreshSchedules();
        return entity.getId();
    }

    /**
     * 更新同步任务。
     *
     * @param dto 参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(SyncTaskDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("任务ID不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SyncTask entity = syncTaskMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("同步任务不存在");
        }
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, entity);
        // 调用normalizeTask方法，复用统一能力并保证业务规则一致。
        normalizeTask(entity);
        // 说明：执行该步骤以保证业务流程正确。
        validateTask(entity, entity.getId());
        // 说明：执行该步骤以保证业务流程正确。
        syncTaskMapper.updateById(entity);
        // 调用refreshSchedules方法，复用统一能力并保证业务规则一致。
        refreshSchedules();
    }

    /**
     * 分页查询日志分页列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public PageResult<SyncTaskLogVO> listLogPage(SyncTaskLogQuery query) {
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<SyncTaskLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SyncTaskLog> wrapper = new LambdaQueryWrapper<>();
        if (query.getTaskId() != null) {
            // 调用getTaskId方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SyncTaskLog::getTaskId, query.getTaskId());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SyncTaskLog::getStatus, query.getStatus().trim());
        }
        // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByDesc(SyncTaskLog::getId);
        // 说明：执行该步骤以保证业务流程正确。
        Page<SyncTaskLog> result = syncTaskLogMapper.selectPage(page, wrapper);

        Map<Long, String> taskNameMap = buildTaskNameMap(result.getRecords().stream()
                .map(SyncTaskLog::getTaskId)
                .filter(Objects::nonNull)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList()));
        List<SyncTaskLogVO> records = result.getRecords().stream()
                .map(item -> {
                    // 调用copyProperties方法，复用统一能力并保证业务规则一致。
                    SyncTaskLogVO vo = BeanUtil.copyProperties(item, SyncTaskLogVO.class);
                    // 调用getTaskId方法，复用统一能力并保证业务规则一致。
                    vo.setTaskName(taskNameMap.get(item.getTaskId()));
                    return vo;
                })
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 分页查询处理Options列表。
     *
     * @return 处理结果
     */
    @Override
    public List<SyncTaskHandlerOptionVO> listHandlerOptions() {
        return syncTaskHandlers.stream()
                .sorted(Comparator.comparing(SyncTaskHandler::getCode))
                .map(handler -> {
                    // 调用SyncTaskHandlerOptionVO方法，复用统一能力并保证业务规则一致。
                    SyncTaskHandlerOptionVO vo = new SyncTaskHandlerOptionVO();
                    // 调用getCode方法，复用统一能力并保证业务规则一致。
                    vo.setHandlerCode(handler.getCode());
                    // 调用getName方法，复用统一能力并保证业务规则一致。
                    vo.setHandlerName(handler.getName());
                    return vo;
                })
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * execute。
     *
     * @return 处理结果
     */
    @Override
    public Long execute(Long id) {
        // 调用requirePlatformManualTrigger方法，复用统一能力并保证业务规则一致。
        requirePlatformManualTrigger(PERMISSION_SYNC_TASK_EXECUTE);
        return syncTaskExecutionService.submitManualExecution(id);
    }

    /**
     * executeDefault机器条码任务。
     *
     * @return 处理结果
     */
    @Override
    public Long executeDefaultMachineBarcodeTask() {
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformManualTrigger(PERMISSION_MACHINE_BARCODE_SYNC);
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncTask::getHandlerCode, DEFAULT_MACHINE_BARCODE_HANDLER)
                .orderByAsc(SyncTask::getId)
                // 调用last方法，复用统一能力并保证业务规则一致。
                .last("LIMIT 1");
        // 说明：执行该步骤以保证业务流程正确。
        SyncTask task = syncTaskMapper.selectOne(wrapper);
        if (task == null) {
            throw new ServiceException("未配置条码同步任务，请先在同步任务中新增 machineBarcodeSync 处理器任务");
        }
        return syncTaskExecutionService.submitManualExecution(task.getId());
    }

    /**
     * requirePlatformManualTrigger。
     *
     * @param permissionCode 参数
     */
    private void requirePlatformManualTrigger(String permissionCode) {
        if (!StpUtil.isLogin()) {
            throw new ServiceException("缺少登录态，无法手动触发同步任务");
        }
        if (!SecurityContext.isPlatformUser()) {
            throw new ServiceException("仅平台用户可以手动触发同步任务");
        }
        if (StrUtil.isBlank(permissionCode) || !StpUtil.hasPermission(permissionCode)) {
            throw new ServiceException("无权手动触发同步任务");
        }
    }

    /**
     * refreshSchedules。
     */
    @Override
    public void refreshSchedules() {
        try {
            // 说明：执行该步骤以保证业务流程正确。
            List<SyncTask> tasks = syncTaskMapper.selectList(new LambdaQueryWrapper<>());
            // 先计算数据库中应当存在的启用任务集合，再清理 Quartz 中多余的历史任务。
            Set<String> activeJobKeys = tasks.stream()
                    .filter(task -> Objects.equals(task.getStatus(), STATUS_ENABLED))
                    .map(task -> buildJobKey(task.getId()).getName())
                    // 调用toSet方法，复用统一能力并保证业务规则一致。
                    .collect(Collectors.toSet());

            for (String jobName : scheduler.getJobKeys(org.quartz.impl.matchers.GroupMatcher.jobGroupEquals(QUARTZ_GROUP))
                    .stream()
                    .map(JobKey::getName)
                    .collect(Collectors.toSet())) {
                if (!activeJobKeys.contains(jobName)) {
                    // 调用JobKey方法，复用统一能力并保证业务规则一致。
                    scheduler.deleteJob(new JobKey(jobName, QUARTZ_GROUP));
                }
            }

            for (SyncTask task : tasks) {
                if (Objects.equals(task.getStatus(), STATUS_ENABLED)) {
                    // 调用scheduleTask方法，复用统一能力并保证业务规则一致。
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

    /**
     * initSchedules。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initSchedules() {
        // 应用启动后先恢复上次异常中断的运行日志，再重建所有调度任务。
        recoverRunningLogs();
        // 调用refreshSchedules方法，复用统一能力并保证业务规则一致。
        refreshSchedules();
    }

    /**
     * recover运行中Logs。
     */
    private void recoverRunningLogs() {
        LambdaQueryWrapper<SyncTaskLog> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SyncTaskLog::getStatus, LOG_STATUS_RUNNING);
        // 说明：执行该步骤以保证业务流程正确。
        List<SyncTaskLog> runningLogs = syncTaskLogMapper.selectList(wrapper);
        if (runningLogs.isEmpty()) {
            return;
        }
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime now = LocalDateTime.now();
        for (SyncTaskLog runningLog : runningLogs) {
            // 应用重启后，上一轮仍处于 RUNNING 的日志已无法继续，统一标记为失败。
            runningLog.setStatus(LOG_STATUS_FAILED);
            // 调用setEndTime方法，复用统一能力并保证业务规则一致。
            runningLog.setEndTime(now);
            // 调用setMessage方法，复用统一能力并保证业务规则一致。
            runningLog.setMessage("应用重启，上一轮执行已中断");
            // 说明：执行该步骤以保证业务流程正确。
            syncTaskLogMapper.updateById(runningLog);
        }
    }

    /**
     * schedule任务。
     *
     * @param task 参数
     */
    private void scheduleTask(SyncTask task) throws SchedulerException {
        // 说明：执行该步骤以保证业务流程正确。
        validateCronExpression(task.getCronExpression());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        JobKey jobKey = buildJobKey(task.getId());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        TriggerKey triggerKey = buildTriggerKey(task.getId());

        JobDetail jobDetail = JobBuilder.newJob(SyncTaskQuartzJob.class)
                .withIdentity(jobKey)
                .usingJobData("taskId", task.getId())
                // 调用build方法，复用统一能力并保证业务规则一致。
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression())
                        // 错过执行窗口时不补跑，避免长时间停机后集中触发大量历史同步。
                        .withMisfireHandlingInstructionDoNothing())
                // 调用build方法，复用统一能力并保证业务规则一致。
                .build();

        if (scheduler.checkExists(jobKey)) {
            // 调用deleteJob方法，复用统一能力并保证业务规则一致。
            scheduler.deleteJob(jobKey);
        }
        // 调用scheduleJob方法，复用统一能力并保证业务规则一致。
        scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * unschedule任务。
     *
     * @param taskId task ID
     */
    private void unscheduleTask(Long taskId) throws SchedulerException {
        // 调用buildJobKey方法，复用统一能力并保证业务规则一致。
        JobKey jobKey = buildJobKey(taskId);
        if (scheduler.checkExists(jobKey)) {
            // 调用deleteJob方法，复用统一能力并保证业务规则一致。
            scheduler.deleteJob(jobKey);
        }
    }

    /**
     * 获取NextFireTime。
     *
     * @param taskId task ID
     * @return 处理结果
     */
    private LocalDateTime getNextFireTime(Long taskId) {
        try {
            // 调用buildTriggerKey方法，复用统一能力并保证业务规则一致。
            Trigger trigger = scheduler.getTrigger(buildTriggerKey(taskId));
            if (trigger == null || trigger.getNextFireTime() == null) {
                return null;
            }
            return LocalDateTime.ofInstant(trigger.getNextFireTime().toInstant(), ZoneId.systemDefault());
        } catch (SchedulerException ex) {
            // 调用warn方法，复用统一能力并保证业务规则一致。
            log.warn("获取同步任务下一次触发时间失败，taskId={}", taskId, ex);
            return null;
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
     * 构建Latest日志Map。
     *
     * @return 处理结果
     */
    private Map<Long, SyncTaskLog> buildLatestLogMap(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SyncTaskLog> wrapper = new LambdaQueryWrapper<>();
        // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
        wrapper.in(SyncTaskLog::getTaskId, taskIds).orderByDesc(SyncTaskLog::getId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SyncTaskLog> logs = syncTaskLogMapper.selectList(wrapper);
        Map<Long, SyncTaskLog> result = new LinkedHashMap<>();
        for (SyncTaskLog logEntity : logs) {
            // 调用getTaskId方法，复用统一能力并保证业务规则一致。
            result.putIfAbsent(logEntity.getTaskId(), logEntity);
        }
        return result;
    }

    /**
     * 获取Latest日志。
     *
     * @param taskId task ID
     * @return 处理结果
     */
    private SyncTaskLog getLatestLog(Long taskId) {
        LambdaQueryWrapper<SyncTaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncTaskLog::getTaskId, taskId)
                .orderByDesc(SyncTaskLog::getId)
                // 调用last方法，复用统一能力并保证业务规则一致。
                .last("LIMIT 1");
        // 说明：执行该步骤以保证业务流程正确。
        return syncTaskLogMapper.selectOne(wrapper);
    }

    /**
     * 构建任务名称Map。
     *
     * @return 处理结果
     */
    private Map<Long, String> buildTaskNameMap(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<>();
        // 调用in方法，复用统一能力并保证业务规则一致。
        wrapper.in(SyncTask::getId, taskIds);
        // 说明：执行该步骤以保证业务流程正确。
        return syncTaskMapper.selectList(wrapper).stream()
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(SyncTask::getId, SyncTask::getTaskName, (a, b) -> a));
    }

    /**
     * 构建任务Key。
     *
     * @param taskId task ID
     * @return 处理结果
     */
    private JobKey buildJobKey(Long taskId) {
        return new JobKey("sync-task-" + taskId, QUARTZ_GROUP);
    }

    /**
     * 构建TriggerKey。
     *
     * @param taskId task ID
     * @return 处理结果
     */
    private TriggerKey buildTriggerKey(Long taskId) {
        return new TriggerKey("sync-trigger-" + taskId, QUARTZ_GROUP);
    }

    /**
     * 校验任务。
     *
     * @param entity 参数
     * @param currentId current ID
     */
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
        // 说明：执行该步骤以保证业务流程正确。
        validateCronExpression(entity.getCronExpression());
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        validateStatus(entity.getStatus());
        if (!buildHandlerMap().containsKey(entity.getHandlerCode())) {
            throw new ServiceException("处理器不存在");
        }
        // 当前设计要求一个处理器只绑定一个任务，避免同类内置同步被重复调度。
        checkUniqueTaskCode(entity.getTaskCode(), currentId);
        // 调用getHandlerCode方法，复用统一能力并保证业务规则一致。
        checkUniqueHandlerCode(entity.getHandlerCode(), currentId);
    }

    /**
     * checkUnique任务编码。
     *
     * @param taskCode 参数
     * @param currentId current ID
     */
    private void checkUniqueTaskCode(String taskCode, Long currentId) {
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<>();
        // 调用last方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SyncTask::getTaskCode, taskCode).last("LIMIT 1");
        // 说明：执行该步骤以保证业务流程正确。
        SyncTask existing = syncTaskMapper.selectOne(wrapper);
        if (existing != null && !Objects.equals(existing.getId(), currentId)) {
            throw new ServiceException("任务编码已存在");
        }
    }

    /**
     * checkUnique处理编码。
     *
     * @param handlerCode 参数
     * @param currentId current ID
     */
    private void checkUniqueHandlerCode(String handlerCode, Long currentId) {
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<>();
        // 调用last方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SyncTask::getHandlerCode, handlerCode).last("LIMIT 1");
        // 说明：执行该步骤以保证业务流程正确。
        SyncTask existing = syncTaskMapper.selectOne(wrapper);
        if (existing != null && !Objects.equals(existing.getId(), currentId)) {
            throw new ServiceException("处理器已绑定其他同步任务");
        }
    }

    /**
     * 校验CronExpression。
     *
     * @param cronExpression 参数
     */
    private void validateCronExpression(String cronExpression) {
        if (StrUtil.isBlank(cronExpression) || !CronExpression.isValidExpression(cronExpression.trim())) {
            throw new ServiceException("Cron表达式不合法");
        }
    }

    /**
     * 校验状态。
     *
     * @param status 参数
     */
    private void validateStatus(Integer status) {
        if (!Objects.equals(status, STATUS_ENABLED) && !Objects.equals(status, STATUS_DISABLED)) {
            throw new ServiceException("任务状态不合法");
        }
    }

    /**
     * 规范化任务。
     *
     * @param entity 参数
     */
    private void normalizeTask(SyncTask entity) {
        // 所有文本字段入库前统一 trim，避免因首尾空格导致唯一性校验和页面展示异常。
        entity.setTaskCode(normalizeRequiredText(entity.getTaskCode()));
        // 调用getTaskName方法，复用统一能力并保证业务规则一致。
        entity.setTaskName(normalizeRequiredText(entity.getTaskName()));
        // 调用getHandlerCode方法，复用统一能力并保证业务规则一致。
        entity.setHandlerCode(normalizeRequiredText(entity.getHandlerCode()));
        // 调用getCronExpression方法，复用统一能力并保证业务规则一致。
        entity.setCronExpression(normalizeRequiredText(entity.getCronExpression()));
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        entity.setRemark(normalizeOptionalText(entity.getRemark()));
    }

    /**
     * 规范化RequiredText。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String normalizeRequiredText(String value) {
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * 规范化OptionalText。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String normalizeOptionalText(String value) {
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }
}


