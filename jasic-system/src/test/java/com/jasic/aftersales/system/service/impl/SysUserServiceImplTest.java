package com.jasic.aftersales.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.entity.SysRole;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import com.jasic.aftersales.system.domain.query.SysUserQuery;
import com.jasic.aftersales.system.domain.vo.SysRoleVO;
import com.jasic.aftersales.system.domain.vo.SysUserVO;
import com.jasic.aftersales.system.mapper.SysRoleMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.mapper.SysUserRoleMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理服务测试。
 * 作用：验证分页列表接口会回填“当前目标公司下”的角色信息，并保证不会混入其它公司的角色。
 *
 * @author Codex
 * @date 2026/05/22
 */
public class SysUserServiceImplTest {

    /** 被测服务：负责执行用户分页查询与角色装配逻辑。 */
    private SysUserServiceImpl service;

    /** 测试态 Mapper 数据仓：集中维护用户、公司归属、角色与用户角色关系。 */
    private MapperState mapperState;

    /**
     * 初始化被测服务与代理 Mapper。
     * 目的：让测试只覆盖 Service 业务逻辑，不依赖数据库或 Spring 容器。
     */
    @Before
    public void setUp() throws Exception {
        service = new SysUserServiceImpl();
        mapperState = buildMapperState();
        setField(service, "sysUserMapper", createUserMapperProxy(mapperState));
        setField(service, "sysUserCompanyMapper", createUserCompanyMapperProxy(mapperState));
        setField(service, "sysUserRoleMapper", createUserRoleMapperProxy(mapperState));
        setField(service, "sysRoleMapper", createRoleMapperProxy(mapperState));
    }

    /**
     * 验证分页列表只返回当前目标公司下的角色。
     * 业务意义：同一用户跨公司挂了多个角色时，用户管理页只能展示当前公司语义下可分配、可见的角色。
     */
    @Test
    public void shouldAttachOnlyCurrentCompanyRolesWhenListingUsers() {
        mapperState.currentTargetCompanyId = 1L;
        SysUserQuery query = new SysUserQuery();
        query.setTargetCompanyId(1L);
        query.setPageNum(1);
        query.setPageSize(10);

        PageResult<SysUserVO> pageResult = service.listPage(query);

        Assert.assertEquals(Long.valueOf(2L), pageResult.getTotal());
        Assert.assertEquals(2, pageResult.getRecords().size());

        SysUserVO alice = pageResult.getRecords().get(0);
        Assert.assertEquals(Long.valueOf(101L), alice.getId());
        Assert.assertNotNull(alice.getRoles());
        Assert.assertEquals(1, alice.getRoles().size());
        Assert.assertEquals("网点管理员", alice.getRoles().get(0).getRoleName());

        SysUserVO bob = pageResult.getRecords().get(1);
        Assert.assertEquals(Long.valueOf(102L), bob.getId());
        Assert.assertNotNull(bob.getRoles());
        Assert.assertTrue(bob.getRoles().isEmpty());
    }

    /**
     * 验证切换目标公司后，列表回填的角色会同步切到该公司口径。
     * 业务意义：证明后端不是简单返回“用户全部角色”，而是严格按当前公司上下文收口。
     */
    @Test
    public void shouldSwitchReturnedRolesWithTargetCompany() {
        mapperState.currentTargetCompanyId = 2L;
        SysUserQuery query = new SysUserQuery();
        query.setTargetCompanyId(2L);
        query.setPageNum(1);
        query.setPageSize(10);

        PageResult<SysUserVO> pageResult = service.listPage(query);

        Assert.assertEquals(Long.valueOf(1L), pageResult.getTotal());
        Assert.assertEquals(1, pageResult.getRecords().size());
        SysUserVO alice = pageResult.getRecords().get(0);
        Assert.assertEquals(Long.valueOf(101L), alice.getId());
        Assert.assertNotNull(alice.getRoles());
        Assert.assertEquals(1, alice.getRoles().size());
        Assert.assertEquals("总部管理员", alice.getRoles().get(0).getRoleName());
    }

