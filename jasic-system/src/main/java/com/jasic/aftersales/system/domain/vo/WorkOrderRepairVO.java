package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单维修登记视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单维修登记视图")
@Data
public class WorkOrderRepairVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 维修登记ID */
    @ApiModelProperty(value = "维修登记ID")
    private Long id;

    /** 维修公司ID */
    @ApiModelProperty(value = "维修公司ID")
    private Long companyId;

    /** 维修公司名称 */
    @ApiModelProperty(value = "维修公司名称")
    private String companyName;

    /** 维修员ID */
    @ApiModelProperty(value = "维修员ID")
    private Long repairUserId;

    /** 维修员姓名 */
    @ApiModelProperty(value = "维修员姓名")
    private String repairUserName;

    /** 是否维修完成 */
    @ApiModelProperty(value = "是否维修完成")
    private Integer isFinished;

    /** 完成时间 */
    @ApiModelProperty(value = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 故障点列表 */
    @ApiModelProperty(value = "故障点列表")
    private List<WorkOrderFaultVO> faults;
}
