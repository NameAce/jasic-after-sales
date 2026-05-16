package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifySceneConfigSaveDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyScenePreviewDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifySceneTargetConfigDTO;
import com.jasic.aftersales.system.notify.domain.entity.NotifyScene;
import com.jasic.aftersales.system.notify.domain.entity.NotifySceneTarget;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelSceneEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyRouteTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTypeEnum;
import com.jasic.aftersales.system.notify.domain.query.NotifySceneConfigQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneConfigDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneConfigOptionsVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneConfigPageVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneMetaOptionVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyScenePreviewFieldMappingVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyScenePreviewVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneTargetConfigVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneTargetMetaVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateEnumOptionVO;
import com.jasic.aftersales.system.notify.mapper.NotifySceneMapper;
import com.jasic.aftersales.system.notify.mapper.NotifySceneTargetMapper;
import com.jasic.aftersales.system.notify.service.NotifySceneTargetConfigService;
import com.jasic.aftersales.system.notify.support.NotifySceneMeta;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
import com.jasic.aftersales.system.notify.support.NotifySceneTargetMeta;
import com.jasic.aftersales.system.notify.support.NotifyTemplateChannelConfig;
import com.jasic.aftersales.system.notify.support.NotifyTemplateVariableMeta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 通知场景目标配置服务实现。
 *
 * <p>该实现负责阶段一“统一通知场景 + 多通知目标配置”的后台维护能力落地。
 * 核心职责包括：
 * 1. 把注册表中的场景与目标初始化到 `notify_scene / notify_scene_target`
 * 2. 以场景视角对外提供列表、详情、保存和预览
 * 3. 对模板变量、路由类型和 `MP_SUBSCRIBE` 专属配置做统一校验
 *
 * <p>该实现不负责事件消费、站内消息落库、分发表创建和真实渠道发送。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Service
public class NotifySceneTargetConfigServiceImpl implements NotifySceneTargetConfigService {

    private static final int SCENE_CODE_MAX_LENGTH = 64;
    private static final int SCENE_NAME_MAX_LENGTH = 128;
    private static final int BIZ_TYPE_MAX_LENGTH = 64;
    private static final int EVENT_CODE_MAX_LENGTH = 64;
    private static final int TITLE_MAX_LENGTH = 128;
    private static final int CONTENT_MAX_LENGTH = 512;
    private static final int ROUTE_TYPE_MAX_LENGTH = 64;
    private static final int ROUTE_VALUE_MAX_LENGTH = 256;
    private static final int CHANNEL_SCENE_MAX_LENGTH = 16;
    private static final int REMARK_MAX_LENGTH = 255;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    @Resource
    private NotifySceneRegistry notifySceneRegistry;

    @Resource
    private NotifySceneMapper notifySceneMapper;

    @Resource
    private NotifySceneTargetMapper notifySceneTargetMapper;

    /**
     * 查询通知场景配置页元数据。
     *
     * @return 场景、目标类型和路由类型元数据
     */
    @Override
    public NotifySceneConfigOptionsVO getOptions() {
        synchronizeRegistryData();

        NotifySceneConfigOptionsVO optionsVO = new NotifySceneConfigOptionsVO();
        optionsVO.setSceneOptions(notifySceneRegistry.listScenes().stream()
                .map(this::toSceneMetaOptionVO)
                .collect(Collectors.toList()));
        optionsVO.setTargetTypeOptions(buildTargetTypeOptions());
        optionsVO.setChannelSceneOptions(buildChannelSceneOptions());
        optionsVO.setRouteTypeOptions(buildRouteTypeOptions());
        return optionsVO;
    }

