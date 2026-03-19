package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.dto.SysRoleTemplateDTO;
import com.jasic.aftersales.system.domain.vo.SysRoleTemplateVO;

import java.util.List;

/**
 * 角色模板管理 Service 接口
 *
 * @author Zoro
 * @date 2026/03/18
 */
public interface ISysRoleTemplateService {

    /**
     * 根据公司类型编码查询角色模板列表
     *
     * @param typeCode 公司类型编码
     * @return 模板列表
     */
    List<SysRoleTemplateVO> listByTypeCode(String typeCode);

    /**
     * 查询模板详情（含菜单ID列表）
     *
     * @param templateId 模板ID
     * @return 模板详情
     */
    SysRoleTemplateVO getById(Long templateId);

    /**
     * 新增角色模板
     *
     * @param dto 模板参数
     * @return 模板ID
     */
    Long save(SysRoleTemplateDTO dto);

    /**
     * 修改角色模板
     *
     * @param dto 模板参数
     */
    void update(SysRoleTemplateDTO dto);

    /**
     * 删除角色模板
     *
     * @param templateId 模板ID
     */
    void remove(Long templateId);

    /**
     * 同步模板到已有公司（取交集策略）
     *
     * @param templateId 模板ID
     */
    void syncToCompanies(Long templateId);

    /**
     * 根据公司类型编码初始化公司角色（创建公司时调用）
     *
     * @param companyId 公司ID
     * @param typeCode  公司类型编码
     * @return 管理员角色ID（is_admin=1 的模板生成的角色），无则返回 null
     */
    Long initCompanyRoles(Long companyId, String typeCode);
}
