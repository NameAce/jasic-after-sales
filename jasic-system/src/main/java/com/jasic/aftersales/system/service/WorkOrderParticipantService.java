package com.jasic.aftersales.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderParticipant;
import com.jasic.aftersales.system.mapper.WorkOrderParticipantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单参与方维护服务
 *
 * @author Codex
 * @date 2026/03/26
 */
@Service
public class WorkOrderParticipantService {

    @Resource
    private WorkOrderParticipantMapper workOrderParticipantMapper;

    /**
     * 建单时初始化参与方快照
     *
     * @param workOrder          工单主表
     * @param createSubjectType  建单公司主体类型
     */
    @Transactional(rollbackFor = Exception.class)
    public void initParticipants(WorkOrder workOrder, String createSubjectType) {
        if (workOrder == null || workOrder.getId() == null) {
            throw new ServiceException("工单不存在，不能初始化参与方");
        }
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime now = LocalDateTime.now();
        boolean sameHandlerCompany = workOrder.getCreateCompanyId() != null
                // 调用getCurrentAcceptCompanyId方法，复用统一能力并保证业务规则一致。
                && workOrder.getCreateCompanyId().equals(workOrder.getCurrentAcceptCompanyId());
        // 说明：执行该步骤以保证业务流程正确。
        saveOrUpdateParticipant(workOrder.getId(), workOrder.getCreateCompanyId(),
                createSubjectType, "CREATE", sameHandlerCompany ? 1 : 0, now);
        if (!sameHandlerCompany) {
            saveOrUpdateParticipant(workOrder.getId(), workOrder.getCurrentAcceptCompanyId(),
                    // 调用getCurrentAcceptSubjectType方法，复用统一能力并保证业务规则一致。
                    workOrder.getCurrentAcceptSubjectType(), "CURRENT", 1, now);
        }
        if (workOrder.getHqCompanyId() != null
                && !workOrder.getHqCompanyId().equals(workOrder.getCreateCompanyId())
                && !workOrder.getHqCompanyId().equals(workOrder.getCurrentAcceptCompanyId())) {
            saveOrUpdateParticipant(workOrder.getId(), workOrder.getHqCompanyId(),
                    "HQ", "HQ_OBSERVER", 0, now);
        }
    }

    /**
     * 转单时刷新参与方快照
     *
     * @param workOrderId     工单ID
     * @param fromCompanyId   来源公司ID
     * @param fromSubjectType 来源主体类型
     * @param toCompanyId     目标公司ID
     * @param toSubjectType   目标主体类型
     */
    @Transactional(rollbackFor = Exception.class)
    public void transferParticipant(Long workOrderId, Long fromCompanyId, String fromSubjectType,
                                    Long toCompanyId, String toSubjectType) {
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime now = LocalDateTime.now();
        if (fromCompanyId != null) {
            // 说明：执行该步骤以保证业务流程正确。
            saveOrUpdateParticipant(workOrderId, fromCompanyId, fromSubjectType, "HISTORY", 0, now);
        }
        // 调用saveOrUpdateParticipant方法，复用统一能力并保证业务规则一致。
        saveOrUpdateParticipant(workOrderId, toCompanyId, toSubjectType, "CURRENT", 1, now);
        // 调用clearOtherCurrentHandler方法，复用统一能力并保证业务规则一致。
        clearOtherCurrentHandler(workOrderId, toCompanyId);
    }

    /**
     * 按工单ID查询参与方列表
     *
     * @param workOrderId 工单ID
     * @return 参与方列表
     */
    public List<WorkOrderParticipant> listByWorkOrderId(Long workOrderId) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<WorkOrderParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderParticipant::getWorkOrderId, workOrderId)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(WorkOrderParticipant::getFirstParticipateTime);
        return workOrderParticipantMapper.selectList(wrapper);
    }

    /**
     * 新增Or更新参与者。
     *
     * @param subjectType 参数
     * @param participateType 参数
     * @param isCurrentHandler 参数
     * @param now 参数
     */
    private void saveOrUpdateParticipant(Long workOrderId, Long companyId, String subjectType,
                                         String participateType, Integer isCurrentHandler, LocalDateTime now) {
        if (workOrderId == null || companyId == null) {
            return;
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<WorkOrderParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderParticipant::getWorkOrderId, workOrderId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(WorkOrderParticipant::getCompanyId, companyId);
        // 调用selectOne方法，复用统一能力并保证业务规则一致。
        WorkOrderParticipant participant = workOrderParticipantMapper.selectOne(wrapper);
        if (participant == null) {
            // 调用WorkOrderParticipant方法，复用统一能力并保证业务规则一致。
            participant = new WorkOrderParticipant();
            // 调用setWorkOrderId方法，复用统一能力并保证业务规则一致。
            participant.setWorkOrderId(workOrderId);
            // 调用setCompanyId方法，复用统一能力并保证业务规则一致。
            participant.setCompanyId(companyId);
            // 调用setFirstParticipateTime方法，复用统一能力并保证业务规则一致。
            participant.setFirstParticipateTime(now);
        }
        // 调用setSubjectType方法，复用统一能力并保证业务规则一致。
        participant.setSubjectType(subjectType);
        // 调用setParticipateType方法，复用统一能力并保证业务规则一致。
        participant.setParticipateType(participateType);
        // 调用setIsCurrentHandler方法，复用统一能力并保证业务规则一致。
        participant.setIsCurrentHandler(isCurrentHandler);
        // 调用setLastParticipateTime方法，复用统一能力并保证业务规则一致。
        participant.setLastParticipateTime(now);
        if (participant.getId() == null) {
            // 调用insert方法，复用统一能力并保证业务规则一致。
            workOrderParticipantMapper.insert(participant);
        } else {
            // 调用updateById方法，复用统一能力并保证业务规则一致。
            workOrderParticipantMapper.updateById(participant);
        }
    }

    /**
     * clearOtherCurrent处理。
     */
    private void clearOtherCurrentHandler(Long workOrderId, Long targetCompanyId) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<WorkOrderParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderParticipant::getWorkOrderId, workOrderId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(WorkOrderParticipant::getIsCurrentHandler, 1);
        // 调用selectList方法，复用统一能力并保证业务规则一致。
        List<WorkOrderParticipant> participants = workOrderParticipantMapper.selectList(wrapper);
        for (WorkOrderParticipant participant : participants) {
            if (participant.getCompanyId().equals(targetCompanyId)) {
                continue;
            }
            // 调用setIsCurrentHandler方法，复用统一能力并保证业务规则一致。
            participant.setIsCurrentHandler(0);
            // 调用updateById方法，复用统一能力并保证业务规则一致。
            workOrderParticipantMapper.updateById(participant);
        }
    }
}


