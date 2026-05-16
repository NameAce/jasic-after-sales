package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTypeEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知场景注册表。
 *
 * <p>该组件是“统一通知场景 + 多通知目标配置”模型下的唯一注册元数据来源。
 * 它负责声明：
 * 1. 系统允许维护哪些 `sceneCode`
 * 2. 每个场景下允许启用哪些通知目标
 * 3. 每个目标的默认模板、默认渠道参数和变量白名单
 *
 * <p>该组件不负责数据库持久化、后台保存、站内消息落库或外部渠道发送。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Component
public class NotifySceneRegistry {

    /**
     * 已注册的通知场景列表。
     */
    private final List<NotifySceneMeta> sceneMetas;

    /**
     * 按 `sceneCode` 索引的场景映射。
     */
    private final Map<String, NotifySceneMeta> sceneMetaMap;

    /**
     * 构造通知场景注册表。
     */
    public NotifySceneRegistry() {
        List<NotifySceneMeta> metas = buildSceneMetaList();
        this.sceneMetas = Collections.unmodifiableList(metas);
        this.sceneMetaMap = Collections.unmodifiableMap(buildSceneMetaMap(metas));
    }

    /**
     * 查询全部已注册通知场景。
     *
     * @return 已注册通知场景列表
     */
    public List<NotifySceneMeta> listScenes() {
        return sceneMetas;
    }

    /**
     * 按场景编码查询通知场景。
     *
     * @param sceneCode 通知场景编码
     * @return 命中的场景元数据；未命中时返回 {@code null}
     */
    public NotifySceneMeta getScene(String sceneCode) {
        if (sceneCode == null) {
            return null;
        }
        return sceneMetaMap.get(sceneCode.trim());
    }

    /**
     * 按场景编码强校验查询通知场景。
     *
     * @param sceneCode 通知场景编码
     * @return 命中的场景元数据
     */
    public NotifySceneMeta getRequiredScene(String sceneCode) {
        NotifySceneMeta sceneMeta = getScene(sceneCode);
        if (sceneMeta == null) {
            throw new ServiceException("不支持的通知场景：" + sceneCode);
        }
        return sceneMeta;
    }

    /**
     * 强校验读取指定场景下的目标元数据。
     *
     * @param sceneCode 通知场景编码
     * @param targetType 通知目标类型
     * @return 命中的目标元数据
     */
    public NotifySceneTargetMeta getRequiredTargetMeta(String sceneCode, String targetType) {
        NotifySceneMeta sceneMeta = getRequiredScene(sceneCode);
        NotifySceneTargetMeta targetMeta = sceneMeta.getTargetMeta(targetType);
        if (targetMeta == null) {
            throw new ServiceException("当前场景不支持该通知目标：" + targetType);
        }
        return targetMeta;
    }

    /**
     * 构造场景列表。
     *
     * @return 场景列表
     */
    private List<NotifySceneMeta> buildSceneMetaList() {
        List<NotifySceneMeta> metas = new ArrayList<>();
        metas.add(buildWorkOrderAssignedScene());
        metas.add(buildWorkOrderEvaluationInviteScene());
        return metas;
    }

    /**
     * 构造按场景编码索引的映射。
     *
     * @param metas 场景列表
     * @return 索引映射
     */
    private Map<String, NotifySceneMeta> buildSceneMetaMap(List<NotifySceneMeta> metas) {
        Map<String, NotifySceneMeta> map = new LinkedHashMap<>();
        for (NotifySceneMeta meta : metas) {
            map.put(meta.getSceneCode(), meta);
        }
        return map;
    }

    /**
     * 构造工单派单场景。
     *
     * <p>该场景是阶段一重点落地的“一个场景支持多个通知目标”的示例场景，
     * 默认开放站内消息、站内待办和小程序订阅消息三个目标。</p>
     *
     * @return 工单派单场景元数据
     */
    private NotifySceneMeta buildWorkOrderAssignedScene() {
        List<NotifySceneTargetMeta> targetMetas = new ArrayList<>();
        targetMetas.add(new NotifySceneTargetMeta(
                NotifyTypeEnum.IN_APP_MESSAGE.getCode(),
                NotifyTypeEnum.IN_APP_MESSAGE.getDesc(),
                NotifyReceiverTypeEnum.REPAIRER.getCode(),
                NotifyReceiverTypeEnum.REPAIRER.getDesc(),
                "维修员",
                0,
                "工单派单消息",
                "您有新的维修工单",
                "工单${orderNo}已派给您，请及时查看处理进度",
                NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL,
                "${workOrderId}",
                null,
                null,
                null
        ));
        targetMetas.add(new NotifySceneTargetMeta(
                NotifyTypeEnum.IN_APP_TODO.getCode(),
                NotifyTypeEnum.IN_APP_TODO.getDesc(),
                NotifyReceiverTypeEnum.REPAIRER.getCode(),
                NotifyReceiverTypeEnum.REPAIRER.getDesc(),
                "维修员",
                1,
                "工单派单待办",
                "您有新的维修工单",
                "工单${orderNo}已派给您，请及时处理",
                NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL,
                "${workOrderId}",
                null,
                null,
                null
        ));
        targetMetas.add(new NotifySceneTargetMeta(
                NotifyTypeEnum.MP_SUBSCRIBE.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE.getDesc(),
                NotifyReceiverTypeEnum.REPAIRER.getCode(),
                NotifyReceiverTypeEnum.REPAIRER.getDesc(),
                "维修员",
                0,
                "工单派单订阅通知",
                "工单派单通知",
                "工单${orderNo}已派给您，请及时处理",
                null,
                null,
                NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode(),
                NotifyChannelTypeEnum.MP_SUBSCRIBE.getDesc(),
                buildDefaultAssignedMpChannelConfig()
        ));
        return new NotifySceneMeta(
                NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(),
                NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getDesc(),
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode(),
                NotifyTypeEnum.IN_APP_TODO.getCode(),
                buildAssignedVariables(),
                targetMetas
        );
    }

