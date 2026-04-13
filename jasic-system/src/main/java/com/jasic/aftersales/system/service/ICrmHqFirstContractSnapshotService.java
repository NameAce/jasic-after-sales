package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.vo.CrmHqFirstContractSyncSummaryVO;

import java.time.LocalDateTime;

/**
 * CRM 签约快照服务。
 *
 * @author Codex
 * @date 2026/04/12
 */
public interface ICrmHqFirstContractSnapshotService {

    /**
     * 查询最早变更时间。
     *
     * @return 最早变更时间
     */
    LocalDateTime getEarliestChangeTime();

    /**
     * 按时间范围增量同步 CRM 签约快照。
     *
     * @param startInclusive 开始时间
     * @param endExclusive 结束时间
     * @return 同步结果
     */
    CrmHqFirstContractSyncSummaryVO syncByTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive);
}
