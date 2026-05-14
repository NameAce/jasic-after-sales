package com.jasic.aftersales.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.enums.WorkOrderUserParticipationActionEnum;
import com.jasic.aftersales.system.domain.entity.WorkOrderUserParticipant;
import com.jasic.aftersales.system.mapper.WorkOrderUserParticipantMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 工单用户级参与事实服务
 *
 * @author Codex
 * @date 2026/04/14
 */
@Service
public class WorkOrderUserParticipantService {

    /**
     * ?? recordAction ?????
     *
     * @param workOrderId ??ID
     * @param companyId ??ID
     * @param userId ??ID
     * @param action ??
     * @param actionTime ??
     */
    @Resource
    private WorkOrderUserParticipantMapper workOrderUserParticipantMapper;

    /**
     * 记录一次用户级参与事实。
     *
     * @param workOrderId 工单ID
     * @param companyId 参与公司ID
     * @param userId 参与用户ID
     * @param action 动作类型
     * @param actionTime 动作发生时间
     */
    public void recordAction(Long workOrderId, Long companyId, Long userId,
                             WorkOrderUserParticipationActionEnum action, LocalDateTime actionTime) {
        if (workOrderId == null || companyId == null || userId == null || action == null) {
            return;
        }
        // ???????????????????????
        WorkOrderUserParticipant participant = new WorkOrderUserParticipant();
        participant.setWorkOrderId(workOrderId);
        participant.setCompanyId(companyId);
        participant.setUserId(userId);
        participant.setActionType(action.getCode());
        participant.setActionTime(actionTime == null ? LocalDateTime.now() : actionTime);
        workOrderUserParticipantMapper.insert(participant);
    }

    /**
     * 判断当前用户是否命中过指定工单的用户级参与事实。
     *
     * @param workOrderId 工单ID
     * @param companyId 公司ID
     * @param userId 用户ID
     * @return true 表示命中过参与事实
     */
    public boolean hasParticipation(Long workOrderId, Long companyId, Long userId) {
        if (workOrderId == null || companyId == null || userId == null) {
            return false;
        }
        // ???????????????????????
        LambdaQueryWrapper<WorkOrderUserParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderUserParticipant::getWorkOrderId, workOrderId)
                .eq(WorkOrderUserParticipant::getCompanyId, companyId)
                .eq(WorkOrderUserParticipant::getUserId, userId);
        return workOrderUserParticipantMapper.selectCount(wrapper) > 0;
    }
}
