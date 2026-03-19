package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-公司关联 Mapper
 * <p>
 * sys_user_company 的 company_id 是用户与公司的关联外键，不是租户标识。
 * 登录、切换公司、查看用户详情等场景需要按 userId 查询所有关联公司，
 * 因此整体豁免 TenantLine 自动隔离，由业务代码手动控制过滤条件。
 * </p>
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface SysUserCompanyMapper extends BaseMapper<SysUserCompany> {

}
