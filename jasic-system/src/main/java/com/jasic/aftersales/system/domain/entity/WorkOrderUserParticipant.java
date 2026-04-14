package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单用户级参与事实实体
 *
 * @author Codex
 * @date 2026/04/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_user_participant")
public class WorkOrderUserParticipant extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 参与公司ID */
    private Long companyId;

    /** 参与用户ID */
    private Long userId;

    /** 动作类型 */
    private String actionType;

    /** 动作发生时间 */
    private LocalDateTime actionTime;
}