    /**
     * 构造测试所需的内存态基础数据。
     * 设计说明：用户 ID 与角色 ID 使用不同数值区间，便于代理 Mapper 从 Wrapper 参数中拆分用户集合与角色集合。
     *
     * @return 完整的测试态 Mapper 数据
     */
    private MapperState buildMapperState() {
        MapperState state = new MapperState();

        SysUser alice = new SysUser();
        alice.setId(101L);
        alice.setUsername("alice");
        alice.setRealName("Alice");
        alice.setCreateTime(LocalDateTime.of(2026, 5, 22, 10, 0, 0));
        state.users.put(alice.getId(), alice);

        SysUser bob = new SysUser();
        bob.setId(102L);
        bob.setUsername("bob");
        bob.setRealName("Bob");
        bob.setCreateTime(LocalDateTime.of(2026, 5, 22, 9, 0, 0));
        state.users.put(bob.getId(), bob);

        state.userCompanies.add(buildUserCompany(1L, 101L, 1L));
        state.userCompanies.add(buildUserCompany(2L, 102L, 1L));
        state.userCompanies.add(buildUserCompany(3L, 101L, 2L));

        state.roles.put(1001L, buildRole(1001L, 1L, "网点管理员", "site_admin"));
        state.roles.put(1002L, buildRole(1002L, 1L, "维修员", "repairer"));
        state.roles.put(2001L, buildRole(2001L, 2L, "总部管理员", "hq_admin"));

        state.userRoles.add(buildUserRole(1L, 101L, 1001L));
        state.userRoles.add(buildUserRole(2L, 101L, 2001L));

        return state;
    }

