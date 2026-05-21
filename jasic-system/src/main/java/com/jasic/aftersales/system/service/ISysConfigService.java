package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.SysConfigDTO;
import com.jasic.aftersales.system.domain.dto.SysConfigGroupSaveDTO;
import com.jasic.aftersales.system.domain.query.SysConfigQuery;
import com.jasic.aftersales.system.domain.vo.SysConfigGroupVO;
import com.jasic.aftersales.system.domain.vo.SysConfigVO;

import java.util.Collections;
import java.util.List;

/**
 * 参数设置 Service 接口。
 *
 * <p>该接口同时服务旧“参数设置”页和后续系统配置分组展示能力。
 * 旧页面继续使用分页列表；分组展示页使用非分页分组接口，避免前端为了展示全部分组而拼接分页数据。</p>
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
     * 查询系统配置分组列表。
     *
     * @param includeLegacy 是否包含 legacy 历史废弃分组；新系统配置页通常传 false
     * @return 固定顺序的分组列表，每个分组包含本组全部配置项
     */
    default List<SysConfigGroupVO> listGroups(Boolean includeLegacy) {
        // 默认实现用于兼容测试替身或非配置模块的轻量实现，真实业务由 SysConfigServiceImpl 覆盖。
        return Collections.emptyList();
    }

    /**
     * 按分组批量保存系统配置。
     *
     * <p>该方法主要服务新的分组配置页保存动作。它只处理同一分组下的整组提交，
     * 不替代旧参数设置页的单条新增、单条修改入口。</p>
     *
     * @param dto 分组保存参数
     */
    default void saveGroup(SysConfigGroupSaveDTO dto) {
        // 默认实现仅用于兼容测试替身；真实业务由 SysConfigServiceImpl 覆盖。
    }

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
