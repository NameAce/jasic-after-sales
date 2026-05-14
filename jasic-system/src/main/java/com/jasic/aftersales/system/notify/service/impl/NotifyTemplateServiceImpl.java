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

@Slf4j
@Service
public class NotifyTemplateServiceImpl implements NotifyTemplateService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");
    private static final int TITLE_MAX_LENGTH = 128;
    private static final int SUMMARY_MAX_LENGTH = 255;
    private static final int ROUTE_VALUE_MAX_LENGTH = 128;
    private static final String TEMPLATE_CACHE_KEY_PREFIX = "notify:template:";

    /**
     * ????????
     */
    @Resource
    private SysNotifyTemplateMapper sysNotifyTemplateMapper;

    @Resource
    private SysNotifyTemplateChannelMapper sysNotifyTemplateChannelMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void initCache() {
        refreshCache();
    }

    /**
     * ???????
     *
     * @param query ????
     * @return ????
     */
    @Override
    public PageResult<NotifyTemplateVO> listPage(NotifyTemplateQuery query) {
        // ????????????????????????
        NotifyTemplateQuery actualQuery = query == null ? new NotifyTemplateQuery() : query;
        Page<SysNotifyTemplate> page = new Page<>(actualQuery.getPageNum(), actualQuery.getPageSize());
        LambdaQueryWrapper<SysNotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(actualQuery.getTemplateCode())) {
            wrapper.like(SysNotifyTemplate::getTemplateCode, actualQuery.getTemplateCode().trim());
        }
        if (StrUtil.isNotBlank(actualQuery.getTemplateName())) {
            wrapper.like(SysNotifyTemplate::getTemplateName, actualQuery.getTemplateName().trim());
        }
        if (StrUtil.isNotBlank(actualQuery.getTemplateSource())) {
            wrapper.eq(SysNotifyTemplate::getTemplateSource, actualQuery.getTemplateSource().trim());
        }
        if (StrUtil.isNotBlank(actualQuery.getEventType())) {
            wrapper.eq(SysNotifyTemplate::getEventType, actualQuery.getEventType().trim());
        }
        wrapper.orderByAsc(SysNotifyTemplate::getTemplateCode)
                .orderByAsc(SysNotifyTemplate::getTemplateSource)
                .orderByDesc(SysNotifyTemplate::getId);
        Page<SysNotifyTemplate> result = sysNotifyTemplateMapper.selectPage(page, wrapper);
        List<NotifyTemplateVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), actualQuery.getPageNum(), actualQuery.getPageSize());
    }

    /**
     * ??By Id?
     *
     * @param id ??ID
     * @return ????
     */
    @Override
    public NotifyTemplateVO getById(Long id) {
        SysNotifyTemplate entity = sysNotifyTemplateMapper.selectById(id);
        return entity == null ? null : toVO(entity);
    }

    /**
     * ?????
     *
     * @param dto ????
     * @return ????
     */
    @Override
    public Long saveCustom(NotifyTemplateDTO dto) {
        // ????????????????????????
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(dto.getTemplateCode());
        SysNotifyTemplate builtInTemplate = getRequiredBuiltInTemplate(templateCodeEnum.getCode());
        if (getCustomTemplate(templateCodeEnum.getCode()) != null) {
            throw new ServiceException("Custom template already exists for templateCode " + templateCodeEnum.getCode());
        }
        SysNotifyTemplate entity = buildCustomTemplate(dto, templateCodeEnum, builtInTemplate, null);
        sysNotifyTemplateMapper.insert(entity);
        refreshTemplateCache(templateCodeEnum.getCode());
        return entity.getId();
    }

    /**
     * ?????
     *
     * @param dto ????
     */
    @Override
    public void updateCustom(NotifyTemplateDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("Template id cannot be null");
        }
        // ????????????????????????
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
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(existing.getTemplateCode());
        SysNotifyTemplate builtInTemplate = getRequiredBuiltInTemplate(templateCodeEnum.getCode());
        SysNotifyTemplate entity = buildCustomTemplate(dto, templateCodeEnum, builtInTemplate, existing);
        entity.setId(existing.getId());
        sysNotifyTemplateMapper.updateById(entity);
        refreshTemplateCache(templateCodeEnum.getCode());
    }

    /**
     * ?????
     *
     * @param id ??ID
     */
    @Override
    public void removeCustom(Long id) {
        // ????????????????????????
        SysNotifyTemplate existing = sysNotifyTemplateMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("Notify template not found");
        }
        if (!NotifyTemplateSourceEnum.CUSTOM.getCode().equals(existing.getTemplateSource())) {
            throw new ServiceException("Built-in templates cannot be deleted");
        }
        sysNotifyTemplateMapper.deleteById(id);
        refreshTemplateCache(existing.getTemplateCode());
    }

    /**
     * ?? preview ?????
     *
     * @param dto ????
     * @return ????
     */
    @Override
    public NotifyTemplatePreviewVO preview(NotifyTemplatePreviewDTO dto) {
        // ????????????????????????
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(dto.getTemplateCode());
        SysNotifyTemplate builtInTemplate = getRequiredBuiltInTemplate(templateCodeEnum.getCode());
        Map<String, Object> variables = dto.getVariables() == null ? Collections.emptyMap() : dto.getVariables();
        NotifyTemplateRenderResult renderResult = renderWithResolvedTemplates(
                templateCodeEnum.getCode(),
                builtInTemplate,
                buildPreviewCustomTemplate(dto, templateCodeEnum, builtInTemplate),
                variables,
                true
        );
        NotifyTemplatePreviewVO previewVO = new NotifyTemplatePreviewVO();
        previewVO.setNotifyEnabled(renderResult.isNotifyEnabled());
        previewVO.setTemplateSource(renderResult.getTemplateSource());
        previewVO.setTitle(renderResult.getTitle());
        previewVO.setSummary(renderResult.getSummary());
        previewVO.setRouteType(renderResult.getRouteType());
        previewVO.setRouteValue(renderResult.getRouteValue());
        previewVO.setErrors(new ArrayList<>(renderResult.getErrors()));
        return previewVO;
    }

    /**
     * ?? render ?????
     *
     * @param templateCode ??
     * @param variables ??
     * @return ????
     */
    @Override
    public NotifyTemplateRenderResult render(String templateCode, Map<String, Object> variables) {
        // ????????????????????????
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(templateCode);
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
     * ????Notify Enabled?
     *
     * @param templateCode ??
     * @return true ??????
     */
    @Override
    public boolean isNotifyEnabled(String templateCode) {
        // ????????????????????????
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(templateCode);
        NotifyTemplateCacheValue cacheValue = getTemplateCache(templateCodeEnum.getCode());
        if (cacheValue.getBuiltInTemplate() == null) {
            throw new ServiceException("Built-in notify template not found: " + templateCodeEnum.getCode());
        }
        SysNotifyTemplate customTemplate = cacheValue.getCustomTemplate();
        return customTemplate == null || !Objects.equals(customTemplate.getNotifyEnabled(), 0);
    }

    /**
     * ???????
     *
     * @param templateCode ??
     * @return ????
     */
    @Override
    public List<NotifyTemplateChannelVO> listChannelConfigs(String templateCode) {
        // ????????????????????????
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(templateCode);
        LambdaQueryWrapper<SysNotifyTemplateChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyTemplateChannel::getTemplateCode, templateCodeEnum.getCode())
                .orderByAsc(SysNotifyTemplateChannel::getId);
        return sysNotifyTemplateChannelMapper.selectList(wrapper).stream()
                .map(this::toChannelVO)
                .collect(Collectors.toList());
    }

    /**
     * ?????
     *
     * @param templateCode ??
     * @param channelConfigs ??
     */
    @Override
    public void saveChannelConfigs(String templateCode, List<NotifyTemplateChannelDTO> channelConfigs) {
        // ????????????????????????
        NotifyTemplateCodeEnum templateCodeEnum = getRequiredTemplateCode(templateCode);
        List<NotifyTemplateChannelDTO> actualConfigs = channelConfigs == null ? Collections.emptyList() : channelConfigs;
        // ?????????????????????????????
        validateChannelConfigs(templateCodeEnum, actualConfigs);

        LambdaQueryWrapper<SysNotifyTemplateChannel> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysNotifyTemplateChannel::getTemplateCode, templateCodeEnum.getCode());
        sysNotifyTemplateChannelMapper.delete(deleteWrapper);

        for (NotifyTemplateChannelDTO dto : actualConfigs) {
            SysNotifyTemplateChannel entity = new SysNotifyTemplateChannel();
            entity.setTemplateCode(templateCodeEnum.getCode());
            entity.setChannelType(dto.getChannelType().trim());
            entity.setChannelEnabled(dto.getChannelEnabled());
            entity.setChannelScene(normalizeField(dto.getChannelScene()));
            entity.setConfigJson(buildChannelConfigJson(templateCodeEnum, dto));
            entity.setRemark(normalizeRemark(dto.getRemark()));
            sysNotifyTemplateChannelMapper.insert(entity);
        }
    }

    /**
     * ?? refreshCache ?????
     */
    @Override
    public void refreshCache() {
        clearAllCache();
        // ????????????????????????
        List<SysNotifyTemplate> templates = sysNotifyTemplateMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, NotifyTemplateCacheValue> grouped = buildCacheMap(templates);
        for (Map.Entry<String, NotifyTemplateCacheValue> entry : grouped.entrySet()) {
            redisTemplate.opsForValue().set(getCacheKey(entry.getKey()), JSONUtil.toJsonStr(entry.getValue()));
        }
    }

    /**
     * ???????
     *
     * @param dto ????
     * @param templateCodeEnum ??
     * @param builtInTemplate ??
     * @param existing ??
     * @return ????
     */
    private SysNotifyTemplate buildCustomTemplate(NotifyTemplateDTO dto, NotifyTemplateCodeEnum templateCodeEnum,
                                                  SysNotifyTemplate builtInTemplate, SysNotifyTemplate existing) {
        // ????????????????????????
        validateSwitch(dto.getNotifyEnabled(), "notifyEnabled");
        // ?????????????????????????????
        validateSwitch(dto.getOverrideEnabled(), "overrideEnabled");
        SysNotifyTemplate entity = existing == null ? new SysNotifyTemplate() : existing;
        entity.setTemplateCode(templateCodeEnum.getCode());
        entity.setTemplateName(StrUtil.isBlank(dto.getTemplateName()) ? builtInTemplate.getTemplateName() : dto.getTemplateName().trim());
        entity.setTemplateSource(NotifyTemplateSourceEnum.CUSTOM.getCode());
        entity.setBizType(templateCodeEnum.getBizType());
        entity.setEventType(templateCodeEnum.getEventType());
        entity.setMessageType(templateCodeEnum.getMessageType());
        entity.setNotifyEnabled(dto.getNotifyEnabled());
        entity.setOverrideEnabled(dto.getOverrideEnabled());
        entity.setRouteType(normalizeRouteType(dto.getRouteType()));
        entity.setTitleTemplate(normalizeField(dto.getTitleTemplate()));
        entity.setSummaryTemplate(normalizeField(dto.getSummaryTemplate()));
        entity.setRouteValueTemplate(normalizeField(dto.getRouteValueTemplate()));
        entity.setVariablesJson(buildVariablesJson(templateCodeEnum));
        entity.setRemark(normalizeRemark(dto.getRemark()));
        validateTemplatePayload(entity, templateCodeEnum);
        return entity;
    }

    /**
     * ???????
     *
     * @param dto ????
     * @param templateCodeEnum ??
     * @param builtInTemplate ??
     * @return ????
     */
    private SysNotifyTemplate buildPreviewCustomTemplate(NotifyTemplatePreviewDTO dto, NotifyTemplateCodeEnum templateCodeEnum,
                                                         SysNotifyTemplate builtInTemplate) {
        // ????????????????????????
        SysNotifyTemplate entity = new SysNotifyTemplate();
        entity.setTemplateCode(templateCodeEnum.getCode());
        entity.setTemplateName(builtInTemplate.getTemplateName());
        entity.setTemplateSource(NotifyTemplateSourceEnum.CUSTOM.getCode());
        entity.setBizType(templateCodeEnum.getBizType());
        entity.setEventType(templateCodeEnum.getEventType());
        entity.setMessageType(templateCodeEnum.getMessageType());
        entity.setNotifyEnabled(dto.getNotifyEnabled() == null ? 1 : dto.getNotifyEnabled());
        entity.setOverrideEnabled(dto.getOverrideEnabled() == null ? 1 : dto.getOverrideEnabled());
        entity.setRouteType(normalizeRouteType(dto.getRouteType()));
        entity.setTitleTemplate(normalizeField(dto.getTitleTemplate()));
        entity.setSummaryTemplate(normalizeField(dto.getSummaryTemplate()));
        entity.setRouteValueTemplate(normalizeField(dto.getRouteValueTemplate()));
        entity.setVariablesJson(buildVariablesJson(templateCodeEnum));
        validateSwitch(entity.getNotifyEnabled(), "notifyEnabled");
        // ?????????????????????????????
        validateSwitch(entity.getOverrideEnabled(), "overrideEnabled");
        validateTemplatePayload(entity, templateCodeEnum);
        return entity;
    }

    /**
     * ?? renderWithResolvedTemplates ?????
     *
     * @param templateCode ??
     * @param builtInTemplate ??
     * @param customTemplate ??
     * @param variables ??
     * @param previewMode ??
     * @return ????
     */
    private NotifyTemplateRenderResult renderWithResolvedTemplates(String templateCode, SysNotifyTemplate builtInTemplate,
                                                                   SysNotifyTemplate customTemplate, Map<String, Object> variables,
                                                                   boolean previewMode) {
        // ????????????????????????
        NotifyTemplateRenderResult result = new NotifyTemplateRenderResult();
        result.setTemplateCode(templateCode);
        if (customTemplate != null && isSwitchOff(customTemplate.getNotifyEnabled())) {
            result.setNotifyEnabled(false);
            result.setTemplateSource(customTemplate.getTemplateSource());
            return result;
        }
        result.setNotifyEnabled(true);
        if (customTemplate == null || isSwitchOff(customTemplate.getOverrideEnabled())) {
            applyRenderedTemplate(result, builtInTemplate, variables, NotifyTemplateSourceEnum.BUILT_IN.getCode());
            return result;
        }
        try {
            applyRenderedTemplate(result, mergeTemplate(builtInTemplate, customTemplate), variables, NotifyTemplateSourceEnum.CUSTOM.getCode());
            return result;
        } catch (ServiceException ex) {
            result.addError(ex.getMessage());
            if (!previewMode) {
                log.warn("Notify template custom render failed, fallback to built-in. templateCode={}, error={}",
                        templateCode, ex.getMessage());
            }
            applyRenderedTemplate(result, builtInTemplate, variables, NotifyTemplateSourceEnum.BUILT_IN.getCode());
            return result;
        }
    }

    /**
     * ?? applyRenderedTemplate ?????
     *
     * @param result ??
     * @param template ??
     * @param variables ??
     * @param templateSource ??
     */
    private void applyRenderedTemplate(NotifyTemplateRenderResult result, SysNotifyTemplate template,
                                       Map<String, Object> variables, String templateSource) {
        String title = renderContent(template.getTitleTemplate(), variables);
        String summary = renderContent(template.getSummaryTemplate(), variables);
        String routeType = normalizeField(template.getRouteType());
        String routeValue = renderContent(template.getRouteValueTemplate(), variables);
        // ?????????????????????????????
        validateRenderedContent(title, summary, routeType, routeValue);
        result.setTemplateSource(templateSource);
        result.setTitle(title);
        result.setSummary(summary);
        result.setRouteType(routeType);
        result.setRouteValue(routeValue);
    }

    /**
     * ?? mergeTemplate ?????
     *
     * @param builtInTemplate ??
     * @param customTemplate ??
     * @return ????
     */
    private SysNotifyTemplate mergeTemplate(SysNotifyTemplate builtInTemplate, SysNotifyTemplate customTemplate) {
        // ????????????????????????
        SysNotifyTemplate merged = new SysNotifyTemplate();
        merged.setTemplateCode(customTemplate.getTemplateCode());
        merged.setTemplateName(customTemplate.getTemplateName());
        merged.setTemplateSource(customTemplate.getTemplateSource());
        merged.setBizType(customTemplate.getBizType());
        merged.setEventType(customTemplate.getEventType());
        merged.setMessageType(customTemplate.getMessageType());
        merged.setNotifyEnabled(customTemplate.getNotifyEnabled());
        merged.setOverrideEnabled(customTemplate.getOverrideEnabled());
        merged.setRouteType(StrUtil.isBlank(customTemplate.getRouteType()) ? builtInTemplate.getRouteType() : customTemplate.getRouteType());
        merged.setTitleTemplate(StrUtil.isBlank(customTemplate.getTitleTemplate()) ? builtInTemplate.getTitleTemplate() : customTemplate.getTitleTemplate());
        merged.setSummaryTemplate(StrUtil.isBlank(customTemplate.getSummaryTemplate()) ? builtInTemplate.getSummaryTemplate() : customTemplate.getSummaryTemplate());
        merged.setRouteValueTemplate(StrUtil.isBlank(customTemplate.getRouteValueTemplate()) ? builtInTemplate.getRouteValueTemplate() : customTemplate.getRouteValueTemplate());
        return merged;
    }

    /**
     * ?? renderContent ?????
     *
     * @param template ??
     * @param variables ??
     * @return ?????
     */
    private String renderContent(String template, Map<String, Object> variables) {
        String actualTemplate = normalizeField(template);
        if (actualTemplate == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(actualTemplate);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value).trim()));
        }
        matcher.appendTail(buffer);
        return buffer.toString().trim();
    }

    /**
     * ???????
     *
     * @param title ??
     * @param summary ??
     * @param routeType ??
     * @param routeValue ??
     */
    private void validateRenderedContent(String title, String summary, String routeType, String routeValue) {
        if (StrUtil.isBlank(title)) {
            throw new ServiceException("Rendered title cannot be blank");
        }
        if (StrUtil.isBlank(summary)) {
            throw new ServiceException("Rendered summary cannot be blank");
        }
        // ????????????????????????
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
     * ???????
     *
     * @param template ??
     * @param templateCodeEnum ??
     */
    private void validateTemplatePayload(SysNotifyTemplate template, NotifyTemplateCodeEnum templateCodeEnum) {
        // ?????????????????????????????
        validateRouteType(template.getRouteType());
        validateLength(template.getTitleTemplate(), TITLE_MAX_LENGTH, "titleTemplate");
        validateLength(template.getSummaryTemplate(), SUMMARY_MAX_LENGTH, "summaryTemplate");
        validateLength(template.getRouteValueTemplate(), ROUTE_VALUE_MAX_LENGTH, "routeValueTemplate");
        validatePlaceholders(template.getTitleTemplate(), getAllowedTemplateVariables(templateCodeEnum));
        validatePlaceholders(template.getSummaryTemplate(), getAllowedTemplateVariables(templateCodeEnum));
        validatePlaceholders(template.getRouteValueTemplate(), getAllowedTemplateVariables(templateCodeEnum));
    }

    /**
     * ???????
     *
     * @param templateCodeEnum ??
     * @param channelConfigs ??
     */
    private void validateChannelConfigs(NotifyTemplateCodeEnum templateCodeEnum, List<NotifyTemplateChannelDTO> channelConfigs) {
        Set<String> uniqueKeys = new LinkedHashSet<>();
        // ????????????????????????
        for (NotifyTemplateChannelDTO dto : channelConfigs) {
            if (dto == null) {
                throw new ServiceException("Channel config cannot be null");
            }
            if (StrUtil.isNotBlank(dto.getTemplateCode())
                    && !StrUtil.equals(dto.getTemplateCode().trim(), templateCodeEnum.getCode())) {
                throw new ServiceException("Channel config templateCode does not match path parameter");
            }
            NotifyChannelTypeEnum channelTypeEnum = NotifyChannelTypeEnum.getByCode(dto.getChannelType());
            if (channelTypeEnum == null) {
                throw new ServiceException("Unsupported channel type: " + dto.getChannelType());
            }
            // ?????????????????????????????
            validateSwitch(dto.getChannelEnabled(), "channelEnabled");
            String channelScene = normalizeField(dto.getChannelScene());
            String uniqueKey = channelTypeEnum.getCode() + ":" + StrUtil.blankToDefault(channelScene, "");
            if (!uniqueKeys.add(uniqueKey)) {
                throw new ServiceException("Duplicate channel config found: " + uniqueKey);
            }
            if (NotifyChannelTypeEnum.MP_SUBSCRIBE == channelTypeEnum) {
                validateMiniProgramChannelConfig(templateCodeEnum, dto, channelScene);
            }
        }
    }

    /**
     * ???????
     *
     * @param templateCodeEnum ??
     * @param dto ????
     * @param channelScene ??
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
            // ?????????????????????????????
            validatePlaceholders(dto.getPagePathTemplate(), getAllowedChannelRouteVariables(templateCodeEnum));
        }
        // ????????????????????????
        List<NotifyChannelFieldMappingDTO> fieldMapping = dto.getFieldMapping() == null
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
            validatePlaceholders(item.getValue(), getAllowedChannelFieldVariables(templateCodeEnum));
        }
    }

    /**
     * ???????
     *
     * @param templateCodeEnum ??
     * @param dto ????
     * @return ?????
     */
    private String buildChannelConfigJson(NotifyTemplateCodeEnum templateCodeEnum, NotifyTemplateChannelDTO dto) {
        // ????????????????????????
        NotifyChannelTypeEnum channelTypeEnum = NotifyChannelTypeEnum.getByCode(dto.getChannelType());
        if (NotifyChannelTypeEnum.MP_SUBSCRIBE != channelTypeEnum) {
            return StrUtil.blankToDefault(normalizeField(dto.getConfigJson()), "{}");
        }
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setScene(normalizeField(dto.getChannelScene()));
        config.setTemplateId(normalizeField(dto.getTemplateId()));
        config.setPagePathTemplate(normalizeField(dto.getPagePathTemplate()));
        List<NotifyChannelFieldMappingDTO> fieldMapping = dto.getFieldMapping() == null
                ? Collections.emptyList() : dto.getFieldMapping();
        List<NotifyChannelFieldMappingDTO> normalizedFieldMapping = new ArrayList<>();
        for (NotifyChannelFieldMappingDTO item : fieldMapping) {
            NotifyChannelFieldMappingDTO copy = new NotifyChannelFieldMappingDTO();
            copy.setField(item == null ? null : normalizeField(item.getField()));
            copy.setValue(item == null ? null : item.getValue().trim());
            normalizedFieldMapping.add(copy);
        }
        config.setFieldMapping(normalizedFieldMapping);
        // ?????????????????????????????
        validateMiniProgramChannelConfig(templateCodeEnum, dto, config.getScene());
        return JSONUtil.toJsonStr(config);
    }

    /**
     * ?? toChannelVO ?????
     *
     * @param entity ????
     * @return ????
     */
    private NotifyTemplateChannelVO toChannelVO(SysNotifyTemplateChannel entity) {
        // ????????????????????????
        NotifyTemplateChannelVO vo = BeanUtil.copyProperties(entity, NotifyTemplateChannelVO.class);
        if (StrUtil.isBlank(entity.getConfigJson())) {
            return vo;
        }
        NotifyTemplateChannelConfig config = JSONUtil.toBean(entity.getConfigJson(), NotifyTemplateChannelConfig.class);
        if (config != null) {
            vo.setTemplateId(config.getTemplateId());
            vo.setPagePathTemplate(config.getPagePathTemplate());
            vo.setFieldMapping(config.getFieldMapping());
            if (StrUtil.isBlank(vo.getChannelScene())) {
                vo.setChannelScene(config.getScene());
            }
        }
        return vo;
    }

    /**
     * ???????
     *
     * @param value ???
     * @param maxLength ??
     * @param fieldName ??
     */
    private void validateLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new ServiceException(fieldName + " length cannot exceed " + maxLength);
        }
    }

    /**
     * ???????
     *
     * @param template ??
     * @param allowedVariables ??
     */
    private void validatePlaceholders(String template, Set<String> allowedVariables) {
        if (StrUtil.isBlank(template)) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!allowedVariables.contains(variableName)) {
                throw new ServiceException("Unsupported template variable: " + variableName);
            }
        }
    }

    /**
     * ???????
     *
     * @param routeType ??
     */
    private void validateRouteType(String routeType) {
        if (StrUtil.isBlank(routeType)) {
            return;
        }
        // ????????????????????????
        if (NotifyRouteTypeEnum.getByCode(routeType) == null) {
            throw new ServiceException("Unsupported routeType: " + routeType);
        }
    }

    /**
     * ??Template Cache?
     *
     * @param templateCode ??
     * @return ????
     */
    private NotifyTemplateCacheValue getTemplateCache(String templateCode) {
        Object cached = redisTemplate.opsForValue().get(getCacheKey(templateCode));
        if (cached != null) {
            // ????????????????????????
            return JSONUtil.toBean(String.valueOf(cached), NotifyTemplateCacheValue.class);
        }
        return refreshTemplateCache(templateCode);
    }

    /**
     * ?? refreshTemplateCache ?????
     *
     * @param templateCode ??
     * @return ????
     */
    private NotifyTemplateCacheValue refreshTemplateCache(String templateCode) {
        NotifyTemplateCacheValue value = buildCacheValue(listByTemplateCode(templateCode));
        redisTemplate.opsForValue().set(getCacheKey(templateCode), JSONUtil.toJsonStr(value));
        return value;
    }

    /**
     * ???????
     *
     * @param templateCode ??
     * @return ????
     */
    private List<SysNotifyTemplate> listByTemplateCode(String templateCode) {
        // ????????????????????????
        LambdaQueryWrapper<SysNotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyTemplate::getTemplateCode, templateCode)
                .orderByAsc(SysNotifyTemplate::getTemplateSource)
                .orderByAsc(SysNotifyTemplate::getId);
        return sysNotifyTemplateMapper.selectList(wrapper);
    }

    /**
     * ??Required Built In Template?
     *
     * @param templateCode ??
     * @return ????
     */
    private SysNotifyTemplate getRequiredBuiltInTemplate(String templateCode) {
        // ????????????????????????
        LambdaQueryWrapper<SysNotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyTemplate::getTemplateCode, templateCode)
                .eq(SysNotifyTemplate::getTemplateSource, NotifyTemplateSourceEnum.BUILT_IN.getCode())
                .last("limit 1");
        SysNotifyTemplate template = sysNotifyTemplateMapper.selectOne(wrapper);
        if (template == null) {
            throw new ServiceException("Built-in notify template not found: " + templateCode);
        }
        return template;
    }

    /**
     * ??Custom Template?
     *
     * @param templateCode ??
     * @return ????
     */
    private SysNotifyTemplate getCustomTemplate(String templateCode) {
        // ????????????????????????
        LambdaQueryWrapper<SysNotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyTemplate::getTemplateCode, templateCode)
                .eq(SysNotifyTemplate::getTemplateSource, NotifyTemplateSourceEnum.CUSTOM.getCode())
                .last("limit 1");
        return sysNotifyTemplateMapper.selectOne(wrapper);
    }

    /**
     * ??Required Template Code?
     *
     * @param templateCode ??
     * @return ????
     */
    private NotifyTemplateCodeEnum getRequiredTemplateCode(String templateCode) {
        // ????????????????????????
        NotifyTemplateCodeEnum value = NotifyTemplateCodeEnum.getByCode(templateCode);
        if (value == null) {
            throw new ServiceException("Unsupported templateCode: " + templateCode);
        }
        return value;
    }

    /**
     * ???????
     *
     * @param value ???
     * @param fieldName ??
     */
    private void validateSwitch(Integer value, String fieldName) {
        if (!Objects.equals(value, 0) && !Objects.equals(value, 1)) {
            throw new ServiceException(fieldName + " must be 0 or 1");
        }
    }

    /**
     * ????Switch Off?
     *
     * @param value ???
     * @return true ??????
     */
    private boolean isSwitchOff(Integer value) {
        return Objects.equals(value, 0);
    }

    /**
     * ????????
     *
     * @param routeType ??
     * @return ?????
     */
    private String normalizeRouteType(String routeType) {
        return StrUtil.isBlank(routeType) ? null : routeType.trim();
    }

    /**
     * ????????
     *
     * @param value ???
     * @return ?????
     */
    private String normalizeField(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    /**
     * ????????
     *
     * @param remark ??
     * @return ?????
     */
    private String normalizeRemark(String remark) {
        return StrUtil.isBlank(remark) ? null : StrUtil.sub(remark.trim(), 0, 255);
    }

    /**
     * ?? toVO ?????
     *
     * @param entity ????
     * @return ????
     */
    private NotifyTemplateVO toVO(SysNotifyTemplate entity) {
        return BeanUtil.copyProperties(entity, NotifyTemplateVO.class);
    }

    /**
     * ??Cache Key?
     *
     * @param templateCode ??
     * @return ?????
     */
    private String getCacheKey(String templateCode) {
        return TEMPLATE_CACHE_KEY_PREFIX + templateCode;
    }

    /**
     * ?????
     */
    private void clearAllCache() {
        Set<String> keys = redisTemplate.keys(TEMPLATE_CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * ???????
     *
     * @param templates ??
     * @return ????
     */
    private Map<String, NotifyTemplateCacheValue> buildCacheMap(List<SysNotifyTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyMap();
        }
        // ????????????????????????
        Map<String, List<SysNotifyTemplate>> grouped = templates.stream()
                .collect(Collectors.groupingBy(SysNotifyTemplate::getTemplateCode));
        Map<String, NotifyTemplateCacheValue> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<SysNotifyTemplate>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), buildCacheValue(entry.getValue()));
        }
        return result;
    }

    /**
     * ???????
     *
     * @param templates ??
     * @return ????
     */
    private NotifyTemplateCacheValue buildCacheValue(List<SysNotifyTemplate> templates) {
        // ????????????????????????
        NotifyTemplateCacheValue value = new NotifyTemplateCacheValue();
        for (SysNotifyTemplate template : templates) {
            if (NotifyTemplateSourceEnum.BUILT_IN.getCode().equals(template.getTemplateSource())) {
                value.setBuiltInTemplate(template);
            } else if (NotifyTemplateSourceEnum.CUSTOM.getCode().equals(template.getTemplateSource())) {
                value.setCustomTemplate(template);
            }
        }
        return value;
    }

    /**
     * ???????
     *
     * @param templateCodeEnum ??
     * @return ?????
     */
    private String buildVariablesJson(NotifyTemplateCodeEnum templateCodeEnum) {
        List<Map<String, String>> items = new ArrayList<>();
        switch (templateCodeEnum) {
            case WORK_ORDER_ASSIGNED:
                items.add(variableMeta("bizId", "Business id"));
                items.add(variableMeta("bizNo", "Business number"));
                items.add(variableMeta("receiverId", "Receiver id"));
                items.add(variableMeta("receiverName", "Receiver name"));
                items.add(variableMeta("operatorId", "Operator id"));
                items.add(variableMeta("oldAssignedUserId", "Old assignee id"));
                items.add(variableMeta("newAssignedUserId", "New assignee id"));
                items.add(variableMeta("assignType", "Assign type"));
                items.add(variableMeta("operationId", "Operation id"));
                break;
            case WORK_ORDER_EVALUATION_INVITE:
                items.add(variableMeta("workOrderId", "Work order id (route only)"));
                items.add(variableMeta("orderNo", "Work order number"));
                items.add(variableMeta("customerId", "Customer id"));
                items.add(variableMeta("customerMobile", "Customer mobile"));
                items.add(variableMeta("customerOpenid", "Customer openid"));
                items.add(variableMeta("companyId", "Company id"));
                items.add(variableMeta("companyName", "Company name"));
                items.add(variableMeta("closedTime", "Closed time"));
                break;
            default:
                break;
        }
        return JSONUtil.toJsonStr(items);
    }

    /**
     * ??Allowed Template Variables?
     *
     * @param templateCodeEnum ??
     * @return ????
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
     * ??Allowed Channel Field Variables?
     *
     * @param templateCodeEnum ??
     * @return ????
     */
    private Set<String> getAllowedChannelFieldVariables(NotifyTemplateCodeEnum templateCodeEnum) {
        // ????????????????????????
        if (NotifyTemplateCodeEnum.WORK_ORDER_EVALUATION_INVITE == templateCodeEnum) {
            return new LinkedHashSet<>(Arrays.asList("orderNo", "customerMobile", "companyName", "closedTime"));
        }
        return Collections.emptySet();
    }

    /**
     * ??Allowed Channel Route Variables?
     *
     * @param templateCodeEnum ??
     * @return ????
     */
    private Set<String> getAllowedChannelRouteVariables(NotifyTemplateCodeEnum templateCodeEnum) {
        // ????????????????????????
        if (NotifyTemplateCodeEnum.WORK_ORDER_EVALUATION_INVITE == templateCodeEnum) {
            return Collections.singleton("workOrderId");
        }
        return Collections.emptySet();
    }

    /**
     * ?? variableMeta ?????
     *
     * @param name ??
     * @param desc ??
     * @return ????
     */
    private Map<String, String> variableMeta(String name, String desc) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("desc", desc);
        return item;
    }
}
