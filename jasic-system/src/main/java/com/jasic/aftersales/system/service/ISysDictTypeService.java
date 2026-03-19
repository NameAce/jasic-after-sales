package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.SysDictTypeDTO;
import com.jasic.aftersales.system.domain.query.SysDictTypeQuery;
import com.jasic.aftersales.system.domain.vo.SysDictTypeVO;

/**
 * 字典类型 Service 接口
 *
 * @author Codex
 * @date 2026/03/19
 */
public interface ISysDictTypeService {

    /**
     * 分页查询字典类型
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<SysDictTypeVO> listPage(SysDictTypeQuery query);

    /**
     * 查询字典类型详情
     *
     * @param id 主键
     * @return 字典类型详情
     */
    SysDictTypeVO getById(Long id);

    /**
     * 新增字典类型
     *
     * @param dto 入参
     * @return 主键
     */
    Long save(SysDictTypeDTO dto);

    /**
     * 修改字典类型
     *
     * @param dto 入参
     */
    void update(SysDictTypeDTO dto);

    /**
     * 删除字典类型
     *
     * @param id 主键
     */
    void remove(Long id);

    /**
     * 刷新字典缓存
     */
    void refreshCache();
}
