package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateChannelDTO;
import com.jasic.aftersales.system.notify.domain.entity.NotifySceneTarget;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTypeEnum;
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
 * <p>阶段一之后，外部渠道专属参数已经并入 `notify_scene_target.config_json`，
 * 因此该实现的职责变为：
 * 1. 把旧的渠道配置读取接口兼容映射到新的目标配置表
 * 2. 继续为运行时 `MP_SUBSCRIBE` sender 提供结构化渠道参数
 *
 * <p>该实现不再维护独立的渠道配置表，也不负责模板主数据、接收人规则和真实发送。</p>
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
     * 查询后台维护页使用的渠道配置。
     *
     * @param sceneCode 通知场景编码
     * @return 渠道配置列表
     */
    @Override
    public List<NotifyTemplateChannelVO> listChannelConfigs(String sceneCode) {
        NotifySceneTargetMeta targetMeta = getExternalTargetMetaOrThrow(sceneCode);
        NotifySceneTarget entity = getTargetEntity(targetMeta, false);
        return entity == null ? Collections.emptyList() : Collections.singletonList(toChannelVO(targetMeta, entity));
    }

    /**
     * 按通知场景查询运行时可发送渠道配置。
     *
     * @param sceneCode 通知场景编码
     * @return 启用中的渠道配置列表
     */
    @Override
    public List<NotifyTemplateChannelVO> listRuntimeChannelConfigs(String sceneCode) {
        NotifySceneTargetMeta targetMeta = getExternalTargetMetaOrThrow(sceneCode);
        NotifySceneTarget entity = getTargetEntity(targetMeta, true);
        return entity == null ? Collections.emptyList() : Collections.singletonList(toChannelVO(targetMeta, entity));
    }

    /**
     * 判断某个通知场景是否存在渠道配置记录。
     *
     * @param sceneCode 通知场景编码
     * @return `true` 表示存在任意渠道配置记录
     */
    @Override
    public boolean hasRuntimeChannelConfigs(String sceneCode) {
        NotifySceneTargetMeta targetMeta = getExternalTargetMetaOrThrow(sceneCode);
        return countTargetEntities(targetMeta) > 0;
    }

    /**
     * 保存通知场景渠道配置。
     *
     * <p>旧接口的保存单位是“按场景整批覆盖渠道配置”，
     * 但阶段一后一个场景下的外部渠道配置已经收口为单条 `MP_SUBSCRIBE` 目标配置。
     * 因此这里会把旧请求转换为目标配置上的 enabled/configJson/remark 更新，
     * 并保留标题、内容、跳转等非渠道字段不被误覆盖。</p>
     *
     * @param sceneCode 通知场景编码
     * @param channelConfigs 渠道配置列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChannelConfigs(String sceneCode, List<NotifyTemplateChannelDTO> channelConfigs) {
        NotifySceneTargetMeta targetMeta = getExternalTargetMetaOrThrow(sceneCode);
        List<NotifyTemplateChannelDTO> actualConfigs = channelConfigs == null ? Collections.emptyList() : channelConfigs;
        validateChannelConfigs(targetMeta, actualConfigs);

        NotifySceneTarget entity = getOrCreateTargetEntity(targetMeta);
        NotifyTemplateChannelDTO channelDTO = actualConfigs.isEmpty() ? buildDisabledChannelDTO(targetMeta) : actualConfigs.get(0);
        entity.setEnabled(channelDTO.getChannelEnabled());
        entity.setConfigJson(buildChannelConfigJson(channelDTO));
        entity.setRemark(normalizeNullableField(channelDTO.getRemark()));
        notifySceneTargetMapper.updateById(entity);
    }

    /**
     * 校验渠道配置请求。
     *
     * @param targetMeta 场景目标元数据
     * @param channelConfigs 渠道配置请求
     */
    private void validateChannelConfigs(NotifySceneTargetMeta targetMeta, List<NotifyTemplateChannelDTO> channelConfigs) {
        if (channelConfigs.size() > 1) {
            throw new ServiceException("当前场景的小程序渠道配置只允许保存一条");
        }
        for (NotifyTemplateChannelDTO channelDTO : channelConfigs) {
            if (channelDTO == null) {
                throw new ServiceException("渠道配置不能为空");
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
     * 构造渠道配置 JSON。
     *
     * @param dto 渠道配置参数
     * @return 渠道配置 JSON
     */
    private String buildChannelConfigJson(NotifyTemplateChannelDTO dto) {
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setTemplateId(normalizeNullableField(dto.getTemplateId()));
        config.setPagePathTemplate(normalizeNullableField(dto.getPagePathTemplate()));
        config.setFieldMapping(copyFieldMappings(dto.getFieldMapping()));
        return JSONUtil.toJsonStr(config);
    }

    /**
     * 把目标配置实体转换为旧渠道配置 VO。
     *
     * @param targetMeta 目标元数据
     * @param entity 目标配置实体
     * @return 旧渠道配置 VO
     */
    private NotifyTemplateChannelVO toChannelVO(NotifySceneTargetMeta targetMeta, NotifySceneTarget entity) {
        NotifyTemplateChannelVO vo = new NotifyTemplateChannelVO();
        vo.setId(entity.getId());
        vo.setSceneCode(entity.getSceneCode());
        vo.setChannelType(targetMeta.getChannelType());
        vo.setChannelTypeDesc(targetMeta.getChannelTypeDesc());
        vo.setChannelEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        vo.setConfigJson(entity.getConfigJson());

        NotifyTemplateChannelConfig config = parseChannelConfig(entity.getConfigJson());
        if (config != null) {
            vo.setTemplateId(config.getTemplateId());
            vo.setPagePathTemplate(config.getPagePathTemplate());
            vo.setFieldMapping(copyFieldMappings(config.getFieldMapping()));
        }
        return vo;
    }

    /**
     * 查询场景下的小程序目标配置实体。
     *
     * @param targetMeta 目标元数据
     * @param enabledOnly 是否只读取启用目标
     * @return 目标配置实体；不存在时返回 {@code null}
     */
    private NotifySceneTarget getTargetEntity(NotifySceneTargetMeta targetMeta, boolean enabledOnly) {
        LambdaQueryWrapper<NotifySceneTarget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifySceneTarget::getSceneCode, targetMeta == null ? null : getSceneCode(targetMeta))
                .eq(NotifySceneTarget::getTargetType, NotifyTypeEnum.MP_SUBSCRIBE.getCode())
                .last("limit 1");
        if (enabledOnly) {
            wrapper.eq(NotifySceneTarget::getEnabled, 1);
        }
        return notifySceneTargetMapper.selectOne(wrapper);
    }

    /**
     * 统计场景下的小程序目标配置数量。
     *
     * @param targetMeta 目标元数据
     * @return 目标配置数量
     */
    private long countTargetEntities(NotifySceneTargetMeta targetMeta) {
        LambdaQueryWrapper<NotifySceneTarget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifySceneTarget::getSceneCode, getSceneCode(targetMeta))
                .eq(NotifySceneTarget::getTargetType, NotifyTypeEnum.MP_SUBSCRIBE.getCode());
        return notifySceneTargetMapper.selectCount(wrapper);
    }

    /**
     * 获取或创建小程序目标配置实体。
     *
     * @param targetMeta 目标元数据
     * @return 目标配置实体
     */
    private NotifySceneTarget getOrCreateTargetEntity(NotifySceneTargetMeta targetMeta) {
        NotifySceneTarget entity = getTargetEntity(targetMeta, false);
        if (entity != null) {
            return entity;
        }

        NotifySceneMeta sceneMeta = notifySceneRegistry.getRequiredScene(getSceneCode(targetMeta));
        NotifySceneTargetMeta actualTargetMeta = notifySceneRegistry.getRequiredTargetMeta(sceneMeta.getSceneCode(), NotifyTypeEnum.MP_SUBSCRIBE.getCode());
        NotifySceneTarget targetEntity = new NotifySceneTarget();
        targetEntity.setSceneCode(sceneMeta.getSceneCode());
        targetEntity.setTargetType(actualTargetMeta.getTargetType());
        targetEntity.setEnabled(actualTargetMeta.getDefaultEnabled());
        targetEntity.setTitleTemplate(actualTargetMeta.getDefaultTitleTemplate());
        targetEntity.setContentTemplate(actualTargetMeta.getDefaultContentTemplate());
        targetEntity.setRouteType(actualTargetMeta.getDefaultRouteType());
        targetEntity.setRouteValueTemplate(actualTargetMeta.getDefaultRouteValueTemplate());
        targetEntity.setRemark(null);
        if (actualTargetMeta.getDefaultChannelConfig() != null) {
            targetEntity.setConfigJson(JSONUtil.toJsonStr(actualTargetMeta.getDefaultChannelConfig()));
        }
        notifySceneTargetMapper.insert(targetEntity);
        return targetEntity;
    }

    /**
     * 解析目标配置中的小程序专属 JSON。
     *
     * @param configJson 配置 JSON
     * @return 解析后的配置对象
     */
    private NotifyTemplateChannelConfig parseChannelConfig(String configJson) {
        if (StrUtil.isBlank(configJson)) {
            return null;
        }
        return JSONUtil.toBean(configJson, NotifyTemplateChannelConfig.class);
    }

    /**
     * 构造默认停用渠道 DTO。
     *
     * @param targetMeta 目标元数据
     * @return 默认停用 DTO
     */
    private NotifyTemplateChannelDTO buildDisabledChannelDTO(NotifySceneTargetMeta targetMeta) {
        NotifyTemplateChannelDTO dto = new NotifyTemplateChannelDTO();
        dto.setChannelType(targetMeta.getChannelType());
        dto.setChannelEnabled(0);
        return dto;
    }

    /**
     * 查询场景对应的外部通知目标元数据。
     *
     * @param sceneCode 场景编码
     * @return 外部通知目标元数据
     */
    private NotifySceneTargetMeta getExternalTargetMetaOrThrow(String sceneCode) {
        NotifySceneMeta sceneMeta = notifySceneRegistry.getRequiredScene(
                normalizeRequiredField(sceneCode, "通知场景编码不能为空")
        );
        NotifySceneTargetMeta targetMeta = sceneMeta.getTargetMeta(NotifyTypeEnum.MP_SUBSCRIBE.getCode());
        if (targetMeta == null || StrUtil.isBlank(targetMeta.getChannelType())) {
            throw new ServiceException("当前通知场景不支持外部渠道配置");
        }
        return targetMeta;
    }

    /**
     * 通过目标元数据反查所属场景编码。
     *
     * @param targetMeta 目标元数据
     * @return 场景编码
     */
    private String getSceneCode(NotifySceneTargetMeta targetMeta) {
        for (NotifySceneMeta sceneMeta : notifySceneRegistry.listScenes()) {
            NotifySceneTargetMeta matched = sceneMeta.getTargetMeta(targetMeta.getTargetType());
            if (matched == targetMeta) {
                return sceneMeta.getSceneCode();
            }
        }
        throw new ServiceException("无法从通知目标元数据反查场景编码");
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
}
