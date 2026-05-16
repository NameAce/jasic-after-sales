package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateChannelDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyTemplateChannel;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.mapper.SysNotifyTemplateChannelMapper;
import com.jasic.aftersales.system.notify.service.NotifyChannelConfigService;
import com.jasic.aftersales.system.notify.support.NotifySceneMeta;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
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
 * <p>该实现负责 `sys_notify_template_channel` 的后台维护和运行时读取。
 * 渠道配置入口统一收口为 `sceneCode`，是否允许配置外部渠道以及允许的渠道类型
 * 全部由 `NotifySceneRegistry` 控制。
 * 它不负责模板主数据、接收人规则和真实发送执行。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Service
public class NotifyChannelConfigServiceImpl implements NotifyChannelConfigService {

    private static final int REMARK_MAX_LENGTH = 255;

    @Resource
    private SysNotifyTemplateChannelMapper sysNotifyTemplateChannelMapper;

    @Resource
    private NotifySceneRegistry notifySceneRegistry;

    /**
     * 查询后台维护页的渠道配置。
     *
     * @param sceneCode 通知场景编码
     * @return 渠道配置列表
     */
    @Override
    public List<NotifyTemplateChannelVO> listChannelConfigs(String sceneCode) {
        NotifySceneMeta sceneMeta = getExternalChannelSceneOrThrow(sceneCode);
        return listChannelConfigsBySceneCode(sceneMeta.getSceneCode(), sceneMeta.getChannelType(), false);
    }

    /**
     * 按通知场景查询运行时可发送渠道配置。
     *
     * @param sceneCode 通知场景编码
     * @return 启用中的渠道配置列表
     */
    @Override
    public List<NotifyTemplateChannelVO> listRuntimeChannelConfigs(String sceneCode) {
        NotifySceneMeta sceneMeta = getExternalChannelSceneOrThrow(sceneCode);
        return listChannelConfigsBySceneCode(sceneMeta.getSceneCode(), sceneMeta.getChannelType(), true);
    }

    /**
     * 判断通知场景下是否存在渠道配置记录。
     *
     * @param sceneCode 通知场景编码
     * @return `true` 表示存在任意渠道配置记录
     */
    @Override
    public boolean hasRuntimeChannelConfigs(String sceneCode) {
        NotifySceneMeta sceneMeta = getExternalChannelSceneOrThrow(sceneCode);
        return countChannelConfigsBySceneCode(sceneMeta.getSceneCode(), sceneMeta.getChannelType()) > 0;
    }

    /**
     * 保存通知场景渠道配置。
     *
     * @param sceneCode 通知场景编码
     * @param channelConfigs 渠道配置列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChannelConfigs(String sceneCode, List<NotifyTemplateChannelDTO> channelConfigs) {
        NotifySceneMeta sceneMeta = getExternalChannelSceneOrThrow(sceneCode);
        List<NotifyTemplateChannelDTO> actualConfigs = channelConfigs == null ? Collections.emptyList() : channelConfigs;
        validateChannelConfigs(sceneMeta, actualConfigs);

        // 当前接口按整场景覆盖保存，先删除旧记录再插入新快照，避免局部更新残留历史配置。
        LambdaQueryWrapper<SysNotifyTemplateChannel> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysNotifyTemplateChannel::getSceneCode, sceneMeta.getSceneCode());
        sysNotifyTemplateChannelMapper.delete(deleteWrapper);

        for (NotifyTemplateChannelDTO channelDTO : actualConfigs) {
            SysNotifyTemplateChannel entity = buildChannelEntity(sceneMeta.getSceneCode(), channelDTO);
            sysNotifyTemplateChannelMapper.insert(entity);
        }
    }

    /**
     * 构造渠道实体。
     *
     * @param sceneCode 通知场景编码
     * @param channelDTO 渠道配置参数
     * @return 渠道实体
     */
    private SysNotifyTemplateChannel buildChannelEntity(String sceneCode, NotifyTemplateChannelDTO channelDTO) {
        SysNotifyTemplateChannel entity = new SysNotifyTemplateChannel();
        entity.setSceneCode(sceneCode);
        entity.setChannelType(normalizeRequiredField(channelDTO.getChannelType(), "渠道类型不能为空"));
        entity.setChannelEnabled(channelDTO.getChannelEnabled());
        entity.setConfigJson(buildChannelConfigJson(channelDTO));
        entity.setRemark(normalizeRemark(channelDTO.getRemark()));
        return entity;
    }

