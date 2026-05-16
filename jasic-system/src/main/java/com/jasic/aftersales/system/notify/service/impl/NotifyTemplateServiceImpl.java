package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplatePreviewDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyTemplate;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyRouteTypeEnum;
import com.jasic.aftersales.system.notify.domain.query.NotifyTemplateQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneOptionVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateEnumOptionVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateOptionsVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplatePreviewVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateVO;
import com.jasic.aftersales.system.notify.mapper.SysNotifyTemplateMapper;
import com.jasic.aftersales.system.notify.service.NotifyTemplateAdminService;
import com.jasic.aftersales.system.notify.support.NotifySceneMeta;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
import com.jasic.aftersales.system.notify.support.NotifyTemplateVariableMeta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 通知模板后台管理服务实现。
 *
 * <p>该实现只负责后台“通知模板配置”菜单下的模板 CRUD、启停、变量白名单校验和预览。
 * 模板元数据统一来自 `NotifySceneRegistry`，因此这里不再维护 `bizType`、`triggerScene`、
 * `notifyType`、`receiverType`、`receiverDesc` 和 `variablesJson` 这些历史字段。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Service
public class NotifyTemplateServiceImpl implements NotifyTemplateAdminService {

    private static final int SCENE_CODE_MAX_LENGTH = 64;
    private static final int TEMPLATE_NAME_MAX_LENGTH = 128;
    private static final int TITLE_MAX_LENGTH = 128;
    private static final int CONTENT_MAX_LENGTH = 512;
    private static final int ROUTE_TYPE_MAX_LENGTH = 64;
    private static final int ROUTE_VALUE_MAX_LENGTH = 128;
    private static final int REMARK_MAX_LENGTH = 255;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    @Resource
    private SysNotifyTemplateMapper sysNotifyTemplateMapper;

    @Resource
    private NotifySceneRegistry notifySceneRegistry;

    /**
     * 分页查询通知模板。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<NotifyTemplateVO> listPage(NotifyTemplateQuery query) {
        NotifyTemplateQuery actualQuery = query == null ? new NotifyTemplateQuery() : query;
        Page<SysNotifyTemplate> page = new Page<>(actualQuery.getPageNum(), actualQuery.getPageSize());
        LambdaQueryWrapper<SysNotifyTemplate> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(actualQuery.getSceneCode())) {
            wrapper.eq(SysNotifyTemplate::getSceneCode, actualQuery.getSceneCode().trim());
        }
        if (StrUtil.isNotBlank(actualQuery.getTemplateName())) {
            wrapper.like(SysNotifyTemplate::getTemplateName, actualQuery.getTemplateName().trim());
        }
        if (actualQuery.getStatus() != null) {
            wrapper.eq(SysNotifyTemplate::getStatus, actualQuery.getStatus());
        }
        if (StrUtil.isNotBlank(actualQuery.getNotifyType())) {
            // 通知类型是注册表元数据，不在模板表落库，因此先根据注册表换算成场景编码集合再查询模板表。
            List<String> sceneCodes = notifySceneRegistry.listScenes().stream()
                    .filter(sceneMeta -> StrUtil.equals(sceneMeta.getNotifyType(), actualQuery.getNotifyType().trim()))
                    .map(NotifySceneMeta::getSceneCode)
                    .collect(Collectors.toList());
            if (sceneCodes.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L, actualQuery.getPageNum(), actualQuery.getPageSize());
            }
            wrapper.in(SysNotifyTemplate::getSceneCode, sceneCodes);
        }
        wrapper.orderByDesc(SysNotifyTemplate::getUpdateTime)
                .orderByDesc(SysNotifyTemplate::getId);

        Page<SysNotifyTemplate> result = sysNotifyTemplateMapper.selectPage(page, wrapper);
        List<NotifyTemplateVO> records = result.getRecords().stream()
                .map(this::toTemplateVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), actualQuery.getPageNum(), actualQuery.getPageSize());
    }

    /**
     * 查询通知模板详情。
     *
     * @param id 模板主键
     * @return 模板详情
     */
    @Override
    public NotifyTemplateVO getById(Long id) {
        SysNotifyTemplate entity = sysNotifyTemplateMapper.selectById(id);
        return entity == null ? null : toTemplateVO(entity);
    }

