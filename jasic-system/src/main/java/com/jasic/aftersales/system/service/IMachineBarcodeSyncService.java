package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.vo.MachineBarcodeSyncResultVO;

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
}
