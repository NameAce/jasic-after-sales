package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.enums.WechatMiniProgramScene;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateChannelDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplatePreviewDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyTemplate;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyTemplateChannel;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyRouteTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTemplateCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTemplateSourceEnum;
import com.jasic.aftersales.system.notify.domain.query.NotifyTemplateQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplatePreviewVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateVO;
import com.jasic.aftersales.system.notify.mapper.SysNotifyTemplateChannelMapper;
import com.jasic.aftersales.system.notify.mapper.SysNotifyTemplateMapper;
import com.jasic.aftersales.system.notify.service.NotifyTemplateService;
import com.jasic.aftersales.system.notify.support.NotifyTemplateCacheValue;
import com.jasic.aftersales.system.notify.support.NotifyTemplateChannelConfig;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * NotifyTemplateServiceImpl。
 *
 * <p>服务实现层组件，负责核心业务流程与规则落地。</p>
 */
@Slf4j
@Service
public class NotifyTemplateServiceImpl implements NotifyTemplateService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");
    private static final int TITLE_MAX_LENGTH = 128;
    private static final int SUMMARY_MAX_LENGTH = 255;
    private static final int ROUTE_VALUE_MAX_LENGTH = 128;
    private static final String TEMPLATE_CACHE_KEY_PREFIX = "notify:template:";

    /**
     * 系统通知模板Mapper数据访问接口。
     */
    @Resource
    private SysNotifyTemplateMapper sysNotifyTemplateMapper;

    @Resource
    private SysNotifyTemplateChannelMapper sysNotifyTemplateChannelMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理initCache业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     */
    @PostConstruct
    public void initCache() {
        // 调用refreshCache方法，复用统一能力并保证业务规则一致。
        refreshCache();
    }

    /**
     * 分页查询通知模板列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public PageResult<NotifyTemplateVO> listPage(NotifyTemplateQuery query) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateQuery actualQuery = query == null ? new NotifyTemplateQuery() : query;
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<SysNotifyTemplate> page = new Page<>(actualQuery.getPageNum(), actualQuery.getPageSize());
        LambdaQueryWrapper<SysNotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(actualQuery.getTemplateCode())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysNotifyTemplate::getTemplateCode, actualQuery.getTemplateCode().trim());
        }
        if (StrUtil.isNotBlank(actualQuery.getTemplateName())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysNotifyTemplate::getTemplateName, actualQuery.getTemplateName().trim());
        }
        if (StrUtil.isNotBlank(actualQuery.getTemplateSource())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyTemplate::getTemplateSource, actualQuery.getTemplateSource().trim());
        }
        if (StrUtil.isNotBlank(actualQuery.getEventType())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyTemplate::getEventType, actualQuery.getEventType().trim());
        }
        wrapper.orderByAsc(SysNotifyTemplate::getTemplateCode)
                .orderByAsc(SysNotifyTemplate::getTemplateSource)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(SysNotifyTemplate::getId);
        // 调用selectPage方法，复用统一能力并保证业务规则一致。
        Page<SysNotifyTemplate> result = sysNotifyTemplateMapper.selectPage(page, wrapper);
        // 调用toList方法，复用统一能力并保证业务规则一致。
        List<NotifyTemplateVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), actualQuery.getPageNum(), actualQuery.getPageSize());
    }

    /**
     * 根据ID查询通知模板详情。
     *
     * @return 处理结果
     */
    @Override
    public NotifyTemplateVO getById(Long id) {
        // 调用selectById方法，复用统一能力并保证业务规则一致。
        SysNotifyTemplate entity = sysNotifyTemplateMapper.selectById(id);
        return entity == null ? null : toVO(entity);
    }

    /**
     * 新增自定义。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Override
    public Long saveCustom(NotifyTemplateDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(dto.getTemplateCode());
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        SysNotifyTemplate builtInTemplate = getRequiredBuiltInTemplate(templateCodeEnum.getCode());
        if (getCustomTemplate(templateCodeEnum.getCode()) != null) {
            throw new ServiceException("Custom template already exists for templateCode " + templateCodeEnum.getCode());
        }
        // 调用buildCustomTemplate方法，复用统一能力并保证业务规则一致。
        SysNotifyTemplate entity = buildCustomTemplate(dto, templateCodeEnum, builtInTemplate, null);
        // 调用insert方法，复用统一能力并保证业务规则一致。
        sysNotifyTemplateMapper.insert(entity);
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        refreshTemplateCache(templateCodeEnum.getCode());
        return entity.getId();
    }

    /**
     * 更新自定义。
     *
     * @param dto 参数
     */
    @Override
    public void updateCustom(NotifyTemplateDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("Template id cannot be null");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyTemplate existing = sysNotifyTemplateMapper.selectById(dto.getId());
        if (existing == null) {
            throw new ServiceException("Notify template not found");
        }
        if (!NotifyTemplateSourceEnum.CUSTOM.getCode().equals(existing.getTemplateSource())) {
            throw new ServiceException("Built-in templates cannot be edited");
        }
        if (StrUtil.isNotBlank(dto.getTemplateCode())
                && !StrUtil.equals(dto.getTemplateCode().trim(), existing.getTemplateCode())) {
            throw new ServiceException("Template code cannot be changed");
        }
        // 调用getTemplateCode方法，复用统一能力并保证业务规则一致。
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(existing.getTemplateCode());
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        SysNotifyTemplate builtInTemplate = getRequiredBuiltInTemplate(templateCodeEnum.getCode());
        // 调用buildCustomTemplate方法，复用统一能力并保证业务规则一致。
        SysNotifyTemplate entity = buildCustomTemplate(dto, templateCodeEnum, builtInTemplate, existing);
        // 调用getId方法，复用统一能力并保证业务规则一致。
        entity.setId(existing.getId());
        // 调用updateById方法，复用统一能力并保证业务规则一致。
        sysNotifyTemplateMapper.updateById(entity);
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        refreshTemplateCache(templateCodeEnum.getCode());
    }

    /**
     * 删除自定义。
     */
    @Override
    public void removeCustom(Long id) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyTemplate existing = sysNotifyTemplateMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("Notify template not found");
        }
        if (!NotifyTemplateSourceEnum.CUSTOM.getCode().equals(existing.getTemplateSource())) {
            throw new ServiceException("Built-in templates cannot be deleted");
        }
        // 调用deleteById方法，复用统一能力并保证业务规则一致。
        sysNotifyTemplateMapper.deleteById(id);
        // 调用getTemplateCode方法，复用统一能力并保证业务规则一致。
        refreshTemplateCache(existing.getTemplateCode());
    }

    /**
     * 预览通知模板内容。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Override
    public NotifyTemplatePreviewVO preview(NotifyTemplatePreviewDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(dto.getTemplateCode());
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        SysNotifyTemplate builtInTemplate = getRequiredBuiltInTemplate(templateCodeEnum.getCode());
        // 调用getVariables方法，复用统一能力并保证业务规则一致。
        Map<String, Object> variables = dto.getVariables() == null ? Collections.emptyMap() : dto.getVariables();
        NotifyTemplateRenderResult renderResult = renderWithResolvedTemplates(
                templateCodeEnum.getCode(),
                builtInTemplate,
                buildPreviewCustomTemplate(dto, templateCodeEnum, builtInTemplate),
                variables,
                true
        );
        // 调用NotifyTemplatePreviewVO方法，复用统一能力并保证业务规则一致。
        NotifyTemplatePreviewVO previewVO = new NotifyTemplatePreviewVO();
        // 调用isNotifyEnabled方法，复用统一能力并保证业务规则一致。
        previewVO.setNotifyEnabled(renderResult.isNotifyEnabled());
        // 调用getTemplateSource方法，复用统一能力并保证业务规则一致。
        previewVO.setTemplateSource(renderResult.getTemplateSource());
        // 调用getTitle方法，复用统一能力并保证业务规则一致。
        previewVO.setTitle(renderResult.getTitle());
        // 调用getSummary方法，复用统一能力并保证业务规则一致。
        previewVO.setSummary(renderResult.getSummary());
        // 调用getRouteType方法，复用统一能力并保证业务规则一致。
        previewVO.setRouteType(renderResult.getRouteType());
        // 调用getRouteValue方法，复用统一能力并保证业务规则一致。
        previewVO.setRouteValue(renderResult.getRouteValue());
        // 调用getErrors方法，复用统一能力并保证业务规则一致。
        previewVO.setErrors(new ArrayList<>(renderResult.getErrors()));
        return previewVO;
    }

    /**
     * 渲染通知模板。
     *
     * @param templateCode 参数
     * @param variables 参数
     * @return 处理结果
     */
    @Override
    public NotifyTemplateRenderResult render(String templateCode, Map<String, Object> variables) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(templateCode);
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        NotifyTemplateCacheValue cacheValue = getTemplateCache(templateCodeEnum.getCode());
        if (cacheValue.getBuiltInTemplate() == null) {
            throw new ServiceException("Built-in notify template not found: " + templateCodeEnum.getCode());
        }
        return renderWithResolvedTemplates(
                templateCodeEnum.getCode(),
                cacheValue.getBuiltInTemplate(),
                cacheValue.getCustomTemplate(),
                variables == null ? Collections.emptyMap() : variables,
                false
        );
    }

    /**
     * 判断是否通知Enabled。
     *
     * @param templateCode 参数
     */
    @Override
    public boolean isNotifyEnabled(String templateCode) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(templateCode);
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        NotifyTemplateCacheValue cacheValue = getTemplateCache(templateCodeEnum.getCode());
        if (cacheValue.getBuiltInTemplate() == null) {
            throw new ServiceException("Built-in notify template not found: " + templateCodeEnum.getCode());
        }
        // 调用getCustomTemplate方法，复用统一能力并保证业务规则一致。
        SysNotifyTemplate customTemplate = cacheValue.getCustomTemplate();
        return customTemplate == null || !Objects.equals(customTemplate.getNotifyEnabled(), 0);
    }

    /**
     * 分页查询渠道Configs列表。
     *
     * @param templateCode 参数
     * @return 处理结果
     */
    @Override
    public List<NotifyTemplateChannelVO> listChannelConfigs(String templateCode) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(templateCode);
        LambdaQueryWrapper<SysNotifyTemplateChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyTemplateChannel::getTemplateCode, templateCodeEnum.getCode())
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysNotifyTemplateChannel::getId);
        return sysNotifyTemplateChannelMapper.selectList(wrapper).stream()
                .map(this::toChannelVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * 新增渠道Configs。
     *
     * @param templateCode 参数
     * @param channelConfigs 参数
     */
    @Override
    public void saveChannelConfigs(String templateCode, List<NotifyTemplateChannelDTO> channelConfigs) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(templateCode);
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        List<NotifyTemplateChannelDTO> actualConfigs = channelConfigs == null ? Collections.emptyList() : channelConfigs;
        // 说明：执行该步骤以保证业务流程正确。
        validateChannelConfigs(templateCodeEnum, actualConfigs);

        LambdaQueryWrapper<SysNotifyTemplateChannel> deleteWrapper = new LambdaQueryWrapper<>();
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        deleteWrapper.eq(SysNotifyTemplateChannel::getTemplateCode, templateCodeEnum.getCode());
        // 调用delete方法，复用统一能力并保证业务规则一致。
        sysNotifyTemplateChannelMapper.delete(deleteWrapper);

        for (NotifyTemplateChannelDTO dto : actualConfigs) {
            // 调用SysNotifyTemplateChannel方法，复用统一能力并保证业务规则一致。
            SysNotifyTemplateChannel entity = new SysNotifyTemplateChannel();
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            entity.setTemplateCode(templateCodeEnum.getCode());
            // 调用trim方法，复用统一能力并保证业务规则一致。
            entity.setChannelType(dto.getChannelType().trim());
            // 调用getChannelEnabled方法，复用统一能力并保证业务规则一致。
            entity.setChannelEnabled(dto.getChannelEnabled());
            // 调用getChannelScene方法，复用统一能力并保证业务规则一致。
            entity.setChannelScene(normalizeField(dto.getChannelScene()));
            // 调用buildChannelConfigJson方法，复用统一能力并保证业务规则一致。
            entity.setConfigJson(buildChannelConfigJson(templateCodeEnum, dto));
            // 调用getRemark方法，复用统一能力并保证业务规则一致。
            entity.setRemark(normalizeRemark(dto.getRemark()));
            // 调用insert方法，复用统一能力并保证业务规则一致。
            sysNotifyTemplateChannelMapper.insert(entity);
        }
    }

    /**
     * 刷新通知模板缓存。
     */
    @Override
    public void refreshCache() {
        // 调用clearAllCache方法，复用统一能力并保证业务规则一致。
        clearAllCache();
        // 说明：执行该步骤以保证业务流程正确。
        List<SysNotifyTemplate> templates = sysNotifyTemplateMapper.selectList(new LambdaQueryWrapper<>());
        // 调用buildCacheMap方法，复用统一能力并保证业务规则一致。
        Map<String, NotifyTemplateCacheValue> grouped = buildCacheMap(templates);
        for (Map.Entry<String, NotifyTemplateCacheValue> entry : grouped.entrySet()) {
            // 调用getValue方法，复用统一能力并保证业务规则一致。
            redisTemplate.opsForValue().set(getCacheKey(entry.getKey()), JSONUtil.toJsonStr(entry.getValue()));
        }
    }

    /**
     * 构建自定义模板。
     *
     * @param dto 参数
     * @param templateCodeEnum 参数
     * @param builtInTemplate 参数
     * @param existing 参数
     * @return 处理结果
     */
    private SysNotifyTemplate buildCustomTemplate(NotifyTemplateDTO dto, NotifyTemplateCodeEnum templateCodeEnum,
                                                  SysNotifyTemplate builtInTemplate, SysNotifyTemplate existing) {
        // 说明：执行该步骤以保证业务流程正确。
        validateSwitch(dto.getNotifyEnabled(), "notifyEnabled");
        // 说明：执行该步骤以保证业务流程正确。
        validateSwitch(dto.getOverrideEnabled(), "overrideEnabled");
        // 调用SysNotifyTemplate方法，复用统一能力并保证业务规则一致。
        SysNotifyTemplate entity = existing == null ? new SysNotifyTemplate() : existing;
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        entity.setTemplateCode(templateCodeEnum.getCode());
        // 调用trim方法，复用统一能力并保证业务规则一致。
        entity.setTemplateName(StrUtil.isBlank(dto.getTemplateName()) ? builtInTemplate.getTemplateName() : dto.getTemplateName().trim());
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        entity.setTemplateSource(NotifyTemplateSourceEnum.CUSTOM.getCode());
        // 调用getBizType方法，复用统一能力并保证业务规则一致。
        entity.setBizType(templateCodeEnum.getBizType());
        // 调用getEventType方法，复用统一能力并保证业务规则一致。
        entity.setEventType(templateCodeEnum.getEventType());
        // 调用getMessageType方法，复用统一能力并保证业务规则一致。
        entity.setMessageType(templateCodeEnum.getMessageType());
        // 调用getNotifyEnabled方法，复用统一能力并保证业务规则一致。
        entity.setNotifyEnabled(dto.getNotifyEnabled());
        // 调用getOverrideEnabled方法，复用统一能力并保证业务规则一致。
        entity.setOverrideEnabled(dto.getOverrideEnabled());
        // 调用getRouteType方法，复用统一能力并保证业务规则一致。
        entity.setRouteType(normalizeRouteType(dto.getRouteType()));
        // 调用getTitleTemplate方法，复用统一能力并保证业务规则一致。
        entity.setTitleTemplate(normalizeField(dto.getTitleTemplate()));
        // 调用getSummaryTemplate方法，复用统一能力并保证业务规则一致。
        entity.setSummaryTemplate(normalizeField(dto.getSummaryTemplate()));
        // 调用getRouteValueTemplate方法，复用统一能力并保证业务规则一致。
        entity.setRouteValueTemplate(normalizeField(dto.getRouteValueTemplate()));
        // 调用buildVariablesJson方法，复用统一能力并保证业务规则一致。
        entity.setVariablesJson(buildVariablesJson(templateCodeEnum));
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        entity.setRemark(normalizeRemark(dto.getRemark()));
        // 调用validateTemplatePayload方法，复用统一能力并保证业务规则一致。
        validateTemplatePayload(entity, templateCodeEnum);
        return entity;
    }

    /**
     * 构建预览自定义模板。
     *
     * @param dto 参数
     * @param templateCodeEnum 参数
     * @param builtInTemplate 参数
     * @return 处理结果
     */
    private SysNotifyTemplate buildPreviewCustomTemplate(NotifyTemplatePreviewDTO dto, NotifyTemplateCodeEnum templateCodeEnum,
                                                         SysNotifyTemplate builtInTemplate) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyTemplate entity = new SysNotifyTemplate();
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        entity.setTemplateCode(templateCodeEnum.getCode());
        // 调用getTemplateName方法，复用统一能力并保证业务规则一致。
        entity.setTemplateName(builtInTemplate.getTemplateName());
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        entity.setTemplateSource(NotifyTemplateSourceEnum.CUSTOM.getCode());
        // 调用getBizType方法，复用统一能力并保证业务规则一致。
        entity.setBizType(templateCodeEnum.getBizType());
        // 调用getEventType方法，复用统一能力并保证业务规则一致。
        entity.setEventType(templateCodeEnum.getEventType());
        // 调用getMessageType方法，复用统一能力并保证业务规则一致。
        entity.setMessageType(templateCodeEnum.getMessageType());
        // 调用getNotifyEnabled方法，复用统一能力并保证业务规则一致。
        entity.setNotifyEnabled(dto.getNotifyEnabled() == null ? 1 : dto.getNotifyEnabled());
        // 调用getOverrideEnabled方法，复用统一能力并保证业务规则一致。
        entity.setOverrideEnabled(dto.getOverrideEnabled() == null ? 1 : dto.getOverrideEnabled());
        // 调用getRouteType方法，复用统一能力并保证业务规则一致。
        entity.setRouteType(normalizeRouteType(dto.getRouteType()));
        // 调用getTitleTemplate方法，复用统一能力并保证业务规则一致。
        entity.setTitleTemplate(normalizeField(dto.getTitleTemplate()));
        // 调用getSummaryTemplate方法，复用统一能力并保证业务规则一致。
        entity.setSummaryTemplate(normalizeField(dto.getSummaryTemplate()));
        // 调用getRouteValueTemplate方法，复用统一能力并保证业务规则一致。
        entity.setRouteValueTemplate(normalizeField(dto.getRouteValueTemplate()));
        // 调用buildVariablesJson方法，复用统一能力并保证业务规则一致。
        entity.setVariablesJson(buildVariablesJson(templateCodeEnum));
        // 调用getNotifyEnabled方法，复用统一能力并保证业务规则一致。
        validateSwitch(entity.getNotifyEnabled(), "notifyEnabled");
        // 说明：执行该步骤以保证业务流程正确。
        validateSwitch(entity.getOverrideEnabled(), "overrideEnabled");
        // 调用validateTemplatePayload方法，复用统一能力并保证业务规则一致。
        validateTemplatePayload(entity, templateCodeEnum);
        return entity;
    }

    /**
     * 渲染WithResolvedTemplates。
     *
     * @param templateCode 参数
     * @param builtInTemplate 参数
     * @param customTemplate 参数
     * @param variables 参数
     * @param previewMode 参数
     * @return 处理结果
     */
    private NotifyTemplateRenderResult renderWithResolvedTemplates(String templateCode, SysNotifyTemplate builtInTemplate,
                                                                   SysNotifyTemplate customTemplate, Map<String, Object> variables,
                                                                   boolean previewMode) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateRenderResult result = new NotifyTemplateRenderResult();
        // 调用setTemplateCode方法，复用统一能力并保证业务规则一致。
        result.setTemplateCode(templateCode);
        if (customTemplate != null && isSwitchOff(customTemplate.getNotifyEnabled())) {
            // 调用setNotifyEnabled方法，复用统一能力并保证业务规则一致。
            result.setNotifyEnabled(false);
            // 调用getTemplateSource方法，复用统一能力并保证业务规则一致。
            result.setTemplateSource(customTemplate.getTemplateSource());
            return result;
        }
        // 调用setNotifyEnabled方法，复用统一能力并保证业务规则一致。
        result.setNotifyEnabled(true);
        if (customTemplate == null || isSwitchOff(customTemplate.getOverrideEnabled())) {
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            applyRenderedTemplate(result, builtInTemplate, variables, NotifyTemplateSourceEnum.BUILT_IN.getCode());
            return result;
        }
        try {
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            applyRenderedTemplate(result, mergeTemplate(builtInTemplate, customTemplate), variables, NotifyTemplateSourceEnum.CUSTOM.getCode());
            return result;
        } catch (ServiceException ex) {
            // 调用getMessage方法，复用统一能力并保证业务规则一致。
            result.addError(ex.getMessage());
            if (!previewMode) {
                log.warn("Notify template custom render failed, fallback to built-in. templateCode={}, error={}",
                        // 调用getMessage方法，复用统一能力并保证业务规则一致。
                        templateCode, ex.getMessage());
            }
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            applyRenderedTemplate(result, builtInTemplate, variables, NotifyTemplateSourceEnum.BUILT_IN.getCode());
            return result;
        }
    }

    /**
     * applyRendered模板。
     *
     * @param result 参数
     * @param template 参数
     * @param variables 参数
     * @param templateSource 参数
     */
    private void applyRenderedTemplate(NotifyTemplateRenderResult result, SysNotifyTemplate template,
                                       Map<String, Object> variables, String templateSource) {
        // 调用getTitleTemplate方法，复用统一能力并保证业务规则一致。
        String title = renderContent(template.getTitleTemplate(), variables);
        // 调用getSummaryTemplate方法，复用统一能力并保证业务规则一致。
        String summary = renderContent(template.getSummaryTemplate(), variables);
        // 调用getRouteType方法，复用统一能力并保证业务规则一致。
        String routeType = normalizeField(template.getRouteType());
        // 调用getRouteValueTemplate方法，复用统一能力并保证业务规则一致。
        String routeValue = renderContent(template.getRouteValueTemplate(), variables);
        // 说明：执行该步骤以保证业务流程正确。
        validateRenderedContent(title, summary, routeType, routeValue);
        // 调用setTemplateSource方法，复用统一能力并保证业务规则一致。
        result.setTemplateSource(templateSource);
        // 调用setTitle方法，复用统一能力并保证业务规则一致。
        result.setTitle(title);
        // 调用setSummary方法，复用统一能力并保证业务规则一致。
        result.setSummary(summary);
        // 调用setRouteType方法，复用统一能力并保证业务规则一致。
        result.setRouteType(routeType);
        // 调用setRouteValue方法，复用统一能力并保证业务规则一致。
        result.setRouteValue(routeValue);
    }

    /**
     * merge模板。
     *
     * @param builtInTemplate 参数
     * @param customTemplate 参数
     * @return 处理结果
     */
    private SysNotifyTemplate mergeTemplate(SysNotifyTemplate builtInTemplate, SysNotifyTemplate customTemplate) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyTemplate merged = new SysNotifyTemplate();
        // 调用getTemplateCode方法，复用统一能力并保证业务规则一致。
        merged.setTemplateCode(customTemplate.getTemplateCode());
        // 调用getTemplateName方法，复用统一能力并保证业务规则一致。
        merged.setTemplateName(customTemplate.getTemplateName());
        // 调用getTemplateSource方法，复用统一能力并保证业务规则一致。
        merged.setTemplateSource(customTemplate.getTemplateSource());
        // 调用getBizType方法，复用统一能力并保证业务规则一致。
        merged.setBizType(customTemplate.getBizType());
        // 调用getEventType方法，复用统一能力并保证业务规则一致。
        merged.setEventType(customTemplate.getEventType());
        // 调用getMessageType方法，复用统一能力并保证业务规则一致。
        merged.setMessageType(customTemplate.getMessageType());
        // 调用getNotifyEnabled方法，复用统一能力并保证业务规则一致。
        merged.setNotifyEnabled(customTemplate.getNotifyEnabled());
        // 调用getOverrideEnabled方法，复用统一能力并保证业务规则一致。
        merged.setOverrideEnabled(customTemplate.getOverrideEnabled());
        // 调用getRouteType方法，复用统一能力并保证业务规则一致。
        merged.setRouteType(StrUtil.isBlank(customTemplate.getRouteType()) ? builtInTemplate.getRouteType() : customTemplate.getRouteType());
        // 调用getTitleTemplate方法，复用统一能力并保证业务规则一致。
        merged.setTitleTemplate(StrUtil.isBlank(customTemplate.getTitleTemplate()) ? builtInTemplate.getTitleTemplate() : customTemplate.getTitleTemplate());
        // 调用getSummaryTemplate方法，复用统一能力并保证业务规则一致。
        merged.setSummaryTemplate(StrUtil.isBlank(customTemplate.getSummaryTemplate()) ? builtInTemplate.getSummaryTemplate() : customTemplate.getSummaryTemplate());
        // 调用getRouteValueTemplate方法，复用统一能力并保证业务规则一致。
        merged.setRouteValueTemplate(StrUtil.isBlank(customTemplate.getRouteValueTemplate()) ? builtInTemplate.getRouteValueTemplate() : customTemplate.getRouteValueTemplate());
        return merged;
    }

    /**
     * 渲染Content。
     *
     * @param template 参数
     * @param variables 参数
     * @return 处理结果
     */
    private String renderContent(String template, Map<String, Object> variables) {
        // 调用normalizeField方法，复用统一能力并保证业务规则一致。
        String actualTemplate = normalizeField(template);
        if (actualTemplate == null) {
            return null;
        }
        // 调用matcher方法，复用统一能力并保证业务规则一致。
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(actualTemplate);
        // 调用StringBuffer方法，复用统一能力并保证业务规则一致。
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            // 调用group方法，复用统一能力并保证业务规则一致。
            String variableName = matcher.group(1);
            // 调用get方法，复用统一能力并保证业务规则一致。
            Object value = variables.get(variableName);
            // 调用trim方法，复用统一能力并保证业务规则一致。
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value).trim()));
        }
        // 调用appendTail方法，复用统一能力并保证业务规则一致。
        matcher.appendTail(buffer);
        return buffer.toString().trim();
    }

    /**
     * 校验RenderedContent。
     *
     * @param title 参数
     * @param summary 参数
     * @param routeType 参数
     * @param routeValue 参数
     */
    private void validateRenderedContent(String title, String summary, String routeType, String routeValue) {
        if (StrUtil.isBlank(title)) {
            throw new ServiceException("Rendered title cannot be blank");
        }
        if (StrUtil.isBlank(summary)) {
            throw new ServiceException("Rendered summary cannot be blank");
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (StrUtil.isBlank(routeType) || NotifyRouteTypeEnum.getByCode(routeType) == null) {
            throw new ServiceException("Rendered routeType is invalid");
        }
        if (StrUtil.isBlank(routeValue)) {
            throw new ServiceException("Rendered routeValue cannot be blank");
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new ServiceException("Rendered title exceeds max length");
        }
        if (summary.length() > SUMMARY_MAX_LENGTH) {
            throw new ServiceException("Rendered summary exceeds max length");
        }
        if (routeValue.length() > ROUTE_VALUE_MAX_LENGTH) {
            throw new ServiceException("Rendered routeValue exceeds max length");
        }
    }

    /**
     * 校验模板Payload。
     *
     * @param template 参数
     * @param templateCodeEnum 参数
     */
    private void validateTemplatePayload(SysNotifyTemplate template, NotifyTemplateCodeEnum templateCodeEnum) {
        // 说明：执行该步骤以保证业务流程正确。
        validateRouteType(template.getRouteType());
        // 调用getTitleTemplate方法，复用统一能力并保证业务规则一致。
        validateLength(template.getTitleTemplate(), TITLE_MAX_LENGTH, "titleTemplate");
        // 调用getSummaryTemplate方法，复用统一能力并保证业务规则一致。
        validateLength(template.getSummaryTemplate(), SUMMARY_MAX_LENGTH, "summaryTemplate");
        // 调用getRouteValueTemplate方法，复用统一能力并保证业务规则一致。
        validateLength(template.getRouteValueTemplate(), ROUTE_VALUE_MAX_LENGTH, "routeValueTemplate");
        // 调用getAllowedTemplateVariables方法，复用统一能力并保证业务规则一致。
        validatePlaceholders(template.getTitleTemplate(), getAllowedTemplateVariables(templateCodeEnum));
        // 调用getAllowedTemplateVariables方法，复用统一能力并保证业务规则一致。
        validatePlaceholders(template.getSummaryTemplate(), getAllowedTemplateVariables(templateCodeEnum));
        // 调用getAllowedTemplateVariables方法，复用统一能力并保证业务规则一致。
        validatePlaceholders(template.getRouteValueTemplate(), getAllowedTemplateVariables(templateCodeEnum));
    }

    /**
     * 校验渠道Configs。
     *
     * @param templateCodeEnum 参数
     * @param channelConfigs 参数
     */
    private void validateChannelConfigs(NotifyTemplateCodeEnum templateCodeEnum, List<NotifyTemplateChannelDTO> channelConfigs) {
        Set<String> uniqueKeys = new LinkedHashSet<>();
        // 说明：执行该步骤以保证业务流程正确。
        for (NotifyTemplateChannelDTO dto : channelConfigs) {
            if (dto == null) {
                throw new ServiceException("Channel config cannot be null");
            }
            if (StrUtil.isNotBlank(dto.getTemplateCode())
                    && !StrUtil.equals(dto.getTemplateCode().trim(), templateCodeEnum.getCode())) {
                throw new ServiceException("Channel config templateCode does not match path parameter");
            }
            // 调用getChannelType方法，复用统一能力并保证业务规则一致。
            NotifyChannelTypeEnum channelTypeEnum = NotifyChannelTypeEnum.getByCode(dto.getChannelType());
            if (channelTypeEnum == null) {
                throw new ServiceException("Unsupported channel type: " + dto.getChannelType());
            }
            // 说明：执行该步骤以保证业务流程正确。
            validateSwitch(dto.getChannelEnabled(), "channelEnabled");
            // 调用getChannelScene方法，复用统一能力并保证业务规则一致。
            String channelScene = normalizeField(dto.getChannelScene());
            // 调用blankToDefault方法，复用统一能力并保证业务规则一致。
            String uniqueKey = channelTypeEnum.getCode() + ":" + StrUtil.blankToDefault(channelScene, "");
            if (!uniqueKeys.add(uniqueKey)) {
                throw new ServiceException("Duplicate channel config found: " + uniqueKey);
            }
            if (NotifyChannelTypeEnum.MP_SUBSCRIBE == channelTypeEnum) {
                // 调用validateMiniProgramChannelConfig方法，复用统一能力并保证业务规则一致。
                validateMiniProgramChannelConfig(templateCodeEnum, dto, channelScene);
            }
        }
    }

    /**
     * 校验小程序程序渠道配置。
     *
     * @param templateCodeEnum 参数
     * @param dto 参数
     * @param channelScene 参数
     */
    private void validateMiniProgramChannelConfig(NotifyTemplateCodeEnum templateCodeEnum,
                                                  NotifyTemplateChannelDTO dto, String channelScene) {
        if (StrUtil.isBlank(channelScene)) {
            throw new ServiceException("Mini program channel scene cannot be blank");
        }
        if (!WechatMiniProgramScene.B.getCode().equals(channelScene)
                && !WechatMiniProgramScene.C.getCode().equals(channelScene)) {
            throw new ServiceException("Unsupported mini program channel scene: " + channelScene);
        }
        if (StrUtil.isNotBlank(dto.getPagePathTemplate())) {
            // 说明：执行该步骤以保证业务流程正确。
            validatePlaceholders(dto.getPagePathTemplate(), getAllowedChannelRouteVariables(templateCodeEnum));
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<NotifyChannelFieldMappingDTO> fieldMapping = dto.getFieldMapping() == null
                // 调用getFieldMapping方法，复用统一能力并保证业务规则一致。
                ? Collections.emptyList() : dto.getFieldMapping();
        for (NotifyChannelFieldMappingDTO item : fieldMapping) {
            if (item == null) {
                throw new ServiceException("Field mapping item cannot be null");
            }
            if (StrUtil.isBlank(item.getField())) {
                throw new ServiceException("Field mapping field cannot be blank");
            }
            if (StrUtil.isBlank(item.getValue())) {
                throw new ServiceException("Field mapping value cannot be blank");
            }
            // 调用getAllowedChannelFieldVariables方法，复用统一能力并保证业务规则一致。
            validatePlaceholders(item.getValue(), getAllowedChannelFieldVariables(templateCodeEnum));
        }
    }

    /**
     * 构建渠道配置Json。
     *
     * @param templateCodeEnum 参数
     * @param dto 参数
     * @return 处理结果
     */
    private String buildChannelConfigJson(NotifyTemplateCodeEnum templateCodeEnum, NotifyTemplateChannelDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyChannelTypeEnum channelTypeEnum = NotifyChannelTypeEnum.getByCode(dto.getChannelType());
        if (NotifyChannelTypeEnum.MP_SUBSCRIBE != channelTypeEnum) {
            return StrUtil.blankToDefault(normalizeField(dto.getConfigJson()), "{}");
        }
        // 调用NotifyTemplateChannelConfig方法，复用统一能力并保证业务规则一致。
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        // 调用getChannelScene方法，复用统一能力并保证业务规则一致。
        config.setScene(normalizeField(dto.getChannelScene()));
        // 调用getTemplateId方法，复用统一能力并保证业务规则一致。
        config.setTemplateId(normalizeField(dto.getTemplateId()));
        // 调用getPagePathTemplate方法，复用统一能力并保证业务规则一致。
        config.setPagePathTemplate(normalizeField(dto.getPagePathTemplate()));
        List<NotifyChannelFieldMappingDTO> fieldMapping = dto.getFieldMapping() == null
                // 调用getFieldMapping方法，复用统一能力并保证业务规则一致。
                ? Collections.emptyList() : dto.getFieldMapping();
        List<NotifyChannelFieldMappingDTO> normalizedFieldMapping = new ArrayList<>();
        for (NotifyChannelFieldMappingDTO item : fieldMapping) {
            // 调用NotifyChannelFieldMappingDTO方法，复用统一能力并保证业务规则一致。
            NotifyChannelFieldMappingDTO copy = new NotifyChannelFieldMappingDTO();
            // 调用getField方法，复用统一能力并保证业务规则一致。
            copy.setField(item == null ? null : normalizeField(item.getField()));
            // 调用trim方法，复用统一能力并保证业务规则一致。
            copy.setValue(item == null ? null : item.getValue().trim());
            // 调用add方法，复用统一能力并保证业务规则一致。
            normalizedFieldMapping.add(copy);
        }
        // 调用setFieldMapping方法，复用统一能力并保证业务规则一致。
        config.setFieldMapping(normalizedFieldMapping);
        // 说明：执行该步骤以保证业务流程正确。
        validateMiniProgramChannelConfig(templateCodeEnum, dto, config.getScene());
        return JSONUtil.toJsonStr(config);
    }

    /**
     * to渠道视图。
     *
     * @param entity 参数
     * @return 处理结果
     */
    private NotifyTemplateChannelVO toChannelVO(SysNotifyTemplateChannel entity) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateChannelVO vo = BeanUtil.copyProperties(entity, NotifyTemplateChannelVO.class);
        if (StrUtil.isBlank(entity.getConfigJson())) {
            return vo;
        }
        // 调用getConfigJson方法，复用统一能力并保证业务规则一致。
        NotifyTemplateChannelConfig config = JSONUtil.toBean(entity.getConfigJson(), NotifyTemplateChannelConfig.class);
        if (config != null) {
            // 调用getTemplateId方法，复用统一能力并保证业务规则一致。
            vo.setTemplateId(config.getTemplateId());
            // 调用getPagePathTemplate方法，复用统一能力并保证业务规则一致。
            vo.setPagePathTemplate(config.getPagePathTemplate());
            // 调用getFieldMapping方法，复用统一能力并保证业务规则一致。
            vo.setFieldMapping(config.getFieldMapping());
            if (StrUtil.isBlank(vo.getChannelScene())) {
                // 调用getScene方法，复用统一能力并保证业务规则一致。
                vo.setChannelScene(config.getScene());
            }
        }
        return vo;
    }

    /**
     * 校验Length。
     *
     * @param value 参数
     * @param maxLength 参数
     * @param fieldName 参数
     */
    private void validateLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new ServiceException(fieldName + " length cannot exceed " + maxLength);
        }
    }

    /**
     * 校验Placeholders。
     *
     * @param template 参数
     * @param allowedVariables 参数
     */
    private void validatePlaceholders(String template, Set<String> allowedVariables) {
        if (StrUtil.isBlank(template)) {
            return;
        }
        // 调用matcher方法，复用统一能力并保证业务规则一致。
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            // 调用group方法，复用统一能力并保证业务规则一致。
            String variableName = matcher.group(1);
            if (!allowedVariables.contains(variableName)) {
                throw new ServiceException("Unsupported template variable: " + variableName);
            }
        }
    }

    /**
     * 校验路由类型。
     *
     * @param routeType 参数
     */
    private void validateRouteType(String routeType) {
        if (StrUtil.isBlank(routeType)) {
            return;
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (NotifyRouteTypeEnum.getByCode(routeType) == null) {
            throw new ServiceException("Unsupported routeType: " + routeType);
        }
    }

    /**
     * 获取模板缓存。
     *
     * @param templateCode 参数
     * @return 处理结果
     */
    private NotifyTemplateCacheValue getTemplateCache(String templateCode) {
        // 调用getCacheKey方法，复用统一能力并保证业务规则一致。
        Object cached = redisTemplate.opsForValue().get(getCacheKey(templateCode));
        if (cached != null) {
            // 说明：执行该步骤以保证业务流程正确。
            return JSONUtil.toBean(String.valueOf(cached), NotifyTemplateCacheValue.class);
        }
        return refreshTemplateCache(templateCode);
    }

    /**
     * refresh模板缓存。
     *
     * @param templateCode 参数
     * @return 处理结果
     */
    private NotifyTemplateCacheValue refreshTemplateCache(String templateCode) {
        // 调用listByTemplateCode方法，复用统一能力并保证业务规则一致。
        NotifyTemplateCacheValue value = buildCacheValue(listByTemplateCode(templateCode));
        // 调用toJsonStr方法，复用统一能力并保证业务规则一致。
        redisTemplate.opsForValue().set(getCacheKey(templateCode), JSONUtil.toJsonStr(value));
        return value;
    }

    /**
     * 分页查询By模板编码列表。
     *
     * @param templateCode 参数
     * @return 处理结果
     */
    private List<SysNotifyTemplate> listByTemplateCode(String templateCode) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyTemplate::getTemplateCode, templateCode)
                .orderByAsc(SysNotifyTemplate::getTemplateSource)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysNotifyTemplate::getId);
        return sysNotifyTemplateMapper.selectList(wrapper);
    }

    /**
     * 获取Required内置In模板。
     *
     * @param templateCode 参数
     * @return 处理结果
     */
    private SysNotifyTemplate getRequiredBuiltInTemplate(String templateCode) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyTemplate::getTemplateCode, templateCode)
                .eq(SysNotifyTemplate::getTemplateSource, NotifyTemplateSourceEnum.BUILT_IN.getCode())
                // 调用last方法，复用统一能力并保证业务规则一致。
                .last("limit 1");
        // 调用selectOne方法，复用统一能力并保证业务规则一致。
        SysNotifyTemplate template = sysNotifyTemplateMapper.selectOne(wrapper);
        if (template == null) {
            throw new ServiceException("Built-in notify template not found: " + templateCode);
        }
        return template;
    }

    /**
     * 获取自定义模板。
     *
     * @param templateCode 参数
     * @return 处理结果
     */
    private SysNotifyTemplate getCustomTemplate(String templateCode) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyTemplate::getTemplateCode, templateCode)
                .eq(SysNotifyTemplate::getTemplateSource, NotifyTemplateSourceEnum.CUSTOM.getCode())
                // 调用last方法，复用统一能力并保证业务规则一致。
                .last("limit 1");
        return sysNotifyTemplateMapper.selectOne(wrapper);
    }

    /**
     * 获取Required模板编码。
     *
     * @param templateCode 参数
     * @return 处理结果
     */
    private NotifyTemplateCodeEnum getRequiredTemplateCode(String templateCode) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateCodeEnum value = NotifyTemplateCodeEnum.getByCode(templateCode);
        if (value == null) {
            throw new ServiceException("Unsupported templateCode: " + templateCode);
        }
        return value;
    }

    /**
     * 校验Switch。
     *
     * @param value 参数
     * @param fieldName 参数
     */
    private void validateSwitch(Integer value, String fieldName) {
        if (!Objects.equals(value, 0) && !Objects.equals(value, 1)) {
            throw new ServiceException(fieldName + " must be 0 or 1");
        }
    }

    /**
     * 判断是否SwitchOff。
     *
     * @param value 参数
     */
    private boolean isSwitchOff(Integer value) {
        return Objects.equals(value, 0);
    }

    /**
     * 规范化路由类型。
     *
     * @param routeType 参数
     * @return 处理结果
     */
    private String normalizeRouteType(String routeType) {
        return StrUtil.isBlank(routeType) ? null : routeType.trim();
    }

    /**
     * 规范化字段。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String normalizeField(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    /**
     * 规范化Remark。
     *
     * @param remark 参数
     * @return 处理结果
     */
    private String normalizeRemark(String remark) {
        return StrUtil.isBlank(remark) ? null : StrUtil.sub(remark.trim(), 0, 255);
    }

    /**
     * to视图。
     *
     * @param entity 参数
     * @return 处理结果
     */
    private NotifyTemplateVO toVO(SysNotifyTemplate entity) {
        return BeanUtil.copyProperties(entity, NotifyTemplateVO.class);
    }

    /**
     * 获取缓存Key。
     *
     * @param templateCode 参数
     * @return 处理结果
     */
    private String getCacheKey(String templateCode) {
        return TEMPLATE_CACHE_KEY_PREFIX + templateCode;
    }

    /**
     * clearAll缓存。
     */
    private void clearAllCache() {
        // 调用keys方法，复用统一能力并保证业务规则一致。
        Set<String> keys = redisTemplate.keys(TEMPLATE_CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            // 调用delete方法，复用统一能力并保证业务规则一致。
            redisTemplate.delete(keys);
        }
    }

    /**
     * 构建缓存Map。
     *
     * @param templates 参数
     * @return 处理结果
     */
    private Map<String, NotifyTemplateCacheValue> buildCacheMap(List<SysNotifyTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyMap();
        }
        // 说明：执行该步骤以保证业务流程正确。
        Map<String, List<SysNotifyTemplate>> grouped = templates.stream()
                // 调用groupingBy方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.groupingBy(SysNotifyTemplate::getTemplateCode));
        Map<String, NotifyTemplateCacheValue> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<SysNotifyTemplate>> entry : grouped.entrySet()) {
            // 调用getValue方法，复用统一能力并保证业务规则一致。
            result.put(entry.getKey(), buildCacheValue(entry.getValue()));
        }
        return result;
    }

    /**
     * 构建缓存值。
     *
     * @param templates 参数
     * @return 处理结果
     */
    private NotifyTemplateCacheValue buildCacheValue(List<SysNotifyTemplate> templates) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateCacheValue value = new NotifyTemplateCacheValue();
        for (SysNotifyTemplate template : templates) {
            if (NotifyTemplateSourceEnum.BUILT_IN.getCode().equals(template.getTemplateSource())) {
                // 调用setBuiltInTemplate方法，复用统一能力并保证业务规则一致。
                value.setBuiltInTemplate(template);
            } else if (NotifyTemplateSourceEnum.CUSTOM.getCode().equals(template.getTemplateSource())) {
                // 调用setCustomTemplate方法，复用统一能力并保证业务规则一致。
                value.setCustomTemplate(template);
            }
        }
        return value;
    }

    /**
     * 构建VariablesJson。
     *
     * @param templateCodeEnum 参数
     * @return 处理结果
     */
    private String buildVariablesJson(NotifyTemplateCodeEnum templateCodeEnum) {
        List<Map<String, String>> items = new ArrayList<>();
        switch (templateCodeEnum) {
            case WORK_ORDER_ASSIGNED:
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("bizId", "Business id"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("bizNo", "Business number"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("receiverId", "Receiver id"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("receiverName", "Receiver name"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("operatorId", "Operator id"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("oldAssignedUserId", "Old assignee id"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("newAssignedUserId", "New assignee id"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("assignType", "Assign type"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("operationId", "Operation id"));
                break;
            case WORK_ORDER_EVALUATION_INVITE:
                // 调用id方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("workOrderId", "Work order id (route only)"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("orderNo", "Work order number"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("customerId", "Customer id"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("customerMobile", "Customer mobile"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("customerOpenid", "Customer openid"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("companyId", "Company id"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("companyName", "Company name"));
                // 调用variableMeta方法，复用统一能力并保证业务规则一致。
                items.add(variableMeta("closedTime", "Closed time"));
                break;
            default:
                break;
        }
        return JSONUtil.toJsonStr(items);
    }

    /**
     * 获取Allowed模板Variables。
     *
     * @param templateCodeEnum 参数
     * @return 处理结果
     */
    private Set<String> getAllowedTemplateVariables(NotifyTemplateCodeEnum templateCodeEnum) {
        switch (templateCodeEnum) {
            case WORK_ORDER_ASSIGNED:
                return new LinkedHashSet<>(Arrays.asList(
                        "bizId", "bizNo", "receiverId", "receiverName", "operatorId",
                        "oldAssignedUserId", "newAssignedUserId", "assignType", "operationId"
                ));
            case WORK_ORDER_EVALUATION_INVITE:
                return new LinkedHashSet<>(Arrays.asList(
                        "workOrderId", "orderNo", "customerId", "customerMobile",
                        "customerOpenid", "companyId", "companyName", "closedTime"
                ));
            default:
                return Collections.emptySet();
        }
    }

    /**
     * 获取Allowed渠道字段Variables。
     *
     * @param templateCodeEnum 参数
     * @return 处理结果
     */
    private Set<String> getAllowedChannelFieldVariables(NotifyTemplateCodeEnum templateCodeEnum) {
        // 说明：执行该步骤以保证业务流程正确。
        if (NotifyTemplateCodeEnum.WORK_ORDER_EVALUATION_INVITE == templateCodeEnum) {
            return new LinkedHashSet<>(Arrays.asList("orderNo", "customerMobile", "companyName", "closedTime"));
        }
        return Collections.emptySet();
    }

    /**
     * 获取Allowed渠道路由Variables。
     *
     * @param templateCodeEnum 参数
     * @return 处理结果
     */
    private Set<String> getAllowedChannelRouteVariables(NotifyTemplateCodeEnum templateCodeEnum) {
        // 说明：执行该步骤以保证业务流程正确。
        if (NotifyTemplateCodeEnum.WORK_ORDER_EVALUATION_INVITE == templateCodeEnum) {
            return Collections.singleton("workOrderId");
        }
        return Collections.emptySet();
    }

    /**
     * variableMeta。
     *
     * @param name 参数
     * @param desc 参数
     * @return 处理结果
     */
    private Map<String, String> variableMeta(String name, String desc) {
        Map<String, String> item = new LinkedHashMap<>();
        // 调用put方法，复用统一能力并保证业务规则一致。
        item.put("name", name);
        // 调用put方法，复用统一能力并保证业务规则一致。
        item.put("desc", desc);
        return item;
    }
}




