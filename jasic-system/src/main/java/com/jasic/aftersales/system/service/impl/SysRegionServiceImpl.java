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
     * 总部一级合同Mapper数据访问接口。
     *
     * @return 处理结果
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

    /**
     * 查询listByTargetCompanyId相关业务数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param targetCompanyId 参数
     * @return 处理结果
     */
    @Override
    public List<SysRegion> listByTargetCompanyId(Long targetCompanyId) {
        // 调用resolveOwnerHqTarget方法，复用统一能力并保证业务规则一致。
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(targetCompanyId);
        return companyDataAccessService.runWithOwnerHqTarget(companyId, () -> {
            LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysRegion::getCompanyId, companyId)
                    // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                    .orderByAsc(SysRegion::getId);
            // 说明：执行该步骤以保证业务流程正确。
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
        // 调用resolveOwnerHqTarget方法，复用统一能力并保证业务规则一致。
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
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(dto.getTargetCompanyId());
        return companyDataAccessService.runWithOwnerHqTarget(companyId, () -> {
            // 调用SysRegion方法，复用统一能力并保证业务规则一致。
            SysRegion entity = new SysRegion();
            // 调用copyProperties方法，复用统一能力并保证业务规则一致。
            BeanUtil.copyProperties(dto, entity);
            // 调用setCompanyId方法，复用统一能力并保证业务规则一致。
            entity.setCompanyId(companyId);
            // 说明：执行该步骤以保证业务流程正确。
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
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(dto.getTargetCompanyId());
        companyDataAccessService.runWithOwnerHqTarget(companyId, () -> {
            // 说明：执行该步骤以保证业务流程正确。
            SysRegion entity = sysRegionMapper.selectById(dto.getId());
            if (entity == null) {
                throw new ServiceException("大区不存在");
            }
            // 调用copyProperties方法，复用统一能力并保证业务规则一致。
            BeanUtil.copyProperties(dto, entity);
            // 调用setCompanyId方法，复用统一能力并保证业务规则一致。
            entity.setCompanyId(companyId);
            // 说明：执行该步骤以保证业务流程正确。
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
        // 调用resolveOwnerHqTarget方法，复用统一能力并保证业务规则一致。
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(targetCompanyId);
        companyDataAccessService.runWithOwnerHqTarget(companyId, () -> {
            // 说明：执行该步骤以保证业务流程正确。
            SysRegion entity = sysRegionMapper.selectById(id);
            if (entity == null) {
                throw new ServiceException("大区不存在");
            }
            LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
            // 调用eq方法，复用统一能力并保证业务规则一致。
            wrapper.eq(HqFirstContract::getRegionId, id);
            if (hqFirstContractMapper.selectCount(wrapper) > 0) {
                throw new ServiceException("该大区已被签约关系引用，不允许删除");
            }
            // 说明：执行该步骤以保证业务流程正确。
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
        // 调用resolveOwnerHqTarget方法，复用统一能力并保证业务规则一致。
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(targetCompanyId);
        // 说明：执行该步骤以保证业务流程正确。
        validateUserInTargetCompany(userId, companyId);

        // 调用normalizeRegionIds方法，复用统一能力并保证业务规则一致。
        List<Long> targetRegionIds = normalizeRegionIds(regionIds);
        // 调用validateRegionsBelongToCompany方法，复用统一能力并保证业务规则一致。
        validateRegionsBelongToCompany(companyId, targetRegionIds);

        // 调用listRegionIdsByUserIdAndCompanyId方法，复用统一能力并保证业务规则一致。
        List<Long> currentCompanyRegionIds = listRegionIdsByUserIdAndCompanyId(userId, companyId);
        if (!currentCompanyRegionIds.isEmpty()) {
            LambdaQueryWrapper<SysUserRegion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserRegion::getUserId, userId)
                    // 调用in方法，复用统一能力并保证业务规则一致。
                    .in(SysUserRegion::getRegionId, currentCompanyRegionIds);
            // 说明：执行该步骤以保证业务流程正确。
            sysUserRegionMapper.delete(wrapper);
        }

        for (Long regionId : targetRegionIds) {
            // 调用SysUserRegion方法，复用统一能力并保证业务规则一致。
            SysUserRegion userRegion = new SysUserRegion();
            // 调用setUserId方法，复用统一能力并保证业务规则一致。
            userRegion.setUserId(userId);
            // 调用setRegionId方法，复用统一能力并保证业务规则一致。
            userRegion.setRegionId(regionId);
            // 调用insert方法，复用统一能力并保证业务规则一致。
            sysUserRegionMapper.insert(userRegion);
        }
    }

    /**
     * 分页查询用户地区IdsByTarget公司ID列表。
     *
     * @return 处理结果
     */
    @Override
    public List<Long> listUserRegionIdsByTargetCompanyId(Long userId, Long targetCompanyId) {
        // 调用resolveOwnerHqTarget方法，复用统一能力并保证业务规则一致。
        Long companyId = companyDataAccessService.resolveOwnerHqTarget(targetCompanyId);
        // 调用validateUserInTargetCompany方法，复用统一能力并保证业务规则一致。
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
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysUserRegion::getUserId, userId);
        // 说明：执行该步骤以保证业务流程正确。
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
        // 调用listRegionIdsByUserId方法，复用统一能力并保证业务规则一致。
        List<Long> regionIds = listRegionIdsByUserId(userId);
        if (regionIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getCompanyId, companyId)
                .in(SysRegion::getId, regionIds)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysRegion::getId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysRegion> regions = sysRegionMapper.selectList(wrapper);
        return regions.stream().map(SysRegion::getId).collect(Collectors.toList());
    }

    /**
     * 校验用户InTarget公司。
     */
    private void validateUserInTargetCompany(Long userId, Long companyId) {
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        LambdaQueryWrapper<SysUserCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserCompany::getUserId, userId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysUserCompany::getCompanyId, companyId);
        // 说明：执行该步骤以保证业务流程正确。
        if (sysUserCompanyMapper.selectCount(wrapper) == 0) {
            throw new ServiceException("用户未关联当前公司");
        }
    }

    /**
     * 规范化地区Ids。
     *
     * @return 处理结果
     */
    private List<Long> normalizeRegionIds(List<Long> regionIds) {
        if (regionIds == null || regionIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> distinctIds = new LinkedHashSet<>();
        for (Long regionId : regionIds) {
            if (regionId != null) {
                // 调用add方法，复用统一能力并保证业务规则一致。
                distinctIds.add(regionId);
            }
        }
        return distinctIds.stream().collect(Collectors.toList());
    }

    /**
     * 校验RegionsBelongTo公司。
     */
    private void validateRegionsBelongToCompany(Long companyId, List<Long> regionIds) {
        if (regionIds == null || regionIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getCompanyId, companyId)
                // 调用in方法，复用统一能力并保证业务规则一致。
                .in(SysRegion::getId, regionIds);
        // 说明：执行该步骤以保证业务流程正确。
        Long matchedCount = sysRegionMapper.selectCount(wrapper);
        if (matchedCount == null || matchedCount.intValue() != regionIds.size()) {
            throw new ServiceException("存在不属于当前总部的大区");
        }
    }

}


