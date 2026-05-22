package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单参与方快照实体
 *
 * @author Zoro
 * @date 2026/03/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_participant")
public class WorkOrderParticipant extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 参与公司ID */
    private Long companyId;

    /** 主体类型（SERVICE/HQ） */
    private String subjectType;

    /** 参与类型（CREATE/CURRENT/HISTORY/HQ_OBSERVER） */
    private String participateType;

    /** 是否当前受理方（1=是，0=否） */
    private Integer isCurrentHandler;

    /** 首次参与时间 */
    private LocalDateTime firstParticipateTime;

    /** 最后参与时间 */
    private LocalDateTime lastParticipateTime;
}
