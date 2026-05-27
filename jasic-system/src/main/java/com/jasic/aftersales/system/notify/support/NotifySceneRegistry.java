package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelSceneEnum;
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
 * <p>该组件是“通知场景 + 通知目标 + 统一分发”模型下的唯一场景元数据入口。
 * 本轮只按文档确认的 6 个保留场景收口注册信息、默认模板配置和变量口径，
 * 明确排除“B 端评价提醒”。</p>
 *
 * <p>该组件负责声明：</p>
 * <p>1. 系统允许维护哪些通知场景编码。</p>
 * <p>2. 每个场景默认支持哪些通知目标。</p>
 * <p>3. 每个目标的默认标题、摘要、跳转路由和小程序模板配置。</p>
 * <p>4. 每个场景可使用哪些变量，以及这些变量的业务语义和兜底规则。</p>
 *
 * <p>该组件不负责事件消费、接收人实时解析、站内消息落库或微信真实发送。</p>
 *
 * @author Zoro
 * @date 2026/05/16
 */
@Component
public class NotifySceneRegistry {

    /**
     * B 端待派单通知模板 ID。
     */
    private static final String TEMPLATE_ID_WORK_ORDER_ACCEPT_B = "JEO-zVGuWBQPIhU0ck7e3I97Tlr1tNk1ouxbbLovCCE";

    /**
     * B 端工单转入通知模板 ID。
     */
    private static final String TEMPLATE_ID_WORK_ORDER_TRANSFER_IN_B = "mw7ebqsdXbJxdQf-A_9161z0CdEVRGSi_I-gQY3dONw";

    /**
     * B 端维修员接单通知模板 ID。
     */
    private static final String TEMPLATE_ID_WORK_ORDER_ASSIGNED_B = "hhXhuNSWE4r98FbVMX8MfveAzBq3h7-QtfAMVOB2fTg";

    /**
     * B端客户评价完成提醒模板 ID。
     */
    private static final String TEMPLATE_ID_WORK_ORDER_EVALUATED_B = "aW97dc0OyW40-vGbO9ekIT9DFfyS6JvR9UhPkPuaW_Q";

    /**
     * C 端接单成功提醒模板 ID。
     */
    private static final String TEMPLATE_ID_WORK_ORDER_ACCEPTED_C = "_p97aAe9-FJ2c6lCcZjVMQgxDnvBz8q6IRdFnnjIyWg";

    /**
     * C 端网点转单通知模板 ID。
     */
    private static final String TEMPLATE_ID_WORK_ORDER_TRANSFER_NOTICE_C = "0_vY_Wlie3dIuqmfpPAp_Hpbj-9yCso8yO1WSzWg3og";

    /**
     * C 端客户满意度评价通知模板 ID。
     */
    private static final String TEMPLATE_ID_WORK_ORDER_EVALUATION_INVITE_C = "01ZBgiyxkgui_wKWFtYsETnkSySMxeANaK2SoShvXkM";

    /**
     * B 端工单详情页路径模板。
     */
    private static final String PAGE_PATH_WORK_ORDER_DETAIL_B = "pages/order/detail?workOrderId=${workOrderId}";

    /**
     * C 端工单详情页路径模板。
     */
    private static final String PAGE_PATH_WORK_ORDER_DETAIL_C = "pages/order/detail?workOrderId=${workOrderId}";

    /**
     * C 端评价页路径模板。
     */
    private static final String PAGE_PATH_WORK_ORDER_EVALUATE_C = "pages/order/evaluate?workOrderId=${workOrderId}";

    /**
     * C 端网点转单固定提示文案。
     */
    private static final String CUSTOMER_TRANSFER_FIXED_TIP = "已转入新网点维修。";

    /**
     * 已注册的通知场景列表。
     */
    private final List<NotifySceneMeta> sceneMetas;

    /**
     * 按 `sceneCode` 索引的场景映射。
     */
    private final Map<String, NotifySceneMeta> sceneMetaMap;

    /**
     * 系统级通知目标元数据池。
     *
     * <p>该列表用于后台配置页展示“系统当前支持哪些通知目标”。
     * 它不再按场景裁剪，但会在合并过程中清空存在场景差异的默认配置字段，
     * 避免把某个场景专属的接收人或默认模板误展示到其它场景上。</p>
     */
    private final List<NotifySceneTargetMeta> systemTargetMetas;

    /**
     * 按 `targetType` 索引的系统级通知目标映射。
     */
    private final Map<String, NotifySceneTargetMeta> systemTargetMetaMap;

