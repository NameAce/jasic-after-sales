package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.notify.domain.dto.NotifySceneConfigSaveDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyScenePreviewDTO;
import com.jasic.aftersales.system.notify.domain.query.NotifySceneConfigQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneConfigDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneConfigOptionsVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifySceneConfigPageVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyScenePreviewVO;

/**
 * 通知场景目标配置服务。
 *
 * <p>该服务是阶段一后台“通知场景配置”页面的唯一后端入口。
 * 它负责场景与目标配置的查询、保存和预览，不负责事件消费、站内落库和外部分发。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public interface NotifySceneTargetConfigService {

    /**
     * 查询通知场景配置页元数据。
     *
     * @return 场景、目标类型和路由类型元数据
     */
    NotifySceneConfigOptionsVO getOptions();

    /**
     * 分页查询通知场景配置。
     *
     * @param query 查询参数
     * @return 场景配置分页结果
     */
    PageResult<NotifySceneConfigPageVO> listPage(NotifySceneConfigQuery query);

    /**
     * 查询单个场景配置详情。
     *
     * @param sceneCode 场景编码
     * @return 场景配置详情
     */
    NotifySceneConfigDetailVO getDetail(String sceneCode);

    /**
     * 保存整个场景下的全部目标配置。
     *
     * @param sceneCode 场景编码
     * @param dto 场景保存参数
     */
    void saveSceneConfig(String sceneCode, NotifySceneConfigSaveDTO dto);

    /**
     * 预览指定场景目标的渲染结果。
     *
     * @param dto 预览参数
     * @return 预览结果
     */
    NotifyScenePreviewVO preview(NotifyScenePreviewDTO dto);
}
