package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.SysCompanyDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.query.SysCompanyQuery;

/**
 * 公司管理 Service 接口
 *
 * @author Zoro
 * @date 2026/03/18
 */
public interface ISysCompanyService {

    /**
     * 分页查询公司列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<SysCompany> listPage(SysCompanyQuery query);

    /**
     * 根据ID查询公司
     *
     * @param id 主键ID
     * @return 公司实体
     */
    SysCompany getById(Long id);

    /**
     * 新增公司（自动初始化角色）
     *
     * @param dto 公司参数
     * @return 主键ID
     */
    Long save(SysCompanyDTO dto);

    /**
     * 修改公司
     *
     * @param dto 公司参数
     */
    void update(SysCompanyDTO dto);

    /**
     * 删除公司
     *
     * @param id 主键ID
     */
    void remove(Long id);
}
