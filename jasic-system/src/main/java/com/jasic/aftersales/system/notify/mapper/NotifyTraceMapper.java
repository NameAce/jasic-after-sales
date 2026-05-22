package com.jasic.aftersales.system.notify.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.system.notify.domain.query.NotifyTraceQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTracePageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 通知记录排障 Mapper。
 *
 * @author Zoro
 * @date 2026/05/14
 */
@Mapper
public interface NotifyTraceMapper {

    /**
     * 分页查询通知记录。
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<NotifyTracePageVO> selectTracePage(Page<NotifyTracePageVO> page, @Param("query") NotifyTraceQuery query);
}
