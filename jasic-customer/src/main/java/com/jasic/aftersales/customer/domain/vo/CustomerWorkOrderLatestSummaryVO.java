package com.jasic.aftersales.customer.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jasic.aftersales.common.enums.BrandTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * C端最近工单摘要视图
 *
 * @author Codex
 * @date 2026/04/08
 */
@ApiModel(description = "C端最近工单摘要视图")
@Data
public class CustomerWorkOrderLatestSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID")
    private Long id;

    /** 工单编号 */
    @ApiModelProperty(value = "工单编号")
    private String orderNo;

    /** 商品名称 */
    @ApiModelProperty(value = "商品名称")
    private String productName;

    /** 机器型号 */
    @ApiModelProperty(value = "机器型号")
    private String productModel;

    /** 故障描述 */
    @ApiModelProperty(value = "故障描述")
    private String faultDesc;

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

    /** 外显状态 */
    @ApiModelProperty(value = "外显状态")
    private String displayStatus;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
