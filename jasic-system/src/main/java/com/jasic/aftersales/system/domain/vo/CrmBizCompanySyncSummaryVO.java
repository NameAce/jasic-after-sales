package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CRM 公司快照同步摘要
 *
 * @author Zoro
 * @date 2026/04/12
 */
@Data
public class CrmBizCompanySyncSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据开始时间 */
    private LocalDateTime dataStartTime;

    /** 数据结束时间 */
    private LocalDateTime dataEndTime;

    /** 处理数量 */
    private Integer processedCount;

    /** 新增数量 */
    private Integer insertedCount;

    /** 更新数量 */
    private Integer updatedCount;
}
