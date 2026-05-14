package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.WorkOrderFault;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单故障点记录 Mapper
 *
 * @author Codex
 * @date 2026/03/26
 */
@Mapper
public interface WorkOrderFaultMapper extends BaseMapper<WorkOrderFault> {

}
