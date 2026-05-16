package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplatePreviewDTO;
import com.jasic.aftersales.system.notify.domain.query.NotifyTemplateQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateOptionsVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplatePreviewVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateVO;

/**
 * 通知模板后台管理服务。
 *
 * <p>该接口聚焦后台模板配置页所需的维护能力，包括列表、详情、新增、编辑、启停、元数据和预览。
 * 模板身份统一收口为 `sceneCode`，运行时场景元数据全部由 `NotifySceneRegistry` 提供。
 * 它不负责事件消费时的模板命中、渠道发送和消息落库。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public interface NotifyTemplateAdminService {

    /**
     * 分页查询通知模板。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<NotifyTemplateVO> listPage(NotifyTemplateQuery query);

    /**
     * 查询通知模板详情。
     *
     * @param id 模板主键
     * @return 模板详情；不存在时返回 {@code null}
     */
    NotifyTemplateVO getById(Long id);

    /**
     * 新增通知模板。
     *
     * @param dto 模板参数
     * @return 新增后的主键
     */
    Long createTemplate(NotifyTemplateDTO dto);

    /**
     * 修改通知模板。
     *
     * @param dto 模板参数
     */
    void updateTemplate(NotifyTemplateDTO dto);

    /**
     * 切换模板启停状态。
     *
     * @param id 模板主键
     * @param status 目标状态：1 启用，0 停用
     */
    void updateStatus(Long id, Integer status);

    /**
     * 查询模板配置页元数据。
     *
     * @return 场景和渠道元数据
     */
    NotifyTemplateOptionsVO getOptions();

    /**
     * 预览模板内容。
     *
     * @param dto 预览参数
     * @return 预览结果
     */
    NotifyTemplatePreviewVO preview(NotifyTemplatePreviewDTO dto);
}
