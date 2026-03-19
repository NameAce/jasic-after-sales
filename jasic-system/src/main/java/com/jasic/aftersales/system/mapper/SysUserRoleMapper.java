package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-角色关联 Mapper
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

}
