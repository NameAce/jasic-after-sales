package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 故障与维修配置视图
 *
 * @author Zoro
 * @date 2026/04/01
 */
@ApiModel(description = "故障与维修配置视图")
@Data
public class FaultRepairConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 归属总部ID */
    @ApiModelProperty(value = "归属总部ID")
    private Long companyId;

    /** 归属总部名称 */
    @ApiModelProperty(value = "归属总部名称")
    private String companyName;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /** 产品型号 */
    @ApiModelProperty(value = "产品型号")
    private String productModel;

    /** 状态 */
    @ApiModelProperty(value = "状态")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 故障摘要 */
    @ApiModelProperty(value = "故障摘要")
    private String faultDescSummary;

    /** 故障项 */
    @ApiModelProperty(value = "故障项")
    private List<FaultRepairConfigFaultVO> faults;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
