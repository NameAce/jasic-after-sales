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

    /**
     * 系统角色Mapper数据访问接口。
     *
     * @param subjectType 参数
     * @return 处理结果
     */
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
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysMenu> menus = sysMenuMapper.selectList(wrapper);
        List<SysMenuVO> voList = menus.stream()
                .map(this::convertToVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        return buildMenuTree(voList);
    }

    /**
     * 根据用户ID和公司ID查询菜单树（用于动态路由）
     */
    @Override
    public List<SysMenuVO> listMenuTreeByUser(Long userId, Long companyId) {
        // 说明：执行该步骤以保证业务流程正确。
        List<SysMenu> menus = sysMenuMapper.selectMenuTreeByUserIdAndCompanyId(userId, companyId);
        if (menus == null || menus.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysMenuVO> voList = menus.stream()
                .map(this::convertToVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        return buildMenuTree(voList);
    }

    /**
     * 根据用户ID和公司ID查询权限标识集合
     */
    @Override
    public Set<String> listPermsByUser(Long userId, Long companyId) {
        // 调用selectPermsByUserIdAndCompanyId方法，复用统一能力并保证业务规则一致。
        Set<String> perms = sysMenuMapper.selectPermsByUserIdAndCompanyId(userId, companyId);
        return perms != null ? perms : Collections.emptySet();
    }

    /**
     * 根据ID查询菜单详情。
     *
     * @return 处理结果
     */
    @Override
    public SysMenu getById(Long menuId) {
        return sysMenuMapper.selectById(menuId);
    }

    /**
     * 新增菜单。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Override
    public Long save(SysMenuDTO dto) {
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        SysMenu menu = BeanUtil.copyProperties(dto, SysMenu.class);
        if (menu.getParentId() == null) {
            // 调用setParentId方法，复用统一能力并保证业务规则一致。
            menu.setParentId(0L);
        }
        // 说明：执行该步骤以保证业务流程正确。
        sysMenuMapper.insert(menu);
        return menu.getId();
    }

    /**
     * 更新菜单。
     *
     * @param dto 参数
     */
    @Override
    public void update(SysMenuDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("菜单ID不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysMenu menu = sysMenuMapper.selectById(dto.getId());
        if (menu == null) {
            throw new ServiceException("菜单不存在");
        }
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, menu);
        if (menu.getParentId() == null) {
            // 调用setParentId方法，复用统一能力并保证业务规则一致。
            menu.setParentId(0L);
        }
        // 说明：执行该步骤以保证业务流程正确。
        sysMenuMapper.updateById(menu);
    }

    /**
     * 删除菜单（含子菜单校验、type_code_menu 关联清理）
     */
    @Override
    public void remove(Long menuId) {
        // 说明：执行该步骤以保证业务流程正确。
        long childCount = sysMenuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
        if (childCount > 0) {
            throw new ServiceException("存在子菜单，不允许删除");
        }
        long roleMenuCount = sysRoleMenuMapper.selectCount(
                new LambdaQueryWrapper<SysRoleMenu>()
                        // 调用eq方法，复用统一能力并保证业务规则一致。
                        .eq(SysRoleMenu::getMenuId, menuId));
        if (roleMenuCount > 0) {
            throw new ServiceException("菜单已分配给角色，请先取消分配");
        }
        long templateMenuCount = sysRoleTemplateMenuMapper.selectCount(
                new LambdaQueryWrapper<SysRoleTemplateMenu>()
                        // 调用eq方法，复用统一能力并保证业务规则一致。
                        .eq(SysRoleTemplateMenu::getMenuId, menuId));
        if (templateMenuCount > 0) {
            throw new ServiceException("菜单已分配给角色模板，请先取消分配");
        }
        // 说明：执行该步骤以保证业务流程正确。
        sysTypeCodeMenuMapper.delete(
                new LambdaQueryWrapper<SysTypeCodeMenu>()
                        // 调用eq方法，复用统一能力并保证业务规则一致。
                        .eq(SysTypeCodeMenu::getMenuId, menuId));
        // 调用deleteById方法，复用统一能力并保证业务规则一致。
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
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum);
        // 说明：执行该步骤以保证业务流程正确。
        return sysMenuMapper.selectList(wrapper);
    }

    /**
     * 查询公司类型的菜单上限ID列表
     */
    @Override
    public List<Long> listTypeCodeMenuIds(String typeCode) {
        LambdaQueryWrapper<SysTypeCodeMenu> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysTypeCodeMenu::getTypeCode, typeCode);
        // 说明：执行该步骤以保证业务流程正确。
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
        // 说明：执行该步骤以保证业务流程正确。
        sysTypeCodeMenuMapper.delete(
                new LambdaQueryWrapper<SysTypeCodeMenu>()
                        // 调用eq方法，复用统一能力并保证业务规则一致。
                        .eq(SysTypeCodeMenu::getTypeCode, typeCode));
        if (menuIds != null && !menuIds.isEmpty()) {
            Set<Long> dedup = new LinkedHashSet<>(menuIds);
            for (Long menuId : dedup) {
                // 调用SysTypeCodeMenu方法，复用统一能力并保证业务规则一致。
                SysTypeCodeMenu record = new SysTypeCodeMenu();
                // 调用setTypeCode方法，复用统一能力并保证业务规则一致。
                record.setTypeCode(typeCode);
                // 调用setMenuId方法，复用统一能力并保证业务规则一致。
                record.setMenuId(menuId);
                // 调用insert方法，复用统一能力并保证业务规则一致。
                sysTypeCodeMenuMapper.insert(record);
            }
        }
    }

    /**
     * 根据公司类型编码查询已分配的菜单树（用于角色模板/角色分配菜单时展示可选范围）
     */
    @Override
    public List<SysMenuVO> listMenuTreeByTypeCode(String typeCode) {
        // 调用listTypeCodeMenuIds方法，复用统一能力并保证业务规则一致。
        List<Long> menuIds = listTypeCodeMenuIds(typeCode);
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, 1)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysMenu> menus = sysMenuMapper.selectList(wrapper);
        List<SysMenuVO> voList = menus.stream()
                .map(this::convertToVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        return buildMenuTree(voList);
    }

    /**
     * 查询菜单发布可选项
     */
    @Override
    public SysMenuPublishOptionsVO getPublishOptions(String subjectType) {
        // 说明：执行该步骤以保证业务流程正确。
        List<SysCompanyType> companyTypes = sysCompanyTypeMapper.selectList(
                new LambdaQueryWrapper<SysCompanyType>()
                        .eq(SysCompanyType::getSubjectType, subjectType)
                        // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                        .orderByAsc(SysCompanyType::getOrderNum, SysCompanyType::getId));

        List<String> typeCodes = companyTypes.stream()
                .map(SysCompanyType::getTypeCode)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());

        List<SysRoleTemplate> templates = typeCodes.isEmpty()
                ? Collections.emptyList()
                : sysRoleTemplateMapper.selectList(
                        new LambdaQueryWrapper<SysRoleTemplate>()
                                .in(SysRoleTemplate::getTypeCode, typeCodes)
                                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                                .orderByAsc(SysRoleTemplate::getTypeCode, SysRoleTemplate::getOrderNum, SysRoleTemplate::getId));

        // 调用SysMenuPublishOptionsVO方法，复用统一能力并保证业务规则一致。
        SysMenuPublishOptionsVO vo = new SysMenuPublishOptionsVO();
        vo.setTypeOptions(companyTypes.stream().map(item -> {
            // 调用SysMenuPublishTypeOptionVO方法，复用统一能力并保证业务规则一致。
            SysMenuPublishTypeOptionVO option = new SysMenuPublishTypeOptionVO();
            // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
            option.setTypeCode(item.getTypeCode());
            // 调用getTypeName方法，复用统一能力并保证业务规则一致。
            option.setTypeName(item.getTypeName());
            return option;
        // 调用toList方法，复用统一能力并保证业务规则一致。
        }).collect(Collectors.toList()));
        vo.setTemplateOptions(templates.stream().map(item -> {
            // 调用SysMenuPublishTemplateOptionVO方法，复用统一能力并保证业务规则一致。
            SysMenuPublishTemplateOptionVO option = new SysMenuPublishTemplateOptionVO();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            option.setId(item.getId());
            // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
            option.setTypeCode(item.getTypeCode());
            // 调用getRoleName方法，复用统一能力并保证业务规则一致。
            option.setRoleName(item.getRoleName());
            // 调用getRoleKey方法，复用统一能力并保证业务规则一致。
            option.setRoleKey(item.getRoleKey());
            // 调用getIsAdmin方法，复用统一能力并保证业务规则一致。
            option.setIsAdmin(item.getIsAdmin());
            return option;
        // 调用toList方法，复用统一能力并保证业务规则一致。
        }).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 保存并发布菜单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuPublishResultVO publishMenu(SysMenuPublishDTO dto) {
        // 调用getMenu方法，复用统一能力并保证业务规则一致。
        SysMenuDTO menuDto = dto.getMenu();
        if (menuDto == null) {
            throw new ServiceException("菜单信息不能为空");
        }
        // 调用getTargetTypeCodes方法，复用统一能力并保证业务规则一致。
        Set<String> targetTypeCodeSet = normalizeTypeCodes(dto.getTargetTypeCodes());
        // 说明：执行该步骤以保证业务流程正确。
        validateTargetCompanyTypes(menuDto.getSubjectType(), targetTypeCodeSet);
        // 调用getTargetTemplateIds方法，复用统一能力并保证业务规则一致。
        Set<Long> targetTemplateIdSet = normalizeTemplateIds(dto.getTargetTemplateIds());
        // 调用validateTargetTemplates方法，复用统一能力并保证业务规则一致。
        List<SysRoleTemplate> targetTemplates = validateTargetTemplates(targetTemplateIdSet, targetTypeCodeSet);

        // 调用getId方法，复用统一能力并保证业务规则一致。
        Long menuId = menuDto.getId();
        if (menuId == null) {
            // 调用save方法，复用统一能力并保证业务规则一致。
            menuId = save(menuDto);
        } else {
            // 调用update方法，复用统一能力并保证业务规则一致。
            update(menuDto);
        }

        // 调用ensureTypeCodeMenuRelations方法，复用统一能力并保证业务规则一致。
        int addedTypeCodeCount = ensureTypeCodeMenuRelations(menuId, targetTypeCodeSet);
        // 调用ensureTemplateMenuRelations方法，复用统一能力并保证业务规则一致。
        int addedTemplateCount = ensureTemplateMenuRelations(menuId, targetTemplateIdSet);

        // 调用empty方法，复用统一能力并保证业务规则一致。
        SyncStats syncStats = SyncStats.empty();
        // 调用getSyncExistingCompanies方法，复用统一能力并保证业务规则一致。
        boolean syncExistingCompanies = dto.getSyncExistingCompanies() == null || dto.getSyncExistingCompanies();
        if (syncExistingCompanies && !targetTemplates.isEmpty()) {
            // 调用syncMenuToCompanies方法，复用统一能力并保证业务规则一致。
            syncStats = syncMenuToCompanies(menuId, targetTemplates);
        }

        // 调用SysMenuPublishResultVO方法，复用统一能力并保证业务规则一致。
        SysMenuPublishResultVO result = new SysMenuPublishResultVO();
        // 调用setMenuId方法，复用统一能力并保证业务规则一致。
        result.setMenuId(menuId);
        // 调用setAddedTypeCodeCount方法，复用统一能力并保证业务规则一致。
        result.setAddedTypeCodeCount(addedTypeCodeCount);
        // 调用setAddedTemplateCount方法，复用统一能力并保证业务规则一致。
        result.setAddedTemplateCount(addedTemplateCount);
        // 调用getUpdatedRoleCount方法，复用统一能力并保证业务规则一致。
        result.setUpdatedRoleCount(syncStats.getUpdatedRoleCount());
        // 调用getKickedUserCount方法，复用统一能力并保证业务规则一致。
        result.setKickedUserCount(syncStats.getKickedUserCount());
        // 调用getSkippedCompanyCount方法，复用统一能力并保证业务规则一致。
        result.setSkippedCompanyCount(syncStats.getSkippedCompanyCount());
        return result;
    }

    /**
     * 构建菜单Tree。
     *
     * @param menus 参数
     * @return 处理结果
     */
    private List<SysMenuVO> buildMenuTree(List<SysMenuVO> menus) {
        if (menus == null || menus.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<SysMenuVO>> groupMap = menus.stream()
                // 调用getParentId方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.groupingBy(m -> m.getParentId() != null ? m.getParentId() : 0L));
        // 调用getOrDefault方法，复用统一能力并保证业务规则一致。
        List<SysMenuVO> topLevel = groupMap.getOrDefault(0L, new ArrayList<>());
        // 调用buildChildren方法，复用统一能力并保证业务规则一致。
        topLevel.forEach(m -> buildChildren(m, groupMap));
        return topLevel;
    }

    /**
     * 构建Children。
     *
     * @param parent 参数
     * @param groupMap 参数
     */
    private void buildChildren(SysMenuVO parent, Map<Long, List<SysMenuVO>> groupMap) {
        // 调用getId方法，复用统一能力并保证业务规则一致。
        List<SysMenuVO> children = groupMap.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            // 调用setChildren方法，复用统一能力并保证业务规则一致。
            parent.setChildren(children);
            // 调用buildChildren方法，复用统一能力并保证业务规则一致。
            children.forEach(c -> buildChildren(c, groupMap));
        }
    }

    /**
     * convertTo视图。
     *
     * @param menu 参数
     * @return 处理结果
     */
    private SysMenuVO convertToVO(SysMenu menu) {
        return BeanUtil.copyProperties(menu, SysMenuVO.class);
    }

    /**
     * copyMenus。
     *
     * @param sourceSubjectType 参数
     * @param targetSubjectType 参数
     * @return 处理结果
     */
    @Override
    public int copyMenus(String sourceSubjectType, String targetSubjectType, List<Long> menuIds) {
        if (sourceSubjectType.equals(targetSubjectType)) {
            throw new ServiceException("源主体类型与目标主体类型不能相同");
        }
        LambdaQueryWrapper<SysMenu> sourceWrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getSubjectType, sourceSubjectType)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysMenu> allSourceMenus = sysMenuMapper.selectList(sourceWrapper);
        if (allSourceMenus == null || allSourceMenus.isEmpty()) {
            throw new ServiceException("源主体下暂无菜单可拷贝");
        }

        Map<Long, SysMenu> idToMenu = new HashMap<>();
        Map<Long, List<SysMenu>> parentToChildren = new HashMap<>();
        for (SysMenu m : allSourceMenus) {
            // 调用getId方法，复用统一能力并保证业务规则一致。
            idToMenu.put(m.getId(), m);
            // 调用getParentId方法，复用统一能力并保证业务规则一致。
            Long pid = m.getParentId() != null ? m.getParentId() : 0L;
            // 调用add方法，复用统一能力并保证业务规则一致。
            parentToChildren.computeIfAbsent(pid, k -> new ArrayList<>()).add(m);
        }

        Set<Long> toCopyIds = new HashSet<>();
        if (menuIds == null || menuIds.isEmpty()) {
            // 调用keySet方法，复用统一能力并保证业务规则一致。
            toCopyIds.addAll(idToMenu.keySet());
        } else {
            Queue<Long> queue = new LinkedList<>(menuIds);
            while (!queue.isEmpty()) {
                // 调用poll方法，复用统一能力并保证业务规则一致。
                Long id = queue.poll();
                if (id == null || !idToMenu.containsKey(id)) {
                    continue;
                }
                if (toCopyIds.add(id)) {
                    // 调用get方法，复用统一能力并保证业务规则一致。
                    List<SysMenu> children = parentToChildren.get(id);
                    if (children != null) {
                        // 调用getId方法，复用统一能力并保证业务规则一致。
                        children.forEach(c -> queue.offer(c.getId()));
                    }
                }
            }
            for (Long id : new ArrayList<>(toCopyIds)) {
                // 调用get方法，复用统一能力并保证业务规则一致。
                SysMenu m = idToMenu.get(id);
                while (m != null) {
                    // 调用getParentId方法，复用统一能力并保证业务规则一致。
                    Long pid = m.getParentId() != null ? m.getParentId() : 0L;
                    if (pid != 0 && idToMenu.containsKey(pid)) {
                        // 调用add方法，复用统一能力并保证业务规则一致。
                        toCopyIds.add(pid);
                        // 调用get方法，复用统一能力并保证业务规则一致。
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
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        Map<Long, Integer> depthMap = new HashMap<>();
        for (SysMenu m : toCopyList) {
            // 调用getParentId方法，复用统一能力并保证业务规则一致。
            Long pid = m.getParentId() != null ? m.getParentId() : 0L;
            // 调用getOrDefault方法，复用统一能力并保证业务规则一致。
            int d = (pid == 0 || !toCopyIds.contains(pid)) ? 0 : (depthMap.getOrDefault(pid, 0) + 1);
            // 调用getId方法，复用统一能力并保证业务规则一致。
            depthMap.put(m.getId(), d);
        }
        toCopyList.sort((a, b) -> {
            // 调用getId方法，复用统一能力并保证业务规则一致。
            int da = depthMap.getOrDefault(a.getId(), 0);
            // 调用getId方法，复用统一能力并保证业务规则一致。
            int db = depthMap.getOrDefault(b.getId(), 0);
            if (da != db) {
                return Integer.compare(da, db);
            }
            return Integer.compare(a.getOrderNum() != null ? a.getOrderNum() : 0,
                    // 调用getOrderNum方法，复用统一能力并保证业务规则一致。
                    b.getOrderNum() != null ? b.getOrderNum() : 0);
        });

        Map<Long, Long> oldIdToNewId = new HashMap<>();
        for (SysMenu src : toCopyList) {
            // 调用SysMenu方法，复用统一能力并保证业务规则一致。
            SysMenu target = new SysMenu();
            // 调用setSubjectType方法，复用统一能力并保证业务规则一致。
            target.setSubjectType(targetSubjectType);
            // 调用getMenuName方法，复用统一能力并保证业务规则一致。
            target.setMenuName(src.getMenuName());
            // 调用getMenuType方法，复用统一能力并保证业务规则一致。
            target.setMenuType(src.getMenuType());
            // 调用getPath方法，复用统一能力并保证业务规则一致。
            target.setPath(src.getPath());
            // 调用getComponent方法，复用统一能力并保证业务规则一致。
            target.setComponent(src.getComponent());
            // 调用getPerms方法，复用统一能力并保证业务规则一致。
            target.setPerms(src.getPerms());
            // 调用getIcon方法，复用统一能力并保证业务规则一致。
            target.setIcon(src.getIcon());
            // 调用getOrderNum方法，复用统一能力并保证业务规则一致。
            target.setOrderNum(src.getOrderNum());
            // 调用getIsVisible方法，复用统一能力并保证业务规则一致。
            target.setIsVisible(src.getIsVisible());
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            target.setStatus(src.getStatus());
            // 调用getRemark方法，复用统一能力并保证业务规则一致。
            target.setRemark(src.getRemark());
            // 调用getParentId方法，复用统一能力并保证业务规则一致。
            Long oldParentId = src.getParentId() != null ? src.getParentId() : 0L;
            Long newParentId = (oldParentId == 0 || !oldIdToNewId.containsKey(oldParentId))
                    // 调用get方法，复用统一能力并保证业务规则一致。
                    ? 0L : oldIdToNewId.get(oldParentId);
            // 调用setParentId方法，复用统一能力并保证业务规则一致。
            target.setParentId(newParentId);
            // 说明：执行该步骤以保证业务流程正确。
            sysMenuMapper.insert(target);
            // 调用getId方法，复用统一能力并保证业务规则一致。
            oldIdToNewId.put(src.getId(), target.getId());
        }
        return toCopyList.size();
    }

    /**
     * 规范化类型Codes。
     *
     * @param targetTypeCodes 参数
     * @return 处理结果
     */
    private Set<String> normalizeTypeCodes(List<String> targetTypeCodes) {
        if (targetTypeCodes == null || targetTypeCodes.isEmpty()) {
            throw new ServiceException("目标公司类型不能为空");
        }
        Set<String> result = targetTypeCodes.stream()
                .filter(code -> code != null && !code.trim().isEmpty())
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (result.isEmpty()) {
            throw new ServiceException("目标公司类型不能为空");
        }
        return result;
    }

    /**
     * 规范化模板Ids。
     *
     * @return 处理结果
     */
    private Set<Long> normalizeTemplateIds(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Collections.emptySet();
        }
        return templateIds.stream()
                .filter(id -> id != null)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 校验Target公司Types。
     *
     * @param subjectType 参数
     * @param targetTypeCodeSet 参数
     * @return 处理结果
     */
    private List<SysCompanyType> validateTargetCompanyTypes(String subjectType, Set<String> targetTypeCodeSet) {
        // 说明：执行该步骤以保证业务流程正确。
        List<SysCompanyType> companyTypes = sysCompanyTypeMapper.selectList(
                new LambdaQueryWrapper<SysCompanyType>()
                        // 调用in方法，复用统一能力并保证业务规则一致。
                        .in(SysCompanyType::getTypeCode, targetTypeCodeSet));
        if (companyTypes.size() != targetTypeCodeSet.size()) {
            throw new ServiceException("存在无效的目标公司类型");
        }
        boolean subjectTypeMatched = companyTypes.stream()
                // 调用getSubjectType方法，复用统一能力并保证业务规则一致。
                .allMatch(item -> subjectType.equals(item.getSubjectType()));
        if (!subjectTypeMatched) {
            throw new ServiceException("目标公司类型与菜单主体类型不一致");
        }
        return companyTypes;
    }

    /**
     * 校验TargetTemplates。
     *
     * @param targetTemplateIdSet 参数
     * @param targetTypeCodeSet 参数
     * @return 处理结果
     */
    private List<SysRoleTemplate> validateTargetTemplates(Set<Long> targetTemplateIdSet, Set<String> targetTypeCodeSet) {
        if (targetTemplateIdSet.isEmpty()) {
            return Collections.emptyList();
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<SysRoleTemplate> templates = sysRoleTemplateMapper.selectList(
                new LambdaQueryWrapper<SysRoleTemplate>()
                        // 调用in方法，复用统一能力并保证业务规则一致。
                        .in(SysRoleTemplate::getId, targetTemplateIdSet));
        if (templates.size() != targetTemplateIdSet.size()) {
            throw new ServiceException("存在无效的角色模板");
        }
        boolean allMatched = templates.stream()
                // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
                .allMatch(item -> targetTypeCodeSet.contains(item.getTypeCode()));
        if (!allMatched) {
            throw new ServiceException("角色模板不属于所选公司类型");
        }
        return templates;
    }

    /**
     * ensure类型编码菜单Relations。
     *
     * @param targetTypeCodeSet 参数
     * @return 处理结果
     */
    private int ensureTypeCodeMenuRelations(Long menuId, Set<String> targetTypeCodeSet) {
        if (targetTypeCodeSet.isEmpty()) {
            return 0;
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<SysTypeCodeMenu> existingList = sysTypeCodeMenuMapper.selectList(
                new LambdaQueryWrapper<SysTypeCodeMenu>()
                        .eq(SysTypeCodeMenu::getMenuId, menuId)
                        // 调用in方法，复用统一能力并保证业务规则一致。
                        .in(SysTypeCodeMenu::getTypeCode, targetTypeCodeSet));
        Set<String> existingTypeCodes = existingList.stream()
                .map(SysTypeCodeMenu::getTypeCode)
                // 调用toSet方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toSet());

        int addedCount = 0;
        for (String typeCode : targetTypeCodeSet) {
            if (existingTypeCodes.contains(typeCode)) {
                continue;
            }
            // 调用SysTypeCodeMenu方法，复用统一能力并保证业务规则一致。
            SysTypeCodeMenu relation = new SysTypeCodeMenu();
            // 调用setTypeCode方法，复用统一能力并保证业务规则一致。
            relation.setTypeCode(typeCode);
            // 调用setMenuId方法，复用统一能力并保证业务规则一致。
            relation.setMenuId(menuId);
            // 说明：执行该步骤以保证业务流程正确。
            sysTypeCodeMenuMapper.insert(relation);
            addedCount++;
        }
        return addedCount;
    }

    /**
     * ensure模板菜单Relations。
     *
     * @param targetTemplateIdSet 参数
     * @return 处理结果
     */
    private int ensureTemplateMenuRelations(Long menuId, Set<Long> targetTemplateIdSet) {
        if (targetTemplateIdSet.isEmpty()) {
            return 0;
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<SysRoleTemplateMenu> existingList = sysRoleTemplateMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleTemplateMenu>()
                        .eq(SysRoleTemplateMenu::getMenuId, menuId)
                        // 调用in方法，复用统一能力并保证业务规则一致。
                        .in(SysRoleTemplateMenu::getTemplateId, targetTemplateIdSet));
        Set<Long> existingTemplateIds = existingList.stream()
                .map(SysRoleTemplateMenu::getTemplateId)
                // 调用toSet方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toSet());

        int addedCount = 0;
        for (Long templateId : targetTemplateIdSet) {
            if (existingTemplateIds.contains(templateId)) {
                continue;
            }
            // 调用SysRoleTemplateMenu方法，复用统一能力并保证业务规则一致。
            SysRoleTemplateMenu relation = new SysRoleTemplateMenu();
            // 调用setTemplateId方法，复用统一能力并保证业务规则一致。
            relation.setTemplateId(templateId);
            // 调用setMenuId方法，复用统一能力并保证业务规则一致。
            relation.setMenuId(menuId);
            // 说明：执行该步骤以保证业务流程正确。
            sysRoleTemplateMenuMapper.insert(relation);
            addedCount++;
        }
        return addedCount;
    }

    /**
     * 同步菜单ToCompanies。
     *
     * @param templates 参数
     * @return 处理结果
     */
    private SyncStats syncMenuToCompanies(Long menuId, List<SysRoleTemplate> templates) {
        Set<String> typeCodes = templates.stream()
                .map(SysRoleTemplate::getTypeCode)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (typeCodes.isEmpty()) {
            return SyncStats.empty();
        }

        // 说明：执行该步骤以保证业务流程正确。
        List<SysCompany> companies = sysCompanyMapper.selectList(
                new LambdaQueryWrapper<SysCompany>()
                        // 调用in方法，复用统一能力并保证业务规则一致。
                        .in(SysCompany::getTypeCode, typeCodes));
        if (companies == null || companies.isEmpty()) {
            return SyncStats.empty();
        }
        Map<String, List<SysCompany>> companiesByTypeCode = companies.stream()
                // 调用groupingBy方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.groupingBy(SysCompany::getTypeCode));

        Set<Long> companyIds = companies.stream()
                .map(SysCompany::getId)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> roleKeys = templates.stream()
                .map(SysRoleTemplate::getRoleKey)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<SysRole> systemRoles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getIsSystem, 1)
                        .in(SysRole::getCompanyId, companyIds)
                        // 调用in方法，复用统一能力并保证业务规则一致。
                        .in(SysRole::getRoleKey, roleKeys));
        Map<String, SysRole> roleMap = systemRoles.stream()
                .collect(Collectors.toMap(
                        role -> buildRoleMapKey(role.getCompanyId(), role.getRoleKey()),
                        role -> role,
                        (left, right) -> left
                ));

        Set<Long> existingRoleIds = systemRoles.stream()
                .map(SysRole::getId)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> roleIdsWithMenu = existingRoleIds.isEmpty()
                ? Collections.emptySet()
                : sysRoleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>()
                                .eq(SysRoleMenu::getMenuId, menuId)
                                .in(SysRoleMenu::getRoleId, existingRoleIds))
                .stream()
                .map(SysRoleMenu::getRoleId)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> updatedRoleIds = new LinkedHashSet<>();
        Set<Long> skippedCompanyIds = new LinkedHashSet<>();
        for (SysRoleTemplate template : templates) {
            // 调用emptyList方法，复用统一能力并保证业务规则一致。
            List<SysCompany> typeCompanies = companiesByTypeCode.getOrDefault(template.getTypeCode(), Collections.emptyList());
            for (SysCompany company : typeCompanies) {
                // 调用getRoleKey方法，复用统一能力并保证业务规则一致。
                SysRole role = roleMap.get(buildRoleMapKey(company.getId(), template.getRoleKey()));
                if (role == null) {
                    // 调用getId方法，复用统一能力并保证业务规则一致。
                    skippedCompanyIds.add(company.getId());
                    continue;
                }
                if (roleIdsWithMenu.contains(role.getId())) {
                    continue;
                }
                // 调用SysRoleMenu方法，复用统一能力并保证业务规则一致。
                SysRoleMenu roleMenu = new SysRoleMenu();
                // 调用getId方法，复用统一能力并保证业务规则一致。
                roleMenu.setRoleId(role.getId());
                // 调用setMenuId方法，复用统一能力并保证业务规则一致。
                roleMenu.setMenuId(menuId);
                // 说明：执行该步骤以保证业务流程正确。
                sysRoleMenuMapper.insert(roleMenu);
                // 调用getId方法，复用统一能力并保证业务规则一致。
                roleIdsWithMenu.add(role.getId());
                // 调用getId方法，复用统一能力并保证业务规则一致。
                updatedRoleIds.add(role.getId());
            }
        }

        // 调用kickAffectedUsers方法，复用统一能力并保证业务规则一致。
        int kickedUserCount = kickAffectedUsers(updatedRoleIds);
        return new SyncStats(updatedRoleIds.size(), kickedUserCount, skippedCompanyIds.size());
    }

    /**
     * kickAffectedUsers。
     *
     * @return 处理结果
     */
    private int kickAffectedUsers(Set<Long> updatedRoleIds) {
        if (updatedRoleIds == null || updatedRoleIds.isEmpty()) {
            return 0;
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        // 调用in方法，复用统一能力并保证业务规则一致。
                        .in(SysUserRole::getRoleId, updatedRoleIds));
        if (userRoles == null || userRoles.isEmpty()) {
            return 0;
        }
        Set<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long userId : userIds) {
            // 说明：执行该步骤以保证业务流程正确。
            sysPermissionService.clearAllPermsCache(userId);
            // 调用kickout方法，复用统一能力并保证业务规则一致。
            StpUtil.kickout(userId);
        }
        return userIds.size();
    }

    /**
     * 构建角色MapKey。
     *
     * @param roleKey 参数
     * @return 处理结果
     */
    private String buildRoleMapKey(Long companyId, String roleKey) {
        return companyId + "#" + roleKey;
    }

    private static class SyncStats {

        /**
     * int字段。
     *
     * @param updatedRoleCount 参数
     * @param kickedUserCount 参数
     * @param skippedCompanyCount 参数
     * @return 处理结果
         */
        private final int updatedRoleCount;
        private final int kickedUserCount;
        private final int skippedCompanyCount;

        /**
     * 构造系统菜单实例。
     *
     * @param updatedRoleCount 参数
     * @param kickedUserCount 参数
     * @param skippedCompanyCount 参数
     * @return 处理结果
         */
        private SyncStats(int updatedRoleCount, int kickedUserCount, int skippedCompanyCount) {
            this.updatedRoleCount = updatedRoleCount;
            this.kickedUserCount = kickedUserCount;
            this.skippedCompanyCount = skippedCompanyCount;
        }

        /**
     * empty。
     *
     * @return 处理结果
         */
        private static SyncStats empty() {
            return new SyncStats(0, 0, 0);
        }

        /**
     * 获取Updated角色Count。
     *
     * @return 处理结果
         */
        private int getUpdatedRoleCount() {
            return updatedRoleCount;
        }

        /**
     * 获取Kicked用户Count。
     *
     * @return 处理结果
         */
        private int getKickedUserCount() {
            return kickedUserCount;
        }

        /**
     * 获取Skipped公司Count。
     *
     * @return 处理结果
         */
        private int getSkippedCompanyCount() {
            return skippedCompanyCount;
        }
    }
}


