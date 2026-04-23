package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户按服务网点聚合的报修历史统计。
 *
 * @author Codex
 * @date 2026/04/22
 */
@Data
public class WorkOrderCompanyRepairHistoryStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 服务网点公司ID */
    private Long companyId;

    /** 报修次数 */
    private Long repairCount;

    /** 最后报修时间 */
    private LocalDateTime lastRepairTime;
}
