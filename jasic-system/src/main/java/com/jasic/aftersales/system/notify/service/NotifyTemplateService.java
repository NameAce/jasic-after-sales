package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateChannelDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplatePreviewDTO;
import com.jasic.aftersales.system.notify.domain.query.NotifyTemplateQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplatePreviewVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateVO;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;

import java.util.List;
import java.util.Map;

/**
 * NotifyTemplateService。
 *
 * <p>服务层接口，定义业务能力边界。</p>
 */
public interface NotifyTemplateService {

    /**
     * 分页查询通知模板列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    PageResult<NotifyTemplateVO> listPage(NotifyTemplateQuery query);

    /**
     * 根据ID查询通知模板详情。
     *
     * @param id 参数
     * @return 处理结果
     */
    NotifyTemplateVO getById(Long id);

    /**
     * 新增自定义通知模板。
     *
     * @param dto 参数
     * @return 处理结果
     */
    Long saveCustom(NotifyTemplateDTO dto);

    /**
     * 更新自定义通知模板。
     *
     * @param dto 参数
     */
    void updateCustom(NotifyTemplateDTO dto);

    /**
     * 删除自定义通知模板。
     *
     * @param id 参数
     */
    void removeCustom(Long id);

    /**
     * 预览通知模板内容。
     *
     * @param dto 参数
     * @return 处理结果
     */
    NotifyTemplatePreviewVO preview(NotifyTemplatePreviewDTO dto);

    /**
     * 渲染通知模板。
     *
     * @param templateCode 参数
     * @param variables 参数
     * @return 处理结果
     */
    NotifyTemplateRenderResult render(String templateCode, Map<String, Object> variables);

    /**
     * 判断是否通知Enabled。
     *
     * @param templateCode 参数
     * @return 处理结果
     */
    boolean isNotifyEnabled(String templateCode);

    /**
     * 分页查询渠道Configs列表。
     *
     * @param templateCode 参数
     * @return 处理结果
     */
    List<NotifyTemplateChannelVO> listChannelConfigs(String templateCode);

    /**
     * 新增渠道Configs。
     *
     * @param templateCode 参数
     * @param channelConfigs 参数
     */
    void saveChannelConfigs(String templateCode, List<NotifyTemplateChannelDTO> channelConfigs);

    /**
     * 刷新通知模板缓存。
     */
    void refreshCache();
}