    /**
     * 新增通知模板。
     *
     * @param dto 模板参数
     * @return 新增后的主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(NotifyTemplateDTO dto) {
        NotifySceneMeta sceneMeta = getRequiredSceneFromTemplate(dto);
        Integer targetStatus = dto.getStatus() == null ? 1 : dto.getStatus();

        validateStatus(targetStatus);
        validateSceneCodeUnique(sceneMeta.getSceneCode(), null);

        SysNotifyTemplate entity = buildTemplateEntity(dto, sceneMeta, targetStatus, new SysNotifyTemplate());
        sysNotifyTemplateMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改通知模板。
     *
     * @param dto 模板参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(NotifyTemplateDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new ServiceException("模板ID不能为空");
        }
        SysNotifyTemplate existing = sysNotifyTemplateMapper.selectById(dto.getId());
        if (existing == null) {
            throw new ServiceException("通知模板不存在");
        }

        NotifySceneMeta sceneMeta = getRequiredSceneFromTemplate(dto);
        Integer targetStatus = dto.getStatus() == null ? existing.getStatus() : dto.getStatus();

        validateStatus(targetStatus);
        validateSceneCodeUnique(sceneMeta.getSceneCode(), existing.getId());

        SysNotifyTemplate entity = buildTemplateEntity(dto, sceneMeta, targetStatus, existing);
        entity.setId(existing.getId());
        sysNotifyTemplateMapper.updateById(entity);
    }

    /**
     * 切换模板启停状态。
     *
     * @param id 模板主键
     * @param status 目标状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);

        SysNotifyTemplate existing = sysNotifyTemplateMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("通知模板不存在");
        }
        if (status.equals(existing.getStatus())) {
            return;
        }

        existing.setStatus(status);
        sysNotifyTemplateMapper.updateById(existing);
    }

    /**
     * 查询模板配置页元数据。
     *
     * @return 场景和渠道元数据
     */
    @Override
    public NotifyTemplateOptionsVO getOptions() {
        NotifyTemplateOptionsVO optionsVO = new NotifyTemplateOptionsVO();
        optionsVO.setSceneOptions(notifySceneRegistry.listScenes().stream()
                .map(this::toSceneOptionVO)
                .collect(Collectors.toList()));
        optionsVO.setChannelTypeOptions(toEnumOptions(Arrays.asList(NotifyChannelTypeEnum.values())));
        return optionsVO;
    }

    /**
     * 预览模板内容。
     *
     * @param dto 预览参数
     * @return 预览结果
     */
    @Override
    public NotifyTemplatePreviewVO preview(NotifyTemplatePreviewDTO dto) {
        NotifySceneMeta sceneMeta = getRequiredScene(dto.getSceneCode());

        // 预览和保存复用同一套变量白名单校验，避免页面预览通过但保存失败。
        validatePreviewTemplateContent(dto, sceneMeta);

        NotifyTemplatePreviewVO previewVO = new NotifyTemplatePreviewVO();
        Map<String, Object> variables = dto.getVariables() == null ? Collections.emptyMap() : dto.getVariables();
        previewVO.setTitle(renderText(dto.getTitleTemplate(), variables));
        previewVO.setContent(renderText(dto.getContentTemplate(), variables));
        previewVO.setRouteType(normalizeNullableField(dto.getRouteType()));
        previewVO.setRouteValue(renderText(dto.getRouteValueTemplate(), variables));
        previewVO.setErrors(Collections.emptyList());
        return previewVO;
    }

    /**
     * 构造模板实体。
     *
     * @param dto 模板参数
     * @param sceneMeta 场景元数据
     * @param targetStatus 目标状态
     * @param targetEntity 目标实体
     * @return 模板实体
     */
    private SysNotifyTemplate buildTemplateEntity(NotifyTemplateDTO dto, NotifySceneMeta sceneMeta,
                                                  Integer targetStatus, SysNotifyTemplate targetEntity) {
        validateTemplateContent(dto, sceneMeta);

        targetEntity.setSceneCode(sceneMeta.getSceneCode());
        targetEntity.setTemplateName(normalizeRequiredField(dto.getTemplateName(), "模板名称不能为空"));
        targetEntity.setTitleTemplate(normalizeNullableField(dto.getTitleTemplate()));
        targetEntity.setContentTemplate(normalizeNullableField(dto.getContentTemplate()));
        targetEntity.setRouteType(normalizeNullableField(dto.getRouteType()));
        targetEntity.setRouteValueTemplate(normalizeNullableField(dto.getRouteValueTemplate()));
        targetEntity.setStatus(targetStatus);
        targetEntity.setRemark(normalizeRemark(dto.getRemark()));
        return targetEntity;
    }

