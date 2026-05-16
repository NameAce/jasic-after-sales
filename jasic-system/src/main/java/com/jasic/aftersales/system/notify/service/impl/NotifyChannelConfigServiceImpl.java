package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateChannelDTO;
import com.jasic.aftersales.system.notify.domain.entity.NotifySceneTarget;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelSceneEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.mapper.NotifySceneTargetMapper;
import com.jasic.aftersales.system.notify.service.NotifyChannelConfigService;
import com.jasic.aftersales.system.notify.support.NotifySceneMeta;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
import com.jasic.aftersales.system.notify.support.NotifySceneTargetMeta;
import com.jasic.aftersales.system.notify.support.NotifyTemplateChannelConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知渠道配置服务实现。
 *
 * <p>重构后外部渠道参数统一落在 `notify_scene_target.config_json`，
 * 该服务主要承担旧渠道接口到新目标配置模型之间的兼容映射。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Service
public class NotifyChannelConfigServiceImpl implements NotifyChannelConfigService {

    private static final int REMARK_MAX_LENGTH = 255;

    @Resource
    private NotifySceneTargetMapper notifySceneTargetMapper;

    @Resource
    private NotifySceneRegistry notifySceneRegistry;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NotifyTemplateChannelVO> listChannelConfigs(String sceneCode) {
        String normalizedSceneCode = normalizeRequiredField(sceneCode, "通知场景编码不能为空");
        List<NotifySceneTargetMeta> targetMetas = getExternalTargetMetasOrThrow(normalizedSceneCode);
        Map<String, NotifySceneTarget> entityMap = listTargetEntities(normalizedSceneCode, targetMetas).stream()
                .collect(Collectors.toMap(NotifySceneTarget::getTargetType, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<NotifyTemplateChannelVO> result = new ArrayList<>();
        for (NotifySceneTargetMeta targetMeta : targetMetas) {
            NotifySceneTarget entity = entityMap.get(targetMeta.getTargetType());
            if (entity == null) {
                entity = buildDefaultTargetEntity(normalizedSceneCode, targetMeta);
            }
            result.add(toChannelVO(targetMeta, entity));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NotifyTemplateChannelVO> listRuntimeChannelConfigs(String sceneCode) {
        String normalizedSceneCode = normalizeRequiredField(sceneCode, "通知场景编码不能为空");
        List<NotifySceneTargetMeta> targetMetas = getExternalTargetMetasOrThrow(normalizedSceneCode);
        Map<String, NotifySceneTargetMeta> targetMetaMap = targetMetas.stream()
                .collect(Collectors.toMap(NotifySceneTargetMeta::getTargetType, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<NotifyTemplateChannelVO> result = new ArrayList<>();
        for (NotifySceneTarget entity : listTargetEntities(normalizedSceneCode, targetMetas)) {
            if (!Objects.equals(entity.getEnabled(), 1)) {
                continue;
            }
            NotifySceneTargetMeta targetMeta = targetMetaMap.get(entity.getTargetType());
            if (targetMeta != null) {
                result.add(toChannelVO(targetMeta, entity));
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRuntimeChannelConfigs(String sceneCode) {
        String normalizedSceneCode = normalizeRequiredField(sceneCode, "通知场景编码不能为空");
        return !listTargetEntities(normalizedSceneCode, getExternalTargetMetasOrThrow(normalizedSceneCode)).isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChannelConfigs(String sceneCode, List<NotifyTemplateChannelDTO> channelConfigs) {
        String normalizedSceneCode = normalizeRequiredField(sceneCode, "通知场景编码不能为空");
        List<NotifySceneTargetMeta> targetMetas = getExternalTargetMetasOrThrow(normalizedSceneCode);
        Map<String, NotifySceneTargetMeta> targetMetaMap = targetMetas.stream()
                .collect(Collectors.toMap(NotifySceneTargetMeta::getTargetType, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<NotifyTemplateChannelDTO> actualConfigs = channelConfigs == null ? Collections.emptyList() : channelConfigs;
        Map<String, NotifyTemplateChannelDTO> requestMap = buildRequestTargetMap(targetMetas, actualConfigs);
        validateChannelConfigs(targetMetaMap, requestMap);

        Map<String, NotifySceneTarget> existingEntityMap = listTargetEntities(normalizedSceneCode, targetMetas).stream()
                .collect(Collectors.toMap(NotifySceneTarget::getTargetType, item -> item, (left, right) -> left, LinkedHashMap::new));

        boolean multipleExternalTargets = targetMetas.size() > 1;
        for (NotifySceneTargetMeta targetMeta : targetMetas) {
            NotifyTemplateChannelDTO channelDTO = requestMap.get(targetMeta.getTargetType());
            if (channelDTO == null && multipleExternalTargets) {
                // 旧渠道页仍是单表单交互，为避免一次保存把其它目标误停用，
                // 多目标场景下对未提交的目标保持现状。
                continue;
            }
            if (channelDTO == null) {
                channelDTO = buildDisabledChannelDTO(targetMeta);
            }
            NotifySceneTarget entity = existingEntityMap.get(targetMeta.getTargetType());
            if (entity == null) {
                entity = buildDefaultTargetEntity(normalizedSceneCode, targetMeta);
                notifySceneTargetMapper.insert(entity);
            }
            entity.setEnabled(channelDTO.getChannelEnabled());
            entity.setConfigJson(buildChannelConfigJson(channelDTO));
            entity.setRemark(normalizeNullableField(channelDTO.getRemark()));
            notifySceneTargetMapper.updateById(entity);
        }
    }

    /**
     * 校验渠道配置请求。
     *
     * @param targetMetaMap 目标元数据映射
     * @param requestMap 请求映射
     */
    private void validateChannelConfigs(Map<String, NotifySceneTargetMeta> targetMetaMap,
                                        Map<String, NotifyTemplateChannelDTO> requestMap) {
        for (Map.Entry<String, NotifyTemplateChannelDTO> entry : requestMap.entrySet()) {
            NotifyTemplateChannelDTO channelDTO = entry.getValue();
            if (channelDTO == null) {
                throw new ServiceException("渠道配置不能为空");
            }
            NotifySceneTargetMeta targetMeta = targetMetaMap.get(entry.getKey());
            if (targetMeta == null) {
                throw new ServiceException("当前通知场景不支持目标类型：" + entry.getKey());
            }

            validateStatus(channelDTO.getChannelEnabled());
            validateLength(normalizeNullableField(channelDTO.getRemark()), REMARK_MAX_LENGTH, "备注");

            String channelTypeCode = normalizeRequiredField(channelDTO.getChannelType(), "渠道类型不能为空");
            if (!NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode().equals(channelTypeCode)) {
                throw new ServiceException("当前阶段仅支持 MP_SUBSCRIBE 渠道配置");
            }
            if (!channelTypeCode.equals(targetMeta.getChannelType())) {
                throw new ServiceException("当前通知目标只允许维护渠道类型：" + targetMeta.getChannelType());
            }
            validateChannelScene(channelDTO.getChannelScene());

            List<NotifyChannelFieldMappingDTO> fieldMappings = copyFieldMappings(channelDTO.getFieldMapping());
            Set<String> uniqueFields = new LinkedHashSet<>();
            for (NotifyChannelFieldMappingDTO fieldMapping : fieldMappings) {
                if (fieldMapping == null
                        || normalizeNullableField(fieldMapping.getField()) == null
                        || normalizeNullableField(fieldMapping.getValue()) == null) {
                    throw new ServiceException("小程序字段映射中的字段和值都不能为空");
                }
                if (!uniqueFields.add(fieldMapping.getField())) {
                    throw new ServiceException("小程序字段映射中的模板字段不允许重复");
                }
            }

            if (Objects.equals(channelDTO.getChannelEnabled(), 1)) {
                if (normalizeNullableField(channelDTO.getTemplateId()) == null) {
                    throw new ServiceException("启用小程序渠道时必须配置模板ID");
                }
                if (normalizeNullableField(channelDTO.getChannelScene()) == null) {
                    throw new ServiceException("启用小程序渠道时必须配置小程序场景");
                }
                if (normalizeNullableField(channelDTO.getPagePathTemplate()) == null) {
                    throw new ServiceException("启用小程序渠道时必须配置页面路径模板");
                }
                if (fieldMappings.isEmpty()) {
                    throw new ServiceException("启用小程序渠道时必须至少配置一条字段映射");
                }
            }
        }
    }

    /**
     * 构建渠道配置JSON。
     *
     * @param dto 渠道配置参数
     * @return 渠道配置JSON
     */
    private String buildChannelConfigJson(NotifyTemplateChannelDTO dto) {
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setTemplateId(normalizeNullableField(dto.getTemplateId()));
        config.setChannelScene(normalizeNullableField(dto.getChannelScene()));
        config.setPagePathTemplate(normalizeNullableField(dto.getPagePathTemplate()));
        config.setFieldMapping(copyFieldMappings(dto.getFieldMapping()));
        return JSONUtil.toJsonStr(config);
    }

    /**
     * 转换为兼容渠道配置VO。
     *
     * @param targetMeta 目标元数据
     * @param entity 目标实体
     * @return 渠道配置VO
     */
    private NotifyTemplateChannelVO toChannelVO(NotifySceneTargetMeta targetMeta, NotifySceneTarget entity) {
        NotifyTemplateChannelVO vo = new NotifyTemplateChannelVO();
        vo.setId(entity.getId());
        vo.setTargetType(targetMeta.getTargetType());
        vo.setTargetTypeDesc(targetMeta.getTargetTypeDesc());
        vo.setSceneCode(entity.getSceneCode());
        vo.setChannelType(targetMeta.getChannelType());
        vo.setChannelTypeDesc(targetMeta.getChannelTypeDesc());
        vo.setChannelEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        vo.setConfigJson(entity.getConfigJson());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());

        NotifyTemplateChannelConfig config = parseChannelConfig(entity.getConfigJson());
        if (config != null) {
            vo.setTemplateId(config.getTemplateId());
            vo.setChannelScene(config.getChannelScene());
            vo.setChannelSceneDesc(resolveChannelSceneDesc(config.getChannelScene()));
            vo.setPagePathTemplate(config.getPagePathTemplate());
            vo.setFieldMapping(copyFieldMappings(config.getFieldMapping()));
        }
        return vo;
    }

    /**
     * 构建请求目标映射。
     *
     * @param targetMetas 外部目标元数据
     * @param channelConfigs 请求列表
     * @return 按 targetType 索引的请求映射
     */
    private Map<String, NotifyTemplateChannelDTO> buildRequestTargetMap(List<NotifySceneTargetMeta> targetMetas,
                                                                        List<NotifyTemplateChannelDTO> channelConfigs) {
        Map<String, NotifyTemplateChannelDTO> requestMap = new LinkedHashMap<>();
        NotifySceneTargetMeta singleTargetMeta = targetMetas.size() == 1 ? targetMetas.get(0) : null;
        for (NotifyTemplateChannelDTO channelDTO : channelConfigs) {
            if (channelDTO == null) {
                throw new ServiceException("渠道配置不能为空");
            }
            String targetType = normalizeNullableField(channelDTO.getTargetType());
            if (targetType == null && singleTargetMeta != null) {
                targetType = singleTargetMeta.getTargetType();
                channelDTO.setTargetType(targetType);
            }
            if (targetType == null) {
                throw new ServiceException("多目标渠道配置时必须传入 targetType");
            }
            if (requestMap.put(targetType, channelDTO) != null) {
                throw new ServiceException("同一个通知目标只允许提交一条渠道配置");
            }
        }
        return requestMap;
    }

    /**
     * 查询场景对应的全部外部目标元数据。
     *
     * @param sceneCode 场景编码
     * @return 外部目标元数据列表
     */
    private List<NotifySceneTargetMeta> getExternalTargetMetasOrThrow(String sceneCode) {
        NotifySceneMeta sceneMeta = notifySceneRegistry.getRequiredScene(sceneCode);
        List<NotifySceneTargetMeta> targetMetas = sceneMeta.getTargetMetas().stream()
                .filter(NotifySceneTargetMeta::isExternalTarget)
                .collect(Collectors.toList());
        if (targetMetas.isEmpty()) {
            throw new ServiceException("当前通知场景不支持外部渠道配置");
        }
        return targetMetas;
    }

    /**
     * 查询场景下的外部目标配置实体。
     *
     * @param sceneCode 场景编码
     * @param targetMetas 目标元数据列表
     * @return 目标配置实体列表
     */
    private List<NotifySceneTarget> listTargetEntities(String sceneCode, List<NotifySceneTargetMeta> targetMetas) {
        Set<String> targetTypes = targetMetas.stream()
                .map(NotifySceneTargetMeta::getTargetType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (targetTypes.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<NotifySceneTarget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifySceneTarget::getSceneCode, sceneCode)
                .in(NotifySceneTarget::getTargetType, targetTypes)
                .orderByAsc(NotifySceneTarget::getId);
        return notifySceneTargetMapper.selectList(wrapper);
    }

    /**
     * 构建默认目标实体。
     *
     * @param sceneCode 场景编码
     * @param targetMeta 目标元数据
     * @return 默认目标实体
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
     * 构建默认停用渠道DTO。
     *
     * @param targetMeta 目标元数据
     * @return 默认停用DTO
     */
    private NotifyTemplateChannelDTO buildDisabledChannelDTO(NotifySceneTargetMeta targetMeta) {
        NotifyTemplateChannelDTO dto = new NotifyTemplateChannelDTO();
        dto.setTargetType(targetMeta.getTargetType());
        dto.setChannelType(targetMeta.getChannelType());
        dto.setChannelEnabled(0);
        return dto;
    }

    /**
     * 解析渠道配置。
     *
     * @param configJson 配置JSON
     * @return 渠道配置对象
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
     * 解析小程序场景说明。
     *
     * @param channelScene 小程序场景编码
     * @return 场景说明
     */
    private String resolveChannelSceneDesc(String channelScene) {
        NotifyChannelSceneEnum channelSceneEnum = NotifyChannelSceneEnum.getByCode(channelScene);
        return channelSceneEnum == null ? null : channelSceneEnum.getDesc();
    }

    /**
     * 校验状态值。
     *
     * @param status 状态值
     */
    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ServiceException("状态只允许为 1 或 0");
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
     * 校验字段长度。
     *
     * @param value 字段值
     * @param maxLength 最大长度
     * @param fieldName 字段名
     */
    private void validateLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new ServiceException(fieldName + "长度不能超过" + maxLength);
        }
    }

    /**
     * 规范化必填字段。
     *
     * @param value 原始值
     * @param emptyMessage 为空时的异常文案
     * @return 规范化后的值
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
     * @return 去除首尾空白后的值；为空时返回 {@code null}
     */
    private String normalizeNullableField(String value) {
        return StrUtil.trimToNull(value);
    }
}
