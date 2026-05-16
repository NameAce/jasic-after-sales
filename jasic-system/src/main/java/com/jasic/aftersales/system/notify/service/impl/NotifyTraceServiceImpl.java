package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessage;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchResultCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTodoStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTypeEnum;
import com.jasic.aftersales.system.notify.domain.query.NotifyTraceQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceDispatchDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceEventDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceMessageDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTracePageVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceStatusCountVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceTargetSummaryVO;
import com.jasic.aftersales.system.notify.mapper.NotifyTraceMapper;
import com.jasic.aftersales.system.notify.mapper.SysNotifyDispatchMapper;
import com.jasic.aftersales.system.notify.mapper.SysNotifyMessageMapper;
import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.NotifyTraceService;
import com.jasic.aftersales.system.notify.support.NotifyDispatchPayload;
import com.jasic.aftersales.system.notify.support.NotifySceneMeta;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知记录排障服务实现。
 *
 * <p>该实现只处理后台排障查询和人工介入动作，
 * 不直接改动通知生成链路，避免排障收口逻辑与前面消费/分发阶段耦合过深。</p>
 *
 * @author Codex
 * @date 2026/05/14
 */
@Slf4j
@Service
public class NotifyTraceServiceImpl implements NotifyTraceService {

    private static final String PRODUCT_CATEGORY_IN_APP = "IN_APP";
    private static final String PRODUCT_CATEGORY_EXTERNAL = "EXTERNAL";
    private static final String UNKNOWN_TARGET_TYPE = "UNKNOWN";
    private static final String EVENT_MANUAL_DEAD_MESSAGE_PREFIX = "人工标记不再处理：";
    private static final String DISPATCH_MANUAL_DEAD_MESSAGE_PREFIX = "人工标记不再处理：";

    @Resource
    private NotifyTraceMapper notifyTraceMapper;

    @Resource
    private NotifyEventService notifyEventService;

    @Resource
    private NotifyDispatchService notifyDispatchService;

    @Resource
    private SysNotifyMessageMapper sysNotifyMessageMapper;

    @Resource
    private SysNotifyDispatchMapper sysNotifyDispatchMapper;

