package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.FaultRepairConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 故障与维修配置 Mapper
 * <p>
 * fault_repair_config 的 company_id 表示“归属总部”，不是当前登录公司的租户标识。
 * 网点侧建单查条码时需要按目标总部读取配置，因此这里豁免 TenantLine 自动隔离，
 * 由业务代码按归属总部显式传入 companyId 控制查询范围。
 * </p>
 *
 * @author Codex
 * @date 2026/04/01
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface FaultRepairConfigMapper extends BaseMapper<FaultRepairConfig> {
}
