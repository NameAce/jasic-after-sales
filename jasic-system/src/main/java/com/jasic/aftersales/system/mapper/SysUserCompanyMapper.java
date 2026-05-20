package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户-公司关联 Mapper
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Mapper
public interface SysUserCompanyMapper extends BaseMapper<SysUserCompany> {

    /**
     * 按公司快速查询主账号用户ID。
     *
     * <p>主账号定义为 `sys_user_company.is_primary_account = 1` 的关联记录。
     * 当前约定每个公司只应存在一个主账号；如历史异常数据产生多条，这里按最早关联记录兜底。</p>
     *
     * @param companyId 公司ID
     * @return 主账号用户ID；不存在时返回 {@code null}
     */
    @Select({
            "SELECT user_id",
            "FROM sys_user_company",
            "WHERE company_id = #{companyId}",
            "  AND is_primary_account = 1",
            "ORDER BY id ASC",
            "LIMIT 1"
    })
    Long selectPrimaryAccountUserIdByCompanyId(@Param("companyId") Long companyId);
}
