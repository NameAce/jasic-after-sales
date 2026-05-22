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
 * @author Zoro
 * @date 2026/03/26
 */
@ApiModel(description = "C端工单详情视图")
@Data
public class CustomerWorkOrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**主键ID，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "工单ID")
    private Long id;

    /**工单号，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "工单编号")
    private String orderNo;

    /**客户ID，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "客户ID")
    private Long customerId;

    /**customerName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "客户姓名")
    private String customerName;

    /**customerMobile 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "客户手机号")
    private String customerMobile;

    /**barcode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "机器条码")
    private String barcode;

    /**productCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /**productName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "商品名称")
    private String productName;

    /**productModel 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "机器型号")
    private String productModel;

    /**machineNo 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "机器小号")
    private String machineNo;

    /**brandCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "品牌编码")
    private String brandCode;

    /**brandName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "品牌名称")
    private String brandName;

    /**brandType 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "品牌类型", allowableValues = "JASIC,NON_JASIC")
    private BrandTypeEnum brandType;

    /**brandTypeLabel 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "品牌类型名称")
    private String brandTypeLabel;

    /**serviceMode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "服务方式编码", allowableValues = "MAIL,STORE")
    private String serviceMode;

    /**serviceModeLabel 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "服务方式名称")
    private String serviceModeLabel;

    /**lastOutDate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "最后出库日期")
    private LocalDateTime lastOutDate;

    /**warrantyStatus 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "质保状态", allowableValues = "IN_WARRANTY,OUT_OF_WARRANTY")
    private String warrantyStatus;

    /**faultDesc 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "客户报修描述")
    private String faultDesc;

    /**faultRemark 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "客户故障备注")
    private String faultRemark;

    /**faultImageFiles 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "故障图片列表")
    private List<SysFileItemVO> faultImageFiles;

    /**faultVideoFiles 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "故障视频列表")
    private List<SysFileItemVO> faultVideoFiles;

    /**faultVoiceFiles 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "故障语音列表")
    private List<SysFileItemVO> faultVoiceFiles;

    /**senderName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "寄件人姓名")
    private String senderName;

    /**senderMobile 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "寄件人手机号")
    private String senderMobile;

    /**senderAddress 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "寄件地址")
    private String senderAddress;

    /**sendExpressNo 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "寄件快递单号")
    private String sendExpressNo;

    /**senderVoucherFiles 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "寄件凭证文件列表")
    private List<SysFileItemVO> senderVoucherFiles;

    /**mainStatus 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "主状态编码", allowableValues = "PENDING_ASSIGN,PENDING_TECH_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String mainStatus;

    /**displayStatus 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "展示状态名称")
    private String displayStatus;

    /**evaluateStatus 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "评价状态编码", allowableValues = "NOT_OPEN,PENDING_EVALUATE,EVALUATED")
    private String evaluateStatus;

    /**evaluateStatusLabel 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "评价状态名称")
    private String evaluateStatusLabel;

    /**currentAcceptCompanyName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "当前受理网点名称")
    private String currentAcceptCompanyName;

    /**currentAcceptCompanyPhone 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "当前受理网点电话")
    private String currentAcceptCompanyPhone;

    /**assignedUserName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "当前维修员姓名")
    private String assignedUserName;

    /**hqCompanyId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    /**returnMethod 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "回寄方式", allowableValues = "回寄,自提")
    private String returnMethod;

    /**returnExpressNo 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "回寄快递单号")
    private String returnExpressNo;

    /**returnVoucherFiles 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "回寄凭证文件列表")
    private List<SysFileItemVO> returnVoucherFiles;

    /**closeReason 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "关闭原因")
    private String closeReason;

    /**canEvaluate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "是否允许评价")
    private Boolean canEvaluate;

    /**canEditSendInfo 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "是否允许修改寄件信息")
    private Boolean canEditSendInfo;

    /**completedTime 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "完成时间")
    private LocalDateTime completedTime;

    /**closedTime 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "关闭时间")
    private LocalDateTime closedTime;

    /**创建时间，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**quotes 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "报价记录列表")
    private List<WorkOrderQuoteVO> quotes;

    /**repairs 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "维修登记列表")
    private List<WorkOrderRepairVO> repairs;

    /**evaluation 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "客户评价")
    private WorkOrderEvaluationVO evaluation;
}


