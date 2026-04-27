package com.jasic.aftersales.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.enums.DataScopeEnum;
import com.jasic.aftersales.common.enums.ServiceModeEnum;
import com.jasic.aftersales.common.enums.WorkOrderActionEnum;
import com.jasic.aftersales.common.enums.WorkOrderRelationTagEnum;
import com.jasic.aftersales.common.enums.WorkOrderRelationTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderParticipant;
import com.jasic.aftersales.system.domain.query.WorkOrderQuery;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.WorkOrderParticipantMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单权限判断服务。
 *
 * @author Codex
 * @date 2026/03/26
 */
@Service
public class WorkOrderPermissionService {

    /**
     * 详情页动作返回顺序。
     *
     * <p>前端按钮通常希望按稳定顺序展示，因此这里集中定义返回顺序，
     * 避免动作判断逻辑改造后，按钮顺序随着代码分支顺序发生漂移。</p>
     */
    private static final List<WorkOrderActionEnum> DETAIL_ACTION_ORDER = Collections.unmodifiableList(Arrays.asList(
            WorkOrderActionEnum.ASSIGN,
            WorkOrderActionEnum.UPLOAD_SEND_EXPRESS,
            WorkOrderActionEnum.TECH_ACCEPT,
            WorkOrderActionEnum.TRANSFER,
            WorkOrderActionEnum.REPAIR_FINISH,
            WorkOrderActionEnum.REVIEW,
            WorkOrderActionEnum.RETURN_METHOD,
            WorkOrderActionEnum.CLOSE
    ));

    @Resource
    private WorkOrderParticipantMapper workOrderParticipantMapper;

    @Resource
    private HqFirstContractMapper hqFirstContractMapper;

    @Resource
    private FirstSecondRelationMapper firstSecondRelationMapper;

    @Resource
    private WorkOrderUserParticipantService workOrderUserParticipantService;

