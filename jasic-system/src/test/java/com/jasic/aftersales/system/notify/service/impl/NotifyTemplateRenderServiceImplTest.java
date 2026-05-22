package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jasic.aftersales.system.notify.domain.entity.NotifyScene;
import com.jasic.aftersales.system.notify.domain.entity.NotifySceneTarget;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTypeEnum;
import com.jasic.aftersales.system.notify.mapper.NotifySceneMapper;
import com.jasic.aftersales.system.notify.mapper.NotifySceneTargetMapper;
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
 * <p>阶段一后运行时渲染已经切换为从 `notify_scene_target` 读取默认目标配置，
 * 本测试重点验证旧的“只传 sceneCode”调用方式仍然可用。</p>
 *
 * @author Zoro
 * @date 2026/05/16
 */
public class NotifyTemplateRenderServiceImplTest {

    /**验证RenderConfiguredTargetBySceneCodeAndTargetType，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRenderConfiguredTargetBySceneCodeAndTargetType() throws Exception {
        NotifyTemplateRenderServiceImpl service = buildService();
        RenderMapperState state = new RenderMapperState();
        state.targets.add(buildTarget(
                2L,
                NotifySceneCode.WORK_ORDER_ASSIGNED.getCode(),
                NotifyTypeEnum.IN_APP_TODO.getCode(),
                1
        ));
        injectMapper(service, state);

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("orderNo", "WO-20260516001");
        variables.put("workOrderId", 88L);
        NotifyTemplateRenderResult renderResult = service.render(
                NotifySceneCode.WORK_ORDER_ASSIGNED.getCode(),
                NotifyTypeEnum.IN_APP_TODO.getCode(),
                variables
        );

        Assert.assertTrue(renderResult.isNotifyEnabled());
        Assert.assertEquals(NotifySceneCode.WORK_ORDER_ASSIGNED.getCode(), renderResult.getTemplateCode());
        Assert.assertEquals("工单WO-20260516001已派给您，请及时处理", renderResult.getSummary());
        Assert.assertEquals("88", renderResult.getRouteValue());
    }

    /**验证ReturnDisabledResultWhenTargetConfigIsNotEnabled，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldReturnDisabledResultWhenTargetConfigIsNotEnabled() throws Exception {
        NotifyTemplateRenderServiceImpl service = buildService();
        RenderMapperState state = new RenderMapperState();
        state.targets.add(buildTarget(
                1L,
                NotifySceneCode.WORK_ORDER_ASSIGNED.getCode(),
                NotifyTypeEnum.IN_APP_TODO.getCode(),
                0
        ));
        injectMapper(service, state);

        NotifyTemplateRenderResult renderResult = service.render(
                NotifySceneCode.WORK_ORDER_ASSIGNED.getCode(),
                NotifyTypeEnum.IN_APP_TODO.getCode(),
                new LinkedHashMap<>()
        );

        Assert.assertFalse(renderResult.isNotifyEnabled());
        Assert.assertEquals(NotifySceneCode.WORK_ORDER_ASSIGNED.getCode(), renderResult.getTemplateCode());
        Assert.assertFalse(renderResult.getErrors().isEmpty());
    }

    /**buildService 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@return 处理后的业务结果。*/
    private NotifyTemplateRenderServiceImpl buildService() throws Exception {
        NotifyTemplateRenderServiceImpl service = new NotifyTemplateRenderServiceImpl();
        setField(service, "notifySceneRegistry", new NotifySceneRegistry());
        initTableInfo();
        return service;
    }

