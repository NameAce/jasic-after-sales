package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysRoleTemplateDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysRole;
import com.jasic.aftersales.system.domain.entity.SysRoleMenu;
import com.jasic.aftersales.system.domain.entity.SysRoleTemplate;
import com.jasic.aftersales.system.domain.entity.SysRoleTemplateMenu;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import com.jasic.aftersales.system.domain.vo.SysRoleTemplateVO;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysRoleMapper;
import com.jasic.aftersales.system.mapper.SysRoleMenuMapper;
import com.jasic.aftersales.system.mapper.SysRoleTemplateMapper;
import com.jasic.aftersales.system.mapper.SysRoleTemplateMenuMapper;
import com.jasic.aftersales.system.mapper.SysUserRoleMapper;
import com.jasic.aftersales.system.service.ISysRoleTemplateService;
import com.jasic.aftersales.system.service.SysPermissionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色模板管理 Service 实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysRoleTemplateServiceImpl implements ISysRoleTemplateService {

    @Resource
    private SysRoleTemplateMapper sysRoleTemplateMapper;

    @Resource
    private SysRoleTemplateMenuMapper sysRoleTemplateMenuMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysPermissionService sysPermissionService;

    /**
     * 根据公司类型编码查询角色模板列表
     *
     * @param typeCode 公司类型编码
     * @return 模板列表
     */
    @Override
    public List<SysRoleTemplateVO> listByTypeCode(String typeCode) {
        LambdaQueryWrapper<SysRoleTemplate> wrapper = new LambdaQueryWrapper<>();
        if (typeCode != null && !typeCode.isEmpty()) {
            wrapper.eq(SysRoleTemplate::getTypeCode, typeCode);
        }
        wrapper.orderByAsc(SysRoleTemplate::getOrderNum);
        List<SysRoleTemplate> list = sysRoleTemplateMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysRoleTemplateVO> voList = list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        for (SysRoleTemplateVO vo : voList) {
            vo.setMenuIds(loadMenuIdsByTemplateId(vo.getId()));
        }
        return voList;
    }

    /**
     * 查询模板详情（含菜单ID列表）
     *
     * @param templateId 模板ID
     * @return 模板详情
     */
    @Override
    public SysRoleTemplateVO getById(Long templateId) {
        SysRoleTemplate template = sysRoleTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException("角色模板不存在");
        }
        SysRoleTemplateVO vo = convertToVO(template);
        vo.setMenuIds(loadMenuIdsByTemplateId(templateId));
        return vo;
    }

    /**
     * 新增角色模板
     *
     * @param dto 模板参数
     * @return 模板ID
     */
    @Override
    public Long save(SysRoleTemplateDTO dto) {
        LambdaQueryWrapper<SysRoleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleTemplate::getTypeCode, dto.getTypeCode())
                .eq(SysRoleTemplate::getRoleKey, dto.getRoleKey());
        if (sysRoleTemplateMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该类型下角色标识已存在");
        }
        validateAdminUnique(dto.getTypeCode(), dto.getIsAdmin(), null);
        SysRoleTemplate template = new SysRoleTemplate();
        BeanUtil.copyProperties(dto, template);
        if (template.getIsAdmin() == null) {
            template.setIsAdmin(0);
        }
        if (template.getOrderNum() == null) {
            template.setOrderNum(0);
        }
        sysRoleTemplateMapper.insert(template);
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            batchInsertTemplateMenu(template.getId(), dto.getMenuIds());
        }
        return template.getId();
    }

    /**
     * 修改角色模板
     *
     * @param dto 模板参数
     */
    @Override
    public void update(SysRoleTemplateDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("模板ID不能为空");
        }
        SysRoleTemplate template = sysRoleTemplateMapper.selectById(dto.getId());
        if (template == null) {
            throw new ServiceException("角色模板不存在");
        }
        validateAdminUnique(dto.getTypeCode() != null ? dto.getTypeCode() : template.getTypeCode(),
                dto.getIsAdmin(), dto.getId());
        BeanUtil.copyProperties(dto, template);
        sysRoleTemplateMapper.updateById(template);
        if (dto.getMenuIds() != null) {
            LambdaQueryWrapper<SysRoleTemplateMenu> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(SysRoleTemplateMenu::getTemplateId, template.getId());
            sysRoleTemplateMenuMapper.delete(delWrapper);
            if (!dto.getMenuIds().isEmpty()) {
                batchInsertTemplateMenu(template.getId(), dto.getMenuIds());
            }
        }
    }

    /**
     * 删除角色模板
     *
     * @param templateId 模板ID
     */
    @Override
    public void remove(Long templateId) {
        LambdaQueryWrapper<SysRoleTemplateMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.eq(SysRoleTemplateMenu::getTemplateId, templateId);
        sysRoleTemplateMenuMapper.delete(menuWrapper);
        sysRoleTemplateMapper.deleteById(templateId);
    }

    /**
     * 同步模板到已有公司（完全同步：移除模板已删除的菜单，并补充模板新增的菜单）
     *
     * @param templateId 模板ID
     */
    @Override
    public void syncToCompanies(Long templateId) {
        SysRoleTemplate template = sysRoleTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException("角色模板不存在");
        }
        String typeCode = template.getTypeCode();
        List<Long> templateMenuIds = loadMenuIdsByTemplateId(templateId);
        Set<Long> templateMenuIdSet = new HashSet<>(templateMenuIds);

        // 查询该类型下的所有公司
        LambdaQueryWrapper<SysCompany> companyWrapper = new LambdaQueryWrapper<>();
        companyWrapper.eq(SysCompany::getTypeCode, typeCode);
        List<SysCompany> companies = sysCompanyMapper.selectList(companyWrapper);
        if (companies == null || companies.isEmpty()) {
            return;
        }

        for (SysCompany company : companies) {
            // 查找该公司下与模板 roleKey 匹配的系统角色
            LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(SysRole::getCompanyId, company.getId())
                    .eq(SysRole::getRoleKey, template.getRoleKey())
                    .eq(SysRole::getIsSystem, 1);
            SysRole role = sysRoleMapper.selectOne(roleWrapper);
            if (role == null) {
                continue;
            }

            // 获取角色当前菜单ID
            LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
            roleMenuWrapper.eq(SysRoleMenu::getRoleId, role.getId());
            List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(roleMenuWrapper);
            if (roleMenus == null || roleMenus.isEmpty()) {
                continue;
            }

            // 移除：删除不在模板菜单中的 role_menu 记录
            Set<Long> roleMenuIdSet = new HashSet<>();
            List<Long> toRemove = new ArrayList<>();
            for (SysRoleMenu rm : roleMenus) {
                roleMenuIdSet.add(rm.getMenuId());
                if (!templateMenuIdSet.contains(rm.getMenuId())) {
                    toRemove.add(rm.getId());
                }
            }
            for (Long rmId : toRemove) {
                sysRoleMenuMapper.deleteById(rmId);
            }

            // 新增：补充模板有而角色没有的菜单
            boolean hasChanges = !toRemove.isEmpty();
            for (Long menuId : templateMenuIds) {
                if (!roleMenuIdSet.contains(menuId)) {
                    SysRoleMenu rm = new SysRoleMenu();
                    rm.setRoleId(role.getId());
                    rm.setMenuId(menuId);
                    sysRoleMenuMapper.insert(rm);
                    hasChanges = true;
                }
            }

            if (hasChanges) {
                kickAffectedUsers(role.getId());
            }
        }
    }

    /**
     * 根据公司类型编码初始化公司角色（创建公司时调用）
     *
     * @param companyId 公司ID
     * @param typeCode  公司类型编码
     * @return 管理员角色ID（is_admin=1 的模板生成的角色），无则返回 null
     */
    @Override
    public Long initCompanyRoles(Long companyId, String typeCode) {
        LambdaQueryWrapper<SysRoleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleTemplate::getTypeCode, typeCode)
                .orderByAsc(SysRoleTemplate::getOrderNum);
        List<SysRoleTemplate> templates = sysRoleTemplateMapper.selectList(wrapper);
        if (templates == null || templates.isEmpty()) {
            return null;
        }

        Long adminRoleId = null;
        for (SysRoleTemplate template : templates) {
            boolean isAdminTemplate = template.getIsAdmin() != null && template.getIsAdmin() == 1;

            SysRole role = new SysRole();
            role.setCompanyId(companyId);
            role.setRoleName(template.getRoleName());
            role.setRoleKey(template.getRoleKey());
            role.setDataScope(template.getDataScope());
            role.setRoleType(isAdminTemplate ? 1 : 2);
            role.setIsSystem(1);
            role.setStatus(1);
            role.setOrderNum(template.getOrderNum() != null ? template.getOrderNum() : 0);
            role.setRemark(template.getRemark());
            sysRoleMapper.insert(role);

            if (isAdminTemplate) {
                adminRoleId = role.getId();
            }

            List<Long> menuIds = loadMenuIdsByTemplateId(template.getId());
            if (menuIds != null && !menuIds.isEmpty()) {
                for (Long menuId : menuIds) {
                    SysRoleMenu rm = new SysRoleMenu();
                    rm.setRoleId(role.getId());
                    rm.setMenuId(menuId);
                    sysRoleMenuMapper.insert(rm);
                }
            }
        }
        return adminRoleId;
    }

    /**
     * 实体转 VO（不含 menuIds）
     */
    private SysRoleTemplateVO convertToVO(SysRoleTemplate template) {
        SysRoleTemplateVO vo = new SysRoleTemplateVO();
        BeanUtil.copyProperties(template, vo);
        return vo;
    }

    /**
     * 根据模板ID加载菜单ID列表
     */
    private List<Long> loadMenuIdsByTemplateId(Long templateId) {
        LambdaQueryWrapper<SysRoleTemplateMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleTemplateMenu::getTemplateId, templateId);
        List<SysRoleTemplateMenu> list = sysRoleTemplateMenuMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(SysRoleTemplateMenu::getMenuId)
                .collect(Collectors.toList());
    }

    /**
     * 批量插入模板-菜单关联
     */
    private void batchInsertTemplateMenu(Long templateId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            SysRoleTemplateMenu rm = new SysRoleTemplateMenu();
            rm.setTemplateId(templateId);
            rm.setMenuId(menuId);
            sysRoleTemplateMenuMapper.insert(rm);
        }
    }

    /**
     * 校验同一 type_code 下只能有一个 is_admin=1 的模板
     *
     * @param typeCode   公司类型编码
     * @param isAdmin    当前设置的 is_admin 值
     * @param excludeId  排除的模板ID（修改时传自身ID）
     */
    private void validateAdminUnique(String typeCode, Integer isAdmin, Long excludeId) {
        if (isAdmin == null || isAdmin != 1) {
            return;
        }
        LambdaQueryWrapper<SysRoleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleTemplate::getTypeCode, typeCode)
                .eq(SysRoleTemplate::getIsAdmin, 1);
        if (excludeId != null) {
            wrapper.ne(SysRoleTemplate::getId, excludeId);
        }
        if (sysRoleTemplateMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该公司类型下已存在管理员角色模板，每种类型最多一个");
        }
    }

    /**
     * 踢出受影响的用户（角色变更后需重新登录）
     */
    private void kickAffectedUsers(Long roleId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getRoleId, roleId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        if (userRoles == null || userRoles.isEmpty()) {
            return;
        }
        List<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .collect(Collectors.toList());
        for (Long userId : userIds) {
            sysPermissionService.clearAllPermsCache(userId);
            StpUtil.kickout(userId);
        }
    }
}
