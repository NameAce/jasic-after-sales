package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单报价视图
 *
 * @author Zoro
 * @date 2026/03/26
 */
@ApiModel(description = "工单报价视图")
@Data
public class WorkOrderQuoteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报价ID */
    @ApiModelProperty(value = "报价ID")
    private Long id;

    /** 报价公司ID */
    @ApiModelProperty(value = "报价公司ID")
    private Long companyId;

    /** 报价公司名称 */
    @ApiModelProperty(value = "报价公司名称")
    private String companyName;

    /** 报价人ID */
    @ApiModelProperty(value = "报价人ID")
    private Long quotedBy;

    /** 报价人姓名 */
    @ApiModelProperty(value = "报价人姓名")
    private String quotedByName;

    /** 故障判定 */
    @ApiModelProperty(value = "故障判定")
    private String faultJudge;

    /** 报价金额 */
    @ApiModelProperty(value = "报价金额")
    private BigDecimal quoteAmount;

    /** 报价说明 */
    @ApiModelProperty(value = "报价说明")
    private String quoteDesc;

    /** 是否当前有效 */
    @ApiModelProperty(value = "是否当前有效")
    private Integer isCurrentValid;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
