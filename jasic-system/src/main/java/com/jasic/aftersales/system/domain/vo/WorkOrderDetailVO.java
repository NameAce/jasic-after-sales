package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jasic.aftersales.common.enums.BrandTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单详情视图
 *
 * @author Zoro
 * @date 2026/03/26
 */
@ApiModel(description = "工单详情视图")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderDetailVO extends WorkOrderListVO {

    private static final long serialVersionUID = 1L;

    /** 客户ID */
    @ApiModelProperty(value = "客户ID")
    private Long customerId;

    /** 申请来源名称 */
    @ApiModelProperty(value = "申请来源名称")
    private String applicationSourceName;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /** 商品名称 */
    @ApiModelProperty(value = "商品名称")
    private String productName;

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

    /** 评价状态 */
    @ApiModelProperty(value = "评价状态", allowableValues = "NOT_OPEN,PENDING_EVALUATE,EVALUATED")
    private String evaluateStatus;

    /** 评价状态名称 */
    @ApiModelProperty(value = "评价状态名称")
    private String evaluateStatusLabel;

    /** 当前受理主体类型 */
    @ApiModelProperty(value = "当前受理主体类型", allowableValues = "SERVICE,HQ")
    private String currentAcceptSubjectType;

    /** 建单来源公司ID */
    @ApiModelProperty(value = "建单来源公司ID")
    private Long createCompanyId;

    /** 建单来源公司名称 */
    @ApiModelProperty(value = "建单来源公司名称")
    private String createCompanyName;

    /** 建单入口类型 */
    @ApiModelProperty(value = "建单入口类型", allowableValues = "PROXY_SELF,UPSTREAM_FIRST,UPSTREAM_HQ,CUSTOMER_REPORT")
    private String createEntryType;

    /** 归属总部ID */
    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    /** 归属总部名称 */
    @ApiModelProperty(value = "归属总部名称")
    private String hqCompanyName;

    /** 机器返回方式 */
    @ApiModelProperty(value = "机器返回方式", allowableValues = "回寄,自提")
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

    /** 完成时间 */
    @ApiModelProperty(value = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;

    /** 关闭时间 */
    @ApiModelProperty(value = "关闭时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedTime;

    /** 参与方列表 */
    @ApiModelProperty(value = "参与方列表")
    private List<WorkOrderParticipantVO> participants;

    /** 报价记录列表 */
    @ApiModelProperty(value = "报价记录列表")
    private List<WorkOrderQuoteVO> quotes;

    /** 维修登记列表 */
    @ApiModelProperty(value = "维修登记列表")
    private List<WorkOrderRepairVO> repairs;

    /** 流转历史列表 */
    @ApiModelProperty(value = "流转历史列表")
    private List<WorkOrderFlowVO> flows;

    /** 客户评价 */
    @ApiModelProperty(value = "客户评价")
    private WorkOrderEvaluationVO evaluation;

    /** 当前详情页可执行动作编码列表；由“可见范围 + 关系标签 + 主状态 + 权限点”共同决定。 */
    @ApiModelProperty(value = "当前可执行动作；返回按钮动作编码列表，如 ASSIGN、REPAIR_FINISH、CLOSE")
    private List<String> availableActions;
}


