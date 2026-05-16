package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyTemplate;
import com.jasic.aftersales.system.notify.mapper.SysNotifyTemplateMapper;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知模板运行时渲染服务测试。
 *
 * <p>该测试只覆盖发送侧运行时语义：
 * 必须按 `sceneCode` 命中启用模板，且模板缺失时返回明确的不可发送结果。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public class NotifyTemplateRenderServiceImplTest {

    @Test
    public void shouldRenderRuntimeTemplateBySceneCode() throws Exception {
        NotifyTemplateRenderServiceImpl service = buildService();
        RenderMapperState state = new RenderMapperState();
        state.templates.add(buildAssignedTemplate(2L, NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(), 1));
        injectMapper(service, state);

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("orderNo", "WO-20260516001");
        variables.put("workOrderId", 88L);
        NotifyTemplateRenderResult renderResult = service.render(
                NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(),
                variables
        );

        Assert.assertTrue(renderResult.isNotifyEnabled());
        Assert.assertEquals(NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(), renderResult.getTemplateCode());
        Assert.assertEquals("工单WO-20260516001已派给您，请及时处理", renderResult.getSummary());
        Assert.assertEquals("88", renderResult.getRouteValue());
    }

    @Test
    public void shouldReturnDisabledResultWhenNoActiveTemplate() throws Exception {
        NotifyTemplateRenderServiceImpl service = buildService();
        RenderMapperState state = new RenderMapperState();
        state.templates.add(buildAssignedTemplate(1L, NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(), 0));
        injectMapper(service, state);

        NotifyTemplateRenderResult renderResult = service.render(
                NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(),
                new LinkedHashMap<>()
        );

        Assert.assertFalse(renderResult.isNotifyEnabled());
        Assert.assertEquals(NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(), renderResult.getTemplateCode());
        Assert.assertFalse(renderResult.getErrors().isEmpty());
    }

    private NotifyTemplateRenderServiceImpl buildService() throws Exception {
        NotifyTemplateRenderServiceImpl service = new NotifyTemplateRenderServiceImpl();
        setField(service, "notifySceneRegistry", new NotifySceneRegistry());
        initTableInfo();
        return service;
    }

    private void initTableInfo() {
        if (TableInfoHelper.getTableInfo(SysNotifyTemplate.class) == null) {
            Configuration configuration = new Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "notify-template-render-test");
            assistant.setCurrentNamespace(SysNotifyTemplateMapper.class.getName());
            TableInfoHelper.initTableInfo(assistant, SysNotifyTemplate.class);
        }
    }

    private void injectMapper(NotifyTemplateRenderServiceImpl service, RenderMapperState state) throws Exception {
        setField(service, "sysNotifyTemplateMapper", createTemplateMapperProxy(state));
    }

    private SysNotifyTemplate buildAssignedTemplate(Long id, String sceneCode, Integer status) {
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
    private SysNotifyTemplateMapper createTemplateMapperProxy(RenderMapperState state) {
        return (SysNotifyTemplateMapper) Proxy.newProxyInstance(
                SysNotifyTemplateMapper.class.getClassLoader(),
                new Class[]{SysNotifyTemplateMapper.class},
                (proxy, method, args) -> {
                    if ("selectList".equals(method.getName())) {
                        return selectTemplates(state.templates, args[0]);
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    @SuppressWarnings("unchecked")
    private List<SysNotifyTemplate> selectTemplates(List<SysNotifyTemplate> templates, Object wrapper) throws Exception {
        String sqlSegment = String.valueOf(invokeWrapperMethod(wrapper, "getSqlSegment"));
        Map<String, Object> params = (Map<String, Object>) invokeWrapperMethod(wrapper, "getParamNameValuePairs");
        List<SysNotifyTemplate> matched = new ArrayList<>();
        for (SysNotifyTemplate template : templates) {
            if (sqlSegment.contains("scene_code") && !params.values().contains(template.getSceneCode())) {
                continue;
            }
            if (sqlSegment.contains("status") && !params.values().contains(template.getStatus())) {
                continue;
            }
            matched.add(copyTemplate(template));
        }
        matched.sort((left, right) -> Long.compare(right.getId(), left.getId()));
        return matched;
    }

    private Object invokeWrapperMethod(Object wrapper, String methodName) throws Exception {
        Method method = wrapper.getClass().getMethod(methodName);
        method.setAccessible(true);
        return method.invoke(wrapper);
    }

    private SysNotifyTemplate copyTemplate(SysNotifyTemplate template) {
        return BeanUtil.copyProperties(template, SysNotifyTemplate.class);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }

    private static class RenderMapperState {
        private final List<SysNotifyTemplate> templates = new ArrayList<>();
    }
}
