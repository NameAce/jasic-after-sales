package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 条码全量同步结果
 *
 * @author Codex
 * @date 2026/04/07
 */
@ApiModel(description = "条码全量同步结果")
@Data
public class MachineBarcodeSyncResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主条码处理条数 */
    @ApiModelProperty(value = "主条码处理条数")
    private Integer barcodeProcessedCount;

    /** 经销商最新出库处理条数 */
    @ApiModelProperty(value = "经销商最新出库处理条数")
    private Integer dealerProcessedCount;

    /** 本地更新经销商出库条数 */
    @ApiModelProperty(value = "本地更新经销商出库条数")
    private Integer dealerUpdatedCount;

    /** 开始时间 */
    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    /** 结束时间 */
    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;
}
