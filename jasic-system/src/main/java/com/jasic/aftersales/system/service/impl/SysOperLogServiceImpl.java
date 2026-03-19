package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.entity.SysOperLog;
import com.jasic.aftersales.system.domain.query.SysOperLogQuery;
import com.jasic.aftersales.system.mapper.SysOperLogMapper;
import com.jasic.aftersales.system.service.ISysOperLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 操作日志 Service 实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysOperLogServiceImpl implements ISysOperLogService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private SysOperLogMapper sysOperLogMapper;

    /**
     * 保存操作日志
     *
     * @param operLog 操作日志
     */
    @Override
    public void save(SysOperLog operLog) {
        sysOperLogMapper.insert(operLog);
    }

    /**
     * 分页查询操作日志
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<SysOperLog> listPage(SysOperLogQuery query) {
        Page<SysOperLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getTitle())) {
            wrapper.like(SysOperLog::getTitle, query.getTitle());
        }
        if (query.getOperType() != null) {
            wrapper.eq(SysOperLog::getOperType, query.getOperType());
        }
        if (StrUtil.isNotBlank(query.getUsername())) {
            wrapper.like(SysOperLog::getUsername, query.getUsername());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysOperLog::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getBeginTime())) {
            try {
                LocalDateTime begin = LocalDateTime.parse(query.getBeginTime(), FORMATTER);
                wrapper.ge(SysOperLog::getOperTime, begin);
            } catch (Exception ignored) {
                // 解析失败则忽略
            }
        }
        if (StrUtil.isNotBlank(query.getEndTime())) {
            try {
                LocalDateTime end = LocalDateTime.parse(query.getEndTime(), FORMATTER);
                wrapper.le(SysOperLog::getOperTime, end);
            } catch (Exception ignored) {
                // 解析失败则忽略
            }
        }
        wrapper.orderByDesc(SysOperLog::getOperTime);
        Page<SysOperLog> result = sysOperLogMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 批量删除操作日志
     *
     * @param ids 主键ID列表
     */
    @Override
    public void removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        sysOperLogMapper.deleteBatchIds(ids);
    }

    /**
     * 清空操作日志
     */
    @Override
    public void clean() {
        sysOperLogMapper.delete(null);
    }
}
