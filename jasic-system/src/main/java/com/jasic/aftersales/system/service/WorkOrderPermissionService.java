package com.jasic.aftersales.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.enums.DataScopeEnum;
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
     * @param workOrder 工单实体
     * @return 关系类型编码
     */
    public String resolveRelationType(WorkOrder workOrder) {
        if (workOrder == null) {
            return "NONE";
        }
        if (SecurityContext.isPlatformUser()) {
            return "PLATFORM_ADMIN";
        }
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        Long currentUserId = SecurityContext.getCurrentUserId();
        if (currentCompanyId == null) {
            return "NONE";
        }
        if (currentCompanyId.equals(workOrder.getCurrentAcceptCompanyId())) {
            if (workOrder.getAssignedUserId() != null && workOrder.getAssignedUserId().equals(currentUserId)) {
                return "CURRENT_ASSIGNEE";
            }
            if (hasAnyPermission("workorder:assign", "workorder:transfer", "workorder:review", "workorder:close")) {
                return "CURRENT_OWNER_MANAGER";
            }
            return "CURRENT_OWNER_MEMBER";
        }
        WorkOrderParticipant participant = getParticipant(workOrder.getId(), currentCompanyId);
        if (participant == null) {
            return "NONE";
        }
        if ("HQ_OBSERVER".equals(participant.getParticipateType())) {
            return "HQ_OBSERVER";
        }
        return "HISTORY_PARTICIPANT_READONLY";
    }

    /**
     * 根据当前关系和工单状态返回前端可执行的操作编码。
     *
     * @param workOrder 工单实体
     * @return 可执行操作列表
     */
    public List<String> listAvailableActions(WorkOrder workOrder) {
        if (!canView(workOrder)) {
            return Collections.emptyList();
        }
        List<String> actions = new ArrayList<>();
        String relationType = resolveRelationType(workOrder);
        String mainStatus = workOrder.getMainStatus();
        if ("CURRENT_OWNER_MANAGER".equals(relationType)) {
            if (WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN.equals(mainStatus)
                    && StpUtil.hasPermission("workorder:assign")) {
                actions.add("ASSIGN");
            }
            if (canUpdateSendExpress(workOrder)) {
                actions.add("UPLOAD_SEND_EXPRESS");
            }
            if (WorkOrderStatusConstants.MainStatus.IN_PROGRESS.equals(mainStatus)
                    && StpUtil.hasPermission("workorder:transfer")) {
                actions.add("TRANSFER");
            }
            if (WorkOrderStatusConstants.MainStatus.COMPLETED.equals(mainStatus)) {
                if (StpUtil.hasPermission("workorder:review")) {
                    actions.add("REVIEW");
                }
                if (StpUtil.hasPermission("workorder:transfer")) {
                    actions.add("TRANSFER");
                }
                if (StpUtil.hasPermission("workorder:close")) {
                    actions.add("RETURN_METHOD");
                    actions.add("CLOSE");
                }
            }
        }
        if ("CURRENT_ASSIGNEE".equals(relationType)) {
            if (WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT.equals(mainStatus)
                    && StpUtil.hasPermission("workorder:accept")) {
                actions.add("TECH_ACCEPT");
            }
            if (WorkOrderStatusConstants.MainStatus.IN_PROGRESS.equals(mainStatus)) {
                if (StpUtil.hasPermission("workorder:quote")) {
                    actions.add("QUOTE");
                }
                if (StpUtil.hasPermission("workorder:repair")) {
                    actions.add("REPAIR_SAVE");
                    actions.add("REPAIR_FINISH");
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
        return "CURRENT_OWNER_MANAGER".equals(resolveRelationType(workOrder))
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
        return "CURRENT_ASSIGNEE".equals(resolveRelationType(workOrder))
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
        return "CURRENT_OWNER_MANAGER".equals(resolveRelationType(workOrder))
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
        return "CURRENT_ASSIGNEE".equals(resolveRelationType(workOrder))
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
        return "CURRENT_ASSIGNEE".equals(resolveRelationType(workOrder))
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
        return "CURRENT_OWNER_MANAGER".equals(resolveRelationType(workOrder))
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
        if (!"CURRENT_OWNER_MANAGER".equals(resolveRelationType(workOrder))) {
            return false;
        }
        if (!"\u5bc4\u4fee".equals(workOrder.getServiceMode())) {
            return false;
        }
        return WorkOrderStatusConstants.isWaitAcceptMainStatus(workOrder.getMainStatus())
                && StpUtil.hasPermission("workorder:assign");
    }

    public boolean canClose(WorkOrder workOrder) {
        return "CURRENT_OWNER_MANAGER".equals(resolveRelationType(workOrder))
                && WorkOrderStatusConstants.MainStatus.COMPLETED.equals(workOrder.getMainStatus())
                && StpUtil.hasPermission("workorder:close");
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
                && "FIRST".equals(SecurityContext.getCurrentTypeCode())
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
