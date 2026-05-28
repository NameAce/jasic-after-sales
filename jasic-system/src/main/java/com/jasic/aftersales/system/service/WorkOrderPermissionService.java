package com.jasic.aftersales.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.enums.DataScopeEnum;
import com.jasic.aftersales.common.enums.ServiceModeEnum;
import com.jasic.aftersales.common.enums.WorkOrderActionEnum;
import com.jasic.aftersales.common.enums.WorkOrderRelationTagEnum;
import com.jasic.aftersales.common.enums.WorkOrderRelationTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.system.domain.access.WorkOrderAccessContext;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderFlow;
import com.jasic.aftersales.system.domain.entity.WorkOrderParticipant;
import com.jasic.aftersales.system.domain.query.WorkOrderQuery;
import com.jasic.aftersales.system.domain.query.WorkOrderScopedQuery;
import com.jasic.aftersales.system.mapper.WorkOrderFlowMapper;
import com.jasic.aftersales.system.mapper.WorkOrderParticipantMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * 工单权限判断服务。
 *
 * @author Zoro
 * @date 2026/03/26
 */
@Service
public class WorkOrderPermissionService {

    /**
     * 动作返回顺序。
     *
     * <p>前端按钮通常希望按稳定顺序展示，因此这里集中定义返回顺序，
     * 避免动作判断逻辑改造后，按钮顺序随着代码分支顺序发生漂移。</p>
     */
    private static final List<WorkOrderActionEnum> ACTION_ORDER = Collections.unmodifiableList(Arrays.asList(
            WorkOrderActionEnum.ASSIGN,
            WorkOrderActionEnum.UPLOAD_SEND_EXPRESS,
            WorkOrderActionEnum.TECH_ACCEPT,
            WorkOrderActionEnum.TRANSFER,
            WorkOrderActionEnum.REPAIR_FINISH,
            WorkOrderActionEnum.REVIEW,
            WorkOrderActionEnum.RETURN_METHOD,
            WorkOrderActionEnum.CLOSE
    ));

    /**workOrderParticipantMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private WorkOrderParticipantMapper workOrderParticipantMapper;

    /**
     * 工单流转记录 Mapper。
     *
     * <p>本轮“上传寄件单号”临时按首条 CREATE 流转回溯建单人，
     * 因此权限服务需要读取流转表；后续主表补齐正式创建人字段后，
     * 这里应切换为读取主表字段。</p>
     */
    @Resource
    private WorkOrderFlowMapper workOrderFlowMapper;

    /**
     * 工单访问上下文解析器依赖。
     */
    @Resource
    private WorkOrderAccessContextResolver accessContextResolver;

    /**workOrderUserParticipantService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private WorkOrderUserParticipantService workOrderUserParticipantService;

    /**
     * 判断当前登录人是否可以查看指定工单。
     *
     * @param workOrder 工单实体
     * @return true 表示允许查看
     */
    public boolean canView(WorkOrder workOrder) {
        return canView(workOrder, resolveAccessContext());
    }

