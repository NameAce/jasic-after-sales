package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.SyncTaskDTO;
import com.jasic.aftersales.system.domain.query.SyncTaskLogQuery;
import com.jasic.aftersales.system.domain.query.SyncTaskQuery;
import com.jasic.aftersales.system.domain.vo.SyncTaskHandlerOptionVO;
import com.jasic.aftersales.system.domain.vo.SyncTaskLogVO;
import com.jasic.aftersales.system.domain.vo.SyncTaskVO;

import java.util.List;

/**
 * 同步任务 Service
 *
 * @author Codex
 * @date 2026/04/12
 */
public interface ISyncTaskService {

    PageResult<SyncTaskVO> listPage(SyncTaskQuery query);

    SyncTaskVO getById(Long id);

    Long save(SyncTaskDTO dto);

    void update(SyncTaskDTO dto);

    PageResult<SyncTaskLogVO> listLogPage(SyncTaskLogQuery query);

    List<SyncTaskHandlerOptionVO> listHandlerOptions();

    Long execute(Long id);

    Long executeDefaultMachineBarcodeTask();

    void refreshSchedules();
}
