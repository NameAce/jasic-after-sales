package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysMenuDTO;
import com.jasic.aftersales.system.domain.entity.SysMenu;
import com.jasic.aftersales.system.domain.entity.SysRoleMenu;
import com.jasic.aftersales.system.domain.entity.SysRoleTemplateMenu;
import com.jasic.aftersales.system.domain.entity.SysTypeCodeMenu;
import com.jasic.aftersales.system.domain.vo.SysMenuVO;
import com.jasic.aftersales.system.mapper.SysMenuMapper;
import com.jasic.aftersales.system.mapper.SysRoleMenuMapper;
import com.jasic.aftersales.system.mapper.SysRoleTemplateMenuMapper;
import com.jasic.aftersales.system.mapper.SysTypeCodeMenuMapper;
import com.jasic.aftersales.system.service.ISysMenuService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
        // 清理公司类型菜单上限关联
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
        // 先删除旧的关联
        sysTypeCodeMenuMapper.delete(
                new LambdaQueryWrapper<SysTypeCodeMenu>()
                        .eq(SysTypeCodeMenu::getTypeCode, typeCode));
        // 批量插入新的关联
        if (menuIds != null && !menuIds.isEmpty()) {
            Set<Long> dedup = new HashSet<>(menuIds);
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
        // 获取源主体下所有菜单（含停用，完整拷贝）
        LambdaQueryWrapper<SysMenu> sourceWrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getSubjectType, sourceSubjectType)
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum);
        List<SysMenu> allSourceMenus = sysMenuMapper.selectList(sourceWrapper);
        if (allSourceMenus == null || allSourceMenus.isEmpty()) {
            throw new ServiceException("源主体下暂无菜单可拷贝");
        }
        // 构建 id -> menu 映射
        Map<Long, SysMenu> idToMenu = new HashMap<>();
        Map<Long, List<SysMenu>> parentToChildren = new HashMap<>();
        for (SysMenu m : allSourceMenus) {
            idToMenu.put(m.getId(), m);
            Long pid = m.getParentId() != null ? m.getParentId() : 0L;
            parentToChildren.computeIfAbsent(pid, k -> new ArrayList<>()).add(m);
        }
        // 确定要拷贝的菜单ID集合（含选中节点及其所有子孙）
        Set<Long> toCopyIds = new HashSet<>();
        if (menuIds == null || menuIds.isEmpty()) {
            toCopyIds.addAll(idToMenu.keySet());
        } else {
            Queue<Long> queue = new LinkedList<>(menuIds);
            while (!queue.isEmpty()) {
                Long id = queue.poll();
                if (id == null || !idToMenu.containsKey(id)) continue;
                if (toCopyIds.add(id)) {
                    List<SysMenu> children = parentToChildren.get(id);
                    if (children != null) {
                        children.forEach(c -> queue.offer(c.getId()));
                    }
                }
            }
            // 自动包含所有父级目录，保证树形结构完整（从每个节点向上遍历到根）
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
        // 按层级排序：父节点先于子节点
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
            if (da != db) return Integer.compare(da, db);
            return Integer.compare(a.getOrderNum() != null ? a.getOrderNum() : 0,
                    b.getOrderNum() != null ? b.getOrderNum() : 0);
        });
        // 拷贝并维护 oldId -> newId 映射
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
}