    /**
     * 校验渠道配置列表。
     *
     * @param sceneMeta 通知场景元数据
     * @param channelConfigs 渠道配置列表
     */
    private void validateChannelConfigs(NotifySceneMeta sceneMeta, List<NotifyTemplateChannelDTO> channelConfigs) {
        Set<String> uniqueChannelTypes = new LinkedHashSet<>();
        for (NotifyTemplateChannelDTO channelDTO : channelConfigs) {
            if (channelDTO == null) {
                throw new ServiceException("渠道配置不能为空");
            }

            validateStatus(channelDTO.getChannelEnabled());
            validateLength(normalizeRemark(channelDTO.getRemark()), REMARK_MAX_LENGTH, "备注");

            String channelTypeCode = normalizeRequiredField(channelDTO.getChannelType(), "渠道类型不能为空");
            NotifyChannelTypeEnum channelType = NotifyChannelTypeEnum.getByCode(channelTypeCode);
            if (channelType == null) {
                throw new ServiceException("不支持的渠道类型：" + channelTypeCode);
            }
            if (!StrUtil.equals(channelTypeCode, sceneMeta.getChannelType())) {
                throw new ServiceException("当前通知场景只允许维护渠道类型：" + sceneMeta.getChannelType());
            }
            if (!uniqueChannelTypes.add(channelTypeCode)) {
                throw new ServiceException("同一通知场景下渠道类型不能重复");
            }

            // 当前阶段允许先停用后补全参数，但只要渠道启用，就必须满足最小可发送配置。
            if (Objects.equals(channelDTO.getChannelEnabled(), 1)
                    && NotifyChannelTypeEnum.MP_SUBSCRIBE == channelType) {
                validateMiniProgramConfig(channelDTO);
            }
        }
    }

