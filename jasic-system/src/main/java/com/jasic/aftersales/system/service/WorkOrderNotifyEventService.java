package com.jasic.aftersales.system.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.constant.WechatConfigConstants;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderNotifyEvent;
import com.jasic.aftersales.system.domain.enums.WechatMiniProgramScene;
import com.jasic.aftersales.system.domain.model.WorkOrderNotifyReceiverInfo;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysMenuMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.mapper.WorkOrderNotifyEventMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单通知事件服务
 *
 * @author Codex
 * @date 2026/03/26
 */
@Slf4j
@Service
public class WorkOrderNotifyEventService {

    private static final String EVENT_REPAIR_FINISHED = "REPAIR_FINISHED_NOTICE";
    private static final String EVENT_EVALUATION_INVITE = "EVALUATION_INVITE_NOTICE";
    private static final String EVENT_CUSTOMER_EVALUATED = "CUSTOMER_EVALUATED_NOTICE";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private static final String RECEIVER_CUSTOMER = "CUSTOMER";
    private static final String RECEIVER_USER = "USER";
    private static final String RECEIVER_COMPANY = "COMPANY";

    private static final String FAIL_NO_COMPANY_RECEIVER = "当前公司无可用接收人";
    private static final List<String> CURRENT_OWNER_MANAGER_PERMS = Arrays.asList(
            "workorder:assign", "workorder:transfer", "workorder:review", "workorder:close"
    );

    @Resource
    private WorkOrderNotifyEventMapper workOrderNotifyEventMapper;

    @Resource
    private SysUserCompanyMapper sysUserCompanyMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private WechatMiniProgramService wechatMiniProgramService;

    @Autowired(required = false)
    private List<WorkOrderNotifyReceiverResolver> receiverResolvers = Collections.emptyList();

    /**
     * 记录维修完成通知事件
     *
     * @param workOrder 工单主表
     * @param summary   维修摘要
     */
    public void recordRepairFinished(WorkOrder workOrder, String summary) {
        WorkOrderNotifyEvent event = buildPendingEvent(workOrder, workOrder.getCurrentAcceptCompanyId(),
                EVENT_REPAIR_FINISHED, "REPAIR_FINISH", RECEIVER_CUSTOMER, workOrder.getCustomerId(),
                "维修完成通知", buildContent("工单已维修完成", workOrder, summary));
        saveEventsAndDispatch(Collections.singletonList(event));
    }

    /**
     * 记录客户评价邀请通知事件
     *
     * @param workOrder 工单主表
     */
    public void recordEvaluationInvite(WorkOrder workOrder) {
        WorkOrderNotifyEvent event = buildPendingEvent(workOrder, workOrder.getCurrentAcceptCompanyId(),
                EVENT_EVALUATION_INVITE, "CLOSE", RECEIVER_CUSTOMER, workOrder.getCustomerId(),
                "客户满意度评价通知", buildContent("工单已关闭，请进行满意度评价", workOrder, null));
        saveEventsAndDispatch(Collections.singletonList(event));
    }

    /**
     * 记录客户评价结果通知事件
     *
     * @param workOrder 工单主表
     * @param score     评分
     * @param content   评价内容
     */
    public void recordCustomerEvaluated(WorkOrder workOrder, Integer score, String content) {
        String snapshotContent = buildContent("客户已完成评价，评分：" + score, workOrder, content);
        List<SysUser> receivers = listCurrentOwnerManagers(workOrder.getCurrentAcceptCompanyId());
        if (receivers.isEmpty()) {
            WorkOrderNotifyEvent event = buildPendingEvent(workOrder, workOrder.getCurrentAcceptCompanyId(),
                    EVENT_CUSTOMER_EVALUATED, "EVALUATE", RECEIVER_COMPANY, workOrder.getCurrentAcceptCompanyId(),
                    "客户评价结果通知", snapshotContent);
            event.setSendStatus(STATUS_FAILED);
            event.setFailReason(FAIL_NO_COMPANY_RECEIVER);
            saveEventsAndDispatch(Collections.singletonList(event));
            return;
        }

        List<WorkOrderNotifyEvent> events = receivers.stream()
                .map(user -> buildPendingEvent(workOrder, workOrder.getCurrentAcceptCompanyId(),
                        EVENT_CUSTOMER_EVALUATED, "EVALUATE", RECEIVER_USER, user.getId(),
                        "客户评价结果通知", snapshotContent))
                .collect(Collectors.toList());
        saveEventsAndDispatch(events);
    }

