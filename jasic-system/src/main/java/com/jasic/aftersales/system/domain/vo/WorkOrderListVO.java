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
 * @author Zoro
 * @date 2026/03/26
 */
@ApiModel(description = "工单列表视图")
@Data
public class WorkOrderListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**主键ID，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "工单ID")
    private Long id;

    /**工单号，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "工单号")
    private String orderNo;

    /**customerName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "客户姓名")
    private String customerName;

    /**customerMobile 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "客户手机号")
    private String customerMobile;

    /**barcode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "条码")
    private String barcode;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastOutDate;

    /**warrantyStatus 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "保修状态", allowableValues = "IN_WARRANTY,OUT_OF_WARRANTY")
    private String warrantyStatus;

    /**productModel 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "机器型号")
    private String productModel;

    /**faultDesc 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "故障描述")
    private String faultDesc;

    /**mainStatus 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "主状态", allowableValues = "PENDING_ASSIGN,PENDING_TECH_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String mainStatus;

    /**mainStatusLabel 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "主状态名称")
    private String mainStatusLabel;

    /**displayStatus 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "展示状态", allowableValues = "WAIT_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String displayStatus;

    /**currentAcceptCompanyId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "当前受理公司ID")
    private Long currentAcceptCompanyId;

    /**currentAcceptCompanyName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "当前受理公司名称")
    private String currentAcceptCompanyName;

    /**currentAcceptCompanyPhone 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "当前受理网点电话")
    private String currentAcceptCompanyPhone;

    /**assignedUserId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "当前维修员ID")
    private Long assignedUserId;

    /**assignedUserName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "当前维修员姓名")
    private String assignedUserName;

    /**createCompanyId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "建单来源公司ID")
    private Long createCompanyId;

    /**createEntryType 字段，用于向前端标识当前工单来源于哪种建单入口。*/
    @ApiModelProperty(value = "建单入口类型", allowableValues = "PROXY_SELF,UPSTREAM_FIRST,UPSTREAM_HQ,CUSTOMER_REPORT")
    private String createEntryType;

    /**hqCompanyId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    /**hasTransfer 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "是否发生过转单")
    private Integer hasTransfer;

    /**transferCount 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "转单次数")
    private Integer transferCount;

    /**quoteAmount 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "当前有效报价金额")
    private BigDecimal quoteAmount;

    /**availableActions 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "当前列表项可执行动作编码列表，如 ASSIGN、REPAIR_FINISH、CLOSE")
    private List<String> availableActions;

    /**readonlyReason 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "当前列表项只读原因")
    private String readonlyReason;

    /**创建时间，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