    /**
     * 校验小程序订阅消息配置。
     *
     * @param channelDTO 渠道配置参数
     */
    private void validateMiniProgramConfig(NotifyTemplateChannelDTO channelDTO) {
        if (normalizeNullableField(channelDTO.getTemplateId()) == null) {
            throw new ServiceException("小程序订阅消息模板ID不能为空");
        }
        if (normalizeNullableField(channelDTO.getPagePathTemplate()) == null) {
            throw new ServiceException("页面路径模板不能为空");
        }
        List<NotifyChannelFieldMappingDTO> fieldMappings = copyFieldMappings(channelDTO.getFieldMapping());
        if (fieldMappings.isEmpty()) {
            throw new ServiceException("字段映射不能为空");
        }
        for (NotifyChannelFieldMappingDTO fieldMapping : fieldMappings) {
            if (fieldMapping == null
                    || normalizeNullableField(fieldMapping.getField()) == null
                    || normalizeNullableField(fieldMapping.getValue()) == null) {
                throw new ServiceException("字段映射中的字段和值不能为空");
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
        String channelTypeCode = normalizeRequiredField(dto.getChannelType(), "渠道类型不能为空");
        if (NotifyChannelTypeEnum.MP_SUBSCRIBE != NotifyChannelTypeEnum.getByCode(channelTypeCode)) {
            return StrUtil.blankToDefault(normalizeNullableField(dto.getConfigJson()), "{}");
        }

        // 小程序参数统一落在 config_json，避免把外部渠道字段再塞回模板主表。
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("templateId", normalizeNullableField(dto.getTemplateId()));
        config.put("pagePathTemplate", normalizeNullableField(dto.getPagePathTemplate()));
        config.put("fieldMapping", copyFieldMappings(dto.getFieldMapping()));
        return JSONUtil.toJsonStr(config);
    }

    /**
     * 复制字段映射列表。
     *
     * @param fieldMappings 原字段映射
     * @return 复制后的字段映射
     */
    private List<NotifyChannelFieldMappingDTO> copyFieldMappings(List<NotifyChannelFieldMappingDTO> fieldMappings) {
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            return Collections.emptyList();
        }
        List<NotifyChannelFieldMappingDTO> copies = new ArrayList<>();
        for (NotifyChannelFieldMappingDTO fieldMapping : fieldMappings) {
            if (fieldMapping == null) {
                continue;
            }
            NotifyChannelFieldMappingDTO copy = new NotifyChannelFieldMappingDTO();
            copy.setField(normalizeNullableField(fieldMapping.getField()));
            copy.setValue(normalizeNullableField(fieldMapping.getValue()));
            copies.add(copy);
        }
        return copies;
    }

    /**
     * 查询场景下的渠道配置。
     *
     * @param sceneCode 通知场景编码
     * @param enabledOnly 是否只读取启用渠道
     * @return 渠道配置列表
     */
    private List<NotifyTemplateChannelVO> listChannelConfigsBySceneCode(String sceneCode, String channelType,
                                                                        boolean enabledOnly) {
        LambdaQueryWrapper<SysNotifyTemplateChannel> wrapper = new LambdaQueryWrapper<>();
        // 运行时和后台维护都按 sceneCode + channelType 定位配置，避免后续新增外部渠道时误读同场景其它渠道。
        wrapper.eq(SysNotifyTemplateChannel::getSceneCode, sceneCode)
                .eq(SysNotifyTemplateChannel::getChannelType, channelType);
        if (enabledOnly) {
            wrapper.eq(SysNotifyTemplateChannel::getChannelEnabled, 1);
        }
        wrapper.orderByAsc(SysNotifyTemplateChannel::getId);
        return sysNotifyTemplateChannelMapper.selectList(wrapper).stream()
                .map(this::toChannelVO)
                .collect(Collectors.toList());
    }

    /**
     * 统计场景下的渠道配置数量。
     *
     * @param sceneCode 通知场景编码
     * @return 渠道配置数量
     */
    private long countChannelConfigsBySceneCode(String sceneCode, String channelType) {
        LambdaQueryWrapper<SysNotifyTemplateChannel> wrapper = new LambdaQueryWrapper<>();
        // 该统计用于区分“未配置渠道”和“渠道存在但停用”，因此必须与发送查询使用同一组定位条件。
        wrapper.eq(SysNotifyTemplateChannel::getSceneCode, sceneCode)
                .eq(SysNotifyTemplateChannel::getChannelType, channelType);
        return sysNotifyTemplateChannelMapper.selectCount(wrapper);
    }

    /**
     * 转换渠道配置返回对象。
     *
     * @param entity 渠道配置实体
     * @return 渠道配置返回对象
     */
    private NotifyTemplateChannelVO toChannelVO(SysNotifyTemplateChannel entity) {
        NotifyTemplateChannelVO vo = BeanUtil.copyProperties(entity, NotifyTemplateChannelVO.class);
        vo.setChannelTypeDesc(resolveChannelTypeDesc(entity.getChannelType()));

        if (StrUtil.isBlank(entity.getConfigJson())) {
            return vo;
        }

        // 统一在这里把 config_json 反序列化成结构化字段，避免后台和发送侧重复解析 JSON。
        NotifyTemplateChannelConfig config = JSONUtil.toBean(entity.getConfigJson(), NotifyTemplateChannelConfig.class);
        if (config != null) {
            vo.setTemplateId(config.getTemplateId());
            vo.setPagePathTemplate(config.getPagePathTemplate());
            vo.setFieldMapping(config.getFieldMapping());
        }
        return vo;
    }

    /**
     * 校验通知场景是否允许配置外部渠道。
     *
     * @param sceneCode 通知场景编码
     * @return 场景元数据
     */
    private NotifySceneMeta getExternalChannelSceneOrThrow(String sceneCode) {
        NotifySceneMeta sceneMeta = notifySceneRegistry.getRequiredScene(
                normalizeRequiredField(sceneCode, "通知场景编码不能为空")
        );
        if (StrUtil.isBlank(sceneMeta.getChannelType())) {
            throw new ServiceException("当前通知场景不支持外部渠道配置");
        }
        return sceneMeta;
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
     * @param fieldName 字段名
     */
    private void validateLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new ServiceException(fieldName + "长度不能超过" + maxLength);
        }
    }

    /**
     * 规范化必填字符串。
     *
     * @param value 原值
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
     * @param value 原值
     * @return 去掉首尾空白后的值；为空白时返回 {@code null}
     */
    private String normalizeNullableField(String value) {
        return StrUtil.trimToNull(value);
    }

    /**
     * 规范化备注。
     *
     * @param remark 原备注
     * @return 规范化后的备注
     */
    private String normalizeRemark(String remark) {
        return StrUtil.trimToNull(remark);
    }

    /**
     * 解析渠道类型说明。
     *
     * @param channelType 渠道类型编码
     * @return 渠道类型说明；未命中时返回 {@code null}
     */
    private String resolveChannelTypeDesc(String channelType) {
        NotifyChannelTypeEnum channelTypeEnum = NotifyChannelTypeEnum.getByCode(channelType);
        return channelTypeEnum == null ? null : channelTypeEnum.getDesc();
    }
}
