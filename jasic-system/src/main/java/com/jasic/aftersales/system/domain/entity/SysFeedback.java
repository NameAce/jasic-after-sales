package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 平台反馈单实体。
 *
 * <p>该实体统一承载“投诉与建议”业务数据。
 * 本期反馈单保持独立业务语义，不挂为工单子表，也不扩展为多状态处理流。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_feedback")
public class SysFeedback extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交主体类型（CUSTOMER/SERVICE_COMPANY_USER） */
    private String submitterType;

    /** 提交人 ID；终端用户存客户 ID，网点用户存系统用户 ID */
    private Long submitterId;

    /** 提交人姓名快照 */
    private String submitterName;

    /** 提交网点 ID；仅网点用户提交时有值 */
    private Long submitCompanyId;

    /** 提交来源类型（CUSTOMER_WORK_ORDER/CUSTOMER_DIRECT/SERVICE_COMPANY） */
    private String submitSourceType;

    /** 提交来源名称快照 */
    private String submitSourceName;

    /** 联系电话快照 */
    private String contactPhone;

    /** 关联工单 ID，仅终端用户场景按规则尝试回填 */
    private Long relatedWorkOrderId;

    /** 归属总部 ID */
    private Long hqCompanyId;

    /** 反馈内容 */
    private String content;

    /** 状态（UNACCEPTED/ACCEPTED） */
    private String status;

    /** 受理人系统用户 ID */
    private Long acceptUserId;

    /** 受理人姓名快照 */
    private String acceptUserName;

    /** 受理时间 */
    private LocalDateTime acceptTime;
}
