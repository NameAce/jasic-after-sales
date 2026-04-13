package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CRM 签约快照同步结果 VO。
 *
 * @author Codex
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 签约快照同步结果 VO")
@Data
public class CrmHqFirstContractSyncSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据开始时间 */
    @ApiModelProperty(value = "数据开始时间")
    private LocalDateTime dataStartTime;

    /** 数据结束时间 */
    @ApiModelProperty(value = "数据结束时间")
    private LocalDateTime dataEndTime;

    /** 处理条数 */
    @ApiModelProperty(value = "处理条数")
    private Integer processedCount;

    /** 新增条数 */
    @ApiModelProperty(value = "新增条数")
    private Integer insertedCount;

    /** 更新条数 */
    @ApiModelProperty(value = "更新条数")
    private Integer updatedCount;
}
