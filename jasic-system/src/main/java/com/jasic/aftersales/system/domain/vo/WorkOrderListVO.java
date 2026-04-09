package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单列表视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单列表视图")
@Data
public class WorkOrderListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID")
    private Long id;

    /** 工单号 */
    @ApiModelProperty(value = "工单号")
    private String orderNo;

    /** 客户姓名 */
    @ApiModelProperty(value = "客户姓名")
    private String customerName;

    /** 客户手机号 */
    @ApiModelProperty(value = "客户手机号")
    private String customerMobile;

    /** 条码 */
    @ApiModelProperty(value = "条码")
    private String barcode;

    /** 机器型号 */
    @ApiModelProperty(value = "机器型号")
    private String productModel;

    /** 主状态 */
    @ApiModelProperty(value = "主状态", allowableValues = "PENDING_ASSIGN,PENDING_TECH_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String mainStatus;

    /** 主状态名称 */
    @ApiModelProperty(value = "主状态名称")
    private String mainStatusLabel;

    /** 展示状态 */
    @ApiModelProperty(value = "展示状态", allowableValues = "WAIT_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String displayStatus;

    /** 当前受理公司ID */
    @ApiModelProperty(value = "当前受理公司ID")
    private Long currentAcceptCompanyId;

    /** 当前受理公司名称 */
    @ApiModelProperty(value = "当前受理公司名称")
    private String currentAcceptCompanyName;

    /** 当前维修员ID */
    @ApiModelProperty(value = "当前维修员ID")
    private Long assignedUserId;

    /** 当前维修员姓名 */
    @ApiModelProperty(value = "当前维修员姓名")
    private String assignedUserName;

    /** 是否发生过转单 */
    @ApiModelProperty(value = "是否发生过转单")
    private Integer hasTransfer;

    /** 转单次数 */
    @ApiModelProperty(value = "转单次数")
    private Integer transferCount;

    /** 当前关系类型 */
    @ApiModelProperty(value = "当前关系类型", allowableValues = "PLATFORM_ADMIN,CURRENT_ASSIGNEE,CURRENT_OWNER_MANAGER,CURRENT_OWNER_MEMBER,HQ_OBSERVER,HISTORY_PARTICIPANT_READONLY,NONE")
    private String relationType;

    /** 是否只读 */
    @ApiModelProperty(value = "是否只读")
    private Integer isReadonly;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
