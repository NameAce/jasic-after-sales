package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.FirstSecondRelationDTO;
import com.jasic.aftersales.system.domain.dto.HqFirstContractDTO;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.query.FirstSecondRelationQuery;
import com.jasic.aftersales.system.domain.query.HqFirstContractQuery;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.service.ISysContractService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 签约管理 Service 实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysContractServiceImpl implements ISysContractService {

    @Resource
    private HqFirstContractMapper hqFirstContractMapper;

    @Resource
    private FirstSecondRelationMapper firstSecondRelationMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    /**
     * 总部-一级签约分页列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<HqFirstContract> listHqFirstPage(HqFirstContractQuery query) {
        Page<HqFirstContract> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        if (query.getHqCompanyId() != null) {
            wrapper.eq(HqFirstContract::getHqCompanyId, query.getHqCompanyId());
        }
        if (query.getFirstCompanyId() != null) {
            wrapper.eq(HqFirstContract::getFirstCompanyId, query.getFirstCompanyId());
        }
        if (query.getRegionId() != null) {
            wrapper.eq(HqFirstContract::getRegionId, query.getRegionId());
        }
        wrapper.orderByDesc(HqFirstContract::getId);
        Page<HqFirstContract> result = hqFirstContractMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 新增总部-一级签约
     *
     * @param dto 签约参数
     * @return 主键ID
     */
    @Override
    public Long saveHqFirst(HqFirstContractDTO dto) {
        checkHqFirstCompanyExists(dto.getHqCompanyId());
        checkFirstCompanyExists(dto.getFirstCompanyId());
        checkHqFirstDuplicate(null, dto.getHqCompanyId(), dto.getFirstCompanyId(), dto.getRegionId());
        if (StrUtil.isNotBlank(dto.getContractNo())) {
            checkContractNoDuplicate(null, dto.getContractNo());
        }
        HqFirstContract entity = new HqFirstContract();
        BeanUtil.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        hqFirstContractMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改总部-一级签约
     *
     * @param dto 签约参数
     */
    @Override
    public void updateHqFirst(HqFirstContractDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("签约ID不能为空");
        }
        HqFirstContract entity = hqFirstContractMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("签约记录不存在");
        }
        checkHqFirstDuplicate(dto.getId(), dto.getHqCompanyId(), dto.getFirstCompanyId(), dto.getRegionId());
        if (StrUtil.isNotBlank(dto.getContractNo())) {
            checkContractNoDuplicate(dto.getId(), dto.getContractNo());
        }
        BeanUtil.copyProperties(dto, entity);
        hqFirstContractMapper.updateById(entity);
    }

    /**
     * 删除总部-一级签约
     *
     * @param id 主键ID
     */
    @Override
    public void removeHqFirst(Long id) {
        HqFirstContract entity = hqFirstContractMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("签约记录不存在");
        }
        hqFirstContractMapper.deleteById(id);
    }

    /**
     * 一级-二级从属分页列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<FirstSecondRelation> listFirstSecondPage(FirstSecondRelationQuery query) {
        Page<FirstSecondRelation> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<FirstSecondRelation> wrapper = new LambdaQueryWrapper<>();
        if (query.getFirstCompanyId() != null) {
            wrapper.eq(FirstSecondRelation::getFirstCompanyId, query.getFirstCompanyId());
        }
        if (query.getSecondCompanyId() != null) {
            wrapper.eq(FirstSecondRelation::getSecondCompanyId, query.getSecondCompanyId());
        }
        wrapper.orderByDesc(FirstSecondRelation::getId);
        Page<FirstSecondRelation> result = firstSecondRelationMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 新增一级-二级从属
     *
     * @param dto 从属关系参数
     * @return 主键ID
     */
    @Override
    public Long saveFirstSecond(FirstSecondRelationDTO dto) {
        checkFirstCompanyExists(dto.getFirstCompanyId());
        checkSecondCompanyExists(dto.getSecondCompanyId());
        checkFirstSecondDuplicate(null, dto.getFirstCompanyId(), dto.getSecondCompanyId());
        FirstSecondRelation entity = new FirstSecondRelation();
        BeanUtil.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        firstSecondRelationMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 删除一级-二级从属
     *
     * @param id 主键ID
     */
    @Override
    public void removeFirstSecond(Long id) {
        FirstSecondRelation entity = firstSecondRelationMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("从属关系记录不存在");
        }
        firstSecondRelationMapper.deleteById(id);
    }

    /**
     * 校验总部公司是否存在
     */
    private void checkHqFirstCompanyExists(Long hqCompanyId) {
        if (hqCompanyId == null) {
            throw new ServiceException("总部公司ID不能为空");
        }
        SysCompany company = sysCompanyMapper.selectById(hqCompanyId);
        if (company == null) {
            throw new ServiceException("总部公司不存在");
        }
    }

    /**
     * 校验一级公司是否存在
     */
    private void checkFirstCompanyExists(Long firstCompanyId) {
        if (firstCompanyId == null) {
            throw new ServiceException("一级网点公司ID不能为空");
        }
        SysCompany company = sysCompanyMapper.selectById(firstCompanyId);
        if (company == null) {
            throw new ServiceException("一级网点公司不存在");
        }
    }

    /**
     * 校验二级公司是否存在
     */
    private void checkSecondCompanyExists(Long secondCompanyId) {
        if (secondCompanyId == null) {
            throw new ServiceException("二级网点公司ID不能为空");
        }
        SysCompany company = sysCompanyMapper.selectById(secondCompanyId);
        if (company == null) {
            throw new ServiceException("二级网点公司不存在");
        }
    }

    /**
     * 校验总部-一级签约是否重复（同一总部+一级+大区组合唯一）
     */
    private void checkHqFirstDuplicate(Long excludeId, Long hqCompanyId, Long firstCompanyId, Long regionId) {
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HqFirstContract::getHqCompanyId, hqCompanyId)
                .eq(HqFirstContract::getFirstCompanyId, firstCompanyId)
                .eq(HqFirstContract::getRegionId, regionId);
        if (excludeId != null) {
            wrapper.ne(HqFirstContract::getId, excludeId);
        }
        if (hqFirstContractMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该总部、一级网点、大区组合的签约已存在");
        }
    }

    /**
     * 校验合同编号是否重复
     */
    private void checkContractNoDuplicate(Long excludeId, String contractNo) {
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HqFirstContract::getContractNo, contractNo);
        if (excludeId != null) {
            wrapper.ne(HqFirstContract::getId, excludeId);
        }
        if (hqFirstContractMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("合同编号已存在");
        }
    }

    /**
     * 校验一级-二级从属是否重复（同一一级+二级组合唯一）
     */
    private void checkFirstSecondDuplicate(Long excludeId, Long firstCompanyId, Long secondCompanyId) {
        LambdaQueryWrapper<FirstSecondRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirstSecondRelation::getFirstCompanyId, firstCompanyId)
                .eq(FirstSecondRelation::getSecondCompanyId, secondCompanyId);
        if (excludeId != null) {
            wrapper.ne(FirstSecondRelation::getId, excludeId);
        }
        if (firstSecondRelationMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该一级、二级网点组合的从属关系已存在");
        }
    }
}
