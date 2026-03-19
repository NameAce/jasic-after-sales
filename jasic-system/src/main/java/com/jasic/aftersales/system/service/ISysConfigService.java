package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.SysConfigDTO;
import com.jasic.aftersales.system.domain.query.SysConfigQuery;
import com.jasic.aftersales.system.domain.vo.SysConfigVO;

/**
 * 参数设置 Service 接口
 *
 * @author Codex
 * @date 2026/03/19
 */
public interface ISysConfigService {

    /**
     * 分页查询参数设置
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<SysConfigVO> listPage(SysConfigQuery query);

    /**
     * 查询参数详情
     *
     * @param id 主键
     * @return 参数详情
     */
    SysConfigVO getById(Long id);

    /**
     * 根据键名查询参数值
     *
     * @param configKey 参数键名
     * @return 参数值
     */
    String getValueByKey(String configKey);

    /**
     * 新增参数
     *
     * @param dto 入参
     * @return 主键
     */
    Long save(SysConfigDTO dto);

    /**
     * 修改参数
     *
     * @param dto 入参
     */
    void update(SysConfigDTO dto);

    /**
     * 删除参数
     *
     * @param id 主键
     */
    void remove(Long id);

    /**
     * 刷新参数缓存
     */
    void refreshCache();
}
