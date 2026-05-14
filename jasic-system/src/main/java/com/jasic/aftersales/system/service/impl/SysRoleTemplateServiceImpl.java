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
     * 系统公司Mapper数据访问接口。
     *
     * @param typeCode 参数
     * @return 处理结果
     */
    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysPermissionService sysPermissionService;

    @Resource
    private SysDataScopeRuleService dataScopeRuleService;

    /**
     * 查询listByTypeCode相关业务数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param typeCode 参数
     * @return 处理结果
     */
    @Override
    public List<SysRoleTemplateVO> listByTypeCode(String typeCode) {
        LambdaQueryWrapper<SysRoleTemplate> wrapper = new LambdaQueryWrapper<>();
        if (typeCode != null && !typeCode.isEmpty()) {
            // 调用eq方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysRoleTemplate::getTypeCode, typeCode);
        }
        // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByAsc(SysRoleTemplate::getOrderNum);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysRoleTemplate> list = sysRoleTemplateMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysRoleTemplateVO> voList = list.stream()
                .map(this::convertToVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        for (SysRoleTemplateVO vo : voList) {
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setMenuIds(loadMenuIdsByTemplateId(vo.getId()));
        }
        return voList;
    }

    /**
     * 根据ID查询角色模板详情。
     *
     * @return 处理结果
     */
    @Override
    public SysRoleTemplateVO getById(Long templateId) {
        // 说明：执行该步骤以保证业务流程正确。
        SysRoleTemplate template = sysRoleTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException("角色模板不存在");
        }
        // 调用convertToVO方法，复用统一能力并保证业务规则一致。
        SysRoleTemplateVO vo = convertToVO(template);
        // 调用loadMenuIdsByTemplateId方法，复用统一能力并保证业务规则一致。
        vo.setMenuIds(loadMenuIdsByTemplateId(templateId));
        return vo;
    }

    /**
     * 新增角色模板。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Override
    public Long save(SysRoleTemplateDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        dataScopeRuleService.validateByTypeCode(dto.getTypeCode(), dto.getDataScope());
        LambdaQueryWrapper<SysRoleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleTemplate::getTypeCode, dto.getTypeCode())
                // 调用getRoleKey方法，复用统一能力并保证业务规则一致。
                .eq(SysRoleTemplate::getRoleKey, dto.getRoleKey());
        // 说明：执行该步骤以保证业务流程正确。
        if (sysRoleTemplateMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该类型下角色标识已存在");
        }
        // 调用getIsAdmin方法，复用统一能力并保证业务规则一致。
        validateAdminUnique(dto.getTypeCode(), dto.getIsAdmin(), null);
        // 调用SysRoleTemplate方法，复用统一能力并保证业务规则一致。
        SysRoleTemplate template = new SysRoleTemplate();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, template);
        if (template.getIsAdmin() == null) {
            // 调用setIsAdmin方法，复用统一能力并保证业务规则一致。
            template.setIsAdmin(0);
        }
        if (template.getOrderNum() == null) {
            // 调用setOrderNum方法，复用统一能力并保证业务规则一致。
            template.setOrderNum(0);
        }
        // 说明：执行该步骤以保证业务流程正确。
        sysRoleTemplateMapper.insert(template);
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            // 调用getMenuIds方法，复用统一能力并保证业务规则一致。
            batchInsertTemplateMenu(template.getId(), dto.getMenuIds());
        }
        return template.getId();
    }

    /**
     * 更新角色模板。
     *
     * @param dto 参数
     */
    @Override
    public void update(SysRoleTemplateDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("模板ID不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysRoleTemplate template = sysRoleTemplateMapper.selectById(dto.getId());
        if (template == null) {
            throw new ServiceException("角色模板不存在");
        }
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        String typeCode = dto.getTypeCode() != null ? dto.getTypeCode() : template.getTypeCode();
        // 说明：执行该步骤以保证业务流程正确。
        validateAdminUnique(typeCode, dto.getIsAdmin(), dto.getId());
        // 调用getDataScope方法，复用统一能力并保证业务规则一致。
        dataScopeRuleService.validateByTypeCode(typeCode, dto.getDataScope());
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, template);
        // 说明：执行该步骤以保证业务流程正确。
        sysRoleTemplateMapper.updateById(template);
        if (dto.getMenuIds() != null) {
            LambdaQueryWrapper<SysRoleTemplateMenu> delWrapper = new LambdaQueryWrapper<>();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            delWrapper.eq(SysRoleTemplateMenu::getTemplateId, template.getId());
            // 调用delete方法，复用统一能力并保证业务规则一致。
            sysRoleTemplateMenuMapper.delete(delWrapper);
            if (!dto.getMenuIds().isEmpty()) {
                // 调用getMenuIds方法，复用统一能力并保证业务规则一致。
                batchInsertTemplateMenu(template.getId(), dto.getMenuIds());
            }
        }
    }

    /**
     * 删除角色模板。
     */
    @Override
    public void remove(Long templateId) {
        LambdaQueryWrapper<SysRoleTemplateMenu> menuWrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        menuWrapper.eq(SysRoleTemplateMenu::getTemplateId, templateId);
        // 说明：执行该步骤以保证业务流程正确。
        sysRoleTemplateMenuMapper.delete(menuWrapper);
        // 调用deleteById方法，复用统一能力并保证业务规则一致。
        sysRoleTemplateMapper.deleteById(templateId);
    }

    /**
     * 同步ToCompanies。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void syncToCompanies(Long templateId) {
        // 说明：执行该步骤以保证业务流程正确。
        SysRoleTemplate template = sysRoleTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException("角色模板不存在");
        }
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        String typeCode = template.getTypeCode();
        // 说明：执行该步骤以保证业务流程正确。
        dataScopeRuleService.validateByTypeCode(typeCode, template.getDataScope());
        // 调用loadMenuIdsByTemplateId方法，复用统一能力并保证业务规则一致。
        List<Long> templateMenuIds = loadMenuIdsByTemplateId(templateId);
        Set<Long> templateMenuIdSet = new HashSet<>(templateMenuIds);

        LambdaQueryWrapper<SysCompany> companyWrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        companyWrapper.eq(SysCompany::getTypeCode, typeCode);
        // 调用selectList方法，复用统一能力并保证业务规则一致。
        List<SysCompany> companies = sysCompanyMapper.selectList(companyWrapper);
        if (companies == null || companies.isEmpty()) {
            return;
        }

        Set<Long> updatedRoleIds = new HashSet<>();
        for (SysCompany company : companies) {
            LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(SysRole::getCompanyId, company.getId())
                    .eq(SysRole::getRoleKey, template.getRoleKey())
                    // 调用eq方法，复用统一能力并保证业务规则一致。
                    .eq(SysRole::getIsSystem, 1);
            // 调用selectOne方法，复用统一能力并保证业务规则一致。
            SysRole role = sysRoleMapper.selectOne(roleWrapper);

            boolean hasChanges;
            if (role == null) {
                // 调用createMissingSystemRole方法，复用统一能力并保证业务规则一致。
                role = createMissingSystemRole(company, template);
                hasChanges = true;
            } else {
                // 调用syncRoleBaseInfo方法，复用统一能力并保证业务规则一致。
                hasChanges = syncRoleBaseInfo(role, template);
            }

            if (syncRoleMenus(role.getId(), templateMenuIds, templateMenuIdSet)) {
                hasChanges = true;
            }
            if (hasChanges) {
                // 调用getId方法，复用统一能力并保证业务规则一致。
                updatedRoleIds.add(role.getId());
            }
        }

        // 调用kickAffectedUsers方法，复用统一能力并保证业务规则一致。
        kickAffectedUsers(updatedRoleIds);
    }

    /**
     * init公司Roles。
     *
     * @param typeCode 参数
     * @return 处理结果
     */
    @Override
    public Long initCompanyRoles(Long companyId, String typeCode) {
        LambdaQueryWrapper<SysRoleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleTemplate::getTypeCode, typeCode)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysRoleTemplate::getOrderNum);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysRoleTemplate> templates = sysRoleTemplateMapper.selectList(wrapper);
        if (templates == null || templates.isEmpty()) {
            return null;
        }

        Long adminRoleId = null;
        for (SysRoleTemplate template : templates) {
            // 说明：执行该步骤以保证业务流程正确。
            dataScopeRuleService.validateByTypeCode(typeCode, template.getDataScope());

            // 调用buildSystemRole方法，复用统一能力并保证业务规则一致。
            SysRole role = buildSystemRole(companyId, template);
            // 说明：执行该步骤以保证业务流程正确。
            sysRoleMapper.insert(role);

            if (isAdminTemplate(template)) {
                // 调用getId方法，复用统一能力并保证业务规则一致。
                adminRoleId = role.getId();
            }

            // 调用getId方法，复用统一能力并保证业务规则一致。
            List<Long> menuIds = loadMenuIdsByTemplateId(template.getId());
            if (menuIds != null && !menuIds.isEmpty()) {
                for (Long menuId : menuIds) {
                    // 调用SysRoleMenu方法，复用统一能力并保证业务规则一致。
                    SysRoleMenu rm = new SysRoleMenu();
                    // 调用getId方法，复用统一能力并保证业务规则一致。
                    rm.setRoleId(role.getId());
                    // 调用setMenuId方法，复用统一能力并保证业务规则一致。
                    rm.setMenuId(menuId);
                    // 调用insert方法，复用统一能力并保证业务规则一致。
                    sysRoleMenuMapper.insert(rm);
                }
            }
        }
        return adminRoleId;
    }

    /**
     * convertTo视图。
     *
     * @param template 参数
     * @return 处理结果
     */
    private SysRoleTemplateVO convertToVO(SysRoleTemplate template) {
        // 调用SysRoleTemplateVO方法，复用统一能力并保证业务规则一致。
        SysRoleTemplateVO vo = new SysRoleTemplateVO();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(template, vo);
        return vo;
    }

    /**
     * load菜单IdsBy模板ID。
     *
     * @return 处理结果
     */
    private List<Long> loadMenuIdsByTemplateId(Long templateId) {
        LambdaQueryWrapper<SysRoleTemplateMenu> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysRoleTemplateMenu::getTemplateId, templateId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysRoleTemplateMenu> list = sysRoleTemplateMenuMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(SysRoleTemplateMenu::getMenuId)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * batch新增模板菜单。
     */
    private void batchInsertTemplateMenu(Long templateId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            // 调用SysRoleTemplateMenu方法，复用统一能力并保证业务规则一致。
            SysRoleTemplateMenu rm = new SysRoleTemplateMenu();
            // 调用setTemplateId方法，复用统一能力并保证业务规则一致。
            rm.setTemplateId(templateId);
            // 调用setMenuId方法，复用统一能力并保证业务规则一致。
            rm.setMenuId(menuId);
            // 说明：执行该步骤以保证业务流程正确。
            sysRoleTemplateMenuMapper.insert(rm);
        }
    }

    /**
     * 创建MissingSystem角色。
     *
     * @param company 参数
     * @param template 参数
     * @return 处理结果
     */
    private SysRole createMissingSystemRole(SysCompany company, SysRoleTemplate template) {
        LambdaQueryWrapper<SysRole> duplicateWrapper = new LambdaQueryWrapper<>();
        duplicateWrapper.eq(SysRole::getCompanyId, company.getId())
                // 调用getRoleKey方法，复用统一能力并保证业务规则一致。
                .eq(SysRole::getRoleKey, template.getRoleKey());
        // 说明：执行该步骤以保证业务流程正确。
        if (sysRoleMapper.selectCount(duplicateWrapper) > 0) {
            throw new ServiceException("公司【" + company.getCompanyName()
                    + "】已存在相同角色标识（" + template.getRoleKey()
                    + "）的角色，无法按模板补建系统角色");
        }
        // 调用getId方法，复用统一能力并保证业务规则一致。
        SysRole role = buildSystemRole(company.getId(), template);
        // 说明：执行该步骤以保证业务流程正确。
        sysRoleMapper.insert(role);
        return role;
    }

    /**
     * 同步角色基础Info。
     *
     * @param role 参数
     * @param template 参数
     */
    private boolean syncRoleBaseInfo(SysRole role, SysRoleTemplate template) {
        boolean changed = false;
        // 调用isAdminTemplate方法，复用统一能力并保证业务规则一致。
        Integer templateRoleType = isAdminTemplate(template) ? 1 : 2;
        // 调用getOrderNum方法，复用统一能力并保证业务规则一致。
        Integer templateOrderNum = template.getOrderNum() != null ? template.getOrderNum() : 0;

        if (!Objects.equals(role.getRoleName(), template.getRoleName())) {
            // 调用getRoleName方法，复用统一能力并保证业务规则一致。
            role.setRoleName(template.getRoleName());
            changed = true;
        }
        if (!Objects.equals(role.getDataScope(), template.getDataScope())) {
            // 调用getDataScope方法，复用统一能力并保证业务规则一致。
            role.setDataScope(template.getDataScope());
            changed = true;
        }
        if (!Objects.equals(role.getRoleType(), templateRoleType)) {
            // 调用setRoleType方法，复用统一能力并保证业务规则一致。
            role.setRoleType(templateRoleType);
            changed = true;
        }
        if (!Objects.equals(role.getOrderNum(), templateOrderNum)) {
            // 调用setOrderNum方法，复用统一能力并保证业务规则一致。
            role.setOrderNum(templateOrderNum);
            changed = true;
        }
        if (!Objects.equals(role.getRemark(), template.getRemark())) {
            // 调用getRemark方法，复用统一能力并保证业务规则一致。
            role.setRemark(template.getRemark());
            changed = true;
        }
        if (changed) {
            // 说明：执行该步骤以保证业务流程正确。
            sysRoleMapper.updateById(role);
        }
        return changed;
    }

    /**
     * 同步角色Menus。
     *
     * @param templateMenuIdSet 参数
     */
    private boolean syncRoleMenus(Long roleId, List<Long> templateMenuIds, Set<Long> templateMenuIdSet) {
        LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        roleMenuWrapper.eq(SysRoleMenu::getRoleId, roleId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(roleMenuWrapper);
        if (roleMenus == null) {
            // 调用emptyList方法，复用统一能力并保证业务规则一致。
            roleMenus = Collections.emptyList();
        }

        Set<Long> roleMenuIdSet = new HashSet<>();
        List<Long> toRemove = new ArrayList<>();
        for (SysRoleMenu rm : roleMenus) {
            // 调用getMenuId方法，复用统一能力并保证业务规则一致。
            roleMenuIdSet.add(rm.getMenuId());
            if (!templateMenuIdSet.contains(rm.getMenuId())) {
                // 调用getId方法，复用统一能力并保证业务规则一致。
                toRemove.add(rm.getId());
            }
        }
        for (Long rmId : toRemove) {
            // 说明：执行该步骤以保证业务流程正确。
            sysRoleMenuMapper.deleteById(rmId);
        }

        // 调用isEmpty方法，复用统一能力并保证业务规则一致。
        boolean changed = !toRemove.isEmpty();
        for (Long menuId : templateMenuIds) {
            if (!roleMenuIdSet.contains(menuId)) {
                // 调用SysRoleMenu方法，复用统一能力并保证业务规则一致。
                SysRoleMenu rm = new SysRoleMenu();
                // 调用setRoleId方法，复用统一能力并保证业务规则一致。
                rm.setRoleId(roleId);
                // 调用setMenuId方法，复用统一能力并保证业务规则一致。
                rm.setMenuId(menuId);
                // 调用insert方法，复用统一能力并保证业务规则一致。
                sysRoleMenuMapper.insert(rm);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 构建System角色。
     *
     * @param template 参数
     * @return 处理结果
     */
    private SysRole buildSystemRole(Long companyId, SysRoleTemplate template) {
        // 调用SysRole方法，复用统一能力并保证业务规则一致。
        SysRole role = new SysRole();
        // 调用setCompanyId方法，复用统一能力并保证业务规则一致。
        role.setCompanyId(companyId);
        // 调用getRoleName方法，复用统一能力并保证业务规则一致。
        role.setRoleName(template.getRoleName());
        // 调用getRoleKey方法，复用统一能力并保证业务规则一致。
        role.setRoleKey(template.getRoleKey());
        // 调用getDataScope方法，复用统一能力并保证业务规则一致。
        role.setDataScope(template.getDataScope());
        // 调用isAdminTemplate方法，复用统一能力并保证业务规则一致。
        role.setRoleType(isAdminTemplate(template) ? 1 : 2);
        // 调用setIsSystem方法，复用统一能力并保证业务规则一致。
        role.setIsSystem(1);
        // 调用setStatus方法，复用统一能力并保证业务规则一致。
        role.setStatus(1);
        // 调用getOrderNum方法，复用统一能力并保证业务规则一致。
        role.setOrderNum(template.getOrderNum() != null ? template.getOrderNum() : 0);
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        role.setRemark(template.getRemark());
        return role;
    }

    /**
     * 判断是否Admin模板。
     *
     * @param template 参数
     */
    private boolean isAdminTemplate(SysRoleTemplate template) {
        return template.getIsAdmin() != null && template.getIsAdmin() == 1;
    }

    /**
     * 校验AdminUnique。
     *
     * @param typeCode 参数
     * @param isAdmin 参数
     * @param excludeId exclude ID
     */
    private void validateAdminUnique(String typeCode, Integer isAdmin, Long excludeId) {
        if (isAdmin == null || isAdmin != 1) {
            return;
        }
        LambdaQueryWrapper<SysRoleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleTemplate::getTypeCode, typeCode)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysRoleTemplate::getIsAdmin, 1);
        if (excludeId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            wrapper.ne(SysRoleTemplate::getId, excludeId);
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (sysRoleTemplateMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该公司类型下已存在管理员角色模板，每种类型最多一个");
        }
    }

    /**
     * kickAffectedUsers。
     */
    private void kickAffectedUsers(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        // 调用in方法，复用统一能力并保证业务规则一致。
        wrapper.in(SysUserRole::getRoleId, roleIds);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        if (userRoles == null || userRoles.isEmpty()) {
            return;
        }
        List<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .distinct()
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        for (Long userId : userIds) {
            // 说明：执行该步骤以保证业务流程正确。
            sysPermissionService.clearAllPermsCache(userId);
            // 调用kickout方法，复用统一能力并保证业务规则一致。
            StpUtil.kickout(userId);
        }
    }
}


