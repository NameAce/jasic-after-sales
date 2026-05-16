package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateChannelDTO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;

import java.util.List;

/**
 * 通知渠道配置服务。
 *
 * <p>该接口聚焦 `sys_notify_template_channel` 的后台维护和运行时读取。
 * 重构后渠道配置改为按 `sceneCode` 维护，是否允许配置、允许什么渠道类型
 * 全部由 `NotifySceneRegistry` 控制。
 * 它不负责模板内容渲染、接收人解析、消息落库和真实渠道发送。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public interface NotifyChannelConfigService {

    /**
     * 查询后台维护页使用的渠道配置。
     *
     * @param sceneCode 通知场景编码
     * @return 渠道配置列表
     */
    List<NotifyTemplateChannelVO> listChannelConfigs(String sceneCode);

    /**
     * 按通知场景查询运行时可发送渠道配置。
     *
     * <p>当前方法只返回启用渠道。
     * 模板启停校验由模板渲染服务负责，handler 应先确认模板可用，再读取渠道配置。</p>
     *
     * @param sceneCode 通知场景编码
     * @return 启用中的渠道配置列表
     */
    List<NotifyTemplateChannelVO> listRuntimeChannelConfigs(String sceneCode);

    /**
     * 判断某个通知场景是否存在渠道配置记录。
     *
     * <p>该方法用于区分“没有配置渠道”与“配置了渠道但全部停用”。</p>
     *
     * @param sceneCode 通知场景编码
     * @return `true` 表示存在任意渠道配置记录
     */
    boolean hasRuntimeChannelConfigs(String sceneCode);

    /**
     * 保存通知场景渠道配置。
     *
     * <p>当前阶段仍采用“按场景全量覆盖”的保存方式，
     * 避免在唯一键和页面交互尚未完全稳定前引入额外并发状态。</p>
     *
     * @param sceneCode 通知场景编码
     * @param channelConfigs 渠道配置列表
     */
    void saveChannelConfigs(String sceneCode, List<NotifyTemplateChannelDTO> channelConfigs);
}
