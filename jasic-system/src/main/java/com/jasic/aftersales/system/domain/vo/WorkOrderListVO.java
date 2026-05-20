package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jasic.aftersales.common.enums.BrandTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单列表视图。
 *
 * <p>该对象同时承担列表展示和当前处理视图下的动作判定快照能力，
 * 因此除了页面直接展示的字段，还需要保留权限计算依赖的总部归属、建单公司等上下文字段。</p>
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

    @ApiModelProperty(value = "保修状态", allowableValues = "IN_WARRANTY,OUT_OF_WARRANTY")
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

    @ApiModelProperty(value = "建单来源公司ID")
    private Long createCompanyId;

    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    @ApiModelProperty(value = "是否发生过转单")
    private Integer hasTransfer;

    @ApiModelProperty(value = "转单次数")
    private Integer transferCount;

    @ApiModelProperty(value = "当前有效报价金额")
    private BigDecimal quoteAmount;

    @ApiModelProperty(value = "当前列表项可执行动作编码列表，如 ASSIGN、REPAIR_FINISH、CLOSE")
    private List<String> availableActions;

    @ApiModelProperty(value = "当前列表项只读原因")
    private String readonlyReason;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
