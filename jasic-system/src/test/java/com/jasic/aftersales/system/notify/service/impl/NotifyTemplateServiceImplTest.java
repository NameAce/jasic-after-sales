package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyTemplate;
import com.jasic.aftersales.system.notify.mapper.SysNotifyTemplateMapper;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通知模板服务实现测试。
 *
 * <p>Phase 1 重点验证模板唯一键已经改为 `sceneCode`，
 * 同时校验变量白名单和启停逻辑都基于通知场景注册表运行。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public class NotifyTemplateServiceImplTest {

    /**
     * 同一通知场景不允许重复新增模板。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldRejectCreateWhenSceneAlreadyExists() throws Exception {
        NotifyTemplateServiceImpl service = buildService();
        TemplateMapperState state = new TemplateMapperState();
        state.templates.add(buildTemplate(1L, NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(), 1));
        injectMappers(service, state);

        try {
            service.createTemplate(buildAssignedTodoTemplateDTO());
            Assert.fail("Expected ServiceException");
        } catch (ServiceException ex) {
            Assert.assertTrue(ex.getMessage().contains("同一通知场景"));
        }
    }

    /**
     * 新增模板时应把 `sceneCode` 落库，并允许使用场景注册表声明的变量。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldCreateTemplateBySceneCode() throws Exception {
        NotifyTemplateServiceImpl service = buildService();
        TemplateMapperState state = new TemplateMapperState();
        injectMappers(service, state);

        Long id = service.createTemplate(buildAssignedTodoTemplateDTO());

        Assert.assertNotNull(id);
        Assert.assertEquals(1, state.templates.size());
        Assert.assertEquals(NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(), state.templates.get(0).getSceneCode());
        Assert.assertEquals("工单派单待办", state.templates.get(0).getTemplateName());
    }

    /**
     * 保存模板时如果出现未注册变量，应直接报错。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldRejectUnknownPlaceholderOnCreate() throws Exception {
        NotifyTemplateServiceImpl service = buildService();
        TemplateMapperState state = new TemplateMapperState();
        injectMappers(service, state);

        NotifyTemplateDTO dto = buildAssignedTodoTemplateDTO();
        dto.setContentTemplate("工单${unknownVar}已派给您");
        try {
            service.createTemplate(dto);
            Assert.fail("Expected ServiceException");
        } catch (ServiceException ex) {
            Assert.assertTrue(ex.getMessage().contains("未注册变量"));
        }
    }

    /**
     * 停用接口应直接切换模板状态，不做物理删除。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldUpdateStatusInsteadOfDeletingTemplate() throws Exception {
        NotifyTemplateServiceImpl service = buildService();
        TemplateMapperState state = new TemplateMapperState();
        state.templates.add(buildTemplate(1L, NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(), 1));
        injectMappers(service, state);

        service.updateStatus(1L, 0);

        Assert.assertEquals(1, state.templates.size());
        Assert.assertEquals(Integer.valueOf(0), state.templates.get(0).getStatus());
    }

    private NotifyTemplateServiceImpl buildService() throws Exception {
        NotifyTemplateServiceImpl service = new NotifyTemplateServiceImpl();
        initTableInfo();
        setField(service, "notifySceneRegistry", new NotifySceneRegistry());
        return service;
    }

    private void initTableInfo() {
        if (TableInfoHelper.getTableInfo(SysNotifyTemplate.class) == null) {
            Configuration configuration = new Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "notify-template-test");
            assistant.setCurrentNamespace(SysNotifyTemplateMapper.class.getName());
            TableInfoHelper.initTableInfo(assistant, SysNotifyTemplate.class);
        }
    }

    private void injectMappers(NotifyTemplateServiceImpl service, TemplateMapperState state) throws Exception {
        setField(service, "sysNotifyTemplateMapper", createTemplateMapperProxy(state));
    }

    private NotifyTemplateDTO buildAssignedTodoTemplateDTO() {
        NotifyTemplateDTO dto = new NotifyTemplateDTO();
        dto.setSceneCode(NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode());
        dto.setTemplateName("工单派单待办");
        dto.setTitleTemplate("您有新的维修工单");
        dto.setContentTemplate("工单${orderNo}已派给您，请及时处理");
        dto.setRouteType("WORK_ORDER_DETAIL");
        dto.setRouteValueTemplate("${workOrderId}");
        dto.setStatus(1);
        return dto;
    }

    private SysNotifyTemplate buildTemplate(Long id, String sceneCode, Integer status) {
        SysNotifyTemplate template = new SysNotifyTemplate();
        template.setId(id);
        template.setSceneCode(sceneCode);
        template.setTemplateName("工单派单待办");
        template.setTitleTemplate("您有新的维修工单");
        template.setContentTemplate("工单${orderNo}已派给您，请及时处理");
        template.setRouteType("WORK_ORDER_DETAIL");
        template.setRouteValueTemplate("${workOrderId}");
        template.setStatus(status);
        template.setCreateTime(LocalDateTime.of(2026, 5, 16, 10, 0, 0));
        template.setUpdateTime(LocalDateTime.of(2026, 5, 16, 10, 0, 0));
        return template;
    }

    @SuppressWarnings("unchecked")
    private SysNotifyTemplateMapper createTemplateMapperProxy(TemplateMapperState state) {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if ("selectById".equals(methodName)) {
                return findTemplateById(state.templates, (Long) args[0]);
            }
            if ("selectCount".equals(methodName)) {
                return Long.valueOf(selectTemplates(state.templates, args[0]).size());
            }
            if ("insert".equals(methodName)) {
                SysNotifyTemplate template = (SysNotifyTemplate) args[0];
                template.setId((long) (state.templates.size() + 1));
                state.templates.add(cloneTemplate(template));
                return 1;
            }
            if ("updateById".equals(methodName)) {
                replaceTemplate(state.templates, (SysNotifyTemplate) args[0]);
                return 1;
            }
            return defaultValue(method.getReturnType());
        };
        return (SysNotifyTemplateMapper) Proxy.newProxyInstance(
                SysNotifyTemplateMapper.class.getClassLoader(),
                new Class[]{SysNotifyTemplateMapper.class},
                handler
        );
    }

    private SysNotifyTemplate findTemplateById(List<SysNotifyTemplate> templates, Long id) {
        for (SysNotifyTemplate template : templates) {
            if (template.getId().equals(id)) {
                return cloneTemplate(template);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<SysNotifyTemplate> selectTemplates(List<SysNotifyTemplate> templates, Object wrapper) {
        String sqlSegment = String.valueOf(invokeWrapperMethod(wrapper, "getSqlSegment"));
        Map<String, Object> params = (Map<String, Object>) invokeWrapperMethod(wrapper, "getParamNameValuePairs");
        List<SysNotifyTemplate> matches = new ArrayList<>();
        for (SysNotifyTemplate template : templates) {
            if (sqlSegment.contains("scene_code") && !params.values().contains(template.getSceneCode())) {
                continue;
            }
            if (sqlSegment.contains("id <>") && params.values().contains(template.getId())) {
                continue;
            }
            matches.add(cloneTemplate(template));
        }
        return matches;
    }

    private void replaceTemplate(List<SysNotifyTemplate> templates, SysNotifyTemplate target) {
        for (int i = 0; i < templates.size(); i++) {
            if (templates.get(i).getId().equals(target.getId())) {
                templates.set(i, cloneTemplate(target));
                return;
            }
        }
    }

    private Object invokeWrapperMethod(Object wrapper, String methodName) {
        try {
            Method method = wrapper.getClass().getMethod(methodName);
            return method.invoke(wrapper);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to inspect wrapper method: " + methodName, ex);
        }
    }

    private SysNotifyTemplate cloneTemplate(SysNotifyTemplate template) {
        return BeanUtil.copyProperties(template, SysNotifyTemplate.class);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = NotifyTemplateServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object defaultValue(Class<?> type) {
        if (type == null || !type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (int.class.equals(type)) {
            return 0;
        }
        if (long.class.equals(type)) {
            return 0L;
        }
        return null;
    }

    private static class TemplateMapperState {
        private final List<SysNotifyTemplate> templates = new ArrayList<>();
    }
}
