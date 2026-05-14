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

    /**
     * 分页查询同步任务列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    PageResult<SyncTaskVO> listPage(SyncTaskQuery query);

    /**
     * 根据ID查询同步任务详情。
     *
     * @param id 参数
     * @return 处理结果
     */
    SyncTaskVO getById(Long id);

    /**
     * 新增同步任务。
     *
     * @param dto 参数
     * @return 处理结果
     */
    Long save(SyncTaskDTO dto);

    /**
     * 更新同步任务。
     *
     * @param dto 参数
     */
    void update(SyncTaskDTO dto);

    /**
     * 分页查询日志分页列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    PageResult<SyncTaskLogVO> listLogPage(SyncTaskLogQuery query);

    /**
     * 分页查询处理Options列表。
     *
     * @return 处理结果
     */
    List<SyncTaskHandlerOptionVO> listHandlerOptions();

    /**
     * execute。
     *
     * @param id 参数
     * @return 处理结果
     */
    Long execute(Long id);

    /**
     * executeDefault机器条码任务。
     *
     * @return 处理结果
     */
    Long executeDefaultMachineBarcodeTask();

    /**
     * refreshSchedules。
     */
    void refreshSchedules();
}




