package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.dto.SysRegionDTO;
import com.jasic.aftersales.system.domain.entity.SysRegion;

import java.util.List;

/**
 * 大区管理 Service 接口
 *
 * @author Zoro
 * @date 2026/03/18
 */
public interface ISysRegionService {

    /**
     * 根据公司ID查询大区列表
     *
     * @param companyId 公司ID
     * @return 大区列表
     */
    List<SysRegion> listByCompanyId(Long companyId);

    /**
     * 根据ID查询大区
     *
     * @param id 主键ID
     * @return 大区实体
     */
    SysRegion getById(Long id);

    /**
     * 新增大区
     *
     * @param dto 大区参数
     * @return 主键ID
     */
    Long save(SysRegionDTO dto);

    /**
     * 修改大区
     *
     * @param dto 大区参数
     */
    void update(SysRegionDTO dto);

    /**
     * 删除大区
     *
     * @param id 主键ID
     */
    void remove(Long id);

    /**
     * 分配用户大区
     *
     * @param userId    用户ID
     * @param regionIds 大区ID列表
     */
    void assignUserRegions(Long userId, List<Long> regionIds);

    /**
     * 根据用户ID查询大区ID列表
     *
     * @param userId 用户ID
     * @return 大区ID列表
     */
    List<Long> listRegionIdsByUserId(Long userId);
}