    @Resource
    private NotifySceneRegistry notifySceneRegistry;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<NotifyTracePageVO> listPage(NotifyTraceQuery query) {
        NotifyTraceQuery actualQuery = query == null ? new NotifyTraceQuery() : query;
        Page<NotifyTracePageVO> page = new Page<>(actualQuery.getPageNum(), actualQuery.getPageSize());
        IPage<NotifyTracePageVO> result = notifyTraceMapper.selectTracePage(page, actualQuery);
        List<NotifyTracePageVO> records = result == null || result.getRecords() == null
                ? Collections.emptyList()
                : result.getRecords();
        // 分页 SQL 只负责筛出事件主记录，真正的“一个事件下有哪些目标产物”在这里统一聚合，
        // 避免列表再回退成“只看最新一条消息/分发”的旧排障视角。
        hydrateTracePageRecords(records);
        long total = result == null ? 0L : result.getTotal();
        return PageResult.of(records, total, actualQuery.getPageNum(), actualQuery.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotifyTraceEventDetailVO getEventDetail(Long eventId) {
        SysNotifyEvent event = getRequiredEvent(eventId);
        NotifyTraceEventDetailVO detailVO = BeanUtil.copyProperties(event, NotifyTraceEventDetailVO.class);
        detailVO.setSceneName(resolveSceneName(event.getSceneCode()));
        // 排障页需要同时看到站内消息和外部分发，方便判断失败发生在事件消费还是下游发送。
        List<NotifyTraceMessageDetailVO> messages = listMessageDetails(eventId);
        List<NotifyTraceDispatchDetailVO> dispatches = listDispatchDetailsByEventId(eventId);
        detailVO.setMessages(messages);
        detailVO.setDispatches(dispatches);
        detailVO.setMessageTargetSummaries(buildMessageTargetSummariesByDetails(messages));
        detailVO.setDispatchTargetSummaries(buildDispatchTargetSummariesByDetails(dispatches));
        return detailVO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotifyTraceDispatchDetailVO getDispatchDetail(Long dispatchId) {
        SysNotifyDispatch dispatch = getRequiredDispatch(dispatchId);
        NotifyTraceDispatchDetailVO detailVO = BeanUtil.copyProperties(dispatch, NotifyTraceDispatchDetailVO.class);
        hydrateDispatchDetail(detailVO);
        return detailVO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void retryEvent(Long eventId) {
        SysNotifyEvent event = getRequiredEvent(eventId);
        if (!isEventRetryAllowed(event.getStatus())) {
            throw new ServiceException("仅 FAILED/DEAD 状态的通知事件允许人工重试");
        }
        // 人工重试入口必须走事件服务统一状态机，避免绕过幂等与重试清理逻辑。
        notifyEventService.resetForRetry(eventId);
        log.info("通知事件人工重试已提交。eventId={}, sceneCode={}, status={}",
                eventId, event.getSceneCode(), event.getStatus());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void retryDispatch(Long dispatchId) {
        SysNotifyDispatch dispatch = getRequiredDispatch(dispatchId);
        if (!isDispatchRetryAllowed(dispatch.getDispatchStatus())) {
            throw new ServiceException("仅 FAILED/DEAD 状态的通知分发任务允许人工重试");
        }
        // 分发人工重试要清空上一轮渠道响应和结果码，必须复用现有 resetForRetry 统一能力。
        notifyDispatchService.resetForRetry(dispatchId);
        log.info("通知分发任务人工重试已提交。dispatchId={}, sceneCode={}, targetType={}, status={}",
                dispatchId, dispatch.getSceneCode(), dispatch.getTargetType(), dispatch.getDispatchStatus());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markEventDead(Long eventId, String reason) {
        SysNotifyEvent event = getRequiredEvent(eventId);
        if (!isEventDeadAllowed(event.getStatus())) {
            throw new ServiceException("仅 NEW/PROCESSING/FAILED 状态的通知事件允许标记死信");
        }
        String message = EVENT_MANUAL_DEAD_MESSAGE_PREFIX + normalizeManualReason(reason);
        // 事件死信文案要显式带上人工关闭原因，方便后续排障确认这是人工终止而非自动失败。
        notifyEventService.markDead(eventId, message);
        log.info("通知事件已人工标记死信。eventId={}, sceneCode={}, status={}, reason={}",
                eventId, event.getSceneCode(), event.getStatus(), message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markDispatchDead(Long dispatchId, String reason) {
        SysNotifyDispatch dispatch = getRequiredDispatch(dispatchId);
        if (!isDispatchDeadAllowed(dispatch.getDispatchStatus())) {
            throw new ServiceException("仅 PENDING/PROCESSING/FAILED 状态的通知分发任务允许标记死信");
        }
        String message = DISPATCH_MANUAL_DEAD_MESSAGE_PREFIX + normalizeManualReason(reason);
        // 分发死信同时落结果码，后续页面可明确区分是人工关闭还是自动重试耗尽。
        notifyDispatchService.markDead(dispatchId, NotifyDispatchResultCodeEnum.DEAD_MANUAL_CLOSED.getCode(), message);
        log.info("通知分发任务已人工标记死信。dispatchId={}, sceneCode={}, targetType={}, status={}, reason={}",
                dispatchId, dispatch.getSceneCode(), dispatch.getTargetType(), dispatch.getDispatchStatus(), message);
    }

    /**
     * 查询并校验事件。
     *
     * @param eventId 事件ID
     * @return 事件
     */
    private SysNotifyEvent getRequiredEvent(Long eventId) {
        if (eventId == null) {
            throw new ServiceException("通知事件ID不能为空");
        }
        SysNotifyEvent event = notifyEventService.getById(eventId);
        if (event == null) {
            throw new ServiceException("通知事件不存在");
        }
        return event;
    }

    /**
     * 查询并校验分发任务。
     *
     * @param dispatchId 分发任务ID
     * @return 分发任务
     */
    private SysNotifyDispatch getRequiredDispatch(Long dispatchId) {
        if (dispatchId == null) {
            throw new ServiceException("通知分发任务ID不能为空");
        }
        SysNotifyDispatch dispatch = notifyDispatchService.getById(dispatchId);
        if (dispatch == null) {
            throw new ServiceException("通知分发任务不存在");
        }
        return dispatch;
    }

    /**
     * 查询事件关联站内消息。
     *
     * @param eventId 事件ID
     * @return 站内消息详情
     */
    private List<NotifyTraceMessageDetailVO> listMessageDetails(Long eventId) {
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyMessage::getEventId, eventId)
                .orderByAsc(SysNotifyMessage::getId);
        List<SysNotifyMessage> messages = sysNotifyMessageMapper.selectList(wrapper);
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        return messages.stream()
                .map(message -> {
                    NotifyTraceMessageDetailVO detailVO =
                            BeanUtil.copyProperties(message, NotifyTraceMessageDetailVO.class);
                    detailVO.setSceneCode(resolveMessageSceneCode(message));
                    detailVO.setSceneName(resolveSceneName(detailVO.getSceneCode()));
                    detailVO.setTargetType(resolveMessageTargetType(message));
                    detailVO.setTargetTypeDesc(resolveTargetTypeDesc(detailVO.getTargetType()));
                    return detailVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 查询事件关联分发任务。
     *
     * @param eventId 事件ID
     * @return 分发任务详情
     */
    private List<NotifyTraceDispatchDetailVO> listDispatchDetailsByEventId(Long eventId) {
        LambdaQueryWrapper<SysNotifyDispatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyDispatch::getEventId, eventId)
                .orderByAsc(SysNotifyDispatch::getId);
        List<SysNotifyDispatch> dispatches = sysNotifyDispatchMapper.selectList(wrapper);
        if (dispatches == null || dispatches.isEmpty()) {
            return Collections.emptyList();
        }
        return dispatches.stream()
                .map(dispatch -> {
                    NotifyTraceDispatchDetailVO detailVO =
                            BeanUtil.copyProperties(dispatch, NotifyTraceDispatchDetailVO.class);
                    hydrateDispatchDetail(detailVO);
                    return detailVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 补充分页记录中的场景名称。
     *
     * <p>分页列表以事件为主表，不强依赖模板或渠道当前是否仍存在。
     * 因此这里通过 `NotifySceneRegistry` 将 sceneCode 翻译成业务名称，翻译失败时保留原编码。</p>
     *
     * @param records 分页记录
     */
    private void hydrateTracePageRecords(List<NotifyTracePageVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> eventIds = records.stream()
                .map(NotifyTracePageVO::getEventId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
        Map<Long, List<SysNotifyMessage>> messageMap = groupByEventId(listMessagesByEventIds(eventIds), SysNotifyMessage::getEventId);
        Map<Long, List<SysNotifyDispatch>> dispatchMap = groupByEventId(listDispatchesByEventIds(eventIds), SysNotifyDispatch::getEventId);
        for (NotifyTracePageVO record : records) {
            record.setSceneName(resolveSceneName(record.getSceneCode()));
            List<SysNotifyMessage> messages = messageMap.getOrDefault(record.getEventId(), Collections.emptyList());
            List<SysNotifyDispatch> dispatches = dispatchMap.getOrDefault(record.getEventId(), Collections.emptyList());
            record.setMessageCount(messages.size());
            record.setDispatchCount(dispatches.size());
            record.setMessageTargetSummaries(buildMessageTargetSummaries(messages));
            record.setDispatchTargetSummaries(buildDispatchTargetSummaries(dispatches));
        }
    }

    /**
     * 补充分发详情中的场景、模板和渠道快照。
     *
     * <p>外部分发 payload 在事件消费阶段固化了 sceneCode、模板名称和渠道状态。
     * 详情页优先展示该快照；旧数据没有快照时，再回退到注册表解析场景名称。</p>
     *
     * @param detailVO 分发详情
     */
    private void hydrateDispatchDetail(NotifyTraceDispatchDetailVO detailVO) {
        if (detailVO == null) {
            return;
        }
        NotifyDispatchPayload payload = parseDispatchPayload(detailVO.getPayloadJson());
        if (payload != null) {
            if (StrUtil.isNotBlank(payload.getSceneCode())) {
                detailVO.setSceneCode(payload.getSceneCode());
            }
            if (StrUtil.isNotBlank(payload.getTargetType())) {
                detailVO.setTargetType(payload.getTargetType());
            }
            detailVO.setSceneName(StrUtil.blankToDefault(payload.getSceneName(),
                    resolveSceneName(detailVO.getSceneCode())));
            detailVO.setTemplateName(payload.getTemplateName());
            detailVO.setChannelEnabled(payload.getChannelEnabled());
        } else {
            detailVO.setSceneName(resolveSceneName(detailVO.getSceneCode()));
        }
        detailVO.setTargetTypeDesc(resolveTargetTypeDesc(detailVO.getTargetType()));
    }

    /**
     * 按事件批量查询站内产物。
     *
     * <p>分页页签需要围绕事件查看同一事件下的全部站内产物，因此这里统一批量捞取后再内存聚合，
     * 避免把“最新一条消息”误当成整个事件的站内执行结果。</p>
     *
     * @param eventIds 事件ID列表
     * @return 站内产物列表
     */
    private List<SysNotifyMessage> listMessagesByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysNotifyMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysNotifyMessage::getEventId, eventIds)
                .orderByAsc(SysNotifyMessage::getEventId)
                .orderByAsc(SysNotifyMessage::getId);
        return sysNotifyMessageMapper.selectList(wrapper);
    }

    /**
     * 按事件批量查询外部分发任务。
     *
     * @param eventIds 事件ID列表
     * @return 分发任务列表
     */
    private List<SysNotifyDispatch> listDispatchesByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysNotifyDispatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysNotifyDispatch::getEventId, eventIds)
                .orderByAsc(SysNotifyDispatch::getEventId)
                .orderByAsc(SysNotifyDispatch::getId);
        return sysNotifyDispatchMapper.selectList(wrapper);
    }

    /**
     * 构建站内目标聚合摘要。
     *
     * @param messages 站内产物实体列表
     * @return 目标聚合摘要
     */
    private List<NotifyTraceTargetSummaryVO> buildMessageTargetSummaries(List<SysNotifyMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, List<SysNotifyMessage>> groupedMap = messages.stream()
                .collect(Collectors.groupingBy(this::resolveMessageTargetType, LinkedHashMap::new, Collectors.toList()));
        List<NotifyTraceTargetSummaryVO> summaries = new ArrayList<>();
        for (Map.Entry<String, List<SysNotifyMessage>> entry : groupedMap.entrySet()) {
            List<SysNotifyMessage> targetMessages = entry.getValue();
            Map<String, Integer> statusCountMap = new LinkedHashMap<>();
            for (SysNotifyMessage message : targetMessages) {
                String status = StrUtil.blankToDefault(message.getTodoStatus(), NotifyTodoStatusEnum.PENDING.getCode());
                statusCountMap.merge(status, 1, Integer::sum);
            }
            summaries.add(buildTargetSummary(
                    entry.getKey(),
                    PRODUCT_CATEGORY_IN_APP,
                    targetMessages.size(),
                    statusCountMap,
                    Arrays.asList(
                            NotifyTodoStatusEnum.PENDING.getCode(),
                            NotifyTodoStatusEnum.READ.getCode(),
                            NotifyTodoStatusEnum.DONE.getCode(),
                            NotifyTodoStatusEnum.INVALID.getCode()
                    ),
                    true
            ));
        }
        return summaries;
    }

    /**
     * 基于详情 VO 构建站内目标聚合摘要。
     *
     * @param messages 站内详情列表
     * @return 目标聚合摘要
     */
    private List<NotifyTraceTargetSummaryVO> buildMessageTargetSummariesByDetails(List<NotifyTraceMessageDetailVO> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, List<NotifyTraceMessageDetailVO>> groupedMap = messages.stream()
                // 历史记录可能缺少 targetType，排障页要兜底展示，不能因为旧数据脏值直接报错。
                .collect(Collectors.groupingBy(message -> resolveTargetTypeKey(message.getTargetType()),
                        LinkedHashMap::new, Collectors.toList()));
        List<NotifyTraceTargetSummaryVO> summaries = new ArrayList<>();
        for (Map.Entry<String, List<NotifyTraceMessageDetailVO>> entry : groupedMap.entrySet()) {
            Map<String, Integer> statusCountMap = new LinkedHashMap<>();
            for (NotifyTraceMessageDetailVO message : entry.getValue()) {
                String status = StrUtil.blankToDefault(message.getTodoStatus(), NotifyTodoStatusEnum.PENDING.getCode());
                statusCountMap.merge(status, 1, Integer::sum);
            }
            summaries.add(buildTargetSummary(
                    entry.getKey(),
                    PRODUCT_CATEGORY_IN_APP,
                    entry.getValue().size(),
                    statusCountMap,
                    Arrays.asList(
                            NotifyTodoStatusEnum.PENDING.getCode(),
                            NotifyTodoStatusEnum.READ.getCode(),
                            NotifyTodoStatusEnum.DONE.getCode(),
                            NotifyTodoStatusEnum.INVALID.getCode()
                    ),
                    true
            ));
        }
        return summaries;
    }

    /**
     * 构建外部分发目标聚合摘要。
     *
     * @param dispatches 外部分发实体列表
     * @return 目标聚合摘要
     */
    private List<NotifyTraceTargetSummaryVO> buildDispatchTargetSummaries(List<SysNotifyDispatch> dispatches) {
        if (dispatches == null || dispatches.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, List<SysNotifyDispatch>> groupedMap = dispatches.stream()
                .collect(Collectors.groupingBy(this::resolveDispatchTargetType, LinkedHashMap::new, Collectors.toList()));
        List<NotifyTraceTargetSummaryVO> summaries = new ArrayList<>();
        for (Map.Entry<String, List<SysNotifyDispatch>> entry : groupedMap.entrySet()) {
            Map<String, Integer> statusCountMap = new LinkedHashMap<>();
            for (SysNotifyDispatch dispatch : entry.getValue()) {
                String status = StrUtil.blankToDefault(dispatch.getDispatchStatus(), NotifyDispatchStatusEnum.PENDING.getCode());
                statusCountMap.merge(status, 1, Integer::sum);
            }
            summaries.add(buildTargetSummary(
                    entry.getKey(),
                    PRODUCT_CATEGORY_EXTERNAL,
                    entry.getValue().size(),
                    statusCountMap,
                    Arrays.asList(
                            NotifyDispatchStatusEnum.FAILED.getCode(),
                            NotifyDispatchStatusEnum.SKIPPED.getCode(),
                            NotifyDispatchStatusEnum.DEAD.getCode(),
                            NotifyDispatchStatusEnum.PROCESSING.getCode(),
                            NotifyDispatchStatusEnum.PENDING.getCode(),
                            NotifyDispatchStatusEnum.SUCCESS.getCode()
                    ),
                    false
            ));
        }
        return summaries;
    }

    /**
     * 基于详情 VO 构建外部分发目标聚合摘要。
     *
     * @param dispatches 外部分发详情列表
     * @return 目标聚合摘要
     */
    private List<NotifyTraceTargetSummaryVO> buildDispatchTargetSummariesByDetails(List<NotifyTraceDispatchDetailVO> dispatches) {
        if (dispatches == null || dispatches.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, List<NotifyTraceDispatchDetailVO>> groupedMap = dispatches.stream()
                // 历史记录可能缺少 targetType，排障页要兜底展示，不能因为旧数据脏值直接报错。
                .collect(Collectors.groupingBy(dispatch -> resolveTargetTypeKey(dispatch.getTargetType()),
                        LinkedHashMap::new, Collectors.toList()));
        List<NotifyTraceTargetSummaryVO> summaries = new ArrayList<>();
        for (Map.Entry<String, List<NotifyTraceDispatchDetailVO>> entry : groupedMap.entrySet()) {
            Map<String, Integer> statusCountMap = new LinkedHashMap<>();
            for (NotifyTraceDispatchDetailVO dispatch : entry.getValue()) {
                String status = StrUtil.blankToDefault(dispatch.getDispatchStatus(), NotifyDispatchStatusEnum.PENDING.getCode());
                statusCountMap.merge(status, 1, Integer::sum);
            }
            summaries.add(buildTargetSummary(
                    entry.getKey(),
                    PRODUCT_CATEGORY_EXTERNAL,
                    entry.getValue().size(),
                    statusCountMap,
                    Arrays.asList(
                            NotifyDispatchStatusEnum.FAILED.getCode(),
                            NotifyDispatchStatusEnum.SKIPPED.getCode(),
                            NotifyDispatchStatusEnum.DEAD.getCode(),
                            NotifyDispatchStatusEnum.PROCESSING.getCode(),
                            NotifyDispatchStatusEnum.PENDING.getCode(),
                            NotifyDispatchStatusEnum.SUCCESS.getCode()
                    ),
                    false
            ));
        }
        return summaries;
    }

    /**
     * 构建单个目标摘要。
     *
     * <p>这里统一把状态计数、重点状态和摘要文案封装好，
     * 让控制层和前端都不再自己拼“失败/跳过/死信”口径，避免不同页面出现不同解释。</p>
     *
     * @param targetType 通知目标类型
     * @param productCategory 产物分类
     * @param totalCount 总数
     * @param statusCountMap 状态计数
     * @param highlightOrder 状态优先级
     * @param inApp 是否站内产物
     * @return 目标摘要
     */
    private NotifyTraceTargetSummaryVO buildTargetSummary(String targetType, String productCategory, int totalCount,
                                                          Map<String, Integer> statusCountMap, List<String> highlightOrder,
                                                          boolean inApp) {
        NotifyTraceTargetSummaryVO summaryVO = new NotifyTraceTargetSummaryVO();
        summaryVO.setTargetType(targetType);
        summaryVO.setTargetTypeDesc(resolveTargetTypeDesc(targetType));
        summaryVO.setProductCategory(productCategory);
        summaryVO.setProductCategoryDesc(PRODUCT_CATEGORY_IN_APP.equals(productCategory) ? "站内产物" : "外部分发");
        summaryVO.setTotalCount(totalCount);
        summaryVO.setStatusCounts(buildStatusCountList(statusCountMap, inApp));
        summaryVO.setHighlightStatus(resolveHighlightStatus(statusCountMap, highlightOrder));
        summaryVO.setHighlightStatusDesc(resolveProductStatusDesc(summaryVO.getHighlightStatus(), inApp));
        summaryVO.setSummaryText(buildSummaryText(totalCount, statusCountMap, inApp));
        return summaryVO;
    }

    /**
     * 构建状态计数列表。
     *
     * @param statusCountMap 状态计数
     * @param inApp 是否站内产物
     * @return 状态计数列表
     */
    private List<NotifyTraceStatusCountVO> buildStatusCountList(Map<String, Integer> statusCountMap, boolean inApp) {
        if (statusCountMap == null || statusCountMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> order = inApp
                ? Arrays.asList(
                NotifyTodoStatusEnum.PENDING.getCode(),
                NotifyTodoStatusEnum.READ.getCode(),
                NotifyTodoStatusEnum.DONE.getCode(),
                NotifyTodoStatusEnum.INVALID.getCode()
        )
                : Arrays.asList(
                NotifyDispatchStatusEnum.PENDING.getCode(),
                NotifyDispatchStatusEnum.PROCESSING.getCode(),
                NotifyDispatchStatusEnum.SUCCESS.getCode(),
                NotifyDispatchStatusEnum.FAILED.getCode(),
                NotifyDispatchStatusEnum.SKIPPED.getCode(),
                NotifyDispatchStatusEnum.DEAD.getCode()
        );
        List<NotifyTraceStatusCountVO> items = new ArrayList<>();
        for (String status : order) {
            Integer count = statusCountMap.get(status);
            if (count == null || count <= 0) {
                continue;
            }
            NotifyTraceStatusCountVO item = new NotifyTraceStatusCountVO();
            item.setStatus(status);
            item.setStatusDesc(resolveProductStatusDesc(status, inApp));
            item.setCount(count);
            items.add(item);
        }
        return items;
    }

    /**
     * 解析应高亮展示的状态。
     *
     * @param statusCountMap 状态计数
     * @param order 状态优先级
     * @return 高亮状态
     */
    private String resolveHighlightStatus(Map<String, Integer> statusCountMap, List<String> order) {
        if (statusCountMap == null || statusCountMap.isEmpty()) {
            return null;
        }
        for (String status : order) {
            Integer count = statusCountMap.get(status);
            if (count != null && count > 0) {
                return status;
            }
        }
        return statusCountMap.keySet().stream().findFirst().orElse(null);
    }

    /**
     * 构建聚合摘要文案。
     *
     * @param totalCount 总数
     * @param statusCountMap 状态计数
     * @param inApp 是否站内产物
     * @return 摘要文案
     */
    private String buildSummaryText(int totalCount, Map<String, Integer> statusCountMap, boolean inApp) {
        if (statusCountMap == null || statusCountMap.isEmpty()) {
            return inApp ? "未生成站内产物" : "未生成外部分发任务";
        }
        List<String> parts = new ArrayList<>();
        parts.add((inApp ? "已生成" : "共") + totalCount + "条");
        for (NotifyTraceStatusCountVO statusCountVO : buildStatusCountList(statusCountMap, inApp)) {
            parts.add(statusCountVO.getStatusDesc() + statusCountVO.getCount() + "条");
        }
        return String.join("，", parts);
    }

    /**
     * 把产物列表按事件ID分组。
     *
     * @param rows 明细列表
     * @param eventIdGetter 事件ID读取器
     * @param <T> 明细类型
     * @return 按事件ID分组后的映射
     */
    private <T> Map<Long, List<T>> groupByEventId(List<T> rows, Function<T, Long> eventIdGetter) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        return rows.stream()
                .filter(row -> row != null && eventIdGetter.apply(row) != null)
                .collect(Collectors.groupingBy(eventIdGetter, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 解析分发 payload 快照。
     *
     * <p>排障页不能因为历史 payload 为空或格式异常而失败，
     * 因此解析异常只视为没有快照，继续展示主表字段。</p>
     *
     * @param payloadJson 分发 payload JSON
     * @return payload 快照；解析失败时返回 {@code null}
     */
    private NotifyDispatchPayload parseDispatchPayload(String payloadJson) {
        if (StrUtil.isBlank(payloadJson)) {
            return null;
        }
        try {
            return JSONUtil.toBean(payloadJson, NotifyDispatchPayload.class);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 解析通知场景名称。
     *
     * @param sceneCode 通知场景编码
     * @return 场景名称；未注册时返回 {@code null}
     */
    private String resolveSceneName(String sceneCode) {
        if (notifySceneRegistry == null) {
            return null;
        }
        NotifySceneMeta sceneMeta = notifySceneRegistry.getScene(sceneCode);
        return sceneMeta == null ? null : sceneMeta.getSceneName();
    }

    /**
     * 解析站内产物的通知场景编码。
     *
     * @param message 站内产物实体
     * @return 通知场景编码
     */
    private String resolveMessageSceneCode(SysNotifyMessage message) {
        if (message == null) {
            return null;
        }
        if (StrUtil.isNotBlank(message.getSceneCode())) {
            return message.getSceneCode();
        }
        if (StrUtil.isNotBlank(message.getTemplateCode())) {
            return message.getTemplateCode();
        }
        return message.getEventType();
    }

    /**
     * 解析站内产物的通知目标类型。
     *
     * @param message 站内产物实体
     * @return 通知目标类型
     */
    private String resolveMessageTargetType(SysNotifyMessage message) {
        if (message == null) {
            return UNKNOWN_TARGET_TYPE;
        }
        if (StrUtil.isNotBlank(message.getTargetType())) {
            return message.getTargetType();
        }
        return resolveTargetTypeKey(message.getMessageType());
    }

    /**
     * 解析分发任务的通知目标类型。
     *
     * @param dispatch 分发任务实体
     * @return 通知目标类型
     */
    private String resolveDispatchTargetType(SysNotifyDispatch dispatch) {
        if (dispatch == null) {
            return UNKNOWN_TARGET_TYPE;
        }
        if (StrUtil.isNotBlank(dispatch.getTargetType())) {
            return dispatch.getTargetType();
        }
        return resolveTargetTypeKey(dispatch.getChannelType());
    }

    /**
     * 归一化通知目标类型分组键。
     *
     * @param targetType 原始目标类型
     * @return 可用于分组和展示的目标类型
     */
    private String resolveTargetTypeKey(String targetType) {
        return StrUtil.blankToDefault(StrUtil.trim(targetType), UNKNOWN_TARGET_TYPE);
    }

    /**
     * 解析通知目标类型说明。
     *
     * @param targetType 通知目标类型
     * @return 目标类型说明
     */
    private String resolveTargetTypeDesc(String targetType) {
        if (StrUtil.isBlank(targetType) || StrUtil.equals(targetType, UNKNOWN_TARGET_TYPE)) {
            return "未知目标";
        }
        NotifyTypeEnum targetTypeEnum = NotifyTypeEnum.getByCode(targetType);
        return targetTypeEnum == null ? targetType : targetTypeEnum.getDesc();
    }

    /**
     * 解析目标状态说明。
     *
     * @param status 状态编码
     * @return 状态说明
     */
    private String resolveProductStatusDesc(String status, boolean inApp) {
        if (StrUtil.isBlank(status)) {
            return "-";
        }
        if (inApp) {
            if (NotifyTodoStatusEnum.PENDING.getCode().equals(status)) {
                return "待处理";
            }
            if (NotifyTodoStatusEnum.READ.getCode().equals(status)) {
                return "已读";
            }
            if (NotifyTodoStatusEnum.DONE.getCode().equals(status)) {
                return "已处理";
            }
            if (NotifyTodoStatusEnum.INVALID.getCode().equals(status)) {
                return "已失效";
            }
            return status;
        }
        if (NotifyEventStatusEnum.PROCESSING.getCode().equals(status)) {
            return "处理中";
        }
        if (NotifyEventStatusEnum.SUCCESS.getCode().equals(status)) {
            return "成功";
        }
        if (NotifyEventStatusEnum.FAILED.getCode().equals(status)) {
            return "失败";
        }
        if (NotifyEventStatusEnum.DEAD.getCode().equals(status)) {
            return "死信";
        }
        if (NotifyDispatchStatusEnum.PENDING.getCode().equals(status)) {
            return "待发送";
        }
        if (NotifyDispatchStatusEnum.SKIPPED.getCode().equals(status)) {
            return "已跳过";
        }
        return status;
    }

    /**
     * 判断事件是否允许人工重试。
     *
     * @param status 当前状态
     * @return 是否允许
     */
    private boolean isEventRetryAllowed(String status) {
        return StrUtil.equals(status, NotifyEventStatusEnum.FAILED.getCode())
                || StrUtil.equals(status, NotifyEventStatusEnum.DEAD.getCode());
    }

    /**
     * 判断分发任务是否允许人工重试。
     *
     * @param status 当前状态
     * @return 是否允许
     */
    private boolean isDispatchRetryAllowed(String status) {
        return StrUtil.equals(status, NotifyDispatchStatusEnum.FAILED.getCode())
                || StrUtil.equals(status, NotifyDispatchStatusEnum.DEAD.getCode());
    }

    /**
     * 判断事件是否允许人工标记死信。
     *
     * @param status 当前状态
     * @return 是否允许
     */
    private boolean isEventDeadAllowed(String status) {
        return StrUtil.equals(status, NotifyEventStatusEnum.NEW.getCode())
                || StrUtil.equals(status, NotifyEventStatusEnum.PROCESSING.getCode())
                || StrUtil.equals(status, NotifyEventStatusEnum.FAILED.getCode());
    }

    /**
     * 判断分发任务是否允许人工标记死信。
     *
     * @param status 当前状态
     * @return 是否允许
     */
    private boolean isDispatchDeadAllowed(String status) {
        return StrUtil.equals(status, NotifyDispatchStatusEnum.PENDING.getCode())
                || StrUtil.equals(status, NotifyDispatchStatusEnum.PROCESSING.getCode())
                || StrUtil.equals(status, NotifyDispatchStatusEnum.FAILED.getCode());
    }

    /**
     * 规范化人工处理原因。
     *
     * @param reason 原始原因
     * @return 规范化后的原因
     */
    private String normalizeManualReason(String reason) {
        String actualReason = StrUtil.trim(reason);
        if (StrUtil.isBlank(actualReason)) {
            throw new ServiceException("处理原因不能为空");
        }
        return actualReason;
    }
}
