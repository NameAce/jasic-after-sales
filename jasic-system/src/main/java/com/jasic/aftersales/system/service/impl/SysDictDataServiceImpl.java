package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.constant.CacheConstants;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysDictDataDTO;
import com.jasic.aftersales.system.domain.entity.SysDictData;
import com.jasic.aftersales.system.domain.entity.SysDictType;
import com.jasic.aftersales.system.domain.query.SysDictDataQuery;
import com.jasic.aftersales.system.domain.vo.SysDictDataVO;
import com.jasic.aftersales.system.mapper.SysDictDataMapper;
import com.jasic.aftersales.system.mapper.SysDictTypeMapper;
import com.jasic.aftersales.system.service.ISysDictDataService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字典数据 Service 实现类
 *
 * @author Codex
 * @date 2026/03/19
 */
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {

    @Resource
    private SysDictDataMapper sysDictDataMapper;

    @Resource
    private SysDictTypeMapper sysDictTypeMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void initCache() {
        refreshCache();
    }

    @Override
    public PageResult<SysDictDataVO> listPage(SysDictDataQuery query) {
        Page<SysDictData> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getDictType())) {
            wrapper.eq(SysDictData::getDictType, query.getDictType());
        }
        if (StrUtil.isNotBlank(query.getDictLabel())) {
            wrapper.like(SysDictData::getDictLabel, query.getDictLabel());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysDictData::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysDictData::getDictSort, SysDictData::getId);
        Page<SysDictData> result = sysDictDataMapper.selectPage(page, wrapper);
        List<SysDictDataVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public SysDictDataVO getById(Long id) {
        SysDictData entity = sysDictDataMapper.selectById(id);
        return entity == null ? null : toVO(entity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SysDictDataVO> listByType(String dictType) {
        String cacheKey = getCacheKey(dictType);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                return (List<SysDictDataVO>) cached;
            }
        }
        List<SysDictDataVO> list = queryActiveByType(dictType);
        redisTemplate.opsForValue().set(cacheKey, list);
        return list;
    }

    @Override
    public Long save(SysDictDataDTO dto) {
        ensureDictTypeExists(dto.getDictType());
        checkDictValueUnique(dto.getDictType(), dto.getDictValue(), null);
        SysDictData entity = BeanUtil.copyProperties(dto, SysDictData.class);
        sysDictDataMapper.insert(entity);
        refreshCache(entity.getDictType());
        return entity.getId();
    }

    @Override
    public void update(SysDictDataDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("字典数据ID不能为空");
        }
        SysDictData entity = sysDictDataMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("字典数据不存在");
        }
        ensureDictTypeExists(dto.getDictType());
        checkDictValueUnique(dto.getDictType(), dto.getDictValue(), dto.getId());
        String oldDictType = entity.getDictType();
        BeanUtil.copyProperties(dto, entity);
        sysDictDataMapper.updateById(entity);
        if (!StrUtil.equals(oldDictType, entity.getDictType())) {
            removeCache(oldDictType);
        }
        refreshCache(entity.getDictType());
    }

    @Override
    public void remove(Long id) {
        SysDictData entity = sysDictDataMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("字典数据不存在");
        }
        sysDictDataMapper.deleteById(id);
        refreshCache(entity.getDictType());
    }

    @Override
    public void refreshCache() {
        clearAllCache();
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getDictType, SysDictData::getDictSort, SysDictData::getId);
        List<SysDictDataVO> allData = sysDictDataMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .sorted(Comparator.comparing(SysDictDataVO::getDictSort).thenComparing(SysDictDataVO::getId))
                .collect(Collectors.toList());
        Map<String, List<SysDictDataVO>> grouped = allData.stream()
                .collect(Collectors.groupingBy(SysDictDataVO::getDictType));
        grouped.forEach((dictType, list) -> redisTemplate.opsForValue().set(getCacheKey(dictType), list));
    }

    @Override
    public void refreshCache(String dictType) {
        redisTemplate.opsForValue().set(getCacheKey(dictType), queryActiveByType(dictType));
    }

    @Override
    public void removeCache(String dictType) {
        redisTemplate.delete(getCacheKey(dictType));
    }

    private void ensureDictTypeExists(String dictType) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictType::getDictType, dictType);
        if (sysDictTypeMapper.selectCount(wrapper) == 0) {
            throw new ServiceException("字典类型不存在");
        }
    }

    private void checkDictValueUnique(String dictType, String dictValue, Long excludeId) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getDictValue, dictValue);
        SysDictData exists = sysDictDataMapper.selectOne(wrapper);
        if (exists != null && (excludeId == null || !exists.getId().equals(excludeId))) {
            throw new ServiceException("同一字典类型下键值不能重复");
        }
    }

    private List<SysDictDataVO> queryActiveByType(String dictType) {
        if (StrUtil.isBlank(dictType)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getDictSort, SysDictData::getId);
        return sysDictDataMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private void clearAllCache() {
        Set<String> keys = redisTemplate.keys(CacheConstants.DICT_DATA_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String getCacheKey(String dictType) {
        return CacheConstants.DICT_DATA_KEY + dictType;
    }

    private SysDictDataVO toVO(SysDictData entity) {
        return BeanUtil.copyProperties(entity, SysDictDataVO.class);
    }
}