    /**
     * canView。
     *
     * @param workOrder 工单业务对象，用于判断状态、权限或组装返回数据。
     * @param context 上下文对象，承载当前操作人、公司和数据范围。
     */
    public boolean canView(WorkOrder workOrder, WorkOrderAccessContext context) {
        if (workOrder == null) {
            return false;
        }
        Long currentCompanyId = context.getCurrentCompanyId();
        if (currentCompanyId == null) {
            return false;
        }
        if ("HQ".equals(context.getSubjectType())
                && !currentCompanyId.equals(workOrder.getHqCompanyId())) {
            return false;
        }
        List<Long> relatedCompanyIds = context.getRelatedCompanyIds();
        boolean matchRelatedCompanyScope = !relatedCompanyIds.isEmpty() && matchRelatedCompanyScope(workOrder, relatedCompanyIds);
        boolean requiresRelatedCompanyLimit = requiresRelatedCompanyLimit(context);
        if (currentCompanyId.equals(workOrder.getCurrentAcceptCompanyId())) {
            if (DataScopeEnum.SELF == context.getDataScopeEnum()) {
                Long currentUserId = context.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(workOrder.getAssignedUserId())) {
                    return false;
                }
            }
            return !requiresRelatedCompanyLimit || matchRelatedCompanyScope;
        }
        WorkOrderParticipant participant = getParticipant(workOrder.getId(), currentCompanyId);
        if (participant == null || Integer.valueOf(1).equals(participant.getIsCurrentHandler())) {
            return false;
        }
        if (requiresRelatedCompanyLimit && !matchRelatedCompanyScope) {
            return false;
        }
        if (DataScopeEnum.SELF == context.getDataScopeEnum()) {
            return hasHistoryUserParticipation(workOrder.getId(), currentCompanyId, context.getCurrentUserId());
        }
        return true;
    }

    /**
     * 构造携带服务端访问上下文的内部查询对象，供列表和统计查询复用。
     *
     * @param query 工单查询参数
     * @return 内部查询对象
     */
    public WorkOrderScopedQuery buildScopedQuery(WorkOrderQuery query) {
        WorkOrderScopedQuery scopedQuery = new WorkOrderScopedQuery();
        if (query != null) {
            scopedQuery.setViewScope(query.getViewScope());
            scopedQuery.setOrderNo(query.getOrderNo());
            scopedQuery.setKeyword(query.getKeyword());
            scopedQuery.setCustomerName(query.getCustomerName());
            scopedQuery.setCustomerMobile(query.getCustomerMobile());
            scopedQuery.setBarcode(query.getBarcode());
            scopedQuery.setProductModel(query.getProductModel());
            scopedQuery.setMainStatus(query.getMainStatus());
            scopedQuery.setDisplayStatus(query.getDisplayStatus());
            scopedQuery.setHasTransfer(query.getHasTransfer());
            // 转出方向筛选只表达“当前登录公司作为转出方”，实际公司边界由 Mapper 使用服务端上下文注入。
            scopedQuery.setTransferDirection(query.getTransferDirection());
            scopedQuery.setPageNum(query.getPageNum());
            scopedQuery.setPageSize(query.getPageSize());
        }
        scopedQuery.setAccessContext(resolveAccessContext());
        return scopedQuery;
    }

    /**
     * 解析Access上下文。
     *
     * @return 业务处理结果
     */
    public WorkOrderAccessContext resolveAccessContext() {
        return accessContextResolver.resolve();
    }

    /**
     * 解析当前登录人与工单之间已经成立的关系标签集合。
     *
     * <p>该方法只负责提炼“事实关系”，不直接产出按钮或放行结果。
     * 之所以单独抽成多值标签，是为了支持一人多岗场景：同一个用户在当前受理公司下，
     * 既可能是当前维修员，又可能同时拥有转单、关闭等管理动作权限。</p>
     *
     * <p>典型使用方式：</p>
     * <p>1. `canView` 先判断是否具备查看工单的资格。</p>
     * <p>2. `resolveRelationTags` 再把工单里的客观业务关系标签补齐。</p>
     * <p>3. `canExecute` 基于关系标签、动作权限点和工单状态决定某个动作是否允许。</p>
     *
     * @param workOrder 工单实体
     * @return 关系标签集合；未命中时返回空集合
     */
    public EnumSet<WorkOrderRelationTagEnum> resolveRelationTags(WorkOrder workOrder) {
        return resolveRelationTags(workOrder, resolveAccessContext());
    }

    /**
     * 解析关系Tags。
     *
     * @param workOrder 工单业务对象，用于判断状态、权限或组装返回数据。
     * @param context 上下文对象，承载当前操作人、公司和数据范围。
     * @return 业务处理结果
     */
    public EnumSet<WorkOrderRelationTagEnum> resolveRelationTags(WorkOrder workOrder, WorkOrderAccessContext context) {
        EnumSet<WorkOrderRelationTagEnum> relationTags = EnumSet.noneOf(WorkOrderRelationTagEnum.class);
        if (workOrder == null) {
            return relationTags;
        }
        Long currentCompanyId = context.getCurrentCompanyId();
        Long currentUserId = context.getCurrentUserId();
        if (currentCompanyId == null) {
            return relationTags;
        }
        // 当前登录公司就是当前受理公司，是大多数管理动作成立的必要前提。
        if (currentCompanyId.equals(workOrder.getCurrentAcceptCompanyId())) {
            relationTags.add(WorkOrderRelationTagEnum.CURRENT_ACCEPT_COMPANY);
        }
        // 建单方标签当前主要用于补齐事实关系，为后续扩展建单方能力预留挂点。
        if (currentCompanyId.equals(workOrder.getCreateCompanyId())) {
            relationTags.add(WorkOrderRelationTagEnum.CREATOR_COMPANY);
        }
        // 被明确派到这张单上的维修员单独打标，供接单/报价/维修动作复用。
        if (workOrder.getAssignedUserId() != null && workOrder.getAssignedUserId().equals(currentUserId)) {
            relationTags.add(WorkOrderRelationTagEnum.ASSIGNEE);
        }
        // 当前受理公司下的标签已经足够判定当前处理方关系，无需再查参与表补历史标签。
        if (relationTags.contains(WorkOrderRelationTagEnum.CURRENT_ACCEPT_COMPANY)) {
            return relationTags;
        }
        WorkOrderParticipant participant = getParticipant(workOrder.getId(), currentCompanyId);
        if (participant == null) {
            return relationTags;
        }
        // 总部参与关系保留独立标签，方便前端或后端单独识别总部只读视角。
        if (WorkOrderRelationTypeEnum.HQ_OBSERVER.getCode().equals(participant.getParticipateType())) {
            relationTags.add(WorkOrderRelationTagEnum.HQ_OBSERVER);
            return relationTags;
        }
        // 其余参与记录统一视为历史参与关系，当前默认只读。
        relationTags.add(WorkOrderRelationTagEnum.HISTORY_PARTICIPANT);
        return relationTags;
    }

    /**
     * 根据当前关系和工单状态返回前端可执行的操作编码。
     *
     * <p>该方法返回的是“这张工单当前真正允许做的动作”，统一复用 `canExecute`。</p>
     *
     * <p>这样做有两个目的：</p>
     * <p>1. 避免 `listAvailableActions`、`canTransfer`、`canClose` 等多个入口各写一套规则。</p>
     * <p>2. 支持一人多岗：当前维修员如果同时拥有转单权限，也可以在动作层被正确放行。</p>
     *
     * <p>返回值面向前端按钮层，使用动作枚举中的字符串编码。</p>
     *
     * @param workOrder 工单实体
     * @return 可执行操作列表
     */
    public List<String> listAvailableActions(WorkOrder workOrder) {
        return listAvailableActions(workOrder, resolveAccessContext());
    }

    /**
     * 分页查询Available动作s列表。
     *
     * @param workOrder 工单业务对象，用于判断状态、权限或组装返回数据。
     * @param context 上下文对象，承载当前操作人、公司和数据范围。
     * @return 业务处理结果
     */
    public List<String> listAvailableActions(WorkOrder workOrder, WorkOrderAccessContext context) {
        if (workOrder == null) {
            return Collections.emptyList();
        }
        List<String> actions = new ArrayList<>();
        for (WorkOrderActionEnum action : ACTION_ORDER) {
            if (canExecute(workOrder, action, context)) {
                addAction(actions, action);
            }
        }
        return actions;
    }

    /**
     * 返回列表只读原因文案。
     *
     * @param workOrder 工单实体
     * @return 只读原因；存在可执行动作时返回 null
     */
    public String getReadonlyReason(WorkOrder workOrder) {
        WorkOrderAccessContext context = resolveAccessContext();
        return getReadonlyReason(workOrder, listAvailableActions(workOrder, context), context);
    }

    /**
     * 返回列表只读原因文案。
     *
     * @param workOrder 工单实体
     * @param availableActions 已计算出的可执行动作
     * @return 只读原因；存在可执行动作时返回 null
     */
    public String getReadonlyReason(WorkOrder workOrder, List<String> availableActions) {
        return getReadonlyReason(workOrder, availableActions, resolveAccessContext());
    }

    /**
     * 获取只读原因。
     *
     * @param workOrder 工单业务对象，用于判断状态、权限或组装返回数据。
     * @param availableActions availableActions，当前业务处理所需的输入值。
     * @param context 上下文对象，承载当前操作人、公司和数据范围。
     * @return 业务处理结果
     */
    public String getReadonlyReason(WorkOrder workOrder, List<String> availableActions, WorkOrderAccessContext context) {
        if (workOrder == null) {
            return null;
        }
        if (availableActions != null && !availableActions.isEmpty()) {
            return null;
        }
        EnumSet<WorkOrderRelationTagEnum> relationTags = resolveRelationTags(workOrder, context);
        if (relationTags.contains(WorkOrderRelationTagEnum.HISTORY_PARTICIPANT)) {
            // 历史参与既包括真实转单后的转出方，也包括报修上级/总部后的发起方；
            // 这里用中性文案，避免未发生转单的工单被误解为“已转出”。
            return "当前非受理方，仅可查看";
        }
        if (relationTags.contains(WorkOrderRelationTagEnum.HQ_OBSERVER)) {
            return "当前由网点处理，仅可查看";
        }
        String mainStatus = workOrder.getMainStatus();
        if (WorkOrderStatusConstants.MainStatus.CLOSED.equals(mainStatus)) {
            return "当前工单已关闭，仅可查看";
        }
        if (relationTags.contains(WorkOrderRelationTagEnum.CURRENT_ACCEPT_COMPANY)) {
            Long currentUserId = context.getCurrentUserId();
            boolean assignedToOther = workOrder.getAssignedUserId() != null
                    && (currentUserId == null || !workOrder.getAssignedUserId().equals(currentUserId));
            if (assignedToOther
                    && (WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT.equals(mainStatus)
                    || WorkOrderStatusConstants.MainStatus.IN_PROGRESS.equals(mainStatus))) {
                return "当前由其他维修人员处理";
            }
            if (WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN.equals(mainStatus)) {
                return "当前待派单，请由负责人员处理";
            }
            if (WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT.equals(mainStatus)) {
                return "当前待接单，请由负责维修人员处理";
            }
            if (WorkOrderStatusConstants.MainStatus.IN_PROGRESS.equals(mainStatus)) {
                return "当前维修中，请由负责人员处理";
            }
            if (WorkOrderStatusConstants.MainStatus.COMPLETED.equals(mainStatus)) {
                return "当前工单已完成，待复检或关闭";
            }
        }
        return "当前仅可查看";
    }

    /**
     * 判断当前登录人是否允许在指定工单上执行某个动作。
     *
     * <p>这是工单动作授权的统一入口。每个动作都会同时叠加三类条件：</p>
     *
     * <p>1. 当前人先要能看见这张工单。</p>
     * <p>2. 当前工单的事实关系标签要满足动作要求，例如维修员动作要求 `ASSIGNEE`。</p>
     * <p>3. 动作对应的基础权限点、工单状态、服务模式等实例条件也要满足。</p>
     *
     * <p>这种写法可以天然支持一人多岗。比如同一个用户既是当前维修员，
     * 又拥有 `TRANSFER` 权限，那么只要他属于当前受理公司且状态允许，就可以转单，
     * 不会再因为单一展示身份而失去管理动作。</p>
     *
     * @param workOrder 工单实体
     * @param action    动作枚举
     * @return true 表示允许执行
     */
    public boolean canExecute(WorkOrder workOrder, WorkOrderActionEnum action) {
        return canExecute(workOrder, action, resolveAccessContext());
    }

    /**
     * canExecute。
     *
     * @param workOrder 工单业务对象，用于判断状态、权限或组装返回数据。
     * @param action action，当前业务处理所需的输入值。
     * @param context 上下文对象，承载当前操作人、公司和数据范围。
     */
    public boolean canExecute(WorkOrder workOrder, WorkOrderActionEnum action, WorkOrderAccessContext context) {
        if (workOrder == null || action == null) {
            return false;
        }
        if (!canView(workOrder, context)) {
            return false;
        }
        EnumSet<WorkOrderRelationTagEnum> relationTags = resolveRelationTags(workOrder, context);
        String mainStatus = workOrder.getMainStatus();
        boolean inCurrentAcceptCompany = relationTags.contains(WorkOrderRelationTagEnum.CURRENT_ACCEPT_COMPANY);
        boolean isAssignee = relationTags.contains(WorkOrderRelationTagEnum.ASSIGNEE);
        switch (action) {
            case ASSIGN:
                // 派单要求当前登录公司就是当前受理公司，且工单仍停留在待派单状态。
                return inCurrentAcceptCompany
                        && WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN.equals(mainStatus)
                        && hasActionPermission(action);
            case UPLOAD_SEND_EXPRESS:
                // 上传寄件单号本轮已从“当前受理方派单动作”收口为“建单人本人补资料动作”，
                // 因此不再依赖 currentAcceptCompanyId、workorder:assign、派单岗或调度岗。
                // 只要当前登录人能看到该工单列表，且经首条 CREATE 流转确认其就是建单人本人，
                // 即使工单出现在历史转出/只读列表，也应允许展示并执行该动作。
                return canCreatorUpdateSendExpress(workOrder, context, mainStatus);
            case TECH_ACCEPT:
                // 接单只属于当前被派到这张单上的维修员。
                return isAssignee
                        && WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT.equals(mainStatus)
                        && hasActionPermission(action);
            case REPAIR_FINISH:
                // 维修登记沿用“维修员 + 处理中 + repair 权限”约束。
                return isAssignee
                        && WorkOrderStatusConstants.MainStatus.IN_PROGRESS.equals(mainStatus)
                        && hasActionPermission(action);
            case TRANSFER:
                // 转单看的是“是否属于当前受理公司 + 是否有转单权限”，
                // 而不是“主展示身份是不是管理岗”，这样才能兼容一人多岗。
                return inCurrentAcceptCompany
                        && WorkOrderStatusConstants.canTransfer(mainStatus)
                        && hasActionPermission(action);
            case REVIEW:
                // 复检和关闭属于完成态管理动作；如果未来要增加“禁止自己复检自己”，
                // 应在这里直接追加实例级约束，而不是再回退到单值身份互斥。
                return inCurrentAcceptCompany
                        && WorkOrderStatusConstants.MainStatus.COMPLETED.equals(mainStatus)
                        && hasActionPermission(action);
            case RETURN_METHOD:
            case CLOSE:
                // 这里控制的是“独立关闭工单”动作：返回方式与关闭原因统一在完成态关闭弹窗里填写，
                // 因此仍然只允许 COMPLETED -> CLOSED。
                // 接单时选择“无故障”后的自动闭单，走的是 TECH_ACCEPT 服务内部流程，
                // 不通过独立 close 动作放行，否则会把待接单阶段错误暴露成单独的关闭按钮。
                return inCurrentAcceptCompany
                        && WorkOrderStatusConstants.MainStatus.COMPLETED.equals(mainStatus)
                        && hasActionPermission(action);
            default:
                return false;
        }
    }

    /**
     * 判断建单人本人是否允许补录寄件快递单号。
     *
     * <p>本方法承载本轮临时权限基线：B 端上传寄件单号不是派单、调度或当前承接方动作，
     * 而是报修发起人补充送修物流资料的动作。判断时只保留“工单可见、寄修服务方式、
     * 待接单状态窗口、当前用户等于临时识别出的建单人”四类业务条件。</p>
     *
     * @param workOrder 工单实体
     * @param context 当前访问上下文
     * @param mainStatus 工单主状态
     * @return true 表示允许补录
     */
    private boolean canCreatorUpdateSendExpress(WorkOrder workOrder, WorkOrderAccessContext context, String mainStatus) {
        if (workOrder == null || context == null || context.getCurrentUserId() == null) {
            return false;
        }
        // 寄件单号只服务于寄修单；到店维修没有送修物流补录语义，不能因为用户是建单人而放开。
        if (!ServiceModeEnum.isMail(workOrder.getServiceMode())) {
            return false;
        }
        // 状态窗口沿用“待接单”口径：PENDING_ASSIGN 表示尚未派给维修员，
        // PENDING_TECH_ACCEPT 表示已派单但维修员尚未接单，二者都仍处于送修信息补齐窗口。
        if (!WorkOrderStatusConstants.isWaitAcceptMainStatus(mainStatus)) {
            return false;
        }
        // 当前主表没有正式 createUserId 字段，本轮只能读取首条 CREATE 流转的 operatorUserId。
        // 若不回溯该字段，就会退回建单公司、当前受理公司或派单权限等旧口径，破坏“本人”边界。
        Long creatorUserId = resolveTemporaryCreatorUserId(workOrder.getId());
        // 旧数据查不到首条 CREATE，或 CREATE.operatorUserId 为空时，无法稳定确认建单人本人。
        // 本轮明确禁止为了兼容旧数据放宽为公司级或权限点级兜底，因此直接拒绝补录。
        if (creatorUserId == null) {
            return false;
        }
        // 历史转出/只读列表只表达该公司不是当前承接方，不代表建单人不能补资料；
        // canExecute 外层已经确认当前用户可见该工单，这里只校验“是否本人”。
        return creatorUserId.equals(context.getCurrentUserId());
    }

    /**
     * 临时解析工单建单人用户ID。
     *
     * <p>`work_order` 主表尚未落正式创建人字段时，首条 `CREATE` 流转是唯一能表达
     * “谁发起了建单动作”的稳定来源。本方法只作为本轮临时方案使用，后续正式字段落地后，
     * 应将读取来源替换为主表创建人字段。</p>
     *
     * @param workOrderId 工单ID
     * @return 建单人用户ID；旧数据缺失 CREATE 或 operatorUserId 为空时返回 null
     */
    protected Long resolveTemporaryCreatorUserId(Long workOrderId) {
        if (workOrderId == null || workOrderFlowMapper == null) {
            return null;
        }
        // 通过 Mapper 按 create_time、id 升序取首条 CREATE，避免重复流转数据导致建单人识别漂移。
        WorkOrderFlow firstCreateFlow = workOrderFlowMapper.selectFirstCreateFlow(workOrderId);
        if (firstCreateFlow == null) {
            return null;
        }
        return firstCreateFlow.getOperatorUserId();
    }

    /**
     * 判断当前登录人是否允许执行指派操作。
     *
     * @param workOrder 工单实体
     * @return true 表示允许指派
     */
    public boolean canAssign(WorkOrder workOrder) {
        return canExecute(workOrder, WorkOrderActionEnum.ASSIGN);
    }

    /**
     * 判断当前登录人是否允许执行技师接单操作。
     *
     * @param workOrder 工单实体
     * @return true 表示允许技师接单
     */
    public boolean canTechAccept(WorkOrder workOrder) {
        return canExecute(workOrder, WorkOrderActionEnum.TECH_ACCEPT);
    }

    /**
     * 判断当前登录人是否允许执行转派操作。
     *
     * @param workOrder 工单实体
     * @return true 表示允许转派
     */
    public boolean canTransfer(WorkOrder workOrder) {
        return canExecute(workOrder, WorkOrderActionEnum.TRANSFER);
    }

    /**
     * 判断当前登录人是否允许提交维修登记。
     *
     * @param workOrder 工单实体
     * @return true 表示允许提交维修登记
     */
    public boolean canSaveRepair(WorkOrder workOrder) {
        return canExecute(workOrder, WorkOrderActionEnum.REPAIR_FINISH);
    }

    /**
     * 判断当前登录人是否允许执行复检操作。
     *
     * @param workOrder 工单实体
     * @return true 表示允许复检
     */
    public boolean canReview(WorkOrder workOrder) {
        return canExecute(workOrder, WorkOrderActionEnum.REVIEW);
    }

    /**
     * 判断当前登录人是否允许上传寄修场景下的寄件快递单号。
     *
     * <p>本轮该动作只允许建单人本人在待接单窗口内补录，
     * 不再要求当前用户具备派单权限，也不再要求当前登录公司是工单当前受理方。</p>
     *
     * @param workOrder 工单实体
     * @return true 表示允许上传寄件快递单号
     */
    public boolean canUpdateSendExpress(WorkOrder workOrder) {
        return canExecute(workOrder, WorkOrderActionEnum.UPLOAD_SEND_EXPRESS);
    }

    /**
     * 判断当前登录人是否允许关闭工单。
     *
     * <p>这里判断的是“独立关闭工单”动作当前要求：</p>
     * <p>1. 当前用户能查看该工单。</p>
     * <p>2. 当前登录公司就是工单当前受理公司。</p>
     * <p>3. 工单主状态已进入已完成。</p>
     * <p>4. 当前账号具备 `workorder:close` 基础权限点。</p>
     *
     * <p>注意：维修员在接单时选择“无故障”后的自动闭单，不走本方法，
     * 而是由 `techAccept` 在接单事务内直接落 RETURN_METHOD/CLOSE 流转记录并收口为已关闭。</p>
     *
     * <p>关闭动作按可见性、当前受理公司、状态和基础权限点共同判断，
     * 支持一人多岗场景下的兼岗人员在满足条件时执行关闭。</p>
     *
     * @param workOrder 工单实体
     * @return true 表示允许关闭
     */
    public boolean canClose(WorkOrder workOrder) {
        return canExecute(workOrder, WorkOrderActionEnum.CLOSE);
    }

    /**
     * 向动作列表中追加动作编码。
     *
     * <p>对外接口使用字符串编码返回；内部统一从动作枚举取码，
     * 避免直接在业务逻辑中硬编码动作字符串。</p>
     *
     * @param actions 动作列表
     * @param action  动作枚举
     */
    private void addAction(List<String> actions, WorkOrderActionEnum action) {
        if (actions == null || action == null) {
            return;
        }
        actions.add(action.getCode());
    }

    /**
     * 获取参与者。
     *
     * @return 业务处理结果
     */
    private WorkOrderParticipant getParticipant(Long workOrderId, Long companyId) {
        LambdaQueryWrapper<WorkOrderParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderParticipant::getWorkOrderId, workOrderId)
                .eq(WorkOrderParticipant::getCompanyId, companyId);
        return workOrderParticipantMapper.selectOne(wrapper);
    }

    /**
     * requiresRelated公司Limit。
     *
     * @param context 上下文对象，承载当前操作人、公司和数据范围。
     */
    private boolean requiresRelatedCompanyLimit(WorkOrderAccessContext context) {
        return "HQ".equals(context.getSubjectType())
                && DataScopeEnum.REGION == context.getDataScopeEnum();
    }

    /**
     * matchRelated公司范围。
     *
     * @param workOrder 工单业务对象，用于判断状态、权限或组装返回数据。
     */
    private boolean matchRelatedCompanyScope(WorkOrder workOrder, List<Long> relatedCompanyIds) {
        if (workOrder == null || relatedCompanyIds == null || relatedCompanyIds.isEmpty()) {
            return false;
        }
        if (relatedCompanyIds.contains(workOrder.getCreateCompanyId())
                || relatedCompanyIds.contains(workOrder.getCurrentAcceptCompanyId())) {
            return true;
        }
        LambdaQueryWrapper<WorkOrderParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderParticipant::getWorkOrderId, workOrder.getId())
                .in(WorkOrderParticipant::getCompanyId, relatedCompanyIds);
        return workOrderParticipantMapper.selectCount(wrapper) > 0;
    }

    /**
     * 判断当前登录人是否具备某个动作对应的基础权限点。
     *
     * <p>该方法刻意只处理“基础权限点”判断，便于测试时通过覆写方式替换权限来源，
     * 而不用把整个 Sa-Token 权限链路搬进单元测试。</p>
     *
     * @param action 工单动作
     * @return true 表示拥有该动作对应的基础权限
     */
    protected boolean hasActionPermission(WorkOrderActionEnum action) {
        if (action == null) {
            return false;
        }
        return hasPermissionCode(action.getPermissionCode());
    }

    /**
     * 判断当前登录人是否具备指定权限点。
     *
     * <p>之所以单独抽这一层，是为了把权限来源和动作实例规则拆开：
     * 生产环境默认走 Sa-Token，测试环境则可以通过覆写该方法快速注入权限集合。</p>
     *
     * @param permissionCode 权限点编码
     * @return true 表示具备权限
     */
    protected boolean hasPermissionCode(String permissionCode) {
        if (permissionCode == null || permissionCode.trim().isEmpty()) {
            return true;
        }
        return StpUtil.hasPermission(permissionCode);
    }

    /**
     * 判断当前用户是否命中过该工单的用户级历史参与事实。
     *
     * @param workOrderId 工单ID
     * @param companyId 公司ID
     * @param userId 用户ID
     * @return true 表示存在用户级参与事实
     */
    protected boolean hasHistoryUserParticipation(Long workOrderId, Long companyId, Long userId) {
        return workOrderUserParticipantService != null
                && workOrderUserParticipantService.hasParticipation(workOrderId, companyId, userId);
    }

}



