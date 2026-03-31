package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysMenuDTO;
import com.jasic.aftersales.system.domain.dto.SysMenuPublishDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysMenu;
import com.jasic.aftersales.system.domain.entity.SysRole;
import com.jasic.aftersales.system.domain.entity.SysRoleMenu;
import com.jasic.aftersales.system.domain.entity.SysRoleTemplate;
import com.jasic.aftersales.system.domain.entity.SysRoleTemplateMenu;
import com.jasic.aftersales.system.domain.entity.SysTypeCodeMenu;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import com.jasic.aftersales.system.domain.vo.SysMenuPublishOptionsVO;
import com.jasic.aftersales.system.domain.vo.SysMenuPublishResultVO;
import com.jasic.aftersales.system.domain.vo.SysMenuPublishTemplateOptionVO;
import com.jasic.aftersales.system.domain.vo.SysMenuPublishTypeOptionVO;
import com.jasic.aftersales.system.domain.vo.SysMenuVO;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysCompanyTypeMapper;
import com.jasic.aftersales.system.mapper.SysMenuMapper;
import com.jasic.aftersales.system.mapper.SysRoleMapper;
import com.jasic.aftersales.system.mapper.SysRoleMenuMapper;
import com.jasic.aftersales.system.mapper.SysRoleTemplateMapper;
import com.jasic.aftersales.system.mapper.SysRoleTemplateMenuMapper;
import com.jasic.aftersales.system.mapper.SysTypeCodeMenuMapper;
import com.jasic.aftersales.system.mapper.SysUserRoleMapper;
import com.jasic.aftersales.system.service.ISysMenuService;
import com.jasic.aftersales.system.service.SysPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单管理 Service 实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysMenuServiceImpl implements ISysMenuService {

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Resource
    private SysRoleTemplateMenuMapper sysRoleTemplateMenuMapper;

    @Resource
    private SysTypeCodeMenuMapper sysTypeCodeMenuMapper;

    @Resource
    private SysCompanyTypeMapper sysCompanyTypeMapper;

    @Resource
    private SysRoleTemplateMapper sysRoleTemplateMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysPermissionService sysPermissionService;

    /**
     * 根据主体类型查询菜单树
     */
    @Override
    public List<SysMenuVO> listMenuTreeBySubjectType(String subjectType) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getSubjectType, subjectType)
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum);
        List<SysMenu> menus = sysMenuMapper.selectList(wrapper);
        List<SysMenuVO> voList = menus.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return buildMenuTree(voList);
    }

    /**
     * 根据用户ID和公司ID查询菜单树（用于动态路由）
     */
    @Override
    public List<SysMenuVO> listMenuTreeByUser(Long userId, Long companyId) {
        List<SysMenu> menus = sysMenuMapper.selectMenuTreeByUserIdAndCompanyId(userId, companyId);
        if (menus == null || menus.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysMenuVO> voList = menus.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return buildMenuTree(voList);
    }

    /**
     * 根据用户ID和公司ID查询权限标识集合
     */
    @Override
    public Set<String> listPermsByUser(Long userId, Long companyId) {
        Set<String> perms = sysMenuMapper.selectPermsByUserIdAndCompanyId(userId, companyId);
        return perms != null ? perms : Collections.emptySet();
    }

    @Override
    public SysMenu getById(Long menuId) {
        return sysMenuMapper.selectById(menuId);
    }

    @Override
    public Long save(SysMenuDTO dto) {
        SysMenu menu = BeanUtil.copyProperties(dto, SysMenu.class);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        sysMenuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    public void update(SysMenuDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("菜单ID不能为空");
        }
        SysMenu menu = sysMenuMapper.selectById(dto.getId());
        if (menu == null) {
            throw new ServiceException("菜单不存在");
        }
        BeanUtil.copyProperties(dto, menu);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        sysMenuMapper.updateById(menu);
    }

    /**
     * 删除菜单（含子菜单校验、type_code_menu 关联清理）
     */
    @Override
    public void remove(Long menuId) {
        long childCount = sysMenuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
        if (childCount > 0) {
            throw new ServiceException("存在子菜单，不允许删除");
        }
        long roleMenuCount = sysRoleMenuMapper.selectCount(
                new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getMenuId, menuId));
        if (roleMenuCount > 0) {
            throw new ServiceException("菜单已分配给角色，请先取消分配");
        }
        long templateMenuCount = sysRoleTemplateMenuMapper.selectCount(
                new LambdaQueryWrapper<SysRoleTemplateMenu>()
                        .eq(SysRoleTemplateMenu::getMenuId, menuId));
        if (templateMenuCount > 0) {
            throw new ServiceException("菜单已分配给角色模板，请先取消分配");
        }
        sysTypeCodeMenuMapper.delete(
                new LambdaQueryWrapper<SysTypeCodeMenu>()
                        .eq(SysTypeCodeMenu::getMenuId, menuId));
        sysMenuMapper.deleteById(menuId);
    }

    /**
     * 根据主体类型查询菜单列表（平铺，不构建树）
     */
    @Override
    public List<SysMenu> listBySubjectType(String subjectType) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getSubjectType, subjectType)
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum);
        return sysMenuMapper.selectList(wrapper);
    }

    /**
     * 查询公司类型的菜单上限ID列表
     */
    @Override
    public List<Long> listTypeCodeMenuIds(String typeCode) {
        LambdaQueryWrapper<SysTypeCodeMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysTypeCodeMenu::getTypeCode, typeCode);
        List<SysTypeCodeMenu> list = sysTypeCodeMenuMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(SysTypeCodeMenu::getMenuId).collect(Collectors.toList());
    }

    /**
     * 分配公司类型的菜单上限
     */
    @Override
    public void assignTypeCodeMenus(String typeCode, List<Long> menuIds) {
        sysTypeCodeMenuMapper.delete(
                new LambdaQueryWrapper<SysTypeCodeMenu>()
                        .eq(SysTypeCodeMenu::getTypeCode, typeCode));
        if (menuIds != null && !menuIds.isEmpty()) {
            Set<Long> dedup = new LinkedHashSet<>(menuIds);
            for (Long menuId : dedup) {
                SysTypeCodeMenu record = new SysTypeCodeMenu();
                record.setTypeCode(typeCode);
                record.setMenuId(menuId);
                sysTypeCodeMenuMapper.insert(record);
            }
        }
    }

    /**
     * 根据公司类型编码查询已分配的菜单树（用于角色模板/角色分配菜单时展示可选范围）
     */
    @Override
    public List<SysMenuVO> listMenuTreeByTypeCode(String typeCode) {
        List<Long> menuIds = listTypeCodeMenuIds(typeCode);
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum);
        List<SysMenu> menus = sysMenuMapper.selectList(wrapper);
        List<SysMenuVO> voList = menus.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return buildMenuTree(voList);
    }

    /**
     * 查询菜单发布可选项
     */
    @Override
    public SysMenuPublishOptionsVO getPublishOptions(String subjectType) {
        List<SysCompanyType> companyTypes = sysCompanyTypeMapper.selectList(
                new LambdaQueryWrapper<SysCompanyType>()
                        .eq(SysCompanyType::getSubjectType, subjectType)
                        .orderByAsc(SysCompanyType::getOrderNum, SysCompanyType::getId));

        List<String> typeCodes = companyTypes.stream()
                .map(SysCompanyType::getTypeCode)
                .collect(Collectors.toList());

        List<SysRoleTemplate> templates = typeCodes.isEmpty()
                ? Collections.emptyList()
                : sysRoleTemplateMapper.selectList(
                        new LambdaQueryWrapper<SysRoleTemplate>()
                                .in(SysRoleTemplate::getTypeCode, typeCodes)
                                .orderByAsc(SysRoleTemplate::getTypeCode, SysRoleTemplate::getOrderNum, SysRoleTemplate::getId));

        SysMenuPublishOptionsVO vo = new SysMenuPublishOptionsVO();
        vo.setTypeOptions(companyTypes.stream().map(item -> {
            SysMenuPublishTypeOptionVO option = new SysMenuPublishTypeOptionVO();
            option.setTypeCode(item.getTypeCode());
            option.setTypeName(item.getTypeName());
            return option;
        }).collect(Collectors.toList()));
        vo.setTemplateOptions(templates.stream().map(item -> {
            SysMenuPublishTemplateOptionVO option = new SysMenuPublishTemplateOptionVO();
            option.setId(item.getId());
            option.setTypeCode(item.getTypeCode());
            option.setRoleName(item.getRoleName());
            option.setRoleKey(item.getRoleKey());
            option.setIsAdmin(item.getIsAdmin());
            return option;
        }).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 保存并发布菜单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuPublishResultVO publishMenu(SysMenuPublishDTO dto) {
        SysMenuDTO menuDto = dto.getMenu();
        if (menuDto == null) {
            throw new ServiceException("菜单信息不能为空");
        }
        Set<String> targetTypeCodeSet = normalizeTypeCodes(dto.getTargetTypeCodes());
        validateTargetCompanyTypes(menuDto.getSubjectType(), targetTypeCodeSet);
        Set<Long> targetTemplateIdSet = normalizeTemplateIds(dto.getTargetTemplateIds());
        List<SysRoleTemplate> targetTemplates = validateTargetTemplates(targetTemplateIdSet, targetTypeCodeSet);

        Long menuId = menuDto.getId();
        if (menuId == null) {
            menuId = save(menuDto);
        } else {
            update(menuDto);
        }

        int addedTypeCodeCount = ensureTypeCodeMenuRelations(menuId, targetTypeCodeSet);
        int addedTemplateCount = ensureTemplateMenuRelations(menuId, targetTemplateIdSet);

        SyncStats syncStats = SyncStats.empty();
        boolean syncExistingCompanies = dto.getSyncExistingCompanies() == null || dto.getSyncExistingCompanies();
        if (syncExistingCompanies && !targetTemplates.isEmpty()) {
            syncStats = syncMenuToCompanies(menuId, targetTemplates);
        }

        SysMenuPublishResultVO result = new SysMenuPublishResultVO();
        result.setMenuId(menuId);
        result.setAddedTypeCodeCount(addedTypeCodeCount);
        result.setAddedTemplateCount(addedTemplateCount);
        result.setUpdatedRoleCount(syncStats.getUpdatedRoleCount());
        result.setKickedUserCount(syncStats.getKickedUserCount());
        result.setSkippedCompanyCount(syncStats.getSkippedCompanyCount());
        return result;
    }

    private List<SysMenuVO> buildMenuTree(List<SysMenuVO> menus) {
        if (menus == null || menus.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<SysMenuVO>> groupMap = menus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() != null ? m.getParentId() : 0L));
        List<SysMenuVO> topLevel = groupMap.getOrDefault(0L, new ArrayList<>());
        topLevel.forEach(m -> buildChildren(m, groupMap));
        return topLevel;
    }

    private void buildChildren(SysMenuVO parent, Map<Long, List<SysMenuVO>> groupMap) {
        List<SysMenuVO> children = groupMap.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            children.forEach(c -> buildChildren(c, groupMap));
        }
    }

    private SysMenuVO convertToVO(SysMenu menu) {
        return BeanUtil.copyProperties(menu, SysMenuVO.class);
    }

    @Override
    public int copyMenus(String sourceSubjectType, String targetSubjectType, List<Long> menuIds) {
        if (sourceSubjectType.equals(targetSubjectType)) {
            throw new ServiceException("源主体类型与目标主体类型不能相同");
        }
        LambdaQueryWrapper<SysMenu> sourceWrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getSubjectType, sourceSubjectType)
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum);
        List<SysMenu> allSourceMenus = sysMenuMapper.selectList(sourceWrapper);
        if (allSourceMenus == null || allSourceMenus.isEmpty()) {
            throw new ServiceException("源主体下暂无菜单可拷贝");
        }

        Map<Long, SysMenu> idToMenu = new HashMap<>();
        Map<Long, List<SysMenu>> parentToChildren = new HashMap<>();
        for (SysMenu m : allSourceMenus) {
            idToMenu.put(m.getId(), m);
            Long pid = m.getParentId() != null ? m.getParentId() : 0L;
            parentToChildren.computeIfAbsent(pid, k -> new ArrayList<>()).add(m);
        }

        Set<Long> toCopyIds = new HashSet<>();
        if (menuIds == null || menuIds.isEmpty()) {
            toCopyIds.addAll(idToMenu.keySet());
        } else {
            Queue<Long> queue = new LinkedList<>(menuIds);
            while (!queue.isEmpty()) {
                Long id = queue.poll();
                if (id == null || !idToMenu.containsKey(id)) {
                    continue;
                }
                if (toCopyIds.add(id)) {
                    List<SysMenu> children = parentToChildren.get(id);
                    if (children != null) {
                        children.forEach(c -> queue.offer(c.getId()));
                    }
                }
            }
            for (Long id : new ArrayList<>(toCopyIds)) {
                SysMenu m = idToMenu.get(id);
                while (m != null) {
                    Long pid = m.getParentId() != null ? m.getParentId() : 0L;
                    if (pid != 0 && idToMenu.containsKey(pid)) {
                        toCopyIds.add(pid);
                        m = idToMenu.get(pid);
                    } else {
                        break;
                    }
                }
            }
        }
        if (toCopyIds.isEmpty()) {
            throw new ServiceException("没有可拷贝的菜单");
        }

        List<SysMenu> toCopyList = toCopyIds.stream()
                .map(idToMenu::get)
                .filter(m -> m != null)
                .collect(Collectors.toList());
        Map<Long, Integer> depthMap = new HashMap<>();
        for (SysMenu m : toCopyList) {
            Long pid = m.getParentId() != null ? m.getParentId() : 0L;
            int d = (pid == 0 || !toCopyIds.contains(pid)) ? 0 : (depthMap.getOrDefault(pid, 0) + 1);
            depthMap.put(m.getId(), d);
        }
        toCopyList.sort((a, b) -> {
            int da = depthMap.getOrDefault(a.getId(), 0);
            int db = depthMap.getOrDefault(b.getId(), 0);
            if (da != db) {
                return Integer.compare(da, db);
            }
            return Integer.compare(a.getOrderNum() != null ? a.getOrderNum() : 0,
                    b.getOrderNum() != null ? b.getOrderNum() : 0);
        });

        Map<Long, Long> oldIdToNewId = new HashMap<>();
        for (SysMenu src : toCopyList) {
            SysMenu target = new SysMenu();
            target.setSubjectType(targetSubjectType);
            target.setMenuName(src.getMenuName());
            target.setMenuType(src.getMenuType());
            target.setPath(src.getPath());
            target.setComponent(src.getComponent());
            target.setPerms(src.getPerms());
            target.setIcon(src.getIcon());
            target.setOrderNum(src.getOrderNum());
            target.setIsVisible(src.getIsVisible());
            target.setStatus(src.getStatus());
            target.setRemark(src.getRemark());
            Long oldParentId = src.getParentId() != null ? src.getParentId() : 0L;
            Long newParentId = (oldParentId == 0 || !oldIdToNewId.containsKey(oldParentId))
                    ? 0L : oldIdToNewId.get(oldParentId);
            target.setParentId(newParentId);
            sysMenuMapper.insert(target);
            oldIdToNewId.put(src.getId(), target.getId());
        }
        return toCopyList.size();
    }

    private Set<String> normalizeTypeCodes(List<String> targetTypeCodes) {
        if (targetTypeCodes == null || targetTypeCodes.isEmpty()) {
            throw new ServiceException("目标公司类型不能为空");
        }
        Set<String> result = targetTypeCodes.stream()
                .filter(code -> code != null && !code.trim().isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (result.isEmpty()) {
            throw new ServiceException("目标公司类型不能为空");
        }
        return result;
    }

    private Set<Long> normalizeTemplateIds(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Collections.emptySet();
        }
        return templateIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<SysCompanyType> validateTargetCompanyTypes(String subjectType, Set<String> targetTypeCodeSet) {
        List<SysCompanyType> companyTypes = sysCompanyTypeMapper.selectList(
                new LambdaQueryWrapper<SysCompanyType>()
                        .in(SysCompanyType::getTypeCode, targetTypeCodeSet));
        if (companyTypes.size() != targetTypeCodeSet.size()) {
            throw new ServiceException("存在无效的目标公司类型");
        }
        boolean subjectTypeMatched = companyTypes.stream()
                .allMatch(item -> subjectType.equals(item.getSubjectType()));
        if (!subjectTypeMatched) {
            throw new ServiceException("目标公司类型与菜单主体类型不一致");
        }
        return companyTypes;
    }

    private List<SysRoleTemplate> validateTargetTemplates(Set<Long> targetTemplateIdSet, Set<String> targetTypeCodeSet) {
        if (targetTemplateIdSet.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysRoleTemplate> templates = sysRoleTemplateMapper.selectList(
                new LambdaQueryWrapper<SysRoleTemplate>()
                        .in(SysRoleTemplate::getId, targetTemplateIdSet));
        if (templates.size() != targetTemplateIdSet.size()) {
            throw new ServiceException("存在无效的角色模板");
        }
        boolean allMatched = templates.stream()
                .allMatch(item -> targetTypeCodeSet.contains(item.getTypeCode()));
        if (!allMatched) {
            throw new ServiceException("角色模板不属于所选公司类型");
        }
        return templates;
    }

    private int ensureTypeCodeMenuRelations(Long menuId, Set<String> targetTypeCodeSet) {
        if (targetTypeCodeSet.isEmpty()) {
            return 0;
        }
        List<SysTypeCodeMenu> existingList = sysTypeCodeMenuMapper.selectList(
                new LambdaQueryWrapper<SysTypeCodeMenu>()
                        .eq(SysTypeCodeMenu::getMenuId, menuId)
                        .in(SysTypeCodeMenu::getTypeCode, targetTypeCodeSet));
        Set<String> existingTypeCodes = existingList.stream()
                .map(SysTypeCodeMenu::getTypeCode)
                .collect(Collectors.toSet());

        int addedCount = 0;
        for (String typeCode : targetTypeCodeSet) {
            if (existingTypeCodes.contains(typeCode)) {
                continue;
            }
            SysTypeCodeMenu relation = new SysTypeCodeMenu();
            relation.setTypeCode(typeCode);
            relation.setMenuId(menuId);
            sysTypeCodeMenuMapper.insert(relation);
            addedCount++;
        }
        return addedCount;
    }

    private int ensureTemplateMenuRelations(Long menuId, Set<Long> targetTemplateIdSet) {
        if (targetTemplateIdSet.isEmpty()) {
            return 0;
        }
        List<SysRoleTemplateMenu> existingList = sysRoleTemplateMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleTemplateMenu>()
                        .eq(SysRoleTemplateMenu::getMenuId, menuId)
                        .in(SysRoleTemplateMenu::getTemplateId, targetTemplateIdSet));
        Set<Long> existingTemplateIds = existingList.stream()
                .map(SysRoleTemplateMenu::getTemplateId)
                .collect(Collectors.toSet());

        int addedCount = 0;
        for (Long templateId : targetTemplateIdSet) {
            if (existingTemplateIds.contains(templateId)) {
                continue;
            }
            SysRoleTemplateMenu relation = new SysRoleTemplateMenu();
            relation.setTemplateId(templateId);
            relation.setMenuId(menuId);
            sysRoleTemplateMenuMapper.insert(relation);
            addedCount++;
        }
        return addedCount;
    }

    private SyncStats syncMenuToCompanies(Long menuId, List<SysRoleTemplate> templates) {
        Set<String> typeCodes = templates.stream()
                .map(SysRoleTemplate::getTypeCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (typeCodes.isEmpty()) {
            return SyncStats.empty();
        }

        List<SysCompany> companies = sysCompanyMapper.selectList(
                new LambdaQueryWrapper<SysCompany>()
                        .in(SysCompany::getTypeCode, typeCodes));
        if (companies == null || companies.isEmpty()) {
            return SyncStats.empty();
        }
        Map<String, List<SysCompany>> companiesByTypeCode = companies.stream()
                .collect(Collectors.groupingBy(SysCompany::getTypeCode));

        Set<Long> companyIds = companies.stream()
                .map(SysCompany::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> roleKeys = templates.stream()
                .map(SysRoleTemplate::getRoleKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<SysRole> systemRoles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getIsSystem, 1)
                        .in(SysRole::getCompanyId, companyIds)
                        .in(SysRole::getRoleKey, roleKeys));
        Map<String, SysRole> roleMap = systemRoles.stream()
                .collect(Collectors.toMap(
                        role -> buildRoleMapKey(role.getCompanyId(), role.getRoleKey()),
                        role -> role,
                        (left, right) -> left
                ));

        Set<Long> existingRoleIds = systemRoles.stream()
                .map(SysRole::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> roleIdsWithMenu = existingRoleIds.isEmpty()
                ? Collections.emptySet()
                : sysRoleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>()
                                .eq(SysRoleMenu::getMenuId, menuId)
                                .in(SysRoleMenu::getRoleId, existingRoleIds))
                .stream()
                .map(SysRoleMenu::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> updatedRoleIds = new LinkedHashSet<>();
        Set<Long> skippedCompanyIds = new LinkedHashSet<>();
        for (SysRoleTemplate template : templates) {
            List<SysCompany> typeCompanies = companiesByTypeCode.getOrDefault(template.getTypeCode(), Collections.emptyList());
            for (SysCompany company : typeCompanies) {
                SysRole role = roleMap.get(buildRoleMapKey(company.getId(), template.getRoleKey()));
                if (role == null) {
                    skippedCompanyIds.add(company.getId());
                    continue;
                }
                if (roleIdsWithMenu.contains(role.getId())) {
                    continue;
                }
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(role.getId());
                roleMenu.setMenuId(menuId);
                sysRoleMenuMapper.insert(roleMenu);
                roleIdsWithMenu.add(role.getId());
                updatedRoleIds.add(role.getId());
            }
        }

        int kickedUserCount = kickAffectedUsers(updatedRoleIds);
        return new SyncStats(updatedRoleIds.size(), kickedUserCount, skippedCompanyIds.size());
    }

    private int kickAffectedUsers(Set<Long> updatedRoleIds) {
        if (updatedRoleIds == null || updatedRoleIds.isEmpty()) {
            return 0;
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .in(SysUserRole::getRoleId, updatedRoleIds));
        if (userRoles == null || userRoles.isEmpty()) {
            return 0;
        }
        Set<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long userId : userIds) {
            sysPermissionService.clearAllPermsCache(userId);
            StpUtil.kickout(userId);
        }
        return userIds.size();
    }

    private String buildRoleMapKey(Long companyId, String roleKey) {
        return companyId + "#" + roleKey;
    }

    private static class SyncStats {

        private final int updatedRoleCount;
        private final int kickedUserCount;
        private final int skippedCompanyCount;

        private SyncStats(int updatedRoleCount, int kickedUserCount, int skippedCompanyCount) {
            this.updatedRoleCount = updatedRoleCount;
            this.kickedUserCount = kickedUserCount;
            this.skippedCompanyCount = skippedCompanyCount;
        }

        private static SyncStats empty() {
            return new SyncStats(0, 0, 0);
        }

        private int getUpdatedRoleCount() {
            return updatedRoleCount;
        }

        private int getKickedUserCount() {
            return kickedUserCount;
        }

        private int getSkippedCompanyCount() {
            return skippedCompanyCount;
        }
    }
}
