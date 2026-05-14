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
     * 工单用户参与者数据访问接口。
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
        // 说明：执行该步骤以保证业务流程正确。
        WorkOrderUserParticipant participant = new WorkOrderUserParticipant();
        // 调用setWorkOrderId方法，复用统一能力并保证业务规则一致。
        participant.setWorkOrderId(workOrderId);
        // 调用setCompanyId方法，复用统一能力并保证业务规则一致。
        participant.setCompanyId(companyId);
        // 调用setUserId方法，复用统一能力并保证业务规则一致。
        participant.setUserId(userId);
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        participant.setActionType(action.getCode());
        // 调用now方法，复用统一能力并保证业务规则一致。
        participant.setActionTime(actionTime == null ? LocalDateTime.now() : actionTime);
        // 调用insert方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<WorkOrderUserParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderUserParticipant::getWorkOrderId, workOrderId)
                .eq(WorkOrderUserParticipant::getCompanyId, companyId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(WorkOrderUserParticipant::getUserId, userId);
        return workOrderUserParticipantMapper.selectCount(wrapper) > 0;
    }
}