    /**
     * 校验模板保存参数。
     *
     * @param dto 模板参数
     * @param sceneMeta 命中的通知场景
     */
    private void validateTemplateContent(NotifyTemplateDTO dto, NotifySceneMeta sceneMeta) {
        validateLength(normalizeRequiredField(sceneMeta.getSceneCode(), "通知场景编码不能为空"),
                SCENE_CODE_MAX_LENGTH, "通知场景编码");
        validateLength(normalizeRequiredField(dto.getTemplateName(), "模板名称不能为空"),
                TEMPLATE_NAME_MAX_LENGTH, "模板名称");
        validateLength(normalizeNullableField(dto.getTitleTemplate()), TITLE_MAX_LENGTH, "标题模板");
        validateLength(normalizeNullableField(dto.getContentTemplate()), CONTENT_MAX_LENGTH, "内容模板");
        validateLength(normalizeNullableField(dto.getRouteType()), ROUTE_TYPE_MAX_LENGTH, "跳转类型");
        validateLength(normalizeNullableField(dto.getRouteValueTemplate()), ROUTE_VALUE_MAX_LENGTH, "跳转值模板");
        validateLength(normalizeRemark(dto.getRemark()), REMARK_MAX_LENGTH, "备注");

        validateRouteType(dto.getRouteType());

        // 模板中的占位符只允许来自当前通知场景注册表声明的变量，避免运行时才暴露非法变量。
        Set<String> allowedVariables = sceneMeta.getVariables().stream()
                .map(NotifyTemplateVariableMeta::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        validatePlaceholders(dto.getTitleTemplate(), allowedVariables);
        validatePlaceholders(dto.getContentTemplate(), allowedVariables);
        validatePlaceholders(dto.getRouteValueTemplate(), allowedVariables);
    }

    /**
     * 校验预览参数中的模板内容。
     *
     * @param dto 预览参数
     * @param sceneMeta 场景元数据
     */
    private void validatePreviewTemplateContent(NotifyTemplatePreviewDTO dto, NotifySceneMeta sceneMeta) {
        validateLength(normalizeNullableField(dto.getTitleTemplate()), TITLE_MAX_LENGTH, "标题模板");
        validateLength(normalizeNullableField(dto.getContentTemplate()), CONTENT_MAX_LENGTH, "内容模板");
        validateLength(normalizeNullableField(dto.getRouteType()), ROUTE_TYPE_MAX_LENGTH, "跳转类型");
        validateLength(normalizeNullableField(dto.getRouteValueTemplate()), ROUTE_VALUE_MAX_LENGTH, "跳转值模板");

        validateRouteType(dto.getRouteType());

        Set<String> allowedVariables = sceneMeta.getVariables().stream()
                .map(NotifyTemplateVariableMeta::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        validatePlaceholders(dto.getTitleTemplate(), allowedVariables);
        validatePlaceholders(dto.getContentTemplate(), allowedVariables);
        validatePlaceholders(dto.getRouteValueTemplate(), allowedVariables);
    }

    /**
     * 转换模板返回对象。
     *
     * @param entity 模板实体
     * @return 模板返回对象
     */
    private NotifyTemplateVO toTemplateVO(SysNotifyTemplate entity) {
        NotifyTemplateVO vo = BeanUtil.copyProperties(entity, NotifyTemplateVO.class);
        NotifySceneMeta sceneMeta = notifySceneRegistry.getScene(entity.getSceneCode());
        if (sceneMeta == null) {
            return vo;
        }

        // 列表和详情页展示需要的只读元数据统一从场景注册表补齐，避免数据库重复维护这些系统字段。
        vo.setSceneName(sceneMeta.getSceneName());
        vo.setNotifyType(sceneMeta.getNotifyType());
        vo.setNotifyTypeDesc(sceneMeta.getNotifyTypeDesc());
        vo.setReceiverType(sceneMeta.getReceiverType());
        vo.setReceiverTypeDesc(sceneMeta.getReceiverTypeDesc());
        vo.setReceiverDesc(sceneMeta.getReceiverDesc());
        vo.setChannelType(sceneMeta.getChannelType());
        vo.setChannelTypeDesc(sceneMeta.getChannelTypeDesc());
        return vo;
    }

    /**
     * 转换通知场景选项。
     *
     * @param sceneMeta 通知场景元数据
     * @return 通知场景选项
     */
    private NotifySceneOptionVO toSceneOptionVO(NotifySceneMeta sceneMeta) {
        NotifySceneOptionVO optionVO = new NotifySceneOptionVO();
        optionVO.setSceneCode(sceneMeta.getSceneCode());
        optionVO.setSceneName(sceneMeta.getSceneName());
        optionVO.setNotifyType(sceneMeta.getNotifyType());
        optionVO.setNotifyTypeDesc(sceneMeta.getNotifyTypeDesc());
        optionVO.setReceiverType(sceneMeta.getReceiverType());
        optionVO.setReceiverTypeDesc(sceneMeta.getReceiverTypeDesc());
        optionVO.setReceiverDesc(sceneMeta.getReceiverDesc());
        optionVO.setChannelType(sceneMeta.getChannelType());
        optionVO.setChannelTypeDesc(sceneMeta.getChannelTypeDesc());
        optionVO.setDefaultTemplateName(sceneMeta.getDefaultTemplateName());
        optionVO.setDefaultTitleTemplate(sceneMeta.getDefaultTitleTemplate());
        optionVO.setDefaultContentTemplate(sceneMeta.getDefaultContentTemplate());
        optionVO.setDefaultRouteType(sceneMeta.getDefaultRouteType());
        optionVO.setDefaultRouteValueTemplate(sceneMeta.getDefaultRouteValueTemplate());
        optionVO.setVariables(sceneMeta.getVariables());
        return optionVO;
    }

    /**
     * 转换渠道类型选项。
     *
     * @param enumValues 渠道类型枚举
     * @return 渠道类型选项
     */
    private List<NotifyTemplateEnumOptionVO> toEnumOptions(List<NotifyChannelTypeEnum> enumValues) {
        List<NotifyTemplateEnumOptionVO> optionVOList = new ArrayList<>();
        for (NotifyChannelTypeEnum enumValue : enumValues) {
            NotifyTemplateEnumOptionVO optionVO = new NotifyTemplateEnumOptionVO();
            optionVO.setCode(enumValue.getCode());
            optionVO.setDesc(enumValue.getDesc());
            optionVOList.add(optionVO);
        }
        return optionVOList;
    }

    /**
     * 校验模板中的通知场景是否存在。
     *
     * @param dto 模板参数
     * @return 命中的通知场景元数据
     */
    private NotifySceneMeta getRequiredSceneFromTemplate(NotifyTemplateDTO dto) {
        if (dto == null) {
            throw new ServiceException("模板参数不能为空");
        }
        return getRequiredScene(dto.getSceneCode());
    }

    /**
     * 按通知场景编码查询场景元数据。
     *
     * @param sceneCode 通知场景编码
     * @return 命中的场景元数据
     */
    private NotifySceneMeta getRequiredScene(String sceneCode) {
        return notifySceneRegistry.getRequiredScene(normalizeRequiredField(sceneCode, "通知场景编码不能为空"));
    }

    /**
     * 校验通知场景唯一。
     *
     * @param sceneCode 通知场景编码
     * @param excludeId 需要排除的主键
     */
    private void validateSceneCodeUnique(String sceneCode, Long excludeId) {
        LambdaQueryWrapper<SysNotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyTemplate::getSceneCode, sceneCode);
        if (excludeId != null) {
            wrapper.ne(SysNotifyTemplate::getId, excludeId);
        }
        if (sysNotifyTemplateMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("同一通知场景只能存在一条模板");
        }
    }

    /**
     * 渲染文本模板。
     *
     * @param template 模板文本
     * @param variables 变量快照
     * @return 渲染后的文本
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
     * 校验占位符是否合法。
     *
     * @param template 模板文本
     * @param allowedVariables 允许的变量名集合
     */
    private void validatePlaceholders(String template, Set<String> allowedVariables) {
        if (StrUtil.isBlank(template)) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!allowedVariables.contains(variableName)) {
                throw new ServiceException("模板中包含未注册变量：" + variableName);
            }
        }
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
}
