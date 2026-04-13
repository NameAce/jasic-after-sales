package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.vo.MachineBarcodeSyncResultVO;

import java.time.LocalDateTime;

/**
 * 条码主数据同步 Service
 *
 * @author Codex
 * @date 2026/04/07
 */
public interface IMachineBarcodeSyncService {

    /**
     * 手动全量同步 CRM 条码主数据
     *
     * @return 同步结果
     */
    MachineBarcodeSyncResultVO fullSyncFromCrm();

    /**
     * 查询 CRM 条码最早同步时间
     *
     * @return 最早时间
     */
    LocalDateTime getEarliestAddTime();

    /**
     * 按 add_time 时间范围同步 CRM 条码主数据
     *
     * @param startInclusive 开始时间（含）
     * @param endExclusive   结束时间（不含）
     * @return 同步结果
     */
    MachineBarcodeSyncResultVO syncByAddTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive);
}
