package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateChannelDTO;
import com.jasic.aftersales.system.notify.domain.entity.NotifySceneTarget;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTypeEnum;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.mapper.NotifySceneTargetMapper;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通知渠道配置服务测试。
 *
 * <p>阶段一后，渠道配置已经并入 `notify_scene_target`，
 * 本测试验证旧渠道读取接口仍然可以从新表拿到结构化 `MP_SUBSCRIBE` 配置。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public class NotifyChannelConfigServiceImplTest {

    @Test
    public void shouldRoundTripMiniProgramChannelConfigBySceneCode() throws Exception {
        NotifyChannelConfigServiceImpl service = buildService();
        ChannelMapperState state = new ChannelMapperState();
        injectMapper(service, state);

        service.saveChannelConfigs(
                NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(),
                buildSingleEnabledChannel("wx-template-001")
        );

        Assert.assertEquals(1, state.targets.size());
        Assert.assertTrue(state.targets.get(0).getConfigJson().contains("templateId"));

        List<NotifyTemplateChannelVO> channelVOList = service.listChannelConfigs(
                NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode()
        );
        Assert.assertEquals(1, channelVOList.size());
        Assert.assertEquals("wx-template-001", channelVOList.get(0).getTemplateId());
    }

    @Test
    public void shouldFilterDisabledRuntimeChannelsButKeepConfigPresence() throws Exception {
        NotifyChannelConfigServiceImpl service = buildService();
        ChannelMapperState state = new ChannelMapperState();
        NotifySceneTarget disabledTarget = new NotifySceneTarget();
        disabledTarget.setId(1L);
        disabledTarget.setSceneCode(NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode());
        disabledTarget.setTargetType(NotifyTypeEnum.MP_SUBSCRIBE.getCode());
        disabledTarget.setEnabled(0);
        disabledTarget.setConfigJson("{\"templateId\":\"wx-template-001\",\"pagePathTemplate\":\"pages/order/evaluate?workOrderId=${workOrderId}\",\"fieldMapping\":[{\"field\":\"thing1\",\"value\":\"${orderNo}\"}]}");
        state.targets.add(disabledTarget);
        injectMapper(service, state);

        Assert.assertTrue(service.listRuntimeChannelConfigs(
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode()
        ).isEmpty());
        Assert.assertTrue(service.hasRuntimeChannelConfigs(
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode()
        ));
    }

    private NotifyChannelConfigServiceImpl buildService() throws Exception {
        NotifyChannelConfigServiceImpl service = new NotifyChannelConfigServiceImpl();
        initTableInfo();
        setField(service, "notifySceneRegistry", new NotifySceneRegistry());
        return service;
    }

    private void initTableInfo() {
        if (TableInfoHelper.getTableInfo(NotifySceneTarget.class) == null) {
            Configuration configuration = new Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "notify-scene-target-channel-test");
            assistant.setCurrentNamespace(NotifySceneTargetMapper.class.getName());
            TableInfoHelper.initTableInfo(assistant, NotifySceneTarget.class);
        }
    }

    private void injectMapper(NotifyChannelConfigServiceImpl service, ChannelMapperState state) throws Exception {
        setField(service, "notifySceneTargetMapper", createTargetMapperProxy(state));
    }

    private List<NotifyTemplateChannelDTO> buildSingleEnabledChannel(String templateId) {
        List<NotifyTemplateChannelDTO> channelConfigs = new ArrayList<>();
        NotifyTemplateChannelDTO dto = new NotifyTemplateChannelDTO();
        dto.setChannelType("MP_SUBSCRIBE");
        dto.setChannelEnabled(1);
        dto.setTemplateId(templateId);
        dto.setPagePathTemplate("pages/order/detail?workOrderId=${workOrderId}");
        List<NotifyChannelFieldMappingDTO> fieldMappings = new ArrayList<>();
        NotifyChannelFieldMappingDTO mapping = new NotifyChannelFieldMappingDTO();
        mapping.setField("thing1");
        mapping.setValue("${orderNo}");
        fieldMappings.add(mapping);
        dto.setFieldMapping(fieldMappings);
        channelConfigs.add(dto);
        return channelConfigs;
    }

    @SuppressWarnings("unchecked")
    private NotifySceneTargetMapper createTargetMapperProxy(ChannelMapperState state) {
        return (NotifySceneTargetMapper) Proxy.newProxyInstance(
                NotifySceneTargetMapper.class.getClassLoader(),
                new Class[]{NotifySceneTargetMapper.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("selectOne".equals(methodName)) {
                        List<NotifySceneTarget> matches = selectTargets(state.targets, args[0]);
                        return matches.isEmpty() ? null : matches.get(0);
                    }
                    if ("selectCount".equals(methodName)) {
                        return Long.valueOf(selectTargets(state.targets, args[0]).size());
                    }
                    if ("insert".equals(methodName)) {
                        NotifySceneTarget target = (NotifySceneTarget) args[0];
                        target.setId((long) (state.targets.size() + 1));
                        state.targets.add(cloneTarget(target));
                        return 1;
                    }
                    if ("updateById".equals(methodName)) {
                        replaceTarget(state.targets, (NotifySceneTarget) args[0]);
                        return 1;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    @SuppressWarnings("unchecked")
    private List<NotifySceneTarget> selectTargets(List<NotifySceneTarget> targets, Object wrapper) {
        String sqlSegment = String.valueOf(invokeWrapperMethod(wrapper, "getSqlSegment"));
        Map<String, Object> params = (Map<String, Object>) invokeWrapperMethod(wrapper, "getParamNameValuePairs");
        List<NotifySceneTarget> matches = new ArrayList<>();
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
            matches.add(cloneTarget(target));
        }
        return matches;
    }

    private void replaceTarget(List<NotifySceneTarget> targets, NotifySceneTarget updated) {
        for (int i = 0; i < targets.size(); i++) {
            if (Objects.equals(targets.get(i).getId(), updated.getId())) {
                targets.set(i, cloneTarget(updated));
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

    private NotifySceneTarget cloneTarget(NotifySceneTarget target) {
        return BeanUtil.copyProperties(target, NotifySceneTarget.class);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = NotifyChannelConfigServiceImpl.class.getDeclaredField(fieldName);
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

    private static class ChannelMapperState {
        private final List<NotifySceneTarget> targets = new ArrayList<>();
    }
}
