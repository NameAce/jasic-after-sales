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
import com.jasic.aftersales.system.notify.domain.query.NotifyTraceQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceDispatchDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceEventDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceMessageDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTracePageVO;
import com.jasic.aftersales.system.notify.mapper.NotifyTraceMapper;
import com.jasic.aftersales.system.notify.mapper.SysNotifyDispatchMapper;
import com.jasic.aftersales.system.notify.mapper.SysNotifyMessageMapper;
import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.NotifyTraceService;
import com.jasic.aftersales.system.notify.support.NotifyDispatchPayload;
import com.jasic.aftersales.system.notify.support.NotifySceneMeta;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知记录排障服务实现。
 *
 * <p>该实现只处理后台排障查询和人工介入动作，
 * 不直接改动通知生成链路，避免 Phase 4 与前面消费/分发阶段耦合过深。</p>
 *
 * @author Codex
 * @date 2026/05/14
 */
@Service
public class NotifyTraceServiceImpl implements NotifyTraceService {

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
        // 分页 SQL 只负责聚合主链路记录，可读场景名称由注册表补齐，避免页面直接展示裸 sceneCode。
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
        // 排障页需要同时看到站内消息和外部分发，方便判断失败发生在事件消费还是下游发送。
        detailVO.setMessages(listMessageDetails(eventId));
        detailVO.setDispatches(listDispatchDetailsByEventId(eventId));
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
                    detailVO.setSceneName(resolveSceneName(message.getTemplateCode()));
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
        for (NotifyTracePageVO record : records) {
            record.setSceneName(resolveSceneName(record.getTemplateCode()));
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
                detailVO.setTemplateCode(payload.getSceneCode());
            }
            detailVO.setSceneName(StrUtil.blankToDefault(payload.getSceneName(),
                    resolveSceneName(detailVO.getTemplateCode())));
            detailVO.setTemplateName(payload.getTemplateName());
            detailVO.setChannelEnabled(payload.getChannelEnabled());
            return;
        }
        detailVO.setSceneName(resolveSceneName(detailVO.getTemplateCode()));
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
