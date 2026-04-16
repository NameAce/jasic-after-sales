package com.jasic.aftersales.customer.domain.vo;

import com.jasic.aftersales.common.enums.BrandTypeEnum;
import com.jasic.aftersales.system.domain.vo.WorkOrderEvaluationVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderQuoteVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairVO;
import com.jasic.aftersales.system.domain.vo.SysFileItemVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * C端工单详情视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "C端工单详情视图")
@Data
public class CustomerWorkOrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID")
    private Long id;

    /** 工单编号 */
    @ApiModelProperty(value = "工单编号")
    private String orderNo;

    /** 客户ID */
    @ApiModelProperty(value = "客户ID")
    private Long customerId;

    /** 客户姓名 */
    @ApiModelProperty(value = "客户姓名")
    private String customerName;

    /** 客户手机号 */
    @ApiModelProperty(value = "客户手机号")
    private String customerMobile;

    /** 机器条码 */
    @ApiModelProperty(value = "机器条码")
    private String barcode;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /** 商品名称 */
    @ApiModelProperty(value = "商品名称")
    private String productName;

    /** 机器型号 */
    @ApiModelProperty(value = "机器型号")
    private String productModel;

    /** 机器小号 */
    @ApiModelProperty(value = "机器小号")
    private String machineNo;

    /** 品牌编码 */
    @ApiModelProperty(value = "品牌编码")
    private String brandCode;

    /** 品牌名称 */
    @ApiModelProperty(value = "品牌名称")
    private String brandName;

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

    /** 质保状态 */
    @ApiModelProperty(value = "质保状态", allowableValues = "IN_WARRANTY,OUT_OF_WARRANTY")
    private String warrantyStatus;

    /** 客户报修描述 */
    @ApiModelProperty(value = "客户报修描述")
    private String faultDesc;

    /** 客户故障备注 */
    @ApiModelProperty(value = "客户故障备注")
    private String faultRemark;

    /** 故障图片列表 */
    @ApiModelProperty(value = "故障图片列表")
    private List<SysFileItemVO> faultImageFiles;

    /** 故障视频列表 */
    @ApiModelProperty(value = "故障视频列表")
    private List<SysFileItemVO> faultVideoFiles;

    /** 故障语音列表 */
    @ApiModelProperty(value = "故障语音列表")
    private List<SysFileItemVO> faultVoiceFiles;

    /** 寄件人姓名 */
    @ApiModelProperty(value = "寄件人姓名")
    private String senderName;

    /** 寄件人手机号 */
    @ApiModelProperty(value = "寄件人手机号")
    private String senderMobile;

    /** 寄件地址 */
    @ApiModelProperty(value = "寄件地址")
    private String senderAddress;

    /** 寄件快递单号 */
    @ApiModelProperty(value = "寄件快递单号")
    private String sendExpressNo;

    /** 寄件凭证文件列表 */
    @ApiModelProperty(value = "寄件凭证文件列表")
    private List<SysFileItemVO> senderVoucherFiles;

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

    @ApiModelProperty(value = "当前受理网点电话")
    private String currentAcceptCompanyPhone;

    /** 当前维修员姓名 */
    @ApiModelProperty(value = "当前维修员姓名")
    private String assignedUserName;

    /** 归属总部ID */
    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    /** 回寄方式 */
    @ApiModelProperty(value = "回寄方式", allowableValues = "回寄,自提")
    private String returnMethod;

    /** 回寄快递单号 */
    @ApiModelProperty(value = "回寄快递单号")
    private String returnExpressNo;

    /** 回寄凭证文件列表 */
    @ApiModelProperty(value = "回寄凭证文件列表")
    private List<SysFileItemVO> returnVoucherFiles;

    /** 关闭原因 */
    @ApiModelProperty(value = "关闭原因")
    private String closeReason;

    /** 是否允许评价 */
    @ApiModelProperty(value = "是否允许评价")
    private Boolean canEvaluate;

    /** 是否允许修改寄件信息 */
    @ApiModelProperty(value = "是否允许修改寄件信息")
    private Boolean canEditSendInfo;

    /** 完成时间 */
    @ApiModelProperty(value = "完成时间")
    private LocalDateTime completedTime;

    /** 关闭时间 */
    @ApiModelProperty(value = "关闭时间")
    private LocalDateTime closedTime;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /** 报价记录列表 */
    @ApiModelProperty(value = "报价记录列表")
    private List<WorkOrderQuoteVO> quotes;

    /** 维修登记列表 */
    @ApiModelProperty(value = "维修登记列表")
    private List<WorkOrderRepairVO> repairs;

    /** 客户评价 */
    @ApiModelProperty(value = "客户评价")
    private WorkOrderEvaluationVO evaluation;
}
