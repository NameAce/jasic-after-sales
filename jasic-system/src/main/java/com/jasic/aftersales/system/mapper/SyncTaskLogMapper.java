package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.SyncTaskLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步任务日志 Mapper
 *
 * @author Codex
 * @date 2026/04/12
 */
@Mapper
public interface SyncTaskLogMapper extends BaseMapper<SyncTaskLog> {
}
