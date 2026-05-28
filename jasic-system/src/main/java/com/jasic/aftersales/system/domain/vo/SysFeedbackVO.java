package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 平台反馈单返回对象。
 *
 * <p>该对象统一用于终端用户、网点用户和总部后台的反馈列表与详情返回，
 * 直接返回提交时和受理时固化的快照字段，避免前端再去关联用户、公司或工单表拼装展示信息。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
@ApiModel(description = "平台反馈单返回对象")
@Data
public class SysFeedbackVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 反馈 ID */
    @ApiModelProperty(value = "反馈ID")
    private Long id;

    /** 提交主体类型 */
    @ApiModelProperty(value = "提交主体类型")
    private String submitterType;

    /** 提交人 ID */
    @ApiModelProperty(value = "提交人ID")
    private Long submitterId;

    /** 提交人姓名快照 */
    @ApiModelProperty(value = "提交人姓名快照")
    private String submitterName;

    /** 提交网点 ID */
    @ApiModelProperty(value = "提交网点ID")
    private Long submitCompanyId;

    /** 提交来源类型 */
    @ApiModelProperty(value = "提交来源类型")
    private String submitSourceType;

    /** 提交来源名称快照 */
    @ApiModelProperty(value = "提交来源名称快照")
    private String submitSourceName;

    /** 联系电话快照 */
    @ApiModelProperty(value = "联系电话快照")
    private String contactPhone;

    /** 关联工单 ID */
    @ApiModelProperty(value = "关联工单ID")
    private Long relatedWorkOrderId;

    /** 归属总部 ID */
    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    /** 反馈内容 */
    @ApiModelProperty(value = "反馈内容")
    private String content;

    /** 反馈状态 */
    @ApiModelProperty(value = "反馈状态")
    private String status;

    /** 受理人 ID */
    @ApiModelProperty(value = "受理人ID")
    private Long acceptUserId;

    /** 受理人姓名快照 */
    @ApiModelProperty(value = "受理人姓名快照")
    private String acceptUserName;

    /** 受理时间 */
    @ApiModelProperty(value = "受理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptTime;

    /** 受理回复 */
    @ApiModelProperty(value = "受理回复")
    private String acceptReply;

    /** 创建时间，同时作为提交时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