    /**initTableInfo 处理逻辑，服务于当前类的业务编排和数据转换。*/
    private void initTableInfo() {
        if (TableInfoHelper.getTableInfo(NotifySceneTarget.class) == null) {
            Configuration configuration = new Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "notify-scene-target-render-test");
            assistant.setCurrentNamespace(NotifySceneTargetMapper.class.getName());
            TableInfoHelper.initTableInfo(assistant, NotifySceneTarget.class);
        }
    }

    /**injectMapper 处理逻辑，服务于当前类的业务编排和数据转换。
@param service service 字段参数。
@param state state 字段参数。*/
    private void injectMapper(NotifyTemplateRenderServiceImpl service, RenderMapperState state) throws Exception {
        setField(service, "notifySceneTargetMapper", createTargetMapperProxy(state));
        setField(service, "notifySceneMapper", createSceneMapperProxy());
    }

    /**
     * 构造场景 Mapper 桩。
     *
     * <p>渲染服务正式按场景总开关过滤目标配置，本测试默认返回启用状态，
     * 让用例继续聚焦目标配置命中和模板变量渲染。</p>
     *
     * @return Mapper桩
     */
    private NotifySceneMapper createSceneMapperProxy() {
        return (NotifySceneMapper) Proxy.newProxyInstance(
                NotifySceneMapper.class.getClassLoader(),
                new Class[]{NotifySceneMapper.class},
                (proxy, method, args) -> {
                    if ("selectOne".equals(method.getName())) {
                        NotifyScene scene = new NotifyScene();
                        scene.setStatus(1);
                        return scene;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    /**buildTarget 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@param sceneCode 业务编码，用于匹配枚举、配置或外部系统数据。
@param targetType targetType 字段参数。
@param enabled enabled 字段参数。
@return 处理后的业务结果。*/
    private NotifySceneTarget buildTarget(Long id, String sceneCode, String targetType, Integer enabled) {
        NotifySceneTarget target = new NotifySceneTarget();
        target.setId(id);
        target.setSceneCode(sceneCode);
        target.setTargetType(targetType);
        target.setEnabled(enabled);
        target.setTitleTemplate("您有新的维修工单");
        target.setContentTemplate("工单${orderNo}已派给您，请及时处理");
        target.setRouteType("WORK_ORDER_DETAIL");
        target.setRouteValueTemplate("${workOrderId}");
        target.setCreateTime(LocalDateTime.of(2026, 5, 16, 10, 0, 0));
        target.setUpdateTime(LocalDateTime.of(2026, 5, 16, 10, 0, 0));
        return target;
    }

    /**createTargetMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param state state 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    @SuppressWarnings("unchecked")
    private NotifySceneTargetMapper createTargetMapperProxy(RenderMapperState state) {
        return (NotifySceneTargetMapper) Proxy.newProxyInstance(
                NotifySceneTargetMapper.class.getClassLoader(),
                new Class[]{NotifySceneTargetMapper.class},
                (proxy, method, args) -> {
                    if ("selectList".equals(method.getName())) {
                        return selectTargets(state.targets, args[0]);
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    /**selectTargets 业务数据，按查询条件和数据权限返回可见范围内的结果。
@param targets 业务数据列表，用于批量处理或返回组装。
@param wrapper wrapper 字段参数。
@return 查询或组装后的业务数据集合。*/
    @SuppressWarnings("unchecked")
    private List<NotifySceneTarget> selectTargets(List<NotifySceneTarget> targets, Object wrapper) throws Exception {
        String sqlSegment = String.valueOf(invokeWrapperMethod(wrapper, "getSqlSegment"));
        Map<String, Object> params = (Map<String, Object>) invokeWrapperMethod(wrapper, "getParamNameValuePairs");
        List<NotifySceneTarget> matched = new ArrayList<>();
        for (NotifySceneTarget target : targets) {
            if (sqlSegment.contains("scene_code") && !params.values().contains(target.getSceneCode())) {
                continue;
            }
            if (sqlSegment.contains("target_type") && !params.values().contains(target.getTargetType())) {
                continue;
            }
            if (sqlSegment.contains("enabled") && !params.values().contains(target.getEnabled())) {
                continue;
            }
            matched.add(copyTarget(target));
        }
        matched.sort((left, right) -> Long.compare(right.getId(), left.getId()));
        return matched;
    }

    /**invokeWrapperMethod 处理逻辑，服务于当前类的业务编排和数据转换。
@param wrapper wrapper 字段参数。
@param methodName 名称文本，用于展示、匹配或保存业务对象名称。
@return 处理后的业务结果。*/
    private Object invokeWrapperMethod(Object wrapper, String methodName) throws Exception {
        Method method = wrapper.getClass().getMethod(methodName);
        method.setAccessible(true);
        return method.invoke(wrapper);
    }

    /**copyTarget 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@return 处理后的业务结果。*/
    private NotifySceneTarget copyTarget(NotifySceneTarget target) {
        return BeanUtil.copyProperties(target, NotifySceneTarget.class);
    }

    /**setField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**defaultValue 处理逻辑，服务于当前类的业务编排和数据转换。
@param returnType returnType 字段参数。
@return 处理后的业务结果。*/
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

    /**RenderMapperState 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class RenderMapperState {
        /**targets 字段，用于当前类内部业务处理。*/
        private final List<NotifySceneTarget> targets = new ArrayList<>();
    }
}
