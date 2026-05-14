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
     * @param targetCompanyId 目标公司ID
     * @return 大区列表
     */
    List<SysRegion> listByTargetCompanyId(Long targetCompanyId);

    /**
     * 根据ID查询大区
     *
     * @param id 主键ID
     * @return 大区实体
     */
    SysRegion getById(Long id, Long targetCompanyId);

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
    void remove(Long id, Long targetCompanyId);

    /**
     * 分配用户大区
     *
     * @param userId    用户ID
     * @param regionIds 大区ID列表
     */
    void assignUserRegions(Long userId, Long targetCompanyId, List<Long> regionIds);

    /**
     * 查询用户在当前公司的大区ID列表。
     *
     * @param userId 用户ID
     * @return 大区ID列表
     */
    List<Long> listUserRegionIdsByTargetCompanyId(Long userId, Long targetCompanyId);

    /**
     * 根据用户ID查询大区ID列表
     *
     * @param userId 用户ID
     * @return 大区ID列表
     */
    List<Long> listRegionIdsByUserId(Long userId);

    /**
     * 根据用户ID和公司ID查询大区ID列表。
     *
     * @param userId    用户ID
     * @param companyId 公司ID
     * @return 大区ID列表
     */
    List<Long> listRegionIdsByUserIdAndCompanyId(Long userId, Long companyId);
}
