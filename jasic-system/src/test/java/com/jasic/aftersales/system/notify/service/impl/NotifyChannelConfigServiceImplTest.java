package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateChannelDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyTemplateChannel;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.mapper.SysNotifyTemplateChannelMapper;
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
 * <p>Phase 1 重点验证渠道配置已经改为按 `sceneCode` 维护，
 * 且站内待办场景不会被误开放外部渠道配置。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public class NotifyChannelConfigServiceImplTest {

    @Test
    public void shouldRejectSaveWhenSceneDoesNotSupportExternalChannel() throws Exception {
        NotifyChannelConfigServiceImpl service = buildService();
        ChannelMapperState state = new ChannelMapperState();
        injectMapper(service, state);

        try {
            service.saveChannelConfigs(NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(),
                    buildSingleEnabledChannel("wx-template-001"));
            Assert.fail("Expected ServiceException");
        } catch (ServiceException ex) {
            Assert.assertTrue(ex.getMessage().contains("不支持外部渠道"));
        }
    }

    @Test
    public void shouldRoundTripMiniProgramChannelConfigBySceneCode() throws Exception {
        NotifyChannelConfigServiceImpl service = buildService();
        ChannelMapperState state = new ChannelMapperState();
        injectMapper(service, state);

        service.saveChannelConfigs(
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode(),
                buildSingleEnabledChannel("wx-template-001")
        );

        Assert.assertEquals(1, state.channels.size());
        Assert.assertTrue(state.channels.get(0).getConfigJson().contains("templateId"));

        List<NotifyTemplateChannelVO> channelVOList = service.listChannelConfigs(
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode()
        );
        Assert.assertEquals(1, channelVOList.size());
        Assert.assertEquals("wx-template-001", channelVOList.get(0).getTemplateId());
    }

    @Test
    public void shouldFilterDisabledRuntimeChannelsButKeepConfigPresence() throws Exception {
        NotifyChannelConfigServiceImpl service = buildService();
        ChannelMapperState state = new ChannelMapperState();
        SysNotifyTemplateChannel disabledChannel = new SysNotifyTemplateChannel();
        disabledChannel.setId(1L);
        disabledChannel.setSceneCode(NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode());
        disabledChannel.setChannelType(NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode());
        disabledChannel.setChannelEnabled(0);
        disabledChannel.setConfigJson("{\"templateId\":\"wx-template-001\",\"pagePathTemplate\":\"pages/order/evaluate?workOrderId=${workOrderId}\",\"fieldMapping\":[{\"field\":\"thing1\",\"value\":\"${orderNo}\"}]}");
        state.channels.add(disabledChannel);
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
        if (TableInfoHelper.getTableInfo(SysNotifyTemplateChannel.class) == null) {
            Configuration configuration = new Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "notify-channel-config-test");
            assistant.setCurrentNamespace(SysNotifyTemplateChannelMapper.class.getName());
            TableInfoHelper.initTableInfo(assistant, SysNotifyTemplateChannel.class);
        }
    }

    private void injectMapper(NotifyChannelConfigServiceImpl service, ChannelMapperState state) throws Exception {
        setField(service, "sysNotifyTemplateChannelMapper", createTemplateChannelMapperProxy(state));
    }

    private List<NotifyTemplateChannelDTO> buildSingleEnabledChannel(String templateId) {
        List<NotifyTemplateChannelDTO> channelConfigs = new ArrayList<>();
        NotifyTemplateChannelDTO dto = new NotifyTemplateChannelDTO();
        dto.setChannelType(NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode());
        dto.setChannelEnabled(1);
        dto.setTemplateId(templateId);
        dto.setPagePathTemplate("pages/order/evaluate?workOrderId=${workOrderId}");
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
    private SysNotifyTemplateChannelMapper createTemplateChannelMapperProxy(ChannelMapperState state) {
        return (SysNotifyTemplateChannelMapper) Proxy.newProxyInstance(
                SysNotifyTemplateChannelMapper.class.getClassLoader(),
                new Class[]{SysNotifyTemplateChannelMapper.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("selectList".equals(methodName)) {
                        return selectChannels(state.channels, args[0]);
                    }
                    if ("selectCount".equals(methodName)) {
                        return countChannels(state.channels, args[0]);
                    }
                    if ("delete".equals(methodName)) {
                        deleteChannels(state.channels, args[0]);
                        return 1;
                    }
                    if ("insert".equals(methodName)) {
                        SysNotifyTemplateChannel channel = (SysNotifyTemplateChannel) args[0];
                        channel.setId((long) (state.channels.size() + 1));
                        state.channels.add(cloneChannel(channel));
                        return 1;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    @SuppressWarnings("unchecked")
    private List<SysNotifyTemplateChannel> selectChannels(List<SysNotifyTemplateChannel> channels, Object wrapper) {
        String sqlSegment = String.valueOf(invokeWrapperMethod(wrapper, "getSqlSegment"));
        Map<String, Object> params = (Map<String, Object>) invokeWrapperMethod(wrapper, "getParamNameValuePairs");
        List<SysNotifyTemplateChannel> matches = new ArrayList<>();
        for (SysNotifyTemplateChannel channel : channels) {
            if (sqlSegment.contains("scene_code") && !params.values().contains(channel.getSceneCode())) {
                continue;
            }
            if (sqlSegment.contains("channel_type") && !params.values().contains(channel.getChannelType())) {
                continue;
            }
            if (sqlSegment.contains("channel_enabled") && !params.values().contains(channel.getChannelEnabled())) {
                continue;
            }
            matches.add(cloneChannel(channel));
        }
        return matches;
    }

    @SuppressWarnings("unchecked")
    private Long countChannels(List<SysNotifyTemplateChannel> channels, Object wrapper) {
        String sqlSegment = String.valueOf(invokeWrapperMethod(wrapper, "getSqlSegment"));
        Map<String, Object> params = (Map<String, Object>) invokeWrapperMethod(wrapper, "getParamNameValuePairs");
        long count = 0L;
        for (SysNotifyTemplateChannel channel : channels) {
            if (sqlSegment.contains("scene_code") && !params.values().contains(channel.getSceneCode())) {
                continue;
            }
            if (sqlSegment.contains("channel_type") && !params.values().contains(channel.getChannelType())) {
                continue;
            }
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private void deleteChannels(List<SysNotifyTemplateChannel> channels, Object wrapper) {
        Map<String, Object> params = (Map<String, Object>) invokeWrapperMethod(wrapper, "getParamNameValuePairs");
        Object sceneCode = params.values().stream()
                .filter(String.class::isInstance)
                .findFirst()
                .orElse(null);
        channels.removeIf(channel -> Objects.equals(channel.getSceneCode(), sceneCode));
    }

    private Object invokeWrapperMethod(Object wrapper, String methodName) {
        try {
            Method method = wrapper.getClass().getMethod(methodName);
            return method.invoke(wrapper);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to inspect wrapper method: " + methodName, ex);
        }
    }

    private SysNotifyTemplateChannel cloneChannel(SysNotifyTemplateChannel channel) {
        return BeanUtil.copyProperties(channel, SysNotifyTemplateChannel.class);
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
        private final List<SysNotifyTemplateChannel> channels = new ArrayList<>();
    }
}
