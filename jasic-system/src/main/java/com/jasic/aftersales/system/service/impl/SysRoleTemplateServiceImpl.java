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
import com.jasic.aftersales.system.service.SysDataScopeRuleService;
import com.jasic.aftersales.system.service.SysPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

    /**
     * ???????
     *
     * @param typeCode ??????
     * @return ????
     */
    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysPermissionService sysPermissionService;

    @Resource
    private SysDataScopeRuleService dataScopeRuleService;

    @Override
    public List<SysRoleTemplateVO> listByTypeCode(String typeCode) {
        LambdaQueryWrapper<SysRoleTemplate> wrapper = new LambdaQueryWrapper<>();
        if (typeCode != null && !typeCode.isEmpty()) {
            wrapper.eq(SysRoleTemplate::getTypeCode, typeCode);
        }
        wrapper.orderByAsc(SysRoleTemplate::getOrderNum);
        // ??????????????????????????
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
     * ??By Id?
     *
     * @param templateId ??ID
     * @return ????
     */
    @Override
    public SysRoleTemplateVO getById(Long templateId) {
        // ??????????????????????????
        SysRoleTemplate template = sysRoleTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException("角色模板不存在");
        }
        SysRoleTemplateVO vo = convertToVO(template);
        vo.setMenuIds(loadMenuIdsByTemplateId(templateId));
        return vo;
    }

    /**
     * ?????
     *
     * @param dto ????
     * @return ????
     */
    @Override
    public Long save(SysRoleTemplateDTO dto) {
        // ?????????????????????????????
        dataScopeRuleService.validateByTypeCode(dto.getTypeCode(), dto.getDataScope());
        LambdaQueryWrapper<SysRoleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleTemplate::getTypeCode, dto.getTypeCode())
                .eq(SysRoleTemplate::getRoleKey, dto.getRoleKey());
        // ??????????????????????????
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
        // ???????????????????????
        sysRoleTemplateMapper.insert(template);
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            batchInsertTemplateMenu(template.getId(), dto.getMenuIds());
        }
        return template.getId();
    }

    /**
     * ?????
     *
     * @param dto ????
     */
    @Override
    public void update(SysRoleTemplateDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("模板ID不能为空");
        }
        // ??????????????????????????
        SysRoleTemplate template = sysRoleTemplateMapper.selectById(dto.getId());
        if (template == null) {
            throw new ServiceException("角色模板不存在");
        }
        String typeCode = dto.getTypeCode() != null ? dto.getTypeCode() : template.getTypeCode();
        // ?????????????????????????????
        validateAdminUnique(typeCode, dto.getIsAdmin(), dto.getId());
        dataScopeRuleService.validateByTypeCode(typeCode, dto.getDataScope());
        BeanUtil.copyProperties(dto, template);
        // ???????????????????????
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
     * ?????
     *
     * @param templateId ??ID
     */
    @Override
    public void remove(Long templateId) {
        LambdaQueryWrapper<SysRoleTemplateMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.eq(SysRoleTemplateMenu::getTemplateId, templateId);
        // ???????????????????????
        sysRoleTemplateMenuMapper.delete(menuWrapper);
        sysRoleTemplateMapper.deleteById(templateId);
    }

    /**
     * ???????
     *
     * @param templateId ??ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void syncToCompanies(Long templateId) {
        // ??????????????????????????
        SysRoleTemplate template = sysRoleTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException("角色模板不存在");
        }
        String typeCode = template.getTypeCode();
        // ?????????????????????????????
        dataScopeRuleService.validateByTypeCode(typeCode, template.getDataScope());
        List<Long> templateMenuIds = loadMenuIdsByTemplateId(templateId);
        Set<Long> templateMenuIdSet = new HashSet<>(templateMenuIds);

        LambdaQueryWrapper<SysCompany> companyWrapper = new LambdaQueryWrapper<>();
        companyWrapper.eq(SysCompany::getTypeCode, typeCode);
        List<SysCompany> companies = sysCompanyMapper.selectList(companyWrapper);
        if (companies == null || companies.isEmpty()) {
            return;
        }

        Set<Long> updatedRoleIds = new HashSet<>();
        for (SysCompany company : companies) {
            LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(SysRole::getCompanyId, company.getId())
                    .eq(SysRole::getRoleKey, template.getRoleKey())
                    .eq(SysRole::getIsSystem, 1);
            SysRole role = sysRoleMapper.selectOne(roleWrapper);

            boolean hasChanges;
            if (role == null) {
                role = createMissingSystemRole(company, template);
                hasChanges = true;
            } else {
                hasChanges = syncRoleBaseInfo(role, template);
            }

            if (syncRoleMenus(role.getId(), templateMenuIds, templateMenuIdSet)) {
                hasChanges = true;
            }
            if (hasChanges) {
                updatedRoleIds.add(role.getId());
            }
        }

        kickAffectedUsers(updatedRoleIds);
    }

    /**
     * ????????
     *
     * @param companyId ??ID
     * @param typeCode ??????
     * @return ????
     */
    @Override
    public Long initCompanyRoles(Long companyId, String typeCode) {
        LambdaQueryWrapper<SysRoleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleTemplate::getTypeCode, typeCode)
                .orderByAsc(SysRoleTemplate::getOrderNum);
        // ??????????????????????????
        List<SysRoleTemplate> templates = sysRoleTemplateMapper.selectList(wrapper);
        if (templates == null || templates.isEmpty()) {
            return null;
        }

        Long adminRoleId = null;
        for (SysRoleTemplate template : templates) {
            // ?????????????????????????????
            dataScopeRuleService.validateByTypeCode(typeCode, template.getDataScope());

            SysRole role = buildSystemRole(companyId, template);
            // ???????????????????????
            sysRoleMapper.insert(role);

            if (isAdminTemplate(template)) {
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
     * ???????
     *
     * @param template ??
     * @return ????
     */
    private SysRoleTemplateVO convertToVO(SysRoleTemplate template) {
        SysRoleTemplateVO vo = new SysRoleTemplateVO();
        BeanUtil.copyProperties(template, vo);
        return vo;
    }

    /**
     * ?????
     *
     * @param templateId ??ID
     * @return ????
     */
    private List<Long> loadMenuIdsByTemplateId(Long templateId) {
        LambdaQueryWrapper<SysRoleTemplateMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleTemplateMenu::getTemplateId, templateId);
        // ??????????????????????????
        List<SysRoleTemplateMenu> list = sysRoleTemplateMenuMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(SysRoleTemplateMenu::getMenuId)
                .collect(Collectors.toList());
    }

    /**
     * ?? batchInsertTemplateMenu ?????
     *
     * @param templateId ??ID
     * @param menuIds ??ID??
     */
    private void batchInsertTemplateMenu(Long templateId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            SysRoleTemplateMenu rm = new SysRoleTemplateMenu();
            rm.setTemplateId(templateId);
            rm.setMenuId(menuId);
            // ???????????????????????
            sysRoleTemplateMenuMapper.insert(rm);
        }
    }

    /**
     * ?????
     *
     * @param company ??
     * @param template ??
     * @return ????
     */
    private SysRole createMissingSystemRole(SysCompany company, SysRoleTemplate template) {
        LambdaQueryWrapper<SysRole> duplicateWrapper = new LambdaQueryWrapper<>();
        duplicateWrapper.eq(SysRole::getCompanyId, company.getId())
                .eq(SysRole::getRoleKey, template.getRoleKey());
        // ??????????????????????????
        if (sysRoleMapper.selectCount(duplicateWrapper) > 0) {
            throw new ServiceException("公司【" + company.getCompanyName()
                    + "】已存在相同角色标识（" + template.getRoleKey()
                    + "）的角色，无法按模板补建系统角色");
        }
        SysRole role = buildSystemRole(company.getId(), template);
        // ???????????????????????
        sysRoleMapper.insert(role);
        return role;
    }

    /**
     * ???????
     *
     * @param role ??
     * @param template ??
     * @return true ??????
     */
    private boolean syncRoleBaseInfo(SysRole role, SysRoleTemplate template) {
        boolean changed = false;
        Integer templateRoleType = isAdminTemplate(template) ? 1 : 2;
        Integer templateOrderNum = template.getOrderNum() != null ? template.getOrderNum() : 0;

        if (!Objects.equals(role.getRoleName(), template.getRoleName())) {
            role.setRoleName(template.getRoleName());
            changed = true;
        }
        if (!Objects.equals(role.getDataScope(), template.getDataScope())) {
            role.setDataScope(template.getDataScope());
            changed = true;
        }
        if (!Objects.equals(role.getRoleType(), templateRoleType)) {
            role.setRoleType(templateRoleType);
            changed = true;
        }
        if (!Objects.equals(role.getOrderNum(), templateOrderNum)) {
            role.setOrderNum(templateOrderNum);
            changed = true;
        }
        if (!Objects.equals(role.getRemark(), template.getRemark())) {
            role.setRemark(template.getRemark());
            changed = true;
        }
        if (changed) {
            // ???????????????????????
            sysRoleMapper.updateById(role);
        }
        return changed;
    }

    /**
     * ???????
     *
     * @param roleId ??ID
     * @param templateMenuIds template Menu ID??
     * @param templateMenuIdSet ??
     * @return true ??????
     */
    private boolean syncRoleMenus(Long roleId, List<Long> templateMenuIds, Set<Long> templateMenuIdSet) {
        LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.eq(SysRoleMenu::getRoleId, roleId);
        // ??????????????????????????
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(roleMenuWrapper);
        if (roleMenus == null) {
            roleMenus = Collections.emptyList();
        }

        Set<Long> roleMenuIdSet = new HashSet<>();
        List<Long> toRemove = new ArrayList<>();
        for (SysRoleMenu rm : roleMenus) {
            roleMenuIdSet.add(rm.getMenuId());
            if (!templateMenuIdSet.contains(rm.getMenuId())) {
                toRemove.add(rm.getId());
            }
        }
        for (Long rmId : toRemove) {
            // ???????????????????????
            sysRoleMenuMapper.deleteById(rmId);
        }

        boolean changed = !toRemove.isEmpty();
        for (Long menuId : templateMenuIds) {
            if (!roleMenuIdSet.contains(menuId)) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                sysRoleMenuMapper.insert(rm);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * ???????
     *
     * @param companyId ??ID
     * @param template ??
     * @return ????
     */
    private SysRole buildSystemRole(Long companyId, SysRoleTemplate template) {
        SysRole role = new SysRole();
        role.setCompanyId(companyId);
        role.setRoleName(template.getRoleName());
        role.setRoleKey(template.getRoleKey());
        role.setDataScope(template.getDataScope());
        role.setRoleType(isAdminTemplate(template) ? 1 : 2);
        role.setIsSystem(1);
        role.setStatus(1);
        role.setOrderNum(template.getOrderNum() != null ? template.getOrderNum() : 0);
        role.setRemark(template.getRemark());
        return role;
    }

    /**
     * ????Admin Template?
     *
     * @param template ??
     * @return true ??????
     */
    private boolean isAdminTemplate(SysRoleTemplate template) {
        return template.getIsAdmin() != null && template.getIsAdmin() == 1;
    }

    /**
     * ???????
     *
     * @param typeCode ??????
     * @param isAdmin ??
     * @param excludeId exclude ID
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
        // ??????????????????????????
        if (sysRoleTemplateMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该公司类型下已存在管理员角色模板，每种类型最多一个");
        }
    }

    /**
     * ?? kickAffectedUsers ?????
     *
     * @param roleIds ??ID??
     */
    private void kickAffectedUsers(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUserRole::getRoleId, roleIds);
        // ??????????????????????????
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        if (userRoles == null || userRoles.isEmpty()) {
            return;
        }
        List<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .collect(Collectors.toList());
        for (Long userId : userIds) {
            // ??????????????????????
            sysPermissionService.clearAllPermsCache(userId);
            StpUtil.kickout(userId);
        }
    }
}
