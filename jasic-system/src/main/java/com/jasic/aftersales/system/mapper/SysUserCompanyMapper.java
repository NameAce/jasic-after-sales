package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-公司关联 Mapper
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Mapper
public interface SysUserCompanyMapper extends BaseMapper<SysUserCompany> {

}
