package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.vo.CrmFirstSecondRelationSyncSummaryVO;

import java.time.LocalDateTime;

/**
 * CRM 一级二级关系来源快照服务
 *
 * @author Codex
 * @date 2026/04/17
 */
public interface ICrmFirstSecondRelationSnapshotService {

    /**
     * 查询最早变更时间
     *
     * @return 最早变更时间
     */
    LocalDateTime getEarliestChangeTime();

    /**
     * 按时间范围增量同步 CRM 一级二级关系来源快照
     *
     * @param startInclusive 开始时间
     * @param endExclusive 结束时间
     * @return 同步结果
     */
    CrmFirstSecondRelationSyncSummaryVO syncByTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive);
}
