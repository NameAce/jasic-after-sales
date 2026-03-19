package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysDictTypeDTO;
import com.jasic.aftersales.system.domain.entity.SysDictData;
import com.jasic.aftersales.system.domain.entity.SysDictType;
import com.jasic.aftersales.system.domain.query.SysDictTypeQuery;
import com.jasic.aftersales.system.domain.vo.SysDictTypeVO;
import com.jasic.aftersales.system.mapper.SysDictDataMapper;
import com.jasic.aftersales.system.mapper.SysDictTypeMapper;
import com.jasic.aftersales.system.service.ISysDictDataService;
import com.jasic.aftersales.system.service.ISysDictTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典类型 Service 实现类
 *
 * @author Codex
 * @date 2026/03/19
 */
@Service
public class SysDictTypeServiceImpl implements ISysDictTypeService {

    @Resource
    private SysDictTypeMapper sysDictTypeMapper;

    @Resource
    private SysDictDataMapper sysDictDataMapper;

    @Resource
    private ISysDictDataService dictDataService;

    @Override
    public PageResult<SysDictTypeVO> listPage(SysDictTypeQuery query) {
        Page<SysDictType> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getDictName())) {
            wrapper.like(SysDictType::getDictName, query.getDictName());
        }
        if (StrUtil.isNotBlank(query.getDictType())) {
            wrapper.like(SysDictType::getDictType, query.getDictType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysDictType::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysDictType::getId);
        Page<SysDictType> result = sysDictTypeMapper.selectPage(page, wrapper);
        List<SysDictTypeVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public SysDictTypeVO getById(Long id) {
        SysDictType entity = sysDictTypeMapper.selectById(id);
        return entity == null ? null : toVO(entity);
    }

    @Override
    public Long save(SysDictTypeDTO dto) {
        checkDictTypeUnique(dto.getDictType(), null);
        SysDictType entity = BeanUtil.copyProperties(dto, SysDictType.class);
        sysDictTypeMapper.insert(entity);
        dictDataService.refreshCache(entity.getDictType());
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(SysDictTypeDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("字典类型ID不能为空");
        }
        SysDictType entity = sysDictTypeMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("字典类型不存在");
        }
        checkDictTypeUnique(dto.getDictType(), dto.getId());
        String oldDictType = entity.getDictType();
        BeanUtil.copyProperties(dto, entity);
        sysDictTypeMapper.updateById(entity);
        if (!StrUtil.equals(oldDictType, entity.getDictType())) {
            LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysDictData::getDictType, oldDictType);
            List<SysDictData> dataList = sysDictDataMapper.selectList(wrapper);
            for (SysDictData data : dataList) {
                data.setDictType(entity.getDictType());
                sysDictDataMapper.updateById(data);
            }
            dictDataService.removeCache(oldDictType);
        }
        dictDataService.refreshCache(entity.getDictType());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void remove(Long id) {
        SysDictType entity = sysDictTypeMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("字典类型不存在");
        }
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, entity.getDictType());
        if (sysDictDataMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该字典类型下存在数据项，不允许删除");
        }
        sysDictTypeMapper.deleteById(id);
        dictDataService.removeCache(entity.getDictType());
    }

    @Override
    public void refreshCache() {
        dictDataService.refreshCache();
    }

    private void checkDictTypeUnique(String dictType, Long excludeId) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictType::getDictType, dictType);
        SysDictType exists = sysDictTypeMapper.selectOne(wrapper);
        if (exists != null && (excludeId == null || !exists.getId().equals(excludeId))) {
            throw new ServiceException("字典类型已存在");
        }
    }

    private SysDictTypeVO toVO(SysDictType entity) {
        return BeanUtil.copyProperties(entity, SysDictTypeVO.class);
    }
}