    /**
     * 分页查询通知场景配置。
     *
     * @param query 查询参数
     * @return 场景配置分页结果
     */
    @Override
    public PageResult<NotifySceneConfigPageVO> listPage(NotifySceneConfigQuery query) {
        synchronizeRegistryData();

        NotifySceneConfigQuery actualQuery = query == null ? new NotifySceneConfigQuery() : query;
        Map<String, NotifyScene> sceneMap = listAllScenes().stream()
                .collect(Collectors.toMap(NotifyScene::getSceneCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<NotifySceneTarget>> targetMap = groupTargetsBySceneCode(listAllTargets());

        List<NotifySceneConfigPageVO> rows = new ArrayList<>();
        for (NotifySceneMeta sceneMeta : notifySceneRegistry.listScenes()) {
            if (!matchesSceneFilter(sceneMeta, actualQuery)) {
                continue;
            }
            rows.add(buildPageVO(sceneMeta, sceneMap.get(sceneMeta.getSceneCode()), targetMap.get(sceneMeta.getSceneCode())));
        }

        rows.sort(Comparator.comparing(NotifySceneConfigPageVO::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(NotifySceneConfigPageVO::getSceneCode, Comparator.nullsLast(String::compareTo)));

        int pageNum = actualQuery.getPageNum();
        int pageSize = actualQuery.getPageSize();
        int fromIndex = Math.max((pageNum - 1) * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        List<NotifySceneConfigPageVO> pageRows = fromIndex >= rows.size() ? Collections.emptyList() : rows.subList(fromIndex, toIndex);
        return PageResult.of(pageRows, (long) rows.size(), pageNum, pageSize);
    }

    /**
     * 查询单个场景配置详情。
     *
     * @param sceneCode 场景编码
     * @return 场景配置详情
     */
    @Override
    public NotifySceneConfigDetailVO getDetail(String sceneCode) {
        synchronizeRegistryData();
        NotifySceneMeta sceneMeta = getRequiredScene(sceneCode);
        NotifyScene scene = getSceneEntity(sceneMeta.getSceneCode());
        Map<String, NotifySceneTarget> targetMap = listTargetsBySceneCode(sceneMeta.getSceneCode()).stream()
                .collect(Collectors.toMap(NotifySceneTarget::getTargetType, item -> item, (left, right) -> left, LinkedHashMap::new));

        NotifySceneConfigDetailVO detailVO = new NotifySceneConfigDetailVO();
        detailVO.setSceneCode(sceneMeta.getSceneCode());
        detailVO.setSceneName(sceneMeta.getSceneName());
        detailVO.setBizType(sceneMeta.getBizType());
        detailVO.setEventCode(sceneMeta.getEventCode());
        detailVO.setStatus(scene.getStatus());
        detailVO.setRemark(scene.getRemark());
        detailVO.setVariables(sceneMeta.getVariables());

        List<NotifySceneTargetConfigVO> targetConfigs = new ArrayList<>();
        for (NotifySceneTargetMeta targetMeta : sceneMeta.getTargetMetas()) {
            targetConfigs.add(toTargetConfigVO(targetMeta, targetMap.get(targetMeta.getTargetType())));
        }
        detailVO.setTargetConfigs(targetConfigs);
        return detailVO;
    }

    /**
     * 保存整个场景下的全部目标配置。
     *
     * @param sceneCode 场景编码
     * @param dto 场景保存参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSceneConfig(String sceneCode, NotifySceneConfigSaveDTO dto) {
        synchronizeRegistryData();
        NotifySceneMeta sceneMeta = getRequiredScene(sceneCode);
        if (dto == null) {
            throw new ServiceException("通知场景保存参数不能为空");
        }

        validateStatus(dto.getStatus(), "场景状态");
        validateLength(normalizeNullableField(dto.getRemark()), REMARK_MAX_LENGTH, "场景备注");

        // 先更新场景主记录，保证后续目标配置与当前场景启停状态保持同一事务。
        NotifyScene scene = getSceneEntity(sceneMeta.getSceneCode());
        scene.setStatus(dto.getStatus());
        scene.setRemark(normalizeNullableField(dto.getRemark()));
        notifySceneMapper.updateById(scene);

        List<NotifySceneTarget> existingTargets = listTargetsBySceneCode(sceneMeta.getSceneCode());
        Map<String, NotifySceneTarget> existingTargetMap = existingTargets.stream()
                .collect(Collectors.toMap(NotifySceneTarget::getTargetType, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, NotifySceneTargetConfigDTO> requestTargetMap = buildRequestTargetMap(dto.getTargetConfigs());

        // 按注册表逐个保存目标配置，避免前端漏传某个目标时把不支持的目标误落库。
        for (NotifySceneTargetMeta targetMeta : sceneMeta.getTargetMetas()) {
            NotifySceneTargetConfigDTO targetDTO = requestTargetMap.get(targetMeta.getTargetType());
            NotifySceneTarget entity = existingTargetMap.get(targetMeta.getTargetType());
            if (entity == null) {
                entity = buildDefaultTargetEntity(sceneMeta.getSceneCode(), targetMeta);
            }
            applyTargetConfig(sceneMeta, targetMeta, targetDTO, entity);
            if (entity.getId() == null) {
                notifySceneTargetMapper.insert(entity);
            } else {
                notifySceneTargetMapper.updateById(entity);
            }
        }

        // 如果注册表已经收口某些旧目标，这里同步清理脏数据，避免后台继续展示无效目标配置。
        Set<String> supportedTargetTypes = sceneMeta.getTargetMetas().stream()
                .map(NotifySceneTargetMeta::getTargetType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (NotifySceneTarget existingTarget : existingTargets) {
            if (!supportedTargetTypes.contains(existingTarget.getTargetType())) {
                notifySceneTargetMapper.deleteById(existingTarget.getId());
            }
        }
    }

    /**
     * 预览指定场景目标的渲染结果。
     *
     * @param dto 预览参数
     * @return 预览结果
     */
    @Override
    public NotifyScenePreviewVO preview(NotifyScenePreviewDTO dto) {
        synchronizeRegistryData();
        if (dto == null) {
            throw new ServiceException("预览参数不能为空");
        }
        NotifySceneMeta sceneMeta = getRequiredScene(dto.getSceneCode());
        NotifySceneTargetMeta targetMeta = notifySceneRegistry.getRequiredTargetMeta(sceneMeta.getSceneCode(), dto.getTargetType());
        Map<String, Object> variables = dto.getVariables() == null ? Collections.emptyMap() : dto.getVariables();
        Set<String> allowedVariables = collectAllowedVariables(sceneMeta);

        validateRouteType(dto.getRouteType());
        validateLength(normalizeNullableField(dto.getTitleTemplate()), TITLE_MAX_LENGTH, "标题模板");
        validateLength(normalizeNullableField(dto.getContentTemplate()), CONTENT_MAX_LENGTH, "内容模板");
        validateLength(normalizeNullableField(dto.getRouteType()), ROUTE_TYPE_MAX_LENGTH, "跳转类型");
        validateLength(normalizeNullableField(dto.getRouteValueTemplate()), ROUTE_VALUE_MAX_LENGTH, "跳转值模板");
        validatePlaceholders(dto.getTitleTemplate(), allowedVariables, "标题模板");
        validatePlaceholders(dto.getContentTemplate(), allowedVariables, "内容模板");
        validatePlaceholders(dto.getRouteValueTemplate(), allowedVariables, "跳转值模板");

        NotifyScenePreviewVO previewVO = new NotifyScenePreviewVO();
        previewVO.setSceneCode(sceneMeta.getSceneCode());
        previewVO.setSceneName(sceneMeta.getSceneName());
        previewVO.setTargetType(targetMeta.getTargetType());
        previewVO.setTargetTypeDesc(targetMeta.getTargetTypeDesc());
        previewVO.setTitle(renderText(resolvePreviewValue(dto.getTitleTemplate(), targetMeta.getDefaultTitleTemplate()), variables));
        previewVO.setContent(renderText(resolvePreviewValue(dto.getContentTemplate(), targetMeta.getDefaultContentTemplate()), variables));
        previewVO.setRouteType(resolvePreviewValue(dto.getRouteType(), targetMeta.getDefaultRouteType()));
        previewVO.setRouteValue(renderText(resolvePreviewValue(dto.getRouteValueTemplate(), targetMeta.getDefaultRouteValueTemplate()), variables));
        previewVO.setErrors(Collections.emptyList());

        if (NotifyTypeEnum.MP_SUBSCRIBE.getCode().equals(targetMeta.getTargetType())) {
            String pagePathTemplate = resolvePreviewValue(
                    dto.getPagePathTemplate(),
                    targetMeta.getDefaultChannelConfig() == null ? null : targetMeta.getDefaultChannelConfig().getPagePathTemplate()
            );
            validatePlaceholders(pagePathTemplate, allowedVariables, "小程序页面路径模板");
            previewVO.setPagePath(renderText(pagePathTemplate, variables));

            List<NotifyChannelFieldMappingDTO> fieldMappings = dto.getFieldMapping();
            if (fieldMappings == null || fieldMappings.isEmpty()) {
                fieldMappings = targetMeta.getDefaultChannelConfig() == null
                        ? Collections.emptyList()
                        : targetMeta.getDefaultChannelConfig().getFieldMapping();
            }
            List<NotifyScenePreviewFieldMappingVO> previewMappings = new ArrayList<>();
            for (NotifyChannelFieldMappingDTO fieldMapping : fieldMappings) {
                if (fieldMapping == null) {
                    continue;
                }
                validatePlaceholders(fieldMapping.getValue(), allowedVariables, "小程序字段映射");
                NotifyScenePreviewFieldMappingVO previewField = new NotifyScenePreviewFieldMappingVO();
                previewField.setField(normalizeNullableField(fieldMapping.getField()));
                previewField.setValueTemplate(normalizeNullableField(fieldMapping.getValue()));
                previewField.setValue(renderText(fieldMapping.getValue(), variables));
                previewMappings.add(previewField);
            }
            previewVO.setFieldMapping(previewMappings);
        }
        return previewVO;
    }

    /**
     * 把单个目标配置请求应用到实体上。
     *
     * @param sceneMeta 场景元数据
     * @param targetMeta 目标元数据
     * @param dto 请求参数
     * @param entity 待更新实体
     */
    private void applyTargetConfig(NotifySceneMeta sceneMeta, NotifySceneTargetMeta targetMeta,
                                   NotifySceneTargetConfigDTO dto, NotifySceneTarget entity) {
        NotifySceneTargetConfigDTO actualDto = dto == null ? buildDefaultTargetDTO(targetMeta) : dto;
        validateTargetConfig(sceneMeta, targetMeta, actualDto);

        entity.setSceneCode(sceneMeta.getSceneCode());
        entity.setTargetType(targetMeta.getTargetType());
        entity.setEnabled(actualDto.getEnabled());
        entity.setTitleTemplate(normalizeNullableField(actualDto.getTitleTemplate()));
        entity.setContentTemplate(normalizeNullableField(actualDto.getContentTemplate()));
        entity.setRouteType(normalizeNullableField(actualDto.getRouteType()));
        entity.setRouteValueTemplate(normalizeNullableField(actualDto.getRouteValueTemplate()));
        entity.setConfigJson(buildTargetConfigJson(targetMeta, actualDto));
        entity.setRemark(normalizeNullableField(actualDto.getRemark()));
    }

    /**
     * 校验目标配置参数。
     *
     * @param sceneMeta 场景元数据
     * @param targetMeta 目标元数据
     * @param dto 目标配置参数
     */
    private void validateTargetConfig(NotifySceneMeta sceneMeta, NotifySceneTargetMeta targetMeta,
                                      NotifySceneTargetConfigDTO dto) {
        if (dto == null) {
            throw new ServiceException("通知目标配置不能为空");
        }
        if (!targetMeta.getTargetType().equals(normalizeRequiredField(dto.getTargetType(), "通知目标类型不能为空"))) {
            throw new ServiceException("保存的通知目标类型与场景注册目标不一致");
        }

        validateStatus(dto.getEnabled(), "目标启用状态");
        validateRouteType(dto.getRouteType());
        validateLength(normalizeNullableField(dto.getTitleTemplate()), TITLE_MAX_LENGTH, "标题模板");
        validateLength(normalizeNullableField(dto.getContentTemplate()), CONTENT_MAX_LENGTH, "内容模板");
        validateLength(normalizeNullableField(dto.getRouteType()), ROUTE_TYPE_MAX_LENGTH, "跳转类型");
        validateLength(normalizeNullableField(dto.getRouteValueTemplate()), ROUTE_VALUE_MAX_LENGTH, "跳转值模板");
        validateLength(normalizeNullableField(dto.getRemark()), REMARK_MAX_LENGTH, "目标备注");

        Set<String> allowedVariables = collectAllowedVariables(sceneMeta);
        validatePlaceholders(dto.getTitleTemplate(), allowedVariables, "标题模板");
        validatePlaceholders(dto.getContentTemplate(), allowedVariables, "内容模板");
        validatePlaceholders(dto.getRouteValueTemplate(), allowedVariables, "跳转值模板");

        if (NotifyTypeEnum.MP_SUBSCRIBE.getCode().equals(targetMeta.getTargetType())) {
            validateMpSubscribeConfig(dto, allowedVariables);
        }
    }

    /**
     * 校验小程序订阅消息专属配置。
     *
     * @param dto 目标配置参数
     * @param allowedVariables 可用变量白名单
     */
    private void validateMpSubscribeConfig(NotifySceneTargetConfigDTO dto, Set<String> allowedVariables) {
        validateLength(normalizeNullableField(dto.getTemplateId()), TITLE_MAX_LENGTH, "小程序模板ID");
        validateLength(normalizeNullableField(dto.getChannelScene()), CHANNEL_SCENE_MAX_LENGTH, "小程序场景");
        validateLength(normalizeNullableField(dto.getPagePathTemplate()), CONTENT_MAX_LENGTH, "小程序页面路径模板");
        validateChannelScene(dto.getChannelScene());
        validatePlaceholders(dto.getPagePathTemplate(), allowedVariables, "小程序页面路径模板");

        List<NotifyChannelFieldMappingDTO> fieldMappings = dto.getFieldMapping() == null
                ? Collections.emptyList()
                : dto.getFieldMapping();
        for (NotifyChannelFieldMappingDTO fieldMapping : fieldMappings) {
            if (fieldMapping == null) {
                throw new ServiceException("小程序字段映射不能为空");
            }
            if (normalizeNullableField(fieldMapping.getField()) == null
                    || normalizeNullableField(fieldMapping.getValue()) == null) {
                throw new ServiceException("小程序字段映射中的字段和值都不能为空");
            }
            validatePlaceholders(fieldMapping.getValue(), allowedVariables, "小程序字段映射");
        }

        // 只有目标启用时才强制要求小程序渠道参数完整，避免管理员先停用后补配置时被阻塞。
        if (Integer.valueOf(1).equals(dto.getEnabled())) {
            if (normalizeNullableField(dto.getTemplateId()) == null) {
                throw new ServiceException("启用小程序通知时必须配置模板ID");
            }
            if (normalizeNullableField(dto.getChannelScene()) == null) {
                throw new ServiceException("启用小程序通知时必须配置小程序场景");
            }
            if (normalizeNullableField(dto.getPagePathTemplate()) == null) {
                throw new ServiceException("启用小程序通知时必须配置页面路径模板");
            }
            if (fieldMappings.isEmpty()) {
                throw new ServiceException("启用小程序通知时必须至少配置一条字段映射");
            }
        }
    }

    /**
     * 构造目标专属配置 JSON。
     *
     * @param targetMeta 目标元数据
     * @param dto 目标配置参数
     * @return 目标专属配置 JSON；当前非外部目标返回 {@code null}
     */
    private String buildTargetConfigJson(NotifySceneTargetMeta targetMeta, NotifySceneTargetConfigDTO dto) {
        if (!NotifyTypeEnum.MP_SUBSCRIBE.getCode().equals(targetMeta.getTargetType())) {
            return null;
        }
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setTemplateId(normalizeNullableField(dto.getTemplateId()));
        config.setChannelScene(normalizeNullableField(dto.getChannelScene()));
        config.setPagePathTemplate(normalizeNullableField(dto.getPagePathTemplate()));
        config.setFieldMapping(copyFieldMappings(dto.getFieldMapping()));
        return JSONUtil.toJsonStr(config);
    }

    /**
     * 构造分页行对象。
     *
     * @param sceneMeta 场景元数据
     * @param scene 场景实体
     * @param targets 目标实体列表
     * @return 列表行对象
     */
    private NotifySceneConfigPageVO buildPageVO(NotifySceneMeta sceneMeta, NotifyScene scene, List<NotifySceneTarget> targets) {
        NotifySceneConfigPageVO pageVO = new NotifySceneConfigPageVO();
        pageVO.setSceneCode(sceneMeta.getSceneCode());
        pageVO.setSceneName(sceneMeta.getSceneName());
        pageVO.setBizType(sceneMeta.getBizType());
        pageVO.setEventCode(sceneMeta.getEventCode());
        pageVO.setStatus(scene == null ? 1 : scene.getStatus());

        List<String> enabledTargetTypes = new ArrayList<>();
        List<String> enabledTargetTypeDescs = new ArrayList<>();
        if (targets != null) {
            Map<String, NotifySceneTarget> targetMap = targets.stream()
                    .collect(Collectors.toMap(NotifySceneTarget::getTargetType, item -> item, (left, right) -> left, LinkedHashMap::new));
            for (NotifySceneTargetMeta targetMeta : sceneMeta.getTargetMetas()) {
                NotifySceneTarget target = targetMap.get(targetMeta.getTargetType());
                if (target != null && Integer.valueOf(1).equals(target.getEnabled())) {
                    enabledTargetTypes.add(targetMeta.getTargetType());
                    enabledTargetTypeDescs.add(targetMeta.getTargetTypeDesc());
                }
            }
        }
        pageVO.setEnabledTargetTypes(enabledTargetTypes);
        pageVO.setEnabledTargetTypeDescs(enabledTargetTypeDescs);
        pageVO.setUpdateTime(resolveLatestUpdateTime(scene, targets));
        return pageVO;
    }

    /**
     * 把注册表场景元数据转换为前端选项。
     *
     * @param sceneMeta 场景元数据
     * @return 场景元数据选项
     */
    private NotifySceneMetaOptionVO toSceneMetaOptionVO(NotifySceneMeta sceneMeta) {
        NotifySceneMetaOptionVO optionVO = new NotifySceneMetaOptionVO();
        optionVO.setSceneCode(sceneMeta.getSceneCode());
        optionVO.setSceneName(sceneMeta.getSceneName());
        optionVO.setBizType(sceneMeta.getBizType());
        optionVO.setEventCode(sceneMeta.getEventCode());
        optionVO.setVariables(sceneMeta.getVariables());

        List<NotifySceneTargetMetaVO> targetMetas = new ArrayList<>();
        for (NotifySceneTargetMeta targetMeta : sceneMeta.getTargetMetas()) {
            NotifySceneTargetMetaVO targetMetaVO = new NotifySceneTargetMetaVO();
            targetMetaVO.setTargetType(targetMeta.getTargetType());
            targetMetaVO.setTargetTypeDesc(targetMeta.getTargetTypeDesc());
            targetMetaVO.setReceiverType(targetMeta.getReceiverType());
            targetMetaVO.setReceiverTypeDesc(targetMeta.getReceiverTypeDesc());
            targetMetaVO.setReceiverDesc(targetMeta.getReceiverDesc());
            targetMetaVO.setDefaultEnabled(targetMeta.getDefaultEnabled());
            targetMetaVO.setDefaultTemplateName(targetMeta.getDefaultTemplateName());
            targetMetaVO.setDefaultTitleTemplate(targetMeta.getDefaultTitleTemplate());
            targetMetaVO.setDefaultContentTemplate(targetMeta.getDefaultContentTemplate());
            targetMetaVO.setDefaultRouteType(targetMeta.getDefaultRouteType());
            targetMetaVO.setDefaultRouteValueTemplate(targetMeta.getDefaultRouteValueTemplate());
            targetMetaVO.setChannelType(targetMeta.getChannelType());
            targetMetaVO.setChannelTypeDesc(targetMeta.getChannelTypeDesc());
            if (targetMeta.getDefaultChannelConfig() != null) {
                targetMetaVO.setTemplateId(targetMeta.getDefaultChannelConfig().getTemplateId());
                targetMetaVO.setChannelScene(targetMeta.getDefaultChannelConfig().getChannelScene());
                targetMetaVO.setChannelSceneDesc(resolveChannelSceneDesc(targetMeta.getDefaultChannelConfig().getChannelScene()));
                targetMetaVO.setPagePathTemplate(targetMeta.getDefaultChannelConfig().getPagePathTemplate());
                targetMetaVO.setFieldMapping(copyFieldMappings(targetMeta.getDefaultChannelConfig().getFieldMapping()));
            }
            targetMetas.add(targetMetaVO);
        }
        optionVO.setTargetMetas(targetMetas);
        return optionVO;
    }

    /**
     * 把目标实体与元数据转换为详情对象。
     *
     * @param targetMeta 目标元数据
     * @param entity 目标实体
     * @return 详情对象
     */
    private NotifySceneTargetConfigVO toTargetConfigVO(NotifySceneTargetMeta targetMeta, NotifySceneTarget entity) {
        NotifySceneTargetConfigVO configVO = new NotifySceneTargetConfigVO();
        configVO.setTargetType(targetMeta.getTargetType());
        configVO.setTargetTypeDesc(targetMeta.getTargetTypeDesc());
        configVO.setReceiverType(targetMeta.getReceiverType());
        configVO.setReceiverTypeDesc(targetMeta.getReceiverTypeDesc());
        configVO.setReceiverDesc(targetMeta.getReceiverDesc());
        configVO.setChannelType(targetMeta.getChannelType());
        configVO.setChannelTypeDesc(targetMeta.getChannelTypeDesc());

        NotifySceneTarget actualEntity = entity == null ? buildDefaultTargetEntity(null, targetMeta) : entity;
        configVO.setEnabled(actualEntity.getEnabled());
        configVO.setTitleTemplate(actualEntity.getTitleTemplate());
        configVO.setContentTemplate(actualEntity.getContentTemplate());
        configVO.setRouteType(actualEntity.getRouteType());
        configVO.setRouteValueTemplate(actualEntity.getRouteValueTemplate());
        configVO.setRemark(actualEntity.getRemark());

        NotifyTemplateChannelConfig channelConfig = parseChannelConfig(actualEntity.getConfigJson());
        if (channelConfig != null) {
            configVO.setTemplateId(channelConfig.getTemplateId());
            configVO.setChannelScene(channelConfig.getChannelScene());
            configVO.setChannelSceneDesc(resolveChannelSceneDesc(channelConfig.getChannelScene()));
            configVO.setPagePathTemplate(channelConfig.getPagePathTemplate());
            configVO.setFieldMapping(copyFieldMappings(channelConfig.getFieldMapping()));
        }
        return configVO;
    }

    /**
     * 按场景分组目标配置。
     *
     * @param targets 全量目标配置
     * @return 分组结果
     */
    private Map<String, List<NotifySceneTarget>> groupTargetsBySceneCode(List<NotifySceneTarget> targets) {
        Map<String, List<NotifySceneTarget>> targetMap = new LinkedHashMap<>();
        for (NotifySceneTarget target : targets) {
            targetMap.computeIfAbsent(target.getSceneCode(), key -> new ArrayList<>()).add(target);
        }
        return targetMap;
    }

    /**
     * 构造请求目标配置映射。
     *
     * @param targetConfigs 请求目标配置列表
     * @return 按目标类型索引的请求映射
     */
    private Map<String, NotifySceneTargetConfigDTO> buildRequestTargetMap(List<NotifySceneTargetConfigDTO> targetConfigs) {
        Map<String, NotifySceneTargetConfigDTO> requestTargetMap = new LinkedHashMap<>();
        if (targetConfigs == null) {
            return requestTargetMap;
        }
        for (NotifySceneTargetConfigDTO targetConfig : targetConfigs) {
            if (targetConfig == null) {
                throw new ServiceException("通知目标配置不能为空");
            }
            String targetType = normalizeRequiredField(targetConfig.getTargetType(), "通知目标类型不能为空");
            if (requestTargetMap.put(targetType, targetConfig) != null) {
                throw new ServiceException("同一场景下同一目标类型只允许提交一份配置");
            }
        }
        return requestTargetMap;
    }

    /**
     * 同步注册表到场景表和目标配置表。
     *
     * <p>后台页面、预览接口和运行时兼容读取都依赖这两个新表。
     * 因此每次入口调用前都先补齐缺失记录，避免因为某个场景尚未被手工初始化而导致查询或发送失败。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    protected void synchronizeRegistryData() {
        List<NotifyScene> existingScenes = listAllScenes();
        Map<String, NotifyScene> existingSceneMap = existingScenes.stream()
                .collect(Collectors.toMap(NotifyScene::getSceneCode, item -> item, (left, right) -> left, LinkedHashMap::new));

        for (NotifySceneMeta sceneMeta : notifySceneRegistry.listScenes()) {
            NotifyScene scene = existingSceneMap.get(sceneMeta.getSceneCode());
            if (scene == null) {
                notifySceneMapper.insert(buildSceneEntity(sceneMeta));
            } else if (shouldRefreshSceneBaseInfo(sceneMeta, scene)) {
                scene.setSceneName(sceneMeta.getSceneName());
                scene.setBizType(sceneMeta.getBizType());
                scene.setEventCode(sceneMeta.getEventCode());
                notifySceneMapper.updateById(scene);
            }

            Map<String, NotifySceneTarget> targetMap = listTargetsBySceneCode(sceneMeta.getSceneCode()).stream()
                    .collect(Collectors.toMap(NotifySceneTarget::getTargetType, item -> item, (left, right) -> left, LinkedHashMap::new));
            for (NotifySceneTargetMeta targetMeta : sceneMeta.getTargetMetas()) {
                if (!targetMap.containsKey(targetMeta.getTargetType())) {
                    notifySceneTargetMapper.insert(buildDefaultTargetEntity(sceneMeta.getSceneCode(), targetMeta));
                }
            }
        }
    }

    /**
     * 判断是否需要刷新场景基础元数据。
     *
     * @param sceneMeta 注册表元数据
     * @param scene 数据库实体
     * @return `true` 表示需要刷新数据库中的系统字段
     */
    private boolean shouldRefreshSceneBaseInfo(NotifySceneMeta sceneMeta, NotifyScene scene) {
        return !StrUtil.equals(sceneMeta.getSceneName(), scene.getSceneName())
                || !StrUtil.equals(sceneMeta.getBizType(), scene.getBizType())
                || !StrUtil.equals(sceneMeta.getEventCode(), scene.getEventCode());
    }

    /**
     * 构造场景主记录。
     *
     * @param sceneMeta 场景元数据
     * @return 场景实体
     */
    private NotifyScene buildSceneEntity(NotifySceneMeta sceneMeta) {
        validateLength(sceneMeta.getSceneCode(), SCENE_CODE_MAX_LENGTH, "场景编码");
        validateLength(sceneMeta.getSceneName(), SCENE_NAME_MAX_LENGTH, "场景名称");
        validateLength(sceneMeta.getBizType(), BIZ_TYPE_MAX_LENGTH, "业务类型");
        validateLength(sceneMeta.getEventCode(), EVENT_CODE_MAX_LENGTH, "事件编码");

        NotifyScene scene = new NotifyScene();
        scene.setSceneCode(sceneMeta.getSceneCode());
        scene.setSceneName(sceneMeta.getSceneName());
        scene.setBizType(sceneMeta.getBizType());
        scene.setEventCode(sceneMeta.getEventCode());
        scene.setStatus(1);
        scene.setRemark(null);
        return scene;
    }

    /**
     * 构造默认目标配置实体。
     *
     * @param sceneCode 场景编码
     * @param targetMeta 目标元数据
     * @return 默认目标配置实体
     */
    private NotifySceneTarget buildDefaultTargetEntity(String sceneCode, NotifySceneTargetMeta targetMeta) {
        NotifySceneTarget entity = new NotifySceneTarget();
        entity.setSceneCode(sceneCode);
        entity.setTargetType(targetMeta.getTargetType());
        entity.setEnabled(targetMeta.getDefaultEnabled());
        entity.setTitleTemplate(targetMeta.getDefaultTitleTemplate());
        entity.setContentTemplate(targetMeta.getDefaultContentTemplate());
        entity.setRouteType(targetMeta.getDefaultRouteType());
        entity.setRouteValueTemplate(targetMeta.getDefaultRouteValueTemplate());
        entity.setRemark(null);
        if (targetMeta.getDefaultChannelConfig() != null) {
            entity.setConfigJson(JSONUtil.toJsonStr(targetMeta.getDefaultChannelConfig()));
        }
        return entity;
    }

    /**
     * 构造默认目标配置 DTO。
     *
     * @param targetMeta 目标元数据
     * @return 默认目标配置 DTO
     */
    private NotifySceneTargetConfigDTO buildDefaultTargetDTO(NotifySceneTargetMeta targetMeta) {
        NotifySceneTargetConfigDTO dto = new NotifySceneTargetConfigDTO();
        dto.setTargetType(targetMeta.getTargetType());
        dto.setEnabled(targetMeta.getDefaultEnabled());
        dto.setTitleTemplate(targetMeta.getDefaultTitleTemplate());
        dto.setContentTemplate(targetMeta.getDefaultContentTemplate());
        dto.setRouteType(targetMeta.getDefaultRouteType());
        dto.setRouteValueTemplate(targetMeta.getDefaultRouteValueTemplate());
        if (targetMeta.getDefaultChannelConfig() != null) {
            dto.setTemplateId(targetMeta.getDefaultChannelConfig().getTemplateId());
            dto.setChannelScene(targetMeta.getDefaultChannelConfig().getChannelScene());
            dto.setPagePathTemplate(targetMeta.getDefaultChannelConfig().getPagePathTemplate());
            dto.setFieldMapping(copyFieldMappings(targetMeta.getDefaultChannelConfig().getFieldMapping()));
        }
        return dto;
    }

    /**
     * 查询全部场景记录。
     *
     * @return 场景记录列表
     */
    private List<NotifyScene> listAllScenes() {
        return notifySceneMapper.selectList(new LambdaQueryWrapper<NotifyScene>()
                .orderByAsc(NotifyScene::getId));
    }

    /**
     * 查询全部目标配置。
     *
     * @return 目标配置列表
     */
    private List<NotifySceneTarget> listAllTargets() {
        return notifySceneTargetMapper.selectList(new LambdaQueryWrapper<NotifySceneTarget>()
                .orderByAsc(NotifySceneTarget::getId));
    }

    /**
     * 查询单个场景实体。
     *
     * @param sceneCode 场景编码
     * @return 场景实体
     */
    private NotifyScene getSceneEntity(String sceneCode) {
        NotifyScene scene = notifySceneMapper.selectOne(new LambdaQueryWrapper<NotifyScene>()
                .eq(NotifyScene::getSceneCode, sceneCode)
                .last("limit 1"));
        if (scene == null) {
            throw new ServiceException("通知场景不存在：" + sceneCode);
        }
        return scene;
    }

    /**
     * 查询单个场景下的全部目标配置。
     *
     * @param sceneCode 场景编码
     * @return 目标配置列表
     */
    private List<NotifySceneTarget> listTargetsBySceneCode(String sceneCode) {
        return notifySceneTargetMapper.selectList(new LambdaQueryWrapper<NotifySceneTarget>()
                .eq(NotifySceneTarget::getSceneCode, sceneCode)
                .orderByAsc(NotifySceneTarget::getId));
    }

    /**
     * 解析小程序渠道配置。
     *
     * @param configJson 配置 JSON
     * @return 解析后的配置对象；为空时返回 {@code null}
     */
    private NotifyTemplateChannelConfig parseChannelConfig(String configJson) {
        if (StrUtil.isBlank(configJson)) {
            return null;
        }
        return JSONUtil.toBean(configJson, NotifyTemplateChannelConfig.class);
    }

    /**
     * 复制字段映射列表。
     *
     * @param fieldMappings 原字段映射列表
     * @return 复制后的字段映射列表
     */
    private List<NotifyChannelFieldMappingDTO> copyFieldMappings(List<NotifyChannelFieldMappingDTO> fieldMappings) {
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            return new ArrayList<>();
        }
        List<NotifyChannelFieldMappingDTO> copied = new ArrayList<>();
        for (NotifyChannelFieldMappingDTO fieldMapping : fieldMappings) {
            if (fieldMapping == null) {
                continue;
            }
            NotifyChannelFieldMappingDTO copy = new NotifyChannelFieldMappingDTO();
            copy.setField(normalizeNullableField(fieldMapping.getField()));
            copy.setValue(normalizeNullableField(fieldMapping.getValue()));
            copied.add(copy);
        }
        return copied;
    }

    /**
     * 汇总场景允许使用的变量白名单。
     *
     * @param sceneMeta 场景元数据
     * @return 变量白名单
     */
    private Set<String> collectAllowedVariables(NotifySceneMeta sceneMeta) {
        Set<String> variables = new LinkedHashSet<>();
        for (NotifyTemplateVariableMeta variableMeta : sceneMeta.getVariables()) {
            variables.add(variableMeta.getName());
        }
        return variables;
    }

    /**
     * 校验模板占位符是否合法。
     *
     * @param template 模板文本
     * @param allowedVariables 允许的变量白名单
     * @param fieldName 字段名称
     */
    private void validatePlaceholders(String template, Set<String> allowedVariables, String fieldName) {
        if (StrUtil.isBlank(template)) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!allowedVariables.contains(variableName)) {
                throw new ServiceException(fieldName + "中包含未注册变量：" + variableName);
            }
        }
    }

    /**
     * 渲染文本模板。
     *
     * @param template 模板文本
     * @param variables 变量快照
     * @return 渲染结果
     */
    private String renderText(String template, Map<String, Object> variables) {
        if (StrUtil.isBlank(template)) {
            return null;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object variableValue = variables.get(variableName);
            String replacement = variableValue == null ? "" : String.valueOf(variableValue);
            matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    /**
     * 解析预览字段值。
     *
     * <p>预览允许页面只传用户当前修改过的字段，未修改字段回退到注册表默认值，
     * 这样可以避免后台页面在初次打开时必须先把所有默认值回填完整后才能预览。</p>
     *
     * @param requestValue 页面传入值
     * @param defaultValue 默认值
     * @return 实际参与预览的值
     */
    private String resolvePreviewValue(String requestValue, String defaultValue) {
        return normalizeNullableField(requestValue) == null ? defaultValue : normalizeNullableField(requestValue);
    }

    /**
     * 计算场景最近更新时间。
     *
     * @param scene 场景实体
     * @param targets 目标配置列表
     * @return 最近更新时间
     */
    private LocalDateTime resolveLatestUpdateTime(NotifyScene scene, List<NotifySceneTarget> targets) {
        LocalDateTime latest = scene == null ? null : scene.getUpdateTime();
        if (targets == null || targets.isEmpty()) {
            return latest;
        }
        for (NotifySceneTarget target : targets) {
            if (target == null || target.getUpdateTime() == null) {
                continue;
            }
            if (latest == null || target.getUpdateTime().isAfter(latest)) {
                latest = target.getUpdateTime();
            }
        }
        return latest;
    }

    /**
     * 判断场景是否命中过滤条件。
     *
     * @param sceneMeta 场景元数据
     * @param query 查询参数
     * @return `true` 表示命中过滤条件
     */
    private boolean matchesSceneFilter(NotifySceneMeta sceneMeta, NotifySceneConfigQuery query) {
        if (StrUtil.isNotBlank(query.getSceneName())
                && !StrUtil.contains(sceneMeta.getSceneName(), query.getSceneName().trim())) {
            return false;
        }
        if (StrUtil.isNotBlank(query.getSceneCode())
                && !StrUtil.contains(sceneMeta.getSceneCode(), query.getSceneCode().trim())) {
            return false;
        }
        if (StrUtil.isNotBlank(query.getBizType())
                && !StrUtil.equals(sceneMeta.getBizType(), query.getBizType().trim())) {
            return false;
        }
        if (StrUtil.isNotBlank(query.getTargetType())
                && sceneMeta.getTargetMeta(query.getTargetType().trim()) == null) {
            return false;
        }
        return true;
    }

    /**
     * 构造通知目标类型选项。
     *
     * @return 通知目标类型选项
     */
    private List<NotifyTemplateEnumOptionVO> buildTargetTypeOptions() {
        List<NotifyTemplateEnumOptionVO> options = new ArrayList<>();
        for (NotifyTypeEnum notifyTypeEnum : NotifyTypeEnum.values()) {
            NotifyTemplateEnumOptionVO optionVO = new NotifyTemplateEnumOptionVO();
            optionVO.setCode(notifyTypeEnum.getCode());
            optionVO.setDesc(notifyTypeEnum.getDesc());
            options.add(optionVO);
        }
        return options;
    }

    /**
     * 构造小程序场景选项。
     *
     * @return 小程序场景选项
     */
    private List<NotifyTemplateEnumOptionVO> buildChannelSceneOptions() {
        List<NotifyTemplateEnumOptionVO> options = new ArrayList<>();
        for (NotifyChannelSceneEnum channelSceneEnum : NotifyChannelSceneEnum.values()) {
            NotifyTemplateEnumOptionVO optionVO = new NotifyTemplateEnumOptionVO();
            optionVO.setCode(channelSceneEnum.getCode());
            optionVO.setDesc(channelSceneEnum.getDesc());
            options.add(optionVO);
        }
        return options;
    }

    /**
     * 构造跳转类型选项。
     *
     * @return 跳转类型选项
     */
    private List<NotifyTemplateEnumOptionVO> buildRouteTypeOptions() {
        List<NotifyTemplateEnumOptionVO> options = new ArrayList<>();
        for (NotifyRouteTypeEnum routeTypeEnum : NotifyRouteTypeEnum.values()) {
            NotifyTemplateEnumOptionVO optionVO = new NotifyTemplateEnumOptionVO();
            optionVO.setCode(routeTypeEnum.getCode());
            optionVO.setDesc(resolveRouteTypeDesc(routeTypeEnum.getCode()));
            options.add(optionVO);
        }
        return options;
    }

    /**
     * 解析跳转类型中文描述。
     *
     * @param routeType 跳转类型编码
     * @return 中文描述
     */
    private String resolveRouteTypeDesc(String routeType) {
        if (NotifyRouteTypeEnum.WORK_ORDER_DETAIL.getCode().equals(routeType)) {
            return "工单详情";
        }
        if (NotifyRouteTypeEnum.WORK_ORDER_EVALUATE.getCode().equals(routeType)) {
            return "工单评价";
        }
        return routeType;
    }

    /**
     * 校验状态值。
     *
     * @param status 状态值
     * @param fieldName 字段名称
     */
    private void validateStatus(Integer status, String fieldName) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ServiceException(fieldName + "只允许为 1 或 0");
        }
    }

    /**
     * 校验跳转类型。
     *
     * @param routeType 跳转类型
     */
    private void validateRouteType(String routeType) {
        String normalizedRouteType = normalizeNullableField(routeType);
        if (normalizedRouteType == null) {
            return;
        }
        if (NotifyRouteTypeEnum.getByCode(normalizedRouteType) == null) {
            throw new ServiceException("不支持的跳转类型：" + normalizedRouteType);
        }
    }

    /**
     * 校验小程序场景。
     *
     * @param channelScene 小程序场景编码
     */
    private void validateChannelScene(String channelScene) {
        String normalizedChannelScene = normalizeNullableField(channelScene);
        if (normalizedChannelScene == null) {
            return;
        }
        if (NotifyChannelSceneEnum.getByCode(normalizedChannelScene) == null) {
            throw new ServiceException("不支持的小程序场景：" + normalizedChannelScene);
        }
    }

    /**
     * 校验字符串长度。
     *
     * @param value 字段值
     * @param maxLength 最大长度
     * @param fieldName 字段名称
     */
    private void validateLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new ServiceException(fieldName + "长度不能超过" + maxLength);
        }
    }

    /**
     * 规范化必填字符串。
     *
     * @param value 原始值
     * @param emptyMessage 为空时的异常文案
     * @return 规范化后的字符串
     */
    private String normalizeRequiredField(String value, String emptyMessage) {
        String normalizedValue = normalizeNullableField(value);
        if (normalizedValue == null) {
            throw new ServiceException(emptyMessage);
        }
        return normalizedValue;
    }

    /**
     * 规范化可空字符串。
     *
     * @param value 原始值
     * @return 去掉首尾空白后的值；为空白时返回 {@code null}
     */
    private String normalizeNullableField(String value) {
        return StrUtil.trimToNull(value);
    }

    /**
     * 解析小程序场景中文描述。
     *
     * @param channelScene 小程序场景编码
     * @return 中文描述
     */
    private String resolveChannelSceneDesc(String channelScene) {
        NotifyChannelSceneEnum channelSceneEnum = NotifyChannelSceneEnum.getByCode(channelScene);
        return channelSceneEnum == null ? null : channelSceneEnum.getDesc();
    }

    /**
     * 按场景编码读取注册表场景并做必填校验。
     *
     * @param sceneCode 场景编码
     * @return 场景元数据
     */
    private NotifySceneMeta getRequiredScene(String sceneCode) {
        return notifySceneRegistry.getRequiredScene(normalizeRequiredField(sceneCode, "通知场景编码不能为空"));
    }
}
