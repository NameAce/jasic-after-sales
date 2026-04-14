package com.jasic.aftersales.customer.domain.vo;

import com.jasic.aftersales.common.enums.BrandTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * C端工单列表视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "C端工单列表视图")
@Data
public class CustomerWorkOrderListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID")
    private Long id;

    /** 工单编号 */
    @ApiModelProperty(value = "工单编号")
    private String orderNo;

    /** 客户姓名 */
    @ApiModelProperty(value = "客户姓名")
    private String customerName;

    /** 客户手机号 */
    @ApiModelProperty(value = "客户手机号")
    private String customerMobile;

    /** 机器条码 */
    @ApiModelProperty(value = "机器条码")
    private String barcode;

    /** 机器型号 */
    @ApiModelProperty(value = "机器型号")
    private String productModel;

    /** 品牌类型 */
    @ApiModelProperty(value = "品牌类型", allowableValues = "JASIC,NON_JASIC")
    private BrandTypeEnum brandType;

    /** 品牌类型名称 */
    @ApiModelProperty(value = "品牌类型名称")
    private String brandTypeLabel;

    /** 服务方式编码 */
    @ApiModelProperty(value = "服务方式编码", allowableValues = "MAIL,STORE")
    private String serviceMode;

    /** 服务方式名称 */
    @ApiModelProperty(value = "服务方式名称")
    private String serviceModeLabel;

    /** 主状态编码 */
    @ApiModelProperty(value = "主状态编码", allowableValues = "PENDING_ASSIGN,PENDING_TECH_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String mainStatus;

    /** 展示状态名称 */
    @ApiModelProperty(value = "展示状态名称")
    private String displayStatus;

    /** 评价状态编码 */
    @ApiModelProperty(value = "评价状态编码", allowableValues = "NOT_OPEN,PENDING_EVALUATE,EVALUATED")
    private String evaluateStatus;

    /** 评价状态名称 */
    @ApiModelProperty(value = "评价状态名称")
    private String evaluateStatusLabel;

    /** 当前受理网点名称 */
    @ApiModelProperty(value = "当前受理网点名称")
    private String currentAcceptCompanyName;

    /** 当前维修员姓名 */
    @ApiModelProperty(value = "当前维修员姓名")
    private String assignedUserName;

    /** 是否发生过转单 */
    @ApiModelProperty(value = "是否发生过转单")
    private Integer hasTransfer;

    /** 是否允许评价 */
    @ApiModelProperty(value = "是否允许评价")
    private Boolean canEvaluate;

    /** 是否允许上传寄件凭证 */
    @ApiModelProperty(value = "是否允许上传寄件凭证")
    private Boolean canUploadSendExpress;

    /** 当前有效报价金额 */
    @ApiModelProperty(value = "当前有效报价金额")
    private BigDecimal quoteAmount;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /** 关闭时间 */
    @ApiModelProperty(value = "关闭时间")
    private LocalDateTime closedTime;
}
