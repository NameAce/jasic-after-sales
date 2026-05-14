package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysRegionDTO;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.SysRegion;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.entity.SysUserRegion;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.SysRegionMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserRegionMapper;
import com.jasic.aftersales.system.service.CompanyDataAccessService;
import com.jasic.aftersales.system.service.ISysRegionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 大区管理 Service 实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysRegionServiceImpl implements ISysRegionService {

    @Resource
    private SysRegionMapper sysRegionMapper;

    @Resource
    private SysUserRegionMapper sysUserRegionMapper;

    @Resource
    private SysUserCompanyMapper sysUserCompanyMapper;

    /**
     * ???????
     *
     * @param targetCompanyId ????ID
     * @return ????
     */
    @Resource
    private HqFirstContractMapper hqFirstContractMapper;

    /**
     * 根据公司ID查询大区列表
     *
     * @param companyId 公司ID
     * @return 大区列表
     */
    @Resource
    private CompanyDataAccessService companyDataAccessService;

    @Override
    public List<SysRegion> listByTargetCompanyId(Long targetCompanyId) {
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(targetCompanyId);
        return companyDataAccessService.runWithOwnerHqTarget(companyId, () -> {
            LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysRegion::getCompanyId, companyId)
                    .orderByAsc(SysRegion::getId);
            // ??????????????????????????
            return sysRegionMapper.selectList(wrapper);
        });
    }

    /**
     * 根据ID查询大区
     *
     * @param id 主键ID
     * @return 大区实体
     */
    @Override
    public SysRegion getById(Long id, Long targetCompanyId) {
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(targetCompanyId);
        return companyDataAccessService.runWithOwnerHqTarget(companyId, () -> sysRegionMapper.selectById(id));
    }

    /**
     * 新增大区
     *
     * @param dto 大区参数
     * @return 主键ID
     */
    @Override
    public Long save(SysRegionDTO dto) {
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(dto.getTargetCompanyId());
        return companyDataAccessService.runWithOwnerHqTarget(companyId, () -> {
            SysRegion entity = new SysRegion();
            BeanUtil.copyProperties(dto, entity);
            entity.setCompanyId(companyId);
            // ???????????????????????
            sysRegionMapper.insert(entity);
            return entity.getId();
        });
    }

    /**
     * 修改大区
     *
     * @param dto 大区参数
     */
    @Override
    public void update(SysRegionDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("大区ID不能为空");
        }
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(dto.getTargetCompanyId());
        companyDataAccessService.runWithOwnerHqTarget(companyId, () -> {
            // ??????????????????????????
            SysRegion entity = sysRegionMapper.selectById(dto.getId());
            if (entity == null) {
                throw new ServiceException("大区不存在");
            }
            BeanUtil.copyProperties(dto, entity);
            entity.setCompanyId(companyId);
            // ???????????????????????
            sysRegionMapper.updateById(entity);
        });
    }

    /**
     * 删除大区
     *
     * @param id 主键ID
     */
    @Override
    public void remove(Long id, Long targetCompanyId) {
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(targetCompanyId);
        companyDataAccessService.runWithOwnerHqTarget(companyId, () -> {
            // ??????????????????????????
            SysRegion entity = sysRegionMapper.selectById(id);
            if (entity == null) {
                throw new ServiceException("大区不存在");
            }
            LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HqFirstContract::getRegionId, id);
            if (hqFirstContractMapper.selectCount(wrapper) > 0) {
                throw new ServiceException("该大区已被签约关系引用，不允许删除");
            }
            // ???????????????????????
            sysRegionMapper.deleteById(id);
        });
    }

    /**
     * 分配用户大区
     *
     * @param userId 用户ID
     * @param regionIds 大区ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRegions(Long userId, Long targetCompanyId, List<Long> regionIds) {
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(targetCompanyId);
        // ?????????????????????????????
        validateUserInTargetCompany(userId, companyId);

        List<Long> targetRegionIds = normalizeRegionIds(regionIds);
        validateRegionsBelongToCompany(companyId, targetRegionIds);

        List<Long> currentCompanyRegionIds = listRegionIdsByUserIdAndCompanyId(userId, companyId);
        if (!currentCompanyRegionIds.isEmpty()) {
            LambdaQueryWrapper<SysUserRegion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserRegion::getUserId, userId)
                    .in(SysUserRegion::getRegionId, currentCompanyRegionIds);
            // ???????????????????????
            sysUserRegionMapper.delete(wrapper);
        }

        for (Long regionId : targetRegionIds) {
            SysUserRegion userRegion = new SysUserRegion();
            userRegion.setUserId(userId);
            userRegion.setRegionId(regionId);
            sysUserRegionMapper.insert(userRegion);
        }
    }

    /**
     * ???????
     *
     * @param userId ??ID
     * @param targetCompanyId ????ID
     * @return ????
     */
    @Override
    public List<Long> listUserRegionIdsByTargetCompanyId(Long userId, Long targetCompanyId) {
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(targetCompanyId);
        validateUserInTargetCompany(userId, companyId);
        return listRegionIdsByUserIdAndCompanyId(userId, companyId);
    }

    /**
     * 根据用户ID查询大区ID列表
     *
     * @param userId 用户ID
     * @return 大区ID列表
     */
    @Override
    public List<Long> listRegionIdsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysUserRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRegion::getUserId, userId);
        // ??????????????????????????
        List<SysUserRegion> list = sysUserRegionMapper.selectList(wrapper);
        return list.stream().map(SysUserRegion::getRegionId).collect(Collectors.toList());
    }

    /**
     * 根据用户ID和公司ID查询大区ID列表
     *
     * @param userId 用户ID
     * @param companyId 公司ID
     * @return 大区ID列表
     */
    @Override
    public List<Long> listRegionIdsByUserIdAndCompanyId(Long userId, Long companyId) {
        if (userId == null || companyId == null) {
            return Collections.emptyList();
        }
        List<Long> regionIds = listRegionIdsByUserId(userId);
        if (regionIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getCompanyId, companyId)
                .in(SysRegion::getId, regionIds)
                .orderByAsc(SysRegion::getId);
        // ??????????????????????????
        List<SysRegion> regions = sysRegionMapper.selectList(wrapper);
        return regions.stream().map(SysRegion::getId).collect(Collectors.toList());
    }

    /**
     * ???????
     *
     * @param userId ??ID
     * @param companyId ??ID
     */
    private void validateUserInTargetCompany(Long userId, Long companyId) {
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        LambdaQueryWrapper<SysUserCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserCompany::getUserId, userId)
                .eq(SysUserCompany::getCompanyId, companyId);
        // ??????????????????????????
        if (sysUserCompanyMapper.selectCount(wrapper) == 0) {
            throw new ServiceException("用户未关联当前公司");
        }
    }

    /**
     * ????????
     *
     * @param regionIds region ID??
     * @return ????
     */
    private List<Long> normalizeRegionIds(List<Long> regionIds) {
        if (regionIds == null || regionIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> distinctIds = new LinkedHashSet<>();
        for (Long regionId : regionIds) {
            if (regionId != null) {
                distinctIds.add(regionId);
            }
        }
        return distinctIds.stream().collect(Collectors.toList());
    }

    /**
     * ???????
     *
     * @param companyId ??ID
     * @param regionIds region ID??
     */
    private void validateRegionsBelongToCompany(Long companyId, List<Long> regionIds) {
        if (regionIds == null || regionIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getCompanyId, companyId)
                .in(SysRegion::getId, regionIds);
        // ??????????????????????????
        Long matchedCount = sysRegionMapper.selectCount(wrapper);
        if (matchedCount == null || matchedCount.intValue() != regionIds.size()) {
            throw new ServiceException("存在不属于当前总部的大区");
        }
    }

}
