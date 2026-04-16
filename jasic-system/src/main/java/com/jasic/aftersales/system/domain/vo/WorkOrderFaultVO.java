package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单故障点视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单故障点视图")
@Data
public class WorkOrderFaultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 故障点ID */
    @ApiModelProperty(value = "故障点ID")
    private Long id;

    /** 登记公司ID */
    @ApiModelProperty(value = "登记公司ID")
    private Long companyId;

    /** 故障描述 */
    @ApiModelProperty(value = "故障描述")
    private String faultDesc;

    /** 其它故障说明 */
    @ApiModelProperty(value = "其它故障说明")
    private String faultRemark;

    /** 维修说明 */
    @ApiModelProperty(value = "维修说明")
    private String repairDesc;

    /** 其他维修说明 */
    @ApiModelProperty(value = "其他维修说明")
    private String otherDesc;

    /** 配件明细列表 */
    @ApiModelProperty(value = "配件明细列表")
    private List<WorkOrderFaultPartVO> partList;

    /** 排序号 */
    @ApiModelProperty(value = "排序号")
    private Integer sortNum;

    /** 登记人ID */
    @ApiModelProperty(value = "登记人ID")
    private Long createdBy;

    /** 登记人姓名 */
    @ApiModelProperty(value = "登记人姓名")
    private String createdByName;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
