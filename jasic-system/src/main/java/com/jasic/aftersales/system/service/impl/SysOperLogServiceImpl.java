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

    /**
     * 系统操作日志Mapper数据访问接口。
     *
     * @param operLog 参数
     */
    @Resource
    private SysOperLogMapper sysOperLogMapper;

    /**
     * 保存操作日志
     *
     * @param operLog 操作日志
     */
    @Override
    public void save(SysOperLog operLog) {
        // 调用insert方法，复用统一能力并保证业务规则一致。
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
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<SysOperLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getTitle())) {
            // 调用getTitle方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysOperLog::getTitle, query.getTitle());
        }
        if (query.getOperType() != null) {
            // 调用getOperType方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysOperLog::getOperType, query.getOperType());
        }
        if (StrUtil.isNotBlank(query.getUsername())) {
            // 调用getUsername方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysOperLog::getUsername, query.getUsername());
        }
        if (query.getStatus() != null) {
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysOperLog::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getBeginTime())) {
            try {
                // 调用getBeginTime方法，复用统一能力并保证业务规则一致。
                LocalDateTime begin = LocalDateTime.parse(query.getBeginTime(), FORMATTER);
                // 调用ge方法，复用统一能力并保证业务规则一致。
                wrapper.ge(SysOperLog::getOperTime, begin);
            } catch (Exception ignored) {
                // 解析失败则忽略
            }
        }
        if (StrUtil.isNotBlank(query.getEndTime())) {
            try {
                // 调用getEndTime方法，复用统一能力并保证业务规则一致。
                LocalDateTime end = LocalDateTime.parse(query.getEndTime(), FORMATTER);
                // 调用le方法，复用统一能力并保证业务规则一致。
                wrapper.le(SysOperLog::getOperTime, end);
            } catch (Exception ignored) {
                // 解析失败则忽略
            }
        }
        // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByDesc(SysOperLog::getOperTime);
        // 说明：执行该步骤以保证业务流程正确。
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
        // 说明：执行该步骤以保证业务流程正确。
        sysOperLogMapper.deleteBatchIds(ids);
    }

    /**
     * 清空操作日志
     */
    @Override
    public void clean() {
        // 调用delete方法，复用统一能力并保证业务规则一致。
        sysOperLogMapper.delete(null);
    }
}


