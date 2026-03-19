package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysRegionDTO;
import com.jasic.aftersales.system.domain.entity.SysRegion;
import com.jasic.aftersales.system.domain.entity.SysUserRegion;
import com.jasic.aftersales.system.mapper.SysRegionMapper;
import com.jasic.aftersales.system.mapper.SysUserRegionMapper;
import com.jasic.aftersales.system.service.ISysRegionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
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

    /**
     * 根据公司ID查询大区列表
     *
     * @param companyId 公司ID
     * @return 大区列表
     */
    @Override
    public List<SysRegion> listByCompanyId(Long companyId) {
        if (companyId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getCompanyId, companyId)
                .orderByAsc(SysRegion::getId);
        return sysRegionMapper.selectList(wrapper);
    }

    /**
     * 根据ID查询大区
     *
     * @param id 主键ID
     * @return 大区实体
     */
    @Override
    public SysRegion getById(Long id) {
        return sysRegionMapper.selectById(id);
    }

    /**
     * 新增大区
     *
     * @param dto 大区参数
     * @return 主键ID
     */
    @Override
    public Long save(SysRegionDTO dto) {
        if (dto.getCompanyId() == null) {
            throw new ServiceException("公司ID不能为空");
        }
        SysRegion entity = new SysRegion();
        BeanUtil.copyProperties(dto, entity);
        sysRegionMapper.insert(entity);
        return entity.getId();
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
        SysRegion entity = sysRegionMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("大区不存在");
        }
        BeanUtil.copyProperties(dto, entity);
        sysRegionMapper.updateById(entity);
    }

    /**
     * 删除大区
     *
     * @param id 主键ID
     */
    @Override
    public void remove(Long id) {
        SysRegion entity = sysRegionMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("大区不存在");
        }
        sysRegionMapper.deleteById(id);
    }

    /**
     * 分配用户大区
     *
     * @param userId    用户ID
     * @param regionIds 大区ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRegions(Long userId, List<Long> regionIds) {
        LambdaQueryWrapper<SysUserRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRegion::getUserId, userId);
        sysUserRegionMapper.delete(wrapper);
        if (regionIds != null && !regionIds.isEmpty()) {
            for (Long regionId : regionIds) {
                SysUserRegion ur = new SysUserRegion();
                ur.setUserId(userId);
                ur.setRegionId(regionId);
                sysUserRegionMapper.insert(ur);
            }
        }
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
        List<SysUserRegion> list = sysUserRegionMapper.selectList(wrapper);
        return list.stream().map(SysUserRegion::getRegionId).collect(Collectors.toList());
    }
}
