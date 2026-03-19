package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 菜单 Mapper
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据用户ID和公司ID查询权限标识集合
     *
     * @param userId    用户ID
     * @param companyId 公司ID
     * @return 权限标识集合
     */
    Set<String> selectPermsByUserIdAndCompanyId(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /**
     * 根据用户ID和公司ID查询菜单树（不含按钮）
     *
     * @param userId    用户ID
     * @param companyId 公司ID
     * @return 菜单列表
     */
    List<SysMenu> selectMenuTreeByUserIdAndCompanyId(@Param("userId") Long userId, @Param("companyId") Long companyId);
}
