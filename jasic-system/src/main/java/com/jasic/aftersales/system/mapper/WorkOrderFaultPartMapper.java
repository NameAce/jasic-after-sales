package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.WorkOrderFaultPart;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单故障点配件明细 Mapper
 *
 * @author Codex
 * @date 2026/04/15
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface WorkOrderFaultPartMapper extends BaseMapper<WorkOrderFaultPart> {

}
