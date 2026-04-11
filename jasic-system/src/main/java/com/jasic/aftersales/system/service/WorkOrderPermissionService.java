package com.jasic.aftersales.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.enums.DataScopeEnum;
import com.jasic.aftersales.common.enums.ServiceModeEnum;
import com.jasic.aftersales.common.enums.WorkOrderActionEnum;
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
import java.util.Collections;
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

    @Resource
    private WorkOrderParticipantMapper workOrderParticipantMapper;

    @Resource
    private HqFirstContractMapper hqFirstContractMapper;

    @Resource
    private FirstSecondRelationMapper firstSecondRelationMapper;

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
        if (DataScopeEnum.SELF == resolveCurrentDataScope()) {
            Long currentUserId = SecurityContext.getCurrentUserId();
            if (currentUserId == null || !currentUserId.equals(workOrder.getAssignedUserId())) {
                return false;
            }
        }
        if (currentCompanyId.equals(workOrder.getCurrentAcceptCompanyId())) {
            return !requiresRelatedCompanyLimit || matchRelatedCompanyScope;
        }
        WorkOrderParticipant participant = getParticipant(workOrder.getId(), currentCompanyId);
        if (participant != null) {
            return !requiresRelatedCompanyLimit || matchRelatedCompanyScope;
        }
        return matchRelatedCompanyScope;
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
     * 解析当前登录人与工单之间的关系类型。
     *
     * <p>这里解析的是“工单内身份”，不是纯菜单权限。
     * 同一个用户即使具备 `workorder:transfer`、`workorder:quote` 等权限点，
     * 也只有在当前工单里处于对应业务角色时，才允许真正执行动作。</p>
     *
     * <p>判定顺序如下：</p>
     * <p>1. 平台账号直接归类为 {@link WorkOrderRelationTypeEnum#PLATFORM_ADMIN}。</p>
     * <p>2. 当前公司等于工单当前受理公司时，再细分为当前维修员、当前受理方管理岗、当前受理方普通成员。</p>
     * <p>3. 如果不是当前受理公司，则根据参与方记录识别为总部观察者或历史参与方只读。</p>
     * <p>4. 全部不命中时返回 {@link WorkOrderRelationTypeEnum#NONE}。</p>
     *
     * @param workOrder 工单实体
     * @return 关系类型枚举
     */
    public WorkOrderRelationTypeEnum resolveRelationType(WorkOrder workOrder) {
        if (workOrder == null) {
            return WorkOrderRelationTypeEnum.NONE;
        }
        // 平台账号不依赖工单参与关系，直接视为平台管理员视角。
        if (SecurityContext.isPlatformUser()) {
            return WorkOrderRelationTypeEnum.PLATFORM_ADMIN;
        }
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        Long currentUserId = SecurityContext.getCurrentUserId();
        if (currentCompanyId == null) {
            return WorkOrderRelationTypeEnum.NONE;
        }
        if (currentCompanyId.equals(workOrder.getCurrentAcceptCompanyId())) {
            // 当前公司就是当前受理公司时，先判断是否就是被派到这张单上的维修员。
            if (workOrder.getAssignedUserId() != null && workOrder.getAssignedUserId().equals(currentUserId)) {
                return WorkOrderRelationTypeEnum.CURRENT_ASSIGNEE;
            }
            // 不是维修员但具备派单/转单/复检/关闭等管理能力时，归类为当前受理方管理岗。
            if (hasAnyPermission("workorder:assign", "workorder:transfer", "workorder:review", "workorder:close")) {
                return WorkOrderRelationTypeEnum.CURRENT_OWNER_MANAGER;
            }
            // 同属当前受理公司、但既不是当前维修员也不是管理岗时，视为当前受理方普通成员。
            return WorkOrderRelationTypeEnum.CURRENT_OWNER_MEMBER;
        }
        // 不是当前受理公司时，只能再从参与记录里判断是总部观察者还是历史参与方。
        WorkOrderParticipant participant = getParticipant(workOrder.getId(), currentCompanyId);
        if (participant == null) {
            return WorkOrderRelationTypeEnum.NONE;
        }
        // 总部参与记录单独保留 HQ_OBSERVER，前端会据此标记总部只读身份。
        if (WorkOrderRelationTypeEnum.HQ_OBSERVER.getCode().equals(participant.getParticipateType())) {
            return WorkOrderRelationTypeEnum.HQ_OBSERVER;
        }
        // 其余有参与记录但不是当前受理方的，统一视为历史参与方只读。
        return WorkOrderRelationTypeEnum.HISTORY_PARTICIPANT_READONLY;
    }

    /**
     * 根据当前关系和工单状态返回前端可执行的操作编码。
     *
     * <p>该方法返回的是“这张工单当前真正允许做的动作”，因此会同时叠加三层条件：</p>
     * <p>1. 当前用户先要有查看该工单的资格。</p>
     * <p>2. 当前用户在该工单中的关系类型要匹配动作归属，例如管理岗和维修员的动作集合不同。</p>
     * <p>3. 工单主状态和权限点都要满足，例如待派单才允许 `ASSIGN`，处理中才允许 `QUOTE`。</p>
     *
     * <p>返回值面向前端按钮层，仍然使用字符串编码，避免接口契约发生变化。</p>
     *
     * @param workOrder 工单实体
     * @return 可执行操作列表
     */
    public List<String> listAvailableActions(WorkOrder workOrder) {
        if (!canView(workOrder)) {
            return Collections.emptyList();
        }
        List<String> actions = new ArrayList<>();
        WorkOrderRelationTypeEnum relationType = resolveRelationType(workOrder);
        String mainStatus = workOrder.getMainStatus();
        if (WorkOrderRelationTypeEnum.CURRENT_OWNER_MANAGER == relationType) {
            // 管理岗负责派单，因此只有待派单时才可能看到 ASSIGN。
            if (WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN.equals(mainStatus)
                    && StpUtil.hasPermission("workorder:assign")) {
                addAction(actions, WorkOrderActionEnum.ASSIGN);
            }
            // 寄修单在待受理阶段需要由管理岗补充寄件单号。
            if (canUpdateSendExpress(workOrder)) {
                addAction(actions, WorkOrderActionEnum.UPLOAD_SEND_EXPRESS);
            }
            // 转单属于当前受理方的管理动作，维修员和历史参与方都不能做。
            if (WorkOrderStatusConstants.MainStatus.IN_PROGRESS.equals(mainStatus)
                    && StpUtil.hasPermission("workorder:transfer")) {
                addAction(actions, WorkOrderActionEnum.TRANSFER);
            }
            if (WorkOrderStatusConstants.MainStatus.COMPLETED.equals(mainStatus)) {
                // 已完成后，管理岗根据权限决定是否复检、转单或关闭。
                if (StpUtil.hasPermission("workorder:review")) {
                    addAction(actions, WorkOrderActionEnum.REVIEW);
                }
                if (StpUtil.hasPermission("workorder:transfer")) {
                    addAction(actions, WorkOrderActionEnum.TRANSFER);
                }
                if (StpUtil.hasPermission("workorder:close")) {
                    addAction(actions, WorkOrderActionEnum.RETURN_METHOD);
                    addAction(actions, WorkOrderActionEnum.CLOSE);
                }
            }
        }
        if (WorkOrderRelationTypeEnum.CURRENT_ASSIGNEE == relationType) {
            // 接单动作只属于当前被派到这张单上的维修员。
            if (WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT.equals(mainStatus)
                    && StpUtil.hasPermission("workorder:accept")) {
                addAction(actions, WorkOrderActionEnum.TECH_ACCEPT);
            }
            if (WorkOrderStatusConstants.MainStatus.IN_PROGRESS.equals(mainStatus)) {
                // 处理中阶段的报价和维修登记由当前维修员承担。
                if (StpUtil.hasPermission("workorder:quote")) {
                    addAction(actions, WorkOrderActionEnum.QUOTE);
                }
                if (StpUtil.hasPermission("workorder:repair")) {
                    addAction(actions, WorkOrderActionEnum.REPAIR_SAVE);
                    addAction(actions, WorkOrderActionEnum.REPAIR_FINISH);
                }
            }
        }
        return actions;
    }

    /**
     * 判断当前登录人是否允许执行指派操作。
     *
     * @param workOrder 工单实体
     * @return true 表示允许指派
     */
    public boolean canAssign(WorkOrder workOrder) {
        return WorkOrderRelationTypeEnum.CURRENT_OWNER_MANAGER == resolveRelationType(workOrder)
                && WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN.equals(workOrder.getMainStatus())
                && StpUtil.hasPermission("workorder:assign");
    }

    /**
     * 判断当前登录人是否允许执行技师接单操作。
     *
     * @param workOrder 工单实体
     * @return true 表示允许技师接单
     */
    public boolean canTechAccept(WorkOrder workOrder) {
        return WorkOrderRelationTypeEnum.CURRENT_ASSIGNEE == resolveRelationType(workOrder)
                && WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT.equals(workOrder.getMainStatus())
                && StpUtil.hasPermission("workorder:accept");
    }

    /**
     * 判断当前登录人是否允许执行转派操作。
     *
     * @param workOrder 工单实体
     * @return true 表示允许转派
     */
    public boolean canTransfer(WorkOrder workOrder) {
        return WorkOrderRelationTypeEnum.CURRENT_OWNER_MANAGER == resolveRelationType(workOrder)
                && WorkOrderStatusConstants.canTransfer(workOrder.getMainStatus())
                && StpUtil.hasPermission("workorder:transfer");
    }

    /**
     * 判断当前登录人是否允许执行报价操作。
     *
     * @param workOrder 工单实体
     * @return true 表示允许报价
     */
    public boolean canQuote(WorkOrder workOrder) {
        return WorkOrderRelationTypeEnum.CURRENT_ASSIGNEE == resolveRelationType(workOrder)
                && WorkOrderStatusConstants.MainStatus.IN_PROGRESS.equals(workOrder.getMainStatus())
                && StpUtil.hasPermission("workorder:quote");
    }

    /**
     * 判断当前登录人是否允许保存维修记录。
     *
     * @param workOrder 工单实体
     * @return true 表示允许保存维修记录
     */
    public boolean canSaveRepair(WorkOrder workOrder) {
        return WorkOrderRelationTypeEnum.CURRENT_ASSIGNEE == resolveRelationType(workOrder)
                && WorkOrderStatusConstants.MainStatus.IN_PROGRESS.equals(workOrder.getMainStatus())
                && StpUtil.hasPermission("workorder:repair");
    }

    /**
     * 判断当前登录人是否允许执行复检操作。
     *
     * @param workOrder 工单实体
     * @return true 表示允许复检
     */
    public boolean canReview(WorkOrder workOrder) {
        return WorkOrderRelationTypeEnum.CURRENT_OWNER_MANAGER == resolveRelationType(workOrder)
                && WorkOrderStatusConstants.MainStatus.COMPLETED.equals(workOrder.getMainStatus())
                && StpUtil.hasPermission("workorder:review");
    }

    /**
     * 判断当前登录人是否允许上传寄修场景下的寄件快递单号。
     *
     * @param workOrder 工单实体
     * @return true 表示允许上传寄件快递单号
     */
    public boolean canUpdateSendExpress(WorkOrder workOrder) {
        if (WorkOrderRelationTypeEnum.CURRENT_OWNER_MANAGER != resolveRelationType(workOrder)) {
            return false;
        }
        if (!ServiceModeEnum.isMail(workOrder.getServiceMode())) {
            return false;
        }
        return WorkOrderStatusConstants.isWaitAcceptMainStatus(workOrder.getMainStatus())
                && StpUtil.hasPermission("workorder:assign");
    }

    /**
     * 判断当前登录人是否允许关闭工单。
     *
     * <p>仅当前受理公司的管理岗，且工单已进入已完成状态时，才允许执行关闭。
     * 关闭动作通常还会伴随返还方式、返件单号、客户评价邀请等后续流程。</p>
     *
     * @param workOrder 工单实体
     * @return true 表示允许关闭
     */
    public boolean canClose(WorkOrder workOrder) {
        return WorkOrderRelationTypeEnum.CURRENT_OWNER_MANAGER == resolveRelationType(workOrder)
                && WorkOrderStatusConstants.MainStatus.COMPLETED.equals(workOrder.getMainStatus())
                && StpUtil.hasPermission("workorder:close");
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

    private boolean hasAnyPermission(String... perms) {
        for (String perm : perms) {
            if (StpUtil.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }
}