    /**
     * 构造通知场景注册表。
     */
    public NotifySceneRegistry() {
        List<NotifySceneMeta> metas = buildSceneMetaList();
        this.sceneMetas = Collections.unmodifiableList(metas);
        this.sceneMetaMap = Collections.unmodifiableMap(buildSceneMetaMap(metas));
        List<NotifySceneTargetMeta> supportedTargetMetas = buildSystemTargetMetaList(metas);
        this.systemTargetMetas = Collections.unmodifiableList(supportedTargetMetas);
        this.systemTargetMetaMap = Collections.unmodifiableMap(buildSystemTargetMetaMap(supportedTargetMetas));
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
     * 查询系统级通知目标元数据池。
     *
     * <p>该方法返回的是“系统当前支持的目标候选项”，
     * 主要用于后台配置页展示启用目标勾选项，不再按单个场景做裁剪。</p>
     *
     * @return 系统级通知目标元数据列表
     */
    public List<NotifySceneTargetMeta> listSystemTargetMetas() {
        return systemTargetMetas;
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
            // 后台配置页现在允许按系统级目标池为场景补充新的目标类型，
            // 因此这里先优先读取场景注册元数据，未命中时再回退到系统级目标池。
            targetMeta = getSystemTargetMeta(targetType);
        }
        if (targetMeta == null) {
            throw new ServiceException("当前场景不支持该通知目标：" + targetType);
        }
        return targetMeta;
    }

    /**
     * 按通知目标类型查询系统级目标元数据。
     *
     * @param targetType 通知目标类型
     * @return 命中的目标元数据；未命中时返回 {@code null}
     */
    public NotifySceneTargetMeta getSystemTargetMeta(String targetType) {
        if (targetType == null) {
            return null;
        }
        return systemTargetMetaMap.get(targetType.trim());
    }

