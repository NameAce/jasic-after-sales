package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.dto.SysMenuDTO;
import com.jasic.aftersales.system.domain.entity.SysMenu;
import com.jasic.aftersales.system.domain.vo.SysMenuVO;

import java.util.List;
import java.util.Set;

/**
 * 菜单管理 Service 接口
 *
 * @author Zoro
 * @date 2026/03/18
 */
public interface ISysMenuService {

    /**
     * 根据主体类型查询菜单树
     *
     * @param subjectType 主体类型（PLATFORM/HQ/SERVICE）
     * @return 菜单树
     */
    List<SysMenuVO> listMenuTreeBySubjectType(String subjectType);

    /**
     * 根据用户ID和公司ID查询菜单树（用于动态路由）
     *
     * @param userId    用户ID
     * @param companyId 公司ID
     * @return 菜单树
     */
    List<SysMenuVO> listMenuTreeByUser(Long userId, Long companyId);

    /**
     * 根据用户ID和公司ID查询权限标识集合
     *
     * @param userId    用户ID
     * @param companyId 公司ID
     * @return 权限标识集合
     */
    Set<String> listPermsByUser(Long userId, Long companyId);

    /**
     * 查询菜单详情
     *
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    SysMenu getById(Long menuId);

    /**
     * 新增菜单
     *
     * @param dto 菜单参数
     * @return 菜单ID
     */
    Long save(SysMenuDTO dto);

    /**
     * 修改菜单
     *
     * @param dto 菜单参数
     */
    void update(SysMenuDTO dto);

    /**
     * 删除菜单（含子菜单校验）
     *
     * @param menuId 菜单ID
     */
    void remove(Long menuId);

    /**
     * 根据主体类型查询菜单列表（平铺，不构建树）
     *
     * @param subjectType 主体类型
     * @return 菜单列表
     */
    List<SysMenu> listBySubjectType(String subjectType);

    /**
     * 查询公司类型的菜单上限ID列表
     *
     * @param typeCode 公司类型编码
     * @return 菜单ID列表
     */
    List<Long> listTypeCodeMenuIds(String typeCode);

    /**
     * 分配公司类型的菜单上限
     *
     * @param typeCode 公司类型编码
     * @param menuIds  菜单ID列表
     */
    void assignTypeCodeMenus(String typeCode, List<Long> menuIds);

    /**
     * 根据公司类型编码查询已分配的菜单树（用于角色模板/角色分配菜单时展示可选范围）
     *
     * @param typeCode 公司类型编码
     * @return 菜单树
     */
    List<SysMenuVO> listMenuTreeByTypeCode(String typeCode);

    /**
     * 从源主体类型拷贝菜单到目标主体类型
     *
     * @param sourceSubjectType 源主体类型
     * @param targetSubjectType 目标主体类型
     * @param menuIds          要拷贝的菜单ID列表（为空则拷贝全部）
     * @return 拷贝的菜单数量
     */
    int copyMenus(String sourceSubjectType, String targetSubjectType, List<Long> menuIds);
}
