package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.entity.SysOperLog;
import com.jasic.aftersales.system.domain.query.SysOperLogQuery;

import java.util.List;

/**
 * 操作日志 Service 接口
 *
 * @author Zoro
 * @date 2026/03/18
 */
public interface ISysOperLogService {

    /**
     * 保存操作日志
     *
     * @param operLog 操作日志
     */
    void save(SysOperLog operLog);

    /**
     * 分页查询操作日志
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<SysOperLog> listPage(SysOperLogQuery query);

    /**
     * 批量删除操作日志
     *
     * @param ids 主键ID列表
     */
    void removeByIds(List<Long> ids);

    /**
     * 清空操作日志
     */
    void clean();
}