    /**
     * 强校验读取系统级目标元数据。
     *
     * @param targetType 通知目标类型
     * @return 命中的目标元数据
     */
    public NotifySceneTargetMeta getRequiredSystemTargetMeta(String targetType) {
        NotifySceneTargetMeta targetMeta = getSystemTargetMeta(targetType);
        if (targetMeta == null) {
            throw new ServiceException("不支持的通知目标：" + targetType);
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
        metas.add(buildWorkOrderAcceptScene());
        metas.add(buildWorkOrderTransferInScene());
        metas.add(buildWorkOrderAssignedScene());
        metas.add(buildWorkOrderEvaluatedScene());
        metas.add(buildWorkOrderAcceptedScene());
        metas.add(buildWorkOrderTransferNoticeScene());
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
     * 构造系统级通知目标元数据池。
     *
     * <p>同一个 `targetType` 可能在不同场景下复用，但接收人语义、默认模板或默认渠道配置不一定一致。
     * 这里按 `targetType` 聚合后，只保留所有场景都一致的字段；
     * 一旦发现存在差异，就把对应默认字段清空，交由场景实际配置兜底。</p>
     *
     * @param metas 场景元数据列表
     * @return 系统级目标元数据列表
     */
    private List<NotifySceneTargetMeta> buildSystemTargetMetaList(List<NotifySceneMeta> metas) {
        Map<String, NotifySceneTargetMeta> map = new LinkedHashMap<>();
        for (NotifySceneMeta sceneMeta : metas) {
            for (NotifySceneTargetMeta targetMeta : sceneMeta.getTargetMetas()) {
                NotifySceneTargetMeta existing = map.get(targetMeta.getTargetType());
                if (existing == null) {
                    map.put(targetMeta.getTargetType(), targetMeta);
                    continue;
                }
                map.put(targetMeta.getTargetType(), mergeSystemTargetMeta(existing, targetMeta));
            }
        }
        return new ArrayList<>(map.values());
    }

    /**
     * 构造系统级目标映射。
     *
     * @param metas 系统级目标元数据列表
     * @return 按目标类型索引的映射
     */
    private Map<String, NotifySceneTargetMeta> buildSystemTargetMetaMap(List<NotifySceneTargetMeta> metas) {
        Map<String, NotifySceneTargetMeta> map = new LinkedHashMap<>();
        for (NotifySceneTargetMeta meta : metas) {
            map.put(meta.getTargetType(), meta);
        }
        return map;
    }

    /**
     * 合并两个同类型的场景目标元数据，生成系统级目标元数据。
     *
     * <p>目标类型、目标描述和渠道语义属于系统能力本身，按一致值保留；
     * 接收人描述、默认模板、默认跳转和默认渠道配置一旦出现差异，就清空为 `null`，
     * 避免系统级目标池误把某个场景专属默认值带到其它场景。</p>
     *
     * @param left 已合并的系统级目标元数据
     * @param right 当前场景目标元数据
     * @return 合并后的系统级目标元数据
     */
    private NotifySceneTargetMeta mergeSystemTargetMeta(NotifySceneTargetMeta left, NotifySceneTargetMeta right) {
        return new NotifySceneTargetMeta(
                left.getTargetType(),
                resolveSameValue(left.getTargetTypeDesc(), right.getTargetTypeDesc()),
                resolveSameValue(left.getReceiverType(), right.getReceiverType()),
                resolveSameValue(left.getReceiverTypeDesc(), right.getReceiverTypeDesc()),
                resolveSameValue(left.getReceiverDesc(), right.getReceiverDesc()),
                resolveSameValue(left.getDefaultEnabled(), right.getDefaultEnabled()),
                resolveSameValue(left.getDefaultTemplateName(), right.getDefaultTemplateName()),
                resolveSameValue(left.getDefaultTitleTemplate(), right.getDefaultTitleTemplate()),
                resolveSameValue(left.getDefaultContentTemplate(), right.getDefaultContentTemplate()),
                resolveSameValue(left.getDefaultRouteType(), right.getDefaultRouteType()),
                resolveSameValue(left.getDefaultRouteValueTemplate(), right.getDefaultRouteValueTemplate()),
                resolveSameValue(left.getChannelType(), right.getChannelType()),
                resolveSameValue(left.getChannelTypeDesc(), right.getChannelTypeDesc()),
                resolveSameChannelConfig(left.getDefaultChannelConfig(), right.getDefaultChannelConfig())
        );
    }

    /**
     * 仅当两个值完全一致时才保留，否则返回 {@code null}。
     *
     * @param left 左值
     * @param right 右值
     * @param <T> 值类型
     * @return 一致值或 {@code null}
     */
    private <T> T resolveSameValue(T left, T right) {
        if (left == null && right == null) {
            return null;
        }
        return left != null && left.equals(right) ? left : null;
    }

    /**
     * 比较两个默认渠道配置是否完全一致。
     *
     * @param left 左配置
     * @param right 右配置
     * @return 一致时返回配置对象，否则返回 {@code null}
     */
    private NotifyTemplateChannelConfig resolveSameChannelConfig(NotifyTemplateChannelConfig left,
                                                                 NotifyTemplateChannelConfig right) {
        if (left == null && right == null) {
            return null;
        }
        if (left == null || right == null) {
            return null;
        }
        if (!equalsNullable(left.getTemplateId(), right.getTemplateId())
                || !equalsNullable(left.getChannelScene(), right.getChannelScene())
                || !equalsNullable(left.getPagePathTemplate(), right.getPagePathTemplate())
                || !isSameFieldMapping(left.getFieldMapping(), right.getFieldMapping())) {
            return null;
        }
        return left;
    }

    /**
     * 比较两个字段映射列表是否一致。
     *
     * @param left 左映射列表
     * @param right 右映射列表
     * @return `true` 表示一致
     */
    private boolean isSameFieldMapping(List<NotifyChannelFieldMappingDTO> left,
                                       List<NotifyChannelFieldMappingDTO> right) {
        if (left == null || left.isEmpty()) {
            return right == null || right.isEmpty();
        }
        if (right == null || left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            NotifyChannelFieldMappingDTO leftItem = left.get(i);
            NotifyChannelFieldMappingDTO rightItem = right.get(i);
            if (leftItem == null || rightItem == null) {
                if (leftItem != rightItem) {
                    return false;
                }
                continue;
            }
            if (!equalsNullable(leftItem.getField(), rightItem.getField())
                    || !equalsNullable(leftItem.getValue(), rightItem.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较两个可空字符串是否一致。
     *
     * @param left 左值
     * @param right 右值
     * @return `true` 表示一致
     */
    private boolean equalsNullable(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    /**
     * 构造 B 端待派单通知场景。
     *
     * <p>该场景对应“新工单进入目标承接网点待派单池”后的网点级通知，
     * 默认只开放 B 端小程序订阅通知目标。</p>
     *
     * @return B 端待派单通知场景元数据
     */
    private NotifySceneMeta buildWorkOrderAcceptScene() {
        List<NotifySceneTargetMeta> targetMetas = new ArrayList<>();
        targetMetas.add(buildMiniProgramTargetMeta(
                NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_B.getDesc(),
                NotifyReceiverTypeEnum.ASSIGN_USER.getCode(),
                NotifyReceiverTypeEnum.ASSIGN_USER.getDesc(),
                "当前目标网点下已启用、具备派单权限且已订阅模板的用户",
                1,
                "B端待派单通知",
                "B端待派单通知",
                "新工单 ${orderNo} 已进入当前网点待派单池，请及时派单处理",
                NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL,
                "${workOrderId}",
                buildMiniProgramConfig(
                        TEMPLATE_ID_WORK_ORDER_ACCEPT_B,
                        NotifyChannelSceneEnum.B.getCode(),
                        PAGE_PATH_WORK_ORDER_DETAIL_B,
                        buildFieldMapping("character_string14", "${orderNo}"),
                        buildFieldMapping("thing15", "${customerName}")
                )
        ));
        return new NotifySceneMeta(
                NotifySceneCode.WORK_ORDER_ACCEPT.getCode(),
                NotifySceneCode.WORK_ORDER_ACCEPT.getDesc(),
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                NotifyEventTypeEnum.WORK_ORDER_ACCEPT.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(),
                buildWorkOrderAcceptVariables(),
                targetMetas
        );
    }

    /**
     * 构造 B 端工单转入通知场景。
     *
     * <p>该场景用于收口“工单从其他网点转入当前网点”后的网点级通知口径，
     * 模板中的“网点名称”统一解释为转出网点名称。</p>
     *
     * @return B 端工单转入通知场景元数据
     */
    private NotifySceneMeta buildWorkOrderTransferInScene() {
        List<NotifySceneTargetMeta> targetMetas = new ArrayList<>();
        targetMetas.add(buildMiniProgramTargetMeta(
                NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_B.getDesc(),
                NotifyReceiverTypeEnum.ACCEPT_USER.getCode(),
                NotifyReceiverTypeEnum.ACCEPT_USER.getDesc(),
                "转入网点下已启用、具备接单权限且已订阅模板的用户",
                1,
                "B端工单转入通知",
                "B端工单转入通知",
                "工单 ${orderNo} 已转入当前网点，请继续跟进处理",
                NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL,
                "${workOrderId}",
                buildMiniProgramConfig(
                        TEMPLATE_ID_WORK_ORDER_TRANSFER_IN_B,
                        NotifyChannelSceneEnum.B.getCode(),
                        PAGE_PATH_WORK_ORDER_DETAIL_B,
                        buildFieldMapping("character_string1", "${orderNo}"),
                        buildFieldMapping("thing2", "${customerName}"),
                        buildFieldMapping("phone_number3", "${customerMobile}"),
                        buildFieldMapping("thing4", "${fromCompanyName}")
                )
        ));
        return new NotifySceneMeta(
                NotifySceneCode.WORK_ORDER_TRANSFER_IN.getCode(),
                NotifySceneCode.WORK_ORDER_TRANSFER_IN.getDesc(),
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                NotifyEventTypeEnum.WORK_ORDER_TRANSFER_IN.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(),
                buildWorkOrderTransferInVariables(),
                targetMetas
        );
    }

    /**
     * 构造 B 端维修员接单通知场景。
     *
     * <p>该场景继续保留现有站内消息、站内待办和 B 端小程序订阅通知三类目标。
     * 其中小程序模板字段中的“用户名称、联系电话”统一解释为客户信息。</p>
     *
     * @return B 端维修员接单通知场景元数据
     */
    private NotifySceneMeta buildWorkOrderAssignedScene() {
        List<NotifySceneTargetMeta> targetMetas = new ArrayList<>();
        targetMetas.add(new NotifySceneTargetMeta(
                NotifyTypeEnum.IN_APP_MESSAGE.getCode(),
                NotifyTypeEnum.IN_APP_MESSAGE.getDesc(),
                NotifyReceiverTypeEnum.REPAIRER.getCode(),
                NotifyReceiverTypeEnum.REPAIRER.getDesc(),
                "被派单工程师本人",
                0,
                "工单派单消息",
                "您有新的维修工单",
                "工单 ${orderNo} 已派给您，请及时查看处理进度",
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
                "被派单工程师本人",
                1,
                "工单派单待办",
                "您有新的维修工单",
                "工单 ${orderNo} 已派给您，请及时处理",
                NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL,
                "${workOrderId}",
                null,
                null,
                null
        ));
        targetMetas.add(buildMiniProgramTargetMeta(
                NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_B.getDesc(),
                NotifyReceiverTypeEnum.REPAIRER.getCode(),
                NotifyReceiverTypeEnum.REPAIRER.getDesc(),
                "被派单工程师本人",
                1,
                "B端维修员接单通知",
                "B端维修员接单通知",
                "工单 ${orderNo} 已派给您，请及时联系客户并处理",
                NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL,
                "${workOrderId}",
                buildMiniProgramConfig(
                        TEMPLATE_ID_WORK_ORDER_ASSIGNED_B,
                        NotifyChannelSceneEnum.B.getCode(),
                        PAGE_PATH_WORK_ORDER_DETAIL_B,
                        buildFieldMapping("character_string1", "${orderNo}"),
                        buildFieldMapping("thing15", "${customerName}"),
                        buildFieldMapping("phone_number16", "${customerMobile}")
                )
        ));
        return new NotifySceneMeta(
                NotifySceneCode.WORK_ORDER_ASSIGNED.getCode(),
                NotifySceneCode.WORK_ORDER_ASSIGNED.getDesc(),
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode(),
                NotifyTypeEnum.IN_APP_TODO.getCode(),
                buildWorkOrderAssignedVariables(),
                targetMetas
        );
    }

    /**
     * 构造 C 端接单成功提醒场景。
     *
     * <p>该场景只用于客户感知“已有工程师正式接单”，
     * 联系电话统一取当前服务网点对外电话，而不是工程师手机号。</p>
     *
     * @return C 端接单成功提醒场景元数据
     */
    /**
     * 该场景在客户评价提交成功后触发，默认通知当前责任维修员、最后派单人和最终处理公司的主账号。
     */
    private NotifySceneMeta buildWorkOrderEvaluatedScene() {
        List<NotifySceneTargetMeta> targetMetas = new ArrayList<>();
        targetMetas.add(buildMiniProgramTargetMeta(
                NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_B.getDesc(),
                NotifyReceiverTypeEnum.EVALUATED_B_USER.getCode(),
                NotifyReceiverTypeEnum.EVALUATED_B_USER.getDesc(),
                "当前责任维修员、最后派单人和最终处理公司主账号",
                1,
                "评价提醒",
                "评价提醒",
                "维修工单 ${orderNo} 已收到客户满意度评价，请及时查看详情",
                NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL,
                "${workOrderId}",
                buildMiniProgramConfig(
                        TEMPLATE_ID_WORK_ORDER_EVALUATED_B,
                        NotifyChannelSceneEnum.B.getCode(),
                        PAGE_PATH_WORK_ORDER_DETAIL_B,
                        buildFieldMapping("character_string8", "${orderNo}"),
                        buildFieldMapping("thing9", "${customerName}"),
                        buildFieldMapping("phone_number10", "${customerMobile}"),
                        buildFieldMapping("thing11", "${assignedUserName}")
                )
        ));
        return new NotifySceneMeta(
                NotifySceneCode.WORK_ORDER_EVALUATED.getCode(),
                NotifySceneCode.WORK_ORDER_EVALUATED.getDesc(),
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                NotifyEventTypeEnum.WORK_ORDER_EVALUATED.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(),
                buildWorkOrderEvaluatedVariables(),
                targetMetas
        );
    }

    /**
     * 该场景只用于客户感知“已有工程师正式接单”，联系电话统一取当前服务网点对外电话。
     */
    private NotifySceneMeta buildWorkOrderAcceptedScene() {
        List<NotifySceneTargetMeta> targetMetas = new ArrayList<>();
        targetMetas.add(buildMiniProgramTargetMeta(
                NotifyTypeEnum.MP_SUBSCRIBE_C.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_C.getDesc(),
                NotifyReceiverTypeEnum.CUSTOMER.getCode(),
                NotifyReceiverTypeEnum.CUSTOMER.getDesc(),
                "工单客户本人",
                1,
                "C端接单成功提醒",
                "C端接单成功提醒",
                "您的工单 ${orderNo} 已有工程师接单，当前网点将继续为您处理",
                NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL,
                "${workOrderId}",
                buildMiniProgramConfig(
                        TEMPLATE_ID_WORK_ORDER_ACCEPTED_C,
                        NotifyChannelSceneEnum.C.getCode(),
                        PAGE_PATH_WORK_ORDER_DETAIL_C,
                        buildFieldMapping("character_string9", "${orderNo}"),
                        buildFieldMapping("thing10", "${companyName}"),
                        buildFieldMapping("phone_number11", "${companyPhone}")
                )
        ));
        return new NotifySceneMeta(
                NotifySceneCode.WORK_ORDER_ACCEPTED.getCode(),
                NotifySceneCode.WORK_ORDER_ACCEPTED.getDesc(),
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                NotifyEventTypeEnum.WORK_ORDER_ACCEPTED.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_C.getCode(),
                buildWorkOrderAcceptedVariables(),
                targetMetas
        );
    }

    /**
     * 构造 C 端网点转单通知场景。
     *
     * <p>该场景用于客户感知当前处理网点变化。
     * 模板中的“网点名称”统一解释为转入后的当前处理网点名称，
     * “温馨提示”统一固定为文档确认文案。</p>
     *
     * @return C 端网点转单通知场景元数据
     */
    private NotifySceneMeta buildWorkOrderTransferNoticeScene() {
        List<NotifySceneTargetMeta> targetMetas = new ArrayList<>();
        targetMetas.add(buildMiniProgramTargetMeta(
                NotifyTypeEnum.MP_SUBSCRIBE_C.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_C.getDesc(),
                NotifyReceiverTypeEnum.CUSTOMER.getCode(),
                NotifyReceiverTypeEnum.CUSTOMER.getDesc(),
                "工单客户本人",
                1,
                "C端网点转单通知",
                "C端网点转单通知",
                "您的工单 ${orderNo} 已转由其他网点继续处理，请留意后续联系",
                NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL,
                "${workOrderId}",
                buildMiniProgramConfig(
                        TEMPLATE_ID_WORK_ORDER_TRANSFER_NOTICE_C,
                        NotifyChannelSceneEnum.C.getCode(),
                        PAGE_PATH_WORK_ORDER_DETAIL_C,
                        buildFieldMapping("character_string1", "${orderNo}"),
                        buildFieldMapping("thing2", "${toCompanyName}"),
                        buildFieldMapping("phone_number3", "${toCompanyPhone}"),
                        buildFieldMapping("thing4", "${transferTip}")
                )
        ));
        return new NotifySceneMeta(
                NotifySceneCode.WORK_ORDER_TRANSFER_NOTICE.getCode(),
                NotifySceneCode.WORK_ORDER_TRANSFER_NOTICE.getDesc(),
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                NotifyEventTypeEnum.WORK_ORDER_TRANSFER_NOTICE.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_C.getCode(),
                buildWorkOrderTransferNoticeVariables(),
                targetMetas
        );
    }

    /**
     * 构造 C 端客户满意度评价通知场景。
     *
     * @return C 端客户满意度评价通知场景元数据
     */
    private NotifySceneMeta buildWorkOrderEvaluationInviteScene() {
        List<NotifySceneTargetMeta> targetMetas = new ArrayList<>();
        targetMetas.add(buildMiniProgramTargetMeta(
                NotifyTypeEnum.MP_SUBSCRIBE_C.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_C.getDesc(),
                NotifyReceiverTypeEnum.CUSTOMER.getCode(),
                NotifyReceiverTypeEnum.CUSTOMER.getDesc(),
                "工单客户本人",
                1,
                "C端客户满意度评价通知",
                "C端客户满意度评价通知",
                "您的维修工单 ${orderNo} 已完成，欢迎对本次服务进行评价",
                NotifyConstants.ROUTE_TYPE_WORK_ORDER_EVALUATE,
                "${workOrderId}",
                buildMiniProgramConfig(
                        TEMPLATE_ID_WORK_ORDER_EVALUATION_INVITE_C,
                        NotifyChannelSceneEnum.C.getCode(),
                        PAGE_PATH_WORK_ORDER_EVALUATE_C,
                        buildFieldMapping("character_string1", "${orderNo}"),
                        buildFieldMapping("phone_number2", "${companyPhone}"),
                        buildFieldMapping("thing3", "${companyName}"),
                        buildFieldMapping("time4", "${closedTime}")
                )
        ));
        return new NotifySceneMeta(
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE.getCode(),
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE.getDesc(),
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_C.getCode(),
                buildWorkOrderEvaluationInviteVariables(),
                targetMetas
        );
    }

    /**
     * 构造 B 端待派单通知变量列表。
     *
     * @return 变量元数据列表
     */
    private List<NotifyTemplateVariableMeta> buildWorkOrderAcceptVariables() {
        List<NotifyTemplateVariableMeta> variables = new ArrayList<>();
        variables.add(buildVariableMeta("workOrderId", "工单ID", "88"));
        variables.add(buildVariableMeta("orderNo", "工单号", "WO-20260516001"));
        variables.add(buildVariableMeta("currentAcceptCompanyId", "当前承接网点ID", "3001"));
        variables.add(buildVariableMeta("currentAcceptCompanyName", "当前承接网点名称", "深圳南山服务网点"));
        variables.add(buildCustomerNameVariable());
        variables.add(buildVariableMeta("customerMobile", "客户联系电话", "13800138000"));
        return variables;
    }

    /**
     * 构造 B 端工单转入通知变量列表。
     *
     * @return 变量元数据列表
     */
    private List<NotifyTemplateVariableMeta> buildWorkOrderTransferInVariables() {
        List<NotifyTemplateVariableMeta> variables = new ArrayList<>();
        variables.add(buildVariableMeta("workOrderId", "工单ID", "89"));
        variables.add(buildVariableMeta("orderNo", "工单号", "WO-20260516002"));
        variables.add(buildVariableMeta("currentAcceptCompanyId", "转入后的当前承接网点ID", "3002"));
        variables.add(buildCustomerNameVariable());
        variables.add(buildVariableMeta("customerMobile", "客户联系电话", "13800138000"));
        variables.add(buildVariableMeta("fromCompanyName",
                "转出网点名称，B端工单转入模板中的“网点名称”统一解释为转出网点名称",
                "广州天河服务网点"));
        return variables;
    }

    /**
     * 构造 B 端维修员接单通知变量列表。
     *
     * @return 变量元数据列表
     */
    private List<NotifyTemplateVariableMeta> buildWorkOrderAssignedVariables() {
        List<NotifyTemplateVariableMeta> variables = new ArrayList<>();
        variables.add(buildVariableMeta("workOrderId", "工单ID", "90"));
        variables.add(buildVariableMeta("orderNo", "工单号", "WO-20260516003"));
        variables.add(buildVariableMeta("receiverId", "被派单工程师ID", "200"));
        variables.add(buildVariableMeta("receiverName", "被派单工程师姓名", "维修员A"));
        variables.add(buildVariableMeta("operatorId", "派单操作人ID", "100"));
        variables.add(buildVariableMeta("oldAssignedUserId", "转派前工程师ID，首次派单时允许为空", "199"));
        variables.add(buildVariableMeta("newAssignedUserId", "转派后工程师ID", "200"));
        variables.add(buildVariableMeta("assignType", "派单类型，取值 ASSIGN 或 TRANSFER", "ASSIGN"));
        variables.add(buildVariableMeta("operationId", "派单动作幂等标识", "op-1001"));
        variables.add(buildCustomerNameVariable());
        variables.add(buildVariableMeta("customerMobile", "客户联系电话，派单模板中的“联系电话”统一解释为客户联系电话", "13800138000"));
        return variables;
    }

    /**
     * 构造 C 端接单成功提醒变量列表。
     *
     * @return 变量元数据列表
     */
    /**
     * 该变量集合固定当前模板需要的工单号、客户信息和最终责任维修员信息。
     */
    private List<NotifyTemplateVariableMeta> buildWorkOrderEvaluatedVariables() {
        List<NotifyTemplateVariableMeta> variables = new ArrayList<>();
        variables.add(buildVariableMeta("workOrderId", "工单ID", "94"));
        variables.add(buildVariableMeta("orderNo", "维修工单号", "JSWX20251205_00001"));
        variables.add(buildVariableMeta("customerId", "客户ID", "9002"));
        variables.add(buildCustomerNameVariable());
        variables.add(buildVariableMeta("customerMobile", "客户联系电话", "18112345678"));
        variables.add(buildVariableMeta("assignedUserId", "当前责任维修员ID", "200"));
        variables.add(buildVariableMeta("assignedUserName",
                "接单人姓名，统一解释为客户评价时工单上的最终责任维修员展示名称",
                "李四"));
        variables.add(buildVariableMeta("currentAcceptCompanyId", "客户评价时工单的最终处理公司ID", "3003"));
        return variables;
    }

    /**buildWorkOrderAcceptedVariables 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@return 查询或组装后的业务数据集合。*/
    private List<NotifyTemplateVariableMeta> buildWorkOrderAcceptedVariables() {
        List<NotifyTemplateVariableMeta> variables = new ArrayList<>();
        variables.add(buildVariableMeta("workOrderId", "工单ID", "91"));
        variables.add(buildVariableMeta("orderNo", "工单号", "WO-20260516004"));
        variables.add(buildVariableMeta("companyName", "当前承接网点名称", "深圳南山服务网点"));
        variables.add(buildCompanyPhoneVariable("companyPhone", "当前承接网点联系电话"));
        return variables;
    }

    /**
     * 构造 C 端网点转单通知变量列表。
     *
     * @return 变量元数据列表
     */
    private List<NotifyTemplateVariableMeta> buildWorkOrderTransferNoticeVariables() {
        List<NotifyTemplateVariableMeta> variables = new ArrayList<>();
        variables.add(buildVariableMeta("workOrderId", "工单ID", "92"));
        variables.add(buildVariableMeta("orderNo", "工单号", "WO-20260516005"));
        variables.add(buildVariableMeta("toCompanyName",
                "转入后的当前处理网点名称，C端转单模板中的“网点名称”统一解释为该字段",
                "东莞南城服务网点"));
        variables.add(buildCompanyPhoneVariable("toCompanyPhone", "转入后的当前处理网点联系电话"));
        variables.add(buildVariableMeta("transferTip",
                "C端转单固定提示文案，当前统一固定为“" + CUSTOMER_TRANSFER_FIXED_TIP + "”",
                CUSTOMER_TRANSFER_FIXED_TIP));
        return variables;
    }

    /**
     * 构造 C 端客户满意度评价通知变量列表。
     *
     * @return 变量元数据列表
     */
    private List<NotifyTemplateVariableMeta> buildWorkOrderEvaluationInviteVariables() {
        List<NotifyTemplateVariableMeta> variables = new ArrayList<>();
        variables.add(buildVariableMeta("workOrderId", "工单ID", "93"));
        variables.add(buildVariableMeta("orderNo", "工单号", "WO-20260516006"));
        variables.add(buildVariableMeta("customerId", "客户ID", "9001"));
        variables.add(buildVariableMeta("customerMobile", "客户手机号", "13800138000"));
        variables.add(buildVariableMeta("customerOpenid", "客户openid", "openid-9001"));
        variables.add(buildVariableMeta("companyId", "服务网点ID", "3001"));
        variables.add(buildVariableMeta("companyName", "服务网点名称", "深圳南山服务网点"));
        variables.add(buildCompanyPhoneVariable("companyPhone", "服务网点联系电话"));
        variables.add(buildVariableMeta("closedTime", "工单关闭时间", "2026-05-16 18:00:00"));
        return variables;
    }

    /**
     * 构造统一的“小程序通知目标”元数据。
     *
     * @param targetType 目标类型
     * @param targetTypeDesc 目标类型描述
     * @param receiverType 接收对象类型
     * @param receiverTypeDesc 接收对象类型描述
     * @param receiverDesc 接收对象说明
     * @param defaultEnabled 默认启用状态
     * @param defaultTemplateName 默认模板名称
     * @param defaultTitleTemplate 默认标题模板
     * @param defaultContentTemplate 默认内容模板
     * @param defaultRouteType 默认路由类型
     * @param defaultRouteValueTemplate 默认路由值模板
     * @param defaultChannelConfig 默认渠道配置
     * @return 小程序通知目标元数据
     */
    private NotifySceneTargetMeta buildMiniProgramTargetMeta(String targetType, String targetTypeDesc,
                                                             String receiverType, String receiverTypeDesc,
                                                             String receiverDesc, Integer defaultEnabled,
                                                             String defaultTemplateName, String defaultTitleTemplate,
                                                             String defaultContentTemplate, String defaultRouteType,
                                                             String defaultRouteValueTemplate,
                                                             NotifyTemplateChannelConfig defaultChannelConfig) {
        return new NotifySceneTargetMeta(
                targetType,
                targetTypeDesc,
                receiverType,
                receiverTypeDesc,
                receiverDesc,
                defaultEnabled,
                defaultTemplateName,
                defaultTitleTemplate,
                defaultContentTemplate,
                defaultRouteType,
                defaultRouteValueTemplate,
                NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode(),
                NotifyChannelTypeEnum.MP_SUBSCRIBE.getDesc(),
                defaultChannelConfig
        );
    }

    /**
     * 构造默认小程序渠道配置。
     *
     * @param templateId 模板ID
     * @param channelScene 小程序场景
     * @param pagePathTemplate 页面路径模板
     * @param fieldMappings 字段映射列表
     * @return 默认小程序渠道配置
     */
    private NotifyTemplateChannelConfig buildMiniProgramConfig(String templateId, String channelScene,
                                                              String pagePathTemplate,
                                                              NotifyChannelFieldMappingDTO... fieldMappings) {
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setTemplateId(templateId);
        config.setChannelScene(channelScene);
        config.setPagePathTemplate(pagePathTemplate);
        List<NotifyChannelFieldMappingDTO> mappings = new ArrayList<>();
        if (fieldMappings != null) {
            Collections.addAll(mappings, fieldMappings);
        }
        config.setFieldMapping(mappings);
        return config;
    }

    /**
     * 构造“用户名称”统一变量元数据。
     *
     * @return 变量元数据
     */
    private NotifyTemplateVariableMeta buildCustomerNameVariable() {
        return buildVariableMeta(
                "customerName",
                "用户名称，统一按客户姓名 -> 客户手机号 -> “客户” 的顺序兜底展示",
                "张三"
        );
    }

    /**
     * 构造“公司联系电话”统一变量元数据。
     *
     * @param variableName 变量名
     * @param label 变量业务说明前缀
     * @return 变量元数据
     */
    private NotifyTemplateVariableMeta buildCompanyPhoneVariable(String variableName, String label) {
        return buildVariableMeta(
                variableName,
                label + "，统一按 sys_company.service_phone -> sys_company.contact_phone 的顺序取值",
                "0755-12345678"
        );
    }

    /**
     * 构造变量元数据。
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
     * 构造字段映射。
     *
     * @param field 模板字段名
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
