package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.SysDictDataDTO;
import com.jasic.aftersales.system.domain.query.SysDictDataQuery;
import com.jasic.aftersales.system.domain.vo.SysDictDataVO;

import java.util.List;

/**
 * 字典数据 Service 接口
 *
 * @author Codex
 * @date 2026/03/19
 */
public interface ISysDictDataService {

    /**
     * 分页查询字典数据
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<SysDictDataVO> listPage(SysDictDataQuery query);

    /**
     * 查询字典数据详情
     *
     * @param id 主键
     * @return 字典数据详情
     */
    SysDictDataVO getById(Long id);

    /**
     * 根据字典类型查询启用数据
     *
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    List<SysDictDataVO> listByType(String dictType);

    /**
     * 新增字典数据
     *
     * @param dto 入参
     * @return 主键
     */
    Long save(SysDictDataDTO dto);

    /**
     * 修改字典数据
     *
     * @param dto 入参
     */
    void update(SysDictDataDTO dto);

    /**
     * 删除字典数据
     *
     * @param id 主键
     */
    void remove(Long id);

    /**
     * 刷新全部缓存
     */
    void refreshCache();

    /**
     * 刷新指定字典类型缓存
     *
     * @param dictType 字典类型
     */
    void refreshCache(String dictType);

    /**
     * 清理指定字典类型缓存
     *
     * @param dictType 字典类型
     */
    void removeCache(String dictType);
}