    /**
     * 判断当前登录人是否可以查看指定工单。
     *
     * @param workOrder 工单实体
     * @return true 表示允许查看
     */
    public boolean canView(WorkOrder workOrder) {
        if (workOrder == null) {
            return false;
        }
        if (SecurityContext.isPlatformUser()) {
            return true;
        }
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            return false;
        }
        if ("HQ".equals(SecurityContext.getCurrentSubjectType())
                && !currentCompanyId.equals(workOrder.getHqCompanyId())) {
            return false;
        }
        List<Long> relatedCompanyIds = resolveRelatedCompanyIds();
        boolean matchRelatedCompanyScope = !relatedCompanyIds.isEmpty() && matchRelatedCompanyScope(workOrder, relatedCompanyIds);
        boolean requiresRelatedCompanyLimit = requiresRelatedCompanyLimit();
        if (currentCompanyId.equals(workOrder.getCurrentAcceptCompanyId())) {
            if (DataScopeEnum.SELF == resolveCurrentDataScope()) {
                Long currentUserId = SecurityContext.getCurrentUserId();
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
        if (DataScopeEnum.SELF == resolveCurrentDataScope()) {
            return hasHistoryUserParticipation(workOrder.getId(), currentCompanyId, SecurityContext.getCurrentUserId());
        }
        return true;
    }

    /**
     * 补齐工单查询需要的数据范围字段，供列表和统计查询复用。
     *
     * @param query 工单查询参数
     */
    public void fillQueryScope(WorkOrderQuery query) {
        if (query == null) {
            return;
        }
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (!SecurityContext.isPlatformUser() || query.getCompanyId() == null) {
            query.setCompanyId(currentCompanyId);
        }
        query.setCurrentUserId(SecurityContext.getCurrentUserId());
        query.setSubjectType(SecurityContext.getCurrentSubjectType());
        query.setDataScope(resolveCurrentDataScope().getCode());
        query.setRelatedCompanyIds(resolveRelatedCompanyIds());
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
        EnumSet<WorkOrderRelationTagEnum> relationTags = EnumSet.noneOf(WorkOrderRelationTagEnum.class);
        if (workOrder == null) {
            return relationTags;
        }
        // 平台视角不依赖工单参与记录，直接保留平台管理员标签，供后续按需扩展特例。
        if (SecurityContext.isPlatformUser()) {
            relationTags.add(WorkOrderRelationTagEnum.PLATFORM_ADMIN);
            return relationTags;
        }
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        Long currentUserId = SecurityContext.getCurrentUserId();
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
     * 解析当前登录人与工单之间的关系类型。
     *
     * <p>这里解析的是“工单内身份”，不是纯菜单权限。
     * 但从本次重构开始，它主要承担“主身份展示”职责，不再作为动作放行的唯一依据。
     * 同一个用户在一张工单上可能同时具备多个关系标签和多个动作权限，
     * 因此真正的动作授权统一改由 `resolveRelationTags + canExecute` 负责。</p>
     *
     * <p>判定顺序如下：</p>
     * <p>1. 平台账号直接归类为 {@link WorkOrderRelationTypeEnum#PLATFORM_ADMIN}。</p>
     * <p>2. 当前公司等于工单当前受理公司时，再细分为当前维修员、当前受理方管理岗、当前受理方普通成员。</p>
     * <p>3. 如果不是当前受理公司，则根据参与方记录识别为总部观察者或历史参与方只读。</p>
     * <p>4. 全部不命中时返回 {@link WorkOrderRelationTypeEnum#NONE}。</p>
     *
     * <p>注意：因为 `relationType` 仍然是单值字段，一人多岗时只能选一个“主展示身份”。
     * 当前优先级仍然保持“当前维修员优先于管理岗”，这样可以保证已被派单的技师在前端看到的主身份
     * 仍然是“当前维修员”；如果需要展示更多兼岗信息，后续应直接补充返回关系标签集合。</p>
     *
     * @param workOrder 工单实体
     * @return 关系类型枚举
     */
    /*public WorkOrderRelationTypeEnum resolveRelationType(WorkOrder workOrder) {
        EnumSet<WorkOrderRelationTagEnum> relationTags = resolveRelationTags(workOrder);
        if (relationTags.isEmpty()) {
            return WorkOrderRelationTypeEnum.NONE;
        }
        if (relationTags.contains(WorkOrderRelationTagEnum.PLATFORM_ADMIN)) {
            return WorkOrderRelationTypeEnum.PLATFORM_ADMIN;
        }
        if (relationTags.contains(WorkOrderRelationTagEnum.CURRENT_ACCEPT_COMPANY)) {
            // 对外展示时，已被派到这张单上的维修员仍优先显示为当前维修员。
            if (relationTags.contains(WorkOrderRelationTagEnum.ASSIGNEE)) {
                return WorkOrderRelationTypeEnum.CURRENT_ASSIGNEE;
            }
            // “管理岗”在这里是展示语义：表示当前受理公司下具备任一管理动作基础权限的人员。
            // 真正能否执行动作，仍以 canExecute 中的实例级规则为准。
            if (hasAnyActionPermission(
                    WorkOrderActionEnum.ASSIGN,
                    WorkOrderActionEnum.TRANSFER,
                    WorkOrderActionEnum.REVIEW,
                    WorkOrderActionEnum.CLOSE
            )) {
                return WorkOrderRelationTypeEnum.CURRENT_OWNER_MANAGER;
            }
            return WorkOrderRelationTypeEnum.CURRENT_OWNER_MEMBER;
        }
        if (relationTags.contains(WorkOrderRelationTagEnum.HQ_OBSERVER)) {
            return WorkOrderRelationTypeEnum.HQ_OBSERVER;
        }
        if (relationTags.contains(WorkOrderRelationTagEnum.HISTORY_PARTICIPANT)) {
            return WorkOrderRelationTypeEnum.HISTORY_PARTICIPANT_READONLY;
        }
        return WorkOrderRelationTypeEnum.NONE;
    }*/

    /**
     * 根据当前关系和工单状态返回前端可执行的操作编码。
     *
     * <p>该方法返回的是“这张工单当前真正允许做的动作”。从本次重构开始，
     * 它不再直接依赖单一 `relationType` 做分支，而是统一复用 `canExecute`。</p>
     *
     * <p>这样做有两个目的：</p>
     * <p>1. 避免 `listAvailableActions`、`canTransfer`、`canClose` 等多个入口各写一套规则。</p>
     * <p>2. 支持一人多岗：当前维修员如果同时拥有转单权限，也可以在动作层被正确放行。</p>
     *
     * <p>返回值面向前端按钮层，仍然使用字符串编码，避免接口契约发生变化。</p>
     *
     * @param workOrder 工单实体
     * @return 可执行操作列表
     */
    public List<String> listAvailableActions(WorkOrder workOrder) {
        if (workOrder == null) {
            return Collections.emptyList();
        }
        List<String> actions = new ArrayList<>();
        for (WorkOrderActionEnum action : DETAIL_ACTION_ORDER) {
            if (canExecute(workOrder, action)) {
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
        return getReadonlyReason(workOrder, listAvailableActions(workOrder));
    }

    /**
     * 返回列表只读原因文案。
     *
     * @param workOrder 工单实体
     * @param availableActions 已计算出的可执行动作
     * @return 只读原因；存在可执行动作时返回 null
     */
    public String getReadonlyReason(WorkOrder workOrder, List<String> availableActions) {
        if (workOrder == null) {
            return null;
        }
        if (availableActions != null && !availableActions.isEmpty()) {
            return null;
        }
        EnumSet<WorkOrderRelationTagEnum> relationTags = resolveRelationTags(workOrder);
        if (relationTags.contains(WorkOrderRelationTagEnum.HISTORY_PARTICIPANT)) {
            return "已转出，当前仅可查看";
        }
        if (relationTags.contains(WorkOrderRelationTagEnum.HQ_OBSERVER)) {
            return "当前由网点处理，仅可查看";
        }
        String mainStatus = workOrder.getMainStatus();
        if (WorkOrderStatusConstants.MainStatus.CLOSED.equals(mainStatus)) {
            return "当前工单已关闭，仅可查看";
        }
        if (relationTags.contains(WorkOrderRelationTagEnum.CURRENT_ACCEPT_COMPANY)) {
            Long currentUserId = SecurityContext.getCurrentUserId();
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
     * <p>这是工单动作授权的统一入口，专门用来替代过去依赖单一 `relationType`
     * 做分支的写法。每个动作都会同时叠加三类条件：</p>
     *
     * <p>1. 当前人先要能看见这张工单。</p>
     * <p>2. 当前工单的事实关系标签要满足动作要求，例如维修员动作要求 `ASSIGNEE`。</p>
     * <p>3. 动作对应的基础权限点、工单状态、服务模式等实例条件也要满足。</p>
     *
     * <p>这种写法可以天然支持一人多岗。比如同一个用户既是当前维修员，
     * 又拥有 `TRANSFER` 权限，那么只要他属于当前受理公司且状态允许，就可以转单，
     * 不会再因为主展示身份被压成 `CURRENT_ASSIGNEE` 而失去管理动作。</p>
     *
     * @param workOrder 工单实体
     * @param action    动作枚举
     * @return true 表示允许执行
     */
    public boolean canExecute(WorkOrder workOrder, WorkOrderActionEnum action) {
        if (workOrder == null || action == null) {
            return false;
        }
        if (!canView(workOrder)) {
            return false;
        }
        EnumSet<WorkOrderRelationTagEnum> relationTags = resolveRelationTags(workOrder);
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
                // 上传寄件单号是寄修单在待受理阶段的补充物流动作，沿用派单权限点控制。
                return inCurrentAcceptCompany
                        && ServiceModeEnum.isMail(workOrder.getServiceMode())
                        && WorkOrderStatusConstants.isWaitAcceptMainStatus(mainStatus)
                        && hasActionPermission(action);
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
     * <p>这里不再硬性要求单值 `relationType` 必须等于“管理岗”，
     * 从而允许一人多岗场景下的兼岗人员在满足条件时执行关闭。</p>
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
     * <p>对外接口仍使用字符串编码返回，便于兼容现有前端；内部则统一从动作枚举取码，
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

    private WorkOrderParticipant getParticipant(Long workOrderId, Long companyId) {
        LambdaQueryWrapper<WorkOrderParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderParticipant::getWorkOrderId, workOrderId)
                .eq(WorkOrderParticipant::getCompanyId, companyId);
        return workOrderParticipantMapper.selectOne(wrapper);
    }

    private DataScopeEnum resolveCurrentDataScope() {
        return DataScopeEnum.normalize(SecurityContext.getEffectiveDataScope(), SecurityContext.getCurrentSubjectType());
    }

    private boolean requiresRelatedCompanyLimit() {
        return "HQ".equals(SecurityContext.getCurrentSubjectType())
                && DataScopeEnum.REGION == resolveCurrentDataScope();
    }

    private List<Long> resolveRelatedCompanyIds() {
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            return Collections.emptyList();
        }
        if (requiresRelatedCompanyLimit()) {
            return resolveRegionCompanyIds(currentCompanyId, SecurityContext.getCurrentRegionIds());
        }
        if ("SERVICE".equals(SecurityContext.getCurrentSubjectType())
                && "SITE_FIRST".equals(SecurityContext.getCurrentTypeCode())
                && DataScopeEnum.ALL == resolveCurrentDataScope()) {
            return resolveFirstLevelCompanyScope(currentCompanyId);
        }
        return Collections.emptyList();
    }

    private List<Long> resolveRegionCompanyIds(Long currentCompanyId, List<Long> currentRegionIds) {
        if (currentRegionIds == null || currentRegionIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<HqFirstContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(HqFirstContract::getHqCompanyId, currentCompanyId)
                .eq(HqFirstContract::getStatus, 1)
                .in(HqFirstContract::getRegionId, currentRegionIds);
        List<HqFirstContract> contracts = hqFirstContractMapper.selectList(contractWrapper);
        if (contracts.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> relatedCompanyIds = contracts.stream()
                .map(HqFirstContract::getFirstCompanyId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return appendSecondLevelCompanies(relatedCompanyIds);
    }

    private List<Long> resolveFirstLevelCompanyScope(Long currentCompanyId) {
        Set<Long> relatedCompanyIds = new LinkedHashSet<>();
        relatedCompanyIds.add(currentCompanyId);
        return appendSecondLevelCompanies(relatedCompanyIds);
    }

    private List<Long> appendSecondLevelCompanies(Set<Long> relatedCompanyIds) {
        if (relatedCompanyIds == null || relatedCompanyIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FirstSecondRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(FirstSecondRelation::getStatus, 1)
                .in(FirstSecondRelation::getFirstCompanyId, relatedCompanyIds);
        List<FirstSecondRelation> relations = firstSecondRelationMapper.selectList(relationWrapper);
        for (FirstSecondRelation relation : relations) {
            if (relation.getSecondCompanyId() != null) {
                relatedCompanyIds.add(relation.getSecondCompanyId());
            }
        }
        return new ArrayList<>(relatedCompanyIds);
    }

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

    /**
     * 判断当前登录人是否具备任一动作对应的基础权限点。
     *
     * <p>该方法当前只用于推导单值 `relationType` 的展示身份，不参与真实动作放行。
     * 这样即便未来管理动作继续扩展，也只需要在这里或动作枚举中集中维护。</p>
     *
     * @param actions 动作列表
     * @return true 表示命中任一动作权限
     */
    private boolean hasAnyActionPermission(WorkOrderActionEnum... actions) {
        for (WorkOrderActionEnum action : actions) {
            if (hasActionPermission(action)) {
                return true;
            }
        }
        return false;
    }
}
