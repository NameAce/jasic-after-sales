package com.jasic.aftersales.system.notify.service.support;

import com.jasic.aftersales.framework.datapermission.CompanyDataAccessContext;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.mapper.SysMenuMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.notify.support.NotifyReceiverSnapshot;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 网点级通知接收人解析器测试。
 *
 * <p>这里重点验证解析器在异步通知链路下会显式压入目标公司上下文，
 * 避免权限查询回退依赖当前 Web 登录态，导致 Quartz/异步线程抛出非 Web 上下文异常。</p>
 *
 * @author Codex
 * @date 2026/05/18
 */
public class WorkOrderUserResolverTest {

    private static final Long TARGET_COMPANY_ID = 2002L;

    /**
     * 建单待派单通知解析派单权限用户时，应显式绑定目标公司上下文。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldResolveAssignUsersWithinTargetCompanyContext() throws Exception {
        CompanyDataAccessContext context = new CompanyDataAccessContext();
        WorkOrderAssignUserResolver resolver = new WorkOrderAssignUserResolver();
        wireResolverDependencies(
                resolver,
                context,
                "workorder:assign"
        );

        List<NotifyReceiverSnapshot> snapshots = resolver.resolveAssignUserSnapshots(TARGET_COMPANY_ID);

        Assert.assertEquals(1, snapshots.size());
        Assert.assertEquals(Long.valueOf(301L), snapshots.get(0).getReceiverId());
        Assert.assertEquals(TARGET_COMPANY_ID, snapshots.get(0).getReceiverCompanyId());
        Assert.assertNull(context.getTargetCompanyId());
    }

    /**
     * 转单转入通知解析接单权限用户时，也应显式绑定目标公司上下文。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldResolveAcceptUsersWithinTargetCompanyContext() throws Exception {
        CompanyDataAccessContext context = new CompanyDataAccessContext();
        WorkOrderAcceptUserResolver resolver = new WorkOrderAcceptUserResolver();
        wireResolverDependencies(
                resolver,
                context,
                "workorder:accept"
        );

        List<NotifyReceiverSnapshot> snapshots = resolver.resolveAcceptUserSnapshots(TARGET_COMPANY_ID);

        Assert.assertEquals(1, snapshots.size());
        Assert.assertEquals(Long.valueOf(301L), snapshots.get(0).getReceiverId());
        Assert.assertEquals(TARGET_COMPANY_ID, snapshots.get(0).getReceiverCompanyId());
        Assert.assertNull(context.getTargetCompanyId());
    }

    /**
     * 为不同解析器注入共享的伪造依赖，便于聚焦验证公司上下文是否正确压入。
     *
     * @param resolver 解析器实例
     * @param context 公司数据访问上下文
     * @param expectedPermission 预期命中的权限编码
     * @throws Exception 反射异常
     */
    private void wireResolverDependencies(Object resolver, CompanyDataAccessContext context,
                                          String expectedPermission) throws Exception {
        setField(resolver, "companyDataAccessContext", context);
        setField(resolver, "sysUserCompanyMapper", buildSysUserCompanyMapper(context));
        setField(resolver, "sysUserMapper", buildSysUserMapper(context));
        setField(resolver, "sysMenuMapper", buildSysMenuMapper(context, expectedPermission));
    }

    /**
     * 构建用户公司关系 Mapper 代理，并断言查询时已压入目标公司上下文。
     *
     * @param context 公司数据访问上下文
     * @return Mapper 代理
     */
    private SysUserCompanyMapper buildSysUserCompanyMapper(CompanyDataAccessContext context) {
        return buildProxy(SysUserCompanyMapper.class, (proxy, method, args) -> {
            if ("selectList".equals(method.getName())) {
                Assert.assertEquals(TARGET_COMPANY_ID, context.getTargetCompanyId());
                SysUserCompany relation = new SysUserCompany();
                relation.setUserId(301L);
                List<SysUserCompany> relations = new ArrayList<>();
                relations.add(relation);
                return relations;
            }
            return defaultValue(method.getReturnType());
        });
    }

    /**
     * 构建用户 Mapper 代理，并断言批量查询用户时仍处于目标公司上下文内。
     *
     * @param context 公司数据访问上下文
     * @return Mapper 代理
     */
    private SysUserMapper buildSysUserMapper(CompanyDataAccessContext context) {
        return buildProxy(SysUserMapper.class, (proxy, method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                Assert.assertEquals(TARGET_COMPANY_ID, context.getTargetCompanyId());
                SysUser user = new SysUser();
                user.setId(301L);
                user.setStatus(1);
                user.setRealName("派单员A");
                user.setOpenid("openid-301");
                List<SysUser> users = new ArrayList<>();
                users.add(user);
                return users;
            }
            return defaultValue(method.getReturnType());
        });
    }

    /**
     * 构建菜单权限 Mapper 代理，并断言角色权限查询时已带上目标公司上下文。
     *
     * @param context 公司数据访问上下文
     * @param expectedPermission 预期命中的权限编码
     * @return Mapper 代理
     */
    private SysMenuMapper buildSysMenuMapper(CompanyDataAccessContext context, String expectedPermission) {
        return buildProxy(SysMenuMapper.class, (proxy, method, args) -> {
            if ("selectPermsByUserIdAndCompanyId".equals(method.getName())) {
                Assert.assertEquals(TARGET_COMPANY_ID, context.getTargetCompanyId());
                Assert.assertEquals(Long.valueOf(301L), args[0]);
                Assert.assertEquals(TARGET_COMPANY_ID, args[1]);
                return Collections.singleton(expectedPermission);
            }
            return defaultValue(method.getReturnType());
        });
    }

    /**
     * 构建简单 JDK 动态代理，避免为当前模块额外引入 mocking 依赖。
     *
     * @param type Mapper 接口类型
     * @param handler 调用处理器
     * @param <T> 接口泛型
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    private <T> T buildProxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    /**
     * 通过反射注入测试依赖。
     *
     * @param target 目标对象
     * @param fieldName 字段名
     * @param value 字段值
     * @throws Exception 反射异常
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 为代理中未覆盖的方法返回一个最小默认值，避免误调时出现空指针噪音。
     *
     * @param returnType 返回值类型
     * @return 默认值
     */
    private Object defaultValue(Class<?> returnType) {
        if (returnType == null || Void.TYPE.equals(returnType)) {
            return null;
        }
        if (Boolean.TYPE.equals(returnType)) {
            return false;
        }
        if (Integer.TYPE.equals(returnType)) {
            return 0;
        }
        if (Long.TYPE.equals(returnType)) {
            return 0L;
        }
        if (Set.class.isAssignableFrom(returnType)) {
            return Collections.emptySet();
        }
        if (List.class.isAssignableFrom(returnType) || Collection.class.isAssignableFrom(returnType)) {
            return Collections.emptyList();
        }
        return null;
    }
}
