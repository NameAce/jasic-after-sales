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
     * @param workOrder 工单主表
     */
    @Transactional(rollbackFor = Exception.class)
    public void initParticipants(WorkOrder workOrder) {
        if (workOrder == null || workOrder.getId() == null) {
            throw new ServiceException("工单不存在，不能初始化参与方");
        }
        LocalDateTime now = LocalDateTime.now();
        saveOrUpdateParticipant(workOrder.getId(), workOrder.getCreateCompanyId(),
                workOrder.getCurrentAcceptSubjectType(), "CREATE", 1, 0, now);
        if (workOrder.getHqCompanyId() != null && !workOrder.getHqCompanyId().equals(workOrder.getCreateCompanyId())) {
            saveOrUpdateParticipant(workOrder.getId(), workOrder.getHqCompanyId(),
                    "HQ", "HQ_OBSERVER", 0, 1, now);
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
        LocalDateTime now = LocalDateTime.now();
        if (fromCompanyId != null) {
            saveOrUpdateParticipant(workOrderId, fromCompanyId, fromSubjectType, "HISTORY", 0, 1, now);
        }
        saveOrUpdateParticipant(workOrderId, toCompanyId, toSubjectType, "CURRENT", 1, 0, now);
        clearOtherCurrentHandler(workOrderId, toCompanyId);
    }

    /**
     * 按工单ID查询参与方列表
     *
     * @param workOrderId 工单ID
     * @return 参与方列表
     */
    public List<WorkOrderParticipant> listByWorkOrderId(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderParticipant::getWorkOrderId, workOrderId)
                .orderByAsc(WorkOrderParticipant::getFirstParticipateTime);
        return workOrderParticipantMapper.selectList(wrapper);
    }

    private void saveOrUpdateParticipant(Long workOrderId, Long companyId, String subjectType,
                                         String participateType, Integer isCurrentHandler,
                                         Integer isReadonly, LocalDateTime now) {
        if (workOrderId == null || companyId == null) {
            return;
        }
        LambdaQueryWrapper<WorkOrderParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderParticipant::getWorkOrderId, workOrderId)
                .eq(WorkOrderParticipant::getCompanyId, companyId);
        WorkOrderParticipant participant = workOrderParticipantMapper.selectOne(wrapper);
        if (participant == null) {
            participant = new WorkOrderParticipant();
            participant.setWorkOrderId(workOrderId);
            participant.setCompanyId(companyId);
            participant.setFirstParticipateTime(now);
        }
        participant.setSubjectType(subjectType);
        participant.setParticipateType(participateType);
        participant.setIsCurrentHandler(isCurrentHandler);
        participant.setIsReadonly(isReadonly);
        participant.setLastParticipateTime(now);
        if (participant.getId() == null) {
            workOrderParticipantMapper.insert(participant);
        } else {
            workOrderParticipantMapper.updateById(participant);
        }
    }

    private void clearOtherCurrentHandler(Long workOrderId, Long targetCompanyId) {
        LambdaQueryWrapper<WorkOrderParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderParticipant::getWorkOrderId, workOrderId)
                .eq(WorkOrderParticipant::getIsCurrentHandler, 1);
        List<WorkOrderParticipant> participants = workOrderParticipantMapper.selectList(wrapper);
        for (WorkOrderParticipant participant : participants) {
            if (participant.getCompanyId().equals(targetCompanyId)) {
                continue;
            }
            participant.setIsCurrentHandler(0);
            participant.setIsReadonly(1);
            workOrderParticipantMapper.updateById(participant);
        }
    }
}
