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
 * @author Codex
 * @date 2026/05/16
 */
@Component
public class NotifySceneRegistry {

    /**
     * B 端接单通知模板 ID。
     */
    private static final String TEMPLATE_ID_WORK_ORDER_ACCEPT_B = "JEO-zVGuWBQPIhU0ck7e3I97Tlr1tNk1ouxbbLovCCE";

    /**
     * B 端工单转入通知模板 ID。
     */
    private static final String TEMPLATE_ID_WORK_ORDER_TRANSFER_IN_B = "mw7ebqsdXbJxdQf-A_9161z0CdEVRGSi_I-gQY3dONw";

    /**
     * B 端工单派单通知模板 ID。
     */
    private static final String TEMPLATE_ID_WORK_ORDER_ASSIGNED_B = "hhXhuNSWE4r98FbVMX8MfveAzBq3h7-QtfAMVOB2fTg";

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
    private static final String CUSTOMER_TRANSFER_FIXED_TIP = "您的工单已转由其他网点继续处理，请留意后续联系。";

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
        metas.add(buildWorkOrderAcceptScene());
        metas.add(buildWorkOrderTransferInScene());
        metas.add(buildWorkOrderAssignedScene());
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
     * 构造 B 端接单通知场景。
     *
     * <p>该场景对应“新工单进入目标承接网点待处理池”后的网点级通知，
     * 默认只开放 B 端小程序订阅通知目标。</p>
     *
     * @return B 端接单通知场景元数据
     */
    private NotifySceneMeta buildWorkOrderAcceptScene() {
        List<NotifySceneTargetMeta> targetMetas = new ArrayList<>();
        targetMetas.add(buildMiniProgramTargetMeta(
                NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(),
                NotifyTypeEnum.MP_SUBSCRIBE_B.getDesc(),
                NotifyReceiverTypeEnum.ACCEPT_USER.getCode(),
                NotifyReceiverTypeEnum.ACCEPT_USER.getDesc(),
                "当前目标网点下已启用、具备接单权限且已订阅模板的用户",
                1,
                "B端接单通知",
                "B端接单通知",
                "新工单 ${orderNo} 已进入当前网点待处理，请及时接单或安排处理",
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
     * 构造 B 端工单派单通知场景。
     *
     * <p>该场景继续保留现有站内消息、站内待办和 B 端小程序订阅通知三类目标。
     * 其中小程序模板字段中的“用户名称、联系电话”统一解释为客户信息。</p>
     *
     * @return B 端工单派单通知场景元数据
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
                "B端工单派单通知",
                "B端工单派单通知",
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
     * 构造 B 端接单通知变量列表。
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
     * 构造 B 端工单派单通知变量列表。
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
