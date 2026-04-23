package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jasic.aftersales.common.enums.BrandTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @ApiModelProperty(value = "工单ID")
    private Long id;

    @ApiModelProperty(value = "工单号")
    private String orderNo;

    @ApiModelProperty(value = "客户姓名")
    private String customerName;

    @ApiModelProperty(value = "客户手机号")
    private String customerMobile;

    @ApiModelProperty(value = "条码")
    private String barcode;

    @ApiModelProperty(value = "品牌类型", allowableValues = "JASIC,NON_JASIC")
    private BrandTypeEnum brandType;

    @ApiModelProperty(value = "品牌类型名称")
    private String brandTypeLabel;

    @ApiModelProperty(value = "服务方式编码", allowableValues = "MAIL,STORE")
    private String serviceMode;

    @ApiModelProperty(value = "服务方式名称")
    private String serviceModeLabel;

    @ApiModelProperty(value = "最后出库日期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastOutDate;

    @ApiModelProperty(value = "Warranty status", allowableValues = "IN_WARRANTY,OUT_OF_WARRANTY")
    private String warrantyStatus;

    @ApiModelProperty(value = "机器型号")
    private String productModel;

    @ApiModelProperty(value = "故障描述")
    private String faultDesc;

    @ApiModelProperty(value = "主状态", allowableValues = "PENDING_ASSIGN,PENDING_TECH_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String mainStatus;

    @ApiModelProperty(value = "主状态名称")
    private String mainStatusLabel;

    @ApiModelProperty(value = "展示状态", allowableValues = "WAIT_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String displayStatus;

    @ApiModelProperty(value = "当前受理公司ID")
    private Long currentAcceptCompanyId;

    @ApiModelProperty(value = "当前受理公司名称")
    private String currentAcceptCompanyName;

    @ApiModelProperty(value = "当前受理网点电话")
    private String currentAcceptCompanyPhone;

    @ApiModelProperty(value = "当前维修员ID")
    private Long assignedUserId;

    @ApiModelProperty(value = "当前维修员姓名")
    private String assignedUserName;

    @ApiModelProperty(value = "是否发生过转单")
    private Integer hasTransfer;

    @ApiModelProperty(value = "转单次数")
    private Integer transferCount;

    @ApiModelProperty(value = "当前有效报价金额")
    private BigDecimal quoteAmount;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
