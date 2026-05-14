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

    /**
     * 系统字典类型Mapper数据访问接口。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Resource
    private SysDictTypeMapper sysDictTypeMapper;

    @Resource
    private SysDictDataMapper sysDictDataMapper;

    @Resource
    private ISysDictDataService dictDataService;

    /**
     * 查询listPage相关业务数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public PageResult<SysDictTypeVO> listPage(SysDictTypeQuery query) {
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<SysDictType> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getDictName())) {
            // 调用getDictName方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysDictType::getDictName, query.getDictName());
        }
        if (StrUtil.isNotBlank(query.getDictType())) {
            // 调用getDictType方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysDictType::getDictType, query.getDictType());
        }
        if (query.getStatus() != null) {
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysDictType::getStatus, query.getStatus());
        }
        // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByDesc(SysDictType::getId);
        // 说明：执行该步骤以保证业务流程正确。
        Page<SysDictType> result = sysDictTypeMapper.selectPage(page, wrapper);
        List<SysDictTypeVO> records = result.getRecords().stream()
                .map(this::toVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 根据ID查询字典类型详情。
     *
     * @return 处理结果
     */
    @Override
    public SysDictTypeVO getById(Long id) {
        // 调用selectById方法，复用统一能力并保证业务规则一致。
        SysDictType entity = sysDictTypeMapper.selectById(id);
        return entity == null ? null : toVO(entity);
    }

    /**
     * 新增字典类型。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Override
    public Long save(SysDictTypeDTO dto) {
        // 调用getDictType方法，复用统一能力并保证业务规则一致。
        checkDictTypeUnique(dto.getDictType(), null);
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        SysDictType entity = BeanUtil.copyProperties(dto, SysDictType.class);
        // 说明：执行该步骤以保证业务流程正确。
        sysDictTypeMapper.insert(entity);
        // 调用getDictType方法，复用统一能力并保证业务规则一致。
        dictDataService.refreshCache(entity.getDictType());
        return entity.getId();
    }

    /**
     * 更新字典类型。
     *
     * @param dto 参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(SysDictTypeDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("字典类型ID不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysDictType entity = sysDictTypeMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("字典类型不存在");
        }
        // 调用getId方法，复用统一能力并保证业务规则一致。
        checkDictTypeUnique(dto.getDictType(), dto.getId());
        // 调用getDictType方法，复用统一能力并保证业务规则一致。
        String oldDictType = entity.getDictType();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, entity);
        // 说明：执行该步骤以保证业务流程正确。
        sysDictTypeMapper.updateById(entity);
        if (!StrUtil.equals(oldDictType, entity.getDictType())) {
            LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
            // 调用eq方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysDictData::getDictType, oldDictType);
            // 调用selectList方法，复用统一能力并保证业务规则一致。
            List<SysDictData> dataList = sysDictDataMapper.selectList(wrapper);
            for (SysDictData data : dataList) {
                // 调用getDictType方法，复用统一能力并保证业务规则一致。
                data.setDictType(entity.getDictType());
                // 调用updateById方法，复用统一能力并保证业务规则一致。
                sysDictDataMapper.updateById(data);
            }
            // 调用removeCache方法，复用统一能力并保证业务规则一致。
            dictDataService.removeCache(oldDictType);
        }
        // 调用getDictType方法，复用统一能力并保证业务规则一致。
        dictDataService.refreshCache(entity.getDictType());
    }

    /**
     * 删除字典类型。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void remove(Long id) {
        // 说明：执行该步骤以保证业务流程正确。
        SysDictType entity = sysDictTypeMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("字典类型不存在");
        }
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        // 调用getDictType方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysDictData::getDictType, entity.getDictType());
        if (sysDictDataMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该字典类型下存在数据项，不允许删除");
        }
        // 说明：执行该步骤以保证业务流程正确。
        sysDictTypeMapper.deleteById(id);
        // 调用getDictType方法，复用统一能力并保证业务规则一致。
        dictDataService.removeCache(entity.getDictType());
    }

    /**
     * 刷新字典类型缓存。
     */
    @Override
    public void refreshCache() {
        // 调用refreshCache方法，复用统一能力并保证业务规则一致。
        dictDataService.refreshCache();
    }

    /**
     * check字典类型Unique。
     *
     * @param dictType 参数
     * @param excludeId exclude ID
     */
    private void checkDictTypeUnique(String dictType, Long excludeId) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysDictType::getDictType, dictType);
        // 说明：执行该步骤以保证业务流程正确。
        SysDictType exists = sysDictTypeMapper.selectOne(wrapper);
        if (exists != null && (excludeId == null || !exists.getId().equals(excludeId))) {
            throw new ServiceException("字典类型已存在");
        }
    }

    /**
     * to视图。
     *
     * @param entity 参数
     * @return 处理结果
     */
    private SysDictTypeVO toVO(SysDictType entity) {
        return BeanUtil.copyProperties(entity, SysDictTypeVO.class);
    }
}