    /**
     * 构造客户评价邀请场景。
     *
     * @return 客户评价邀请场景元数据
     */
    private NotifySceneMeta buildWorkOrderEvaluationInviteScene() {
        List<NotifySceneTargetMeta> targetMetas = new ArrayList<>();
        targetMetas.add(new NotifySceneTargetMeta(
                NotifyTypeEnum.MP_SUBSCRIBE.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE.getDesc(),
                NotifyReceiverTypeEnum.CUSTOMER.getCode(),
                NotifyReceiverTypeEnum.CUSTOMER.getDesc(),
                "C端客户",
                1,
                "客户评价邀请订阅通知",
                "客户满意度评价通知",
                "您的维修工单${orderNo}已关闭，邀请您对本次服务进行评价",
                NotifyConstants.ROUTE_TYPE_WORK_ORDER_EVALUATE,
                "${workOrderId}",
                NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode(),
                NotifyChannelTypeEnum.MP_SUBSCRIBE.getDesc(),
                buildDefaultEvaluationInviteMpChannelConfig()
        ));
        return new NotifySceneMeta(
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode(),
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getDesc(),
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE.getCode(),
                buildEvaluationInviteVariables(),
                targetMetas
        );
    }

    /**
     * 构造工单派单场景可用变量。
     *
     * @return 变量元数据列表
     */
    private List<NotifyTemplateVariableMeta> buildAssignedVariables() {
        List<NotifyTemplateVariableMeta> variables = new ArrayList<>();
        variables.add(buildVariableMeta("workOrderId", "工单ID", "88"));
        variables.add(buildVariableMeta("orderNo", "工单号", "WO-20260515001"));
        variables.add(buildVariableMeta("receiverId", "接收维修员ID", "200"));
        variables.add(buildVariableMeta("receiverName", "接收维修员姓名", "维修员A"));
        variables.add(buildVariableMeta("operatorId", "操作人ID", "100"));
        variables.add(buildVariableMeta("oldAssignedUserId", "旧维修员ID", "199"));
        variables.add(buildVariableMeta("newAssignedUserId", "新维修员ID", "200"));
        variables.add(buildVariableMeta("assignType", "派单类型", "ASSIGN"));
        variables.add(buildVariableMeta("operationId", "派单动作幂等标识", "op-1001"));
        return variables;
    }

    /**
     * 构造客户评价邀请场景可用变量。
     *
     * @return 变量元数据列表
     */
    private List<NotifyTemplateVariableMeta> buildEvaluationInviteVariables() {
        List<NotifyTemplateVariableMeta> variables = new ArrayList<>();
        variables.add(buildVariableMeta("workOrderId", "工单ID", "91"));
        variables.add(buildVariableMeta("orderNo", "工单号", "WO-20260515002"));
        variables.add(buildVariableMeta("customerId", "客户ID", "9001"));
        variables.add(buildVariableMeta("customerMobile", "客户手机号", "13800138000"));
        variables.add(buildVariableMeta("customerOpenid", "客户openid", "openid-9001"));
        variables.add(buildVariableMeta("companyId", "服务网点ID", "3001"));
        variables.add(buildVariableMeta("companyName", "服务网点名称", "深圳南山服务网点"));
        variables.add(buildVariableMeta("closedTime", "工单关闭时间", "2026-05-15 18:00:00"));
        return variables;
    }

    /**
     * 构造工单派单小程序通知默认渠道配置。
     *
     * @return 默认渠道配置
     */
    private NotifyTemplateChannelConfig buildDefaultAssignedMpChannelConfig() {
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setTemplateId("");
        config.setPagePathTemplate("pages/order/detail?workOrderId=${workOrderId}");
        List<NotifyChannelFieldMappingDTO> mappings = new ArrayList<>();
        mappings.add(buildFieldMapping("thing1", "${orderNo}"));
        mappings.add(buildFieldMapping("thing2", "${receiverName}"));
        config.setFieldMapping(mappings);
        return config;
    }

    /**
     * 构造客户评价邀请小程序通知默认渠道配置。
     *
     * @return 默认渠道配置
     */
    private NotifyTemplateChannelConfig buildDefaultEvaluationInviteMpChannelConfig() {
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setTemplateId("");
        config.setPagePathTemplate("pages/order/evaluate?workOrderId=${workOrderId}");
        List<NotifyChannelFieldMappingDTO> mappings = new ArrayList<>();
        mappings.add(buildFieldMapping("thing1", "${orderNo}"));
        mappings.add(buildFieldMapping("thing2", "${companyName}"));
        mappings.add(buildFieldMapping("date3", "${closedTime}"));
        config.setFieldMapping(mappings);
        return config;
    }

    /**
     * 构造单个变量元数据。
     *
     * @param name 变量名
     * @param desc 变量说明
     * @param example 示例值
     * @return 变量元数据
     */
    private NotifyTemplateVariableMeta buildVariableMeta(String name, String desc, String example) {
        return new NotifyTemplateVariableMeta(name, desc, example);
    }

    /**
     * 构造单条字段映射。
     *
     * @param field 渠道字段
     * @param value 值模板
     * @return 字段映射
     */
    private NotifyChannelFieldMappingDTO buildFieldMapping(String field, String value) {
        NotifyChannelFieldMappingDTO mapping = new NotifyChannelFieldMappingDTO();
        mapping.setField(field);
        mapping.setValue(value);
        return mapping;
    }
}
