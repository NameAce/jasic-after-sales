package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.SyncTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步任务 Mapper
 *
 * @author Zoro
 * @date 2026/04/12
 */
@Mapper
public interface SyncTaskMapper extends BaseMapper<SyncTask> {
}
