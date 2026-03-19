package com.jasic.aftersales.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysCompanyTypeMapper;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 公司类型管理 Service 实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysCompanyTypeServiceImpl implements ISysCompanyTypeService {

    @Resource
    private SysCompanyTypeMapper sysCompanyTypeMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    /**
     * 查询所有公司类型（按排序号升序）
     *
     * @return 公司类型列表
     */
    @Override
    public List<SysCompanyType> listAll() {
        LambdaQueryWrapper<SysCompanyType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysCompanyType::getOrderNum);
        return sysCompanyTypeMapper.selectList(wrapper);
    }

    /**
     * 根据ID查询公司类型
     *
     * @param id 主键ID
     * @return 公司类型
     */
    @Override
    public SysCompanyType getById(Long id) {
        return sysCompanyTypeMapper.selectById(id);
    }

    /**
     * 新增公司类型
     *
     * @param entity 公司类型实体
     * @return 主键ID
     */
    @Override
    public Long save(SysCompanyType entity) {
        LambdaQueryWrapper<SysCompanyType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCompanyType::getTypeCode, entity.getTypeCode());
        if (sysCompanyTypeMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("类型编码已存在");
        }
        sysCompanyTypeMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改公司类型
     *
     * @param entity 公司类型实体
     */
    @Override
    public void update(SysCompanyType entity) {
        sysCompanyTypeMapper.updateById(entity);
    }

    /**
     * 删除公司类型
     *
     * @param id 主键ID
     */
    @Override
    public void remove(Long id) {
        SysCompanyType companyType = sysCompanyTypeMapper.selectById(id);
        if (companyType == null) {
            return;
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCompany::getTypeCode, companyType.getTypeCode());
        if (sysCompanyMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该类型下存在公司，不允许删除");
        }
        sysCompanyTypeMapper.deleteById(id);
    }
}
