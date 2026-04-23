package com.jasic.aftersales.customer.domain.vo;

import com.jasic.aftersales.common.enums.BrandTypeEnum;
import com.jasic.aftersales.system.domain.vo.SysFileItemVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderEvaluationVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderQuoteVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

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

    @ApiModelProperty(value = "工单ID")
    private Long id;

    @ApiModelProperty(value = "工单编号")
    private String orderNo;

    @ApiModelProperty(value = "客户ID")
    private Long customerId;

    @ApiModelProperty(value = "客户姓名")
    private String customerName;

    @ApiModelProperty(value = "客户手机号")
    private String customerMobile;

    @ApiModelProperty(value = "机器条码")
    private String barcode;

    @ApiModelProperty(value = "物料编码")
    private String productCode;

    @ApiModelProperty(value = "商品名称")
    private String productName;

    @ApiModelProperty(value = "机器型号")
    private String productModel;

    @ApiModelProperty(value = "机器小号")
    private String machineNo;

    @ApiModelProperty(value = "品牌编码")
    private String brandCode;

    @ApiModelProperty(value = "品牌名称")
    private String brandName;

    @ApiModelProperty(value = "品牌类型", allowableValues = "JASIC,NON_JASIC")
    private BrandTypeEnum brandType;

    @ApiModelProperty(value = "品牌类型名称")
    private String brandTypeLabel;

    @ApiModelProperty(value = "服务方式编码", allowableValues = "MAIL,STORE")
    private String serviceMode;

    @ApiModelProperty(value = "服务方式名称")
    private String serviceModeLabel;

    @ApiModelProperty(value = "最后出库日期")
    private LocalDateTime lastOutDate;

    @ApiModelProperty(value = "质保状态", allowableValues = "IN_WARRANTY,OUT_OF_WARRANTY")
    private String warrantyStatus;

    @ApiModelProperty(value = "客户报修描述")
    private String faultDesc;

    @ApiModelProperty(value = "客户故障备注")
    private String faultRemark;

    @ApiModelProperty(value = "故障图片列表")
    private List<SysFileItemVO> faultImageFiles;

    @ApiModelProperty(value = "故障视频列表")
    private List<SysFileItemVO> faultVideoFiles;

    @ApiModelProperty(value = "故障语音列表")
    private List<SysFileItemVO> faultVoiceFiles;

    @ApiModelProperty(value = "寄件人姓名")
    private String senderName;

    @ApiModelProperty(value = "寄件人手机号")
    private String senderMobile;

    @ApiModelProperty(value = "寄件地址")
    private String senderAddress;

    @ApiModelProperty(value = "寄件快递单号")
    private String sendExpressNo;

    @ApiModelProperty(value = "寄件凭证文件列表")
    private List<SysFileItemVO> senderVoucherFiles;

    @ApiModelProperty(value = "主状态编码", allowableValues = "PENDING_ASSIGN,PENDING_TECH_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String mainStatus;

    @ApiModelProperty(value = "展示状态名称")
    private String displayStatus;

    @ApiModelProperty(value = "评价状态编码", allowableValues = "NOT_OPEN,PENDING_EVALUATE,EVALUATED")
    private String evaluateStatus;

    @ApiModelProperty(value = "评价状态名称")
    private String evaluateStatusLabel;

    @ApiModelProperty(value = "当前受理网点名称")
    private String currentAcceptCompanyName;

    @ApiModelProperty(value = "当前受理网点电话")
    private String currentAcceptCompanyPhone;

    @ApiModelProperty(value = "当前维修员姓名")
    private String assignedUserName;

    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    @ApiModelProperty(value = "回寄方式", allowableValues = "回寄,自提")
    private String returnMethod;

    @ApiModelProperty(value = "回寄快递单号")
    private String returnExpressNo;

    @ApiModelProperty(value = "回寄凭证文件列表")
    private List<SysFileItemVO> returnVoucherFiles;

    @ApiModelProperty(value = "关闭原因")
    private String closeReason;

    @ApiModelProperty(value = "是否允许评价")
    private Boolean canEvaluate;

    @ApiModelProperty(value = "是否允许修改寄件信息")
    private Boolean canEditSendInfo;

    @ApiModelProperty(value = "完成时间")
    private LocalDateTime completedTime;

    @ApiModelProperty(value = "关闭时间")
    private LocalDateTime closedTime;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "报价记录列表")
    private List<WorkOrderQuoteVO> quotes;

    @ApiModelProperty(value = "维修登记列表")
    private List<WorkOrderRepairVO> repairs;

    @ApiModelProperty(value = "客户评价")
    private WorkOrderEvaluationVO evaluation;
}