    private WorkOrderNotifyEvent buildPendingEvent(WorkOrder workOrder, Long companyId, String eventType, String triggerNode,
                                                   String receiverType, Long receiverId, String title, String content) {
        WorkOrderNotifyEvent event = new WorkOrderNotifyEvent();
        event.setWorkOrderId(workOrder.getId());
        event.setCompanyId(companyId);
        event.setEventType(eventType);
        event.setTriggerNode(triggerNode);
        event.setReceiverType(receiverType);
        event.setReceiverId(receiverId);
        event.setTitleSnapshot(title);
        event.setContentSnapshot(content);
        event.setSendStatus(STATUS_PENDING);
        return event;
    }

    private void saveEventsAndDispatch(List<WorkOrderNotifyEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        List<Long> pendingEventIds = new ArrayList<>();
        for (WorkOrderNotifyEvent event : events) {
            if (event == null || event.getReceiverId() == null) {
                continue;
            }
            workOrderNotifyEventMapper.insert(event);
            if (STATUS_PENDING.equals(event.getSendStatus())) {
                pendingEventIds.add(event.getId());
            }
        }
        dispatchAfterCommit(pendingEventIds);
    }

    private void dispatchAfterCommit(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return;
        }
        Runnable task = () -> eventIds.forEach(this::dispatchSafely);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }

    private void dispatchSafely(Long eventId) {
        try {
            dispatch(eventId);
        } catch (Exception ex) {
            log.error("发送工单通知失败，eventId={}", eventId, ex);
            markFailed(eventId, "微信通知发送失败");
        }
    }

    private void dispatch(Long eventId) {
        WorkOrderNotifyEvent event = workOrderNotifyEventMapper.selectById(eventId);
        if (event == null || !STATUS_PENDING.equals(event.getSendStatus())) {
            return;
        }
        if (RECEIVER_COMPANY.equals(event.getReceiverType())) {
            markFailed(eventId, FAIL_NO_COMPANY_RECEIVER);
            return;
        }

        WorkOrderNotifyReceiverInfo receiverInfo = resolveReceiverInfo(event);
        if (receiverInfo == null || StrUtil.isBlank(receiverInfo.getOpenid())) {
            markFailed(eventId, receiverInfo == null ? "通知接收人解析失败" : receiverInfo.getFailReason());
            return;
        }

        NotifyTemplateConfig templateConfig = resolveTemplateConfig(event.getEventType());
        if (templateConfig == null || StrUtil.isBlank(templateConfig.getTemplateId())) {
            markFailed(eventId, "微信配置未完成");
            return;
        }

        JSONObject data = buildTemplateData(event);
        wechatMiniProgramService.sendSubscribeMessage(templateConfig.getScene(), receiverInfo.getOpenid(),
                templateConfig.getTemplateId(), templateConfig.getPagePath(), data);
        markSuccess(eventId);
    }

    private WorkOrderNotifyReceiverInfo resolveReceiverInfo(WorkOrderNotifyEvent event) {
        for (WorkOrderNotifyReceiverResolver resolver : receiverResolvers) {
            if (resolver.supports(event.getReceiverType())) {
                return resolver.resolve(event.getReceiverId());
            }
        }
        WorkOrderNotifyReceiverInfo info = new WorkOrderNotifyReceiverInfo();
        info.setFailReason("未找到通知接收人解析器");
        return info;
    }

    private NotifyTemplateConfig resolveTemplateConfig(String eventType) {
        if (EVENT_REPAIR_FINISHED.equals(eventType)) {
            return buildTemplateConfig(WechatMiniProgramScene.C,
                    WechatConfigConstants.TEMPLATE_REPAIR_FINISHED,
                    WechatConfigConstants.PAGE_REPAIR_FINISHED);
        }
        if (EVENT_EVALUATION_INVITE.equals(eventType)) {
            return buildTemplateConfig(WechatMiniProgramScene.C,
                    WechatConfigConstants.TEMPLATE_EVALUATION_INVITE,
                    WechatConfigConstants.PAGE_EVALUATION_INVITE);
        }
        if (EVENT_CUSTOMER_EVALUATED.equals(eventType)) {
            return buildTemplateConfig(WechatMiniProgramScene.B,
                    WechatConfigConstants.TEMPLATE_CUSTOMER_EVALUATED,
                    WechatConfigConstants.PAGE_CUSTOMER_EVALUATED);
        }
        return null;
    }

    private NotifyTemplateConfig buildTemplateConfig(WechatMiniProgramScene scene, String templateKey, String pageKey) {
        NotifyTemplateConfig config = new NotifyTemplateConfig();
        config.setScene(scene);
        config.setTemplateId(sysConfigService.getValueByKey(templateKey));
        config.setPagePath(sysConfigService.getValueByKey(pageKey));
        return config;
    }

    private JSONObject buildTemplateData(WorkOrderNotifyEvent event) {
        WorkOrder workOrder = workOrderMapper.selectById(event.getWorkOrderId());
        String orderNo = workOrder == null ? null : workOrder.getOrderNo();
        SysCompany company = event.getCompanyId() == null ? null : sysCompanyMapper.selectById(event.getCompanyId());
        String companyName = company == null ? "售后通知" : company.getCompanyName();

        JSONObject data = JSONUtil.createObj();
        data.set("thing1", buildTemplateValue(truncateText(event.getTitleSnapshot(), 20)));
        data.set("character_string2", buildTemplateValue(truncateText(orderNo, 32)));
        data.set("thing3", buildTemplateValue(truncateText(companyName, 20)));
        data.set("thing4", buildTemplateValue(truncateText(event.getContentSnapshot(), 20)));
        return data;
    }

    private JSONObject buildTemplateValue(String value) {
        return JSONUtil.createObj().set("value", StrUtil.blankToDefault(value, "-"));
    }

    private void markSuccess(Long eventId) {
        WorkOrderNotifyEvent update = new WorkOrderNotifyEvent();
        update.setId(eventId);
        update.setSendStatus(STATUS_SUCCESS);
        update.setSendTime(LocalDateTime.now());
        update.setFailReason(null);
        workOrderNotifyEventMapper.updateById(update);
    }

    private void markFailed(Long eventId, String failReason) {
        WorkOrderNotifyEvent update = new WorkOrderNotifyEvent();
        update.setId(eventId);
        update.setSendStatus(STATUS_FAILED);
        update.setFailReason(StrUtil.blankToDefault(failReason, "微信通知发送失败"));
        workOrderNotifyEventMapper.updateById(update);
    }

    private List<SysUser> listCurrentOwnerManagers(Long companyId) {
        if (companyId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysUserCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserCompany::getCompanyId, companyId);
        List<SysUserCompany> relations = sysUserCompanyMapper.selectList(wrapper);
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = relations.stream()
                .map(SysUserCompany::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        return users.stream()
                .filter(user -> user != null && user.getStatus() != null && user.getStatus() == 1)
                .filter(user -> hasCurrentOwnerManagerPerm(user.getId(), companyId))
                .sorted(java.util.Comparator.comparing(SysUser::getId))
                .collect(Collectors.toList());
    }

    private boolean hasCurrentOwnerManagerPerm(Long userId, Long companyId) {
        Set<String> perms = sysMenuMapper.selectPermsByUserIdAndCompanyId(userId, companyId);
        if (perms == null || perms.isEmpty()) {
            return false;
        }
        return CURRENT_OWNER_MANAGER_PERMS.stream().anyMatch(perms::contains);
    }

    private String buildContent(String prefix, WorkOrder workOrder, String detail) {
        StringBuilder builder = new StringBuilder(prefix)
                .append("，工单号：")
                .append(workOrder.getOrderNo());
        if (StrUtil.isNotBlank(detail)) {
            builder.append("，说明：").append(detail);
        }
        return builder.toString();
    }

    private String truncateText(String value, int maxLength) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 订阅消息模板配置
     */
    private static class NotifyTemplateConfig {

        private WechatMiniProgramScene scene;
        private String templateId;
        private String pagePath;

        public WechatMiniProgramScene getScene() {
            return scene;
        }

        public void setScene(WechatMiniProgramScene scene) {
            this.scene = scene;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }

        public String getPagePath() {
            return pagePath;
        }

        public void setPagePath(String pagePath) {
            this.pagePath = pagePath;
        }
    }
}