    /**
     * 创建用户主表 Mapper 代理。
     * 关键点：根据分页查询 Wrapper 中的用户 ID 集合过滤内存数据，模拟当前公司下用户分页结果。
     *
     * @param state 测试态数据仓
     * @return 用户 Mapper 代理
     */
    private SysUserMapper createUserMapperProxy(MapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectPage".equals(method.getName())) {
                    @SuppressWarnings("unchecked")
                    Page<SysUser> page = (Page<SysUser>) args[0];
                    List<Long> userIds = state.userCompanies.stream()
                            .filter(item -> state.currentTargetCompanyId != null
                                    && state.currentTargetCompanyId.equals(item.getCompanyId()))
                            .map(SysUserCompany::getUserId)
                            .distinct()
                            .collect(Collectors.toList());
                    List<SysUser> records = state.users.values().stream()
                            .filter(user -> userIds.contains(user.getId()))
                            .sorted((left, right) -> right.getCreateTime().compareTo(left.getCreateTime()))
                            .collect(Collectors.toList());
                    Page<SysUser> result = new Page<>(page.getCurrent(), page.getSize());
                    result.setTotal(records.size());
                    result.setRecords(records);
                    return result;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysUserMapper) Proxy.newProxyInstance(
                SysUserMapper.class.getClassLoader(),
                new Class<?>[]{SysUserMapper.class},
                handler
        );
    }

    /**
     * 创建用户公司关系 Mapper 代理。
     * 关键点：只返回当前目标公司下的用户归属关系，驱动列表主查询先做公司级用户范围收口。
     *
     * @param state 测试态数据仓
     * @return 用户公司关系 Mapper 代理
     */
    private SysUserCompanyMapper createUserCompanyMapperProxy(MapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return state.userCompanies.stream()
                            .filter(item -> state.currentTargetCompanyId != null
                                    && state.currentTargetCompanyId.equals(item.getCompanyId()))
                            .collect(Collectors.toList());
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysUserCompanyMapper) Proxy.newProxyInstance(
                SysUserCompanyMapper.class.getClassLoader(),
                new Class<?>[]{SysUserCompanyMapper.class},
                handler
        );
    }

    /**
     * 创建用户角色关系 Mapper 代理。
     * 关键点：按 Wrapper 中的用户集合和角色集合双重过滤，验证服务层确实只会装配当前公司下角色。
     *
     * @param state 测试态数据仓
     * @return 用户角色关系 Mapper 代理
     */
    private SysUserRoleMapper createUserRoleMapperProxy(MapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    List<Long> currentCompanyUserIds = state.userCompanies.stream()
                            .filter(item -> state.currentTargetCompanyId != null
                                    && state.currentTargetCompanyId.equals(item.getCompanyId()))
                            .map(SysUserCompany::getUserId)
                            .distinct()
                            .collect(Collectors.toList());
                    List<Long> currentCompanyRoleIds = state.roles.values().stream()
                            .filter(item -> state.currentTargetCompanyId != null
                                    && state.currentTargetCompanyId.equals(item.getCompanyId()))
                            .map(SysRole::getId)
                            .collect(Collectors.toList());
                    return state.userRoles.stream()
                            .filter(item -> currentCompanyUserIds.contains(item.getUserId()))
                            .filter(item -> currentCompanyRoleIds.contains(item.getRoleId()))
                            .collect(Collectors.toList());
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysUserRoleMapper) Proxy.newProxyInstance(
                SysUserRoleMapper.class.getClassLoader(),
                new Class<?>[]{SysUserRoleMapper.class},
                handler
        );
    }

    /**
     * 创建角色 Mapper 代理。
     * 关键点：既支持“按公司查角色 ID”，也支持“按角色 ID 批量查详情”，覆盖列表角色装配所需的全部路径。
     *
     * @param state 测试态数据仓
     * @return 角色 Mapper 代理
     */
    private SysRoleMapper createRoleMapperProxy(MapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return state.roles.values().stream()
                            .filter(role -> state.currentTargetCompanyId != null
                                    && state.currentTargetCompanyId.equals(role.getCompanyId()))
                            .collect(Collectors.toList());
                }
                if ("selectBatchIds".equals(method.getName())) {
                    return state.roles.values().stream()
                            .filter(role -> state.currentTargetCompanyId != null
                                    && state.currentTargetCompanyId.equals(role.getCompanyId()))
                            .collect(Collectors.toList());
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysRoleMapper) Proxy.newProxyInstance(
                SysRoleMapper.class.getClassLoader(),
                new Class<?>[]{SysRoleMapper.class},
                handler
        );
    }

    /**
     * 构造用户公司关系实体。
     *
     * @param id        主键ID
     * @param userId    用户ID
     * @param companyId 公司ID
     * @return 用户公司关系实体
     */
    private SysUserCompany buildUserCompany(Long id, Long userId, Long companyId) {
        SysUserCompany relation = new SysUserCompany();
        relation.setId(id);
        relation.setUserId(userId);
        relation.setCompanyId(companyId);
        relation.setIsDefault(0);
        relation.setIsPrimaryAccount(0);
        return relation;
    }

    /**
     * 构造角色实体。
     *
     * @param id        角色ID
     * @param companyId 公司ID
     * @param roleName  角色名称
     * @param roleKey   角色标识
     * @return 角色实体
     */
    private SysRole buildRole(Long id, Long companyId, String roleName, String roleKey) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setCompanyId(companyId);
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        role.setStatus(1);
        return role;
    }

    /**
     * 构造用户角色关系实体。
     *
     * @param id     主键ID
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 用户角色关系实体
     */
    private SysUserRole buildUserRole(Long id, Long userId, Long roleId) {
        SysUserRole relation = new SysUserRole();
        relation.setId(id);
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        return relation;
    }

    /**
     * 通过反射给被测服务注入依赖。
     *
     * @param target    目标对象
     * @param fieldName 字段名
     * @param value     注入值
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SysUserServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 返回 Java 默认值。
     * 作用：让代理 Mapper 只处理测试关心的方法，其余方法统一安全返回。
     *
     * @param type 返回类型
     * @return 对应默认值
     */
    private Object defaultValue(Class<?> type) {
        if (type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Double.TYPE) {
            return 0D;
        }
        if (type == Float.TYPE) {
            return 0F;
        }
        return null;
    }

    /**
     * 测试态 Mapper 数据仓。
     * 作用：用稳定、可读的内存结构表达用户、角色和关系表数据，便于断言当前公司口径下的返回结果。
     */
    private static class MapperState {
        /** 当前测试指定的目标公司ID，用于驱动代理 Mapper 按当前公司口径返回数据。 */
        private Long currentTargetCompanyId;
        /** 用户主表数据。 */
        private final Map<Long, SysUser> users = new LinkedHashMap<>();
        /** 用户公司关系表数据。 */
        private final List<SysUserCompany> userCompanies = new ArrayList<>();
        /** 角色主表数据。 */
        private final Map<Long, SysRole> roles = new LinkedHashMap<>();
        /** 用户角色关系表数据。 */
        private final List<SysUserRole> userRoles = new ArrayList<>();
    }
}
