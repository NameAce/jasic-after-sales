package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.entity.SysCompanyType;

import java.util.List;

/**
 * 公司类型管理 Service 接口
 *
 * @author Zoro
 * @date 2026/03/18
 */
public interface ISysCompanyTypeService {

    /**
     * 查询所有公司类型（按排序号升序）
     *
     * @return 公司类型列表
     */
    List<SysCompanyType> listAll();

    /**
     * 根据ID查询公司类型
     *
     * @param id 主键ID
     * @return 公司类型
     */
    SysCompanyType getById(Long id);

    /**
     * 新增公司类型
     *
     * @param entity 公司类型实体
     * @return 主键ID
     */
    Long save(SysCompanyType entity);

    /**
     * 修改公司类型
     *
     * @param entity 公司类型实体
     */
    void update(SysCompanyType entity);

    /**
     * 删除公司类型
     *
     * @param id 主键ID
     */
    void remove(Long id);
}
