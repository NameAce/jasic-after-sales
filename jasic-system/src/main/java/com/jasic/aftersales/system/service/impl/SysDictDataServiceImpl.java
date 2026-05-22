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
 * @author Zoro
 * @date 2026/03/19
 */
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {

    /**
     * 系统字典数据Mapper数据访问接口。
     */
    @Resource
    private SysDictDataMapper sysDictDataMapper;

    /**sysDictTypeMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysDictTypeMapper sysDictTypeMapper;

    /**redisTemplate 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理initCache业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     */
    @PostConstruct
    public void initCache() {
        refreshCache();
    }

    /**
     * 分页查询字典数据列表。
     *
     * @param query 查询条件，包含分页、筛选和权限收口所需字段。
     * @return 业务处理结果
     */
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

    /**
     * 根据ID查询字典数据详情。
     *
     * @return 业务处理结果
     */
    @Override
    public SysDictDataVO getById(Long id) {
        SysDictData entity = sysDictDataMapper.selectById(id);
        return entity == null ? null : toVO(entity);
    }

    /**
     * 分页查询By类型列表。
     *
     * @param dictType dictType，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
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

    /**
     * 新增字典数据。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     * @return 业务处理结果
     */
    @Override
    public Long save(SysDictDataDTO dto) {
        ensureDictTypeExists(dto.getDictType());
        checkDictValueUnique(dto.getDictType(), dto.getDictValue(), null);
        SysDictData entity = BeanUtil.copyProperties(dto, SysDictData.class);
        sysDictDataMapper.insert(entity);
        refreshCache(entity.getDictType());
        return entity.getId();
    }

    /**
     * 更新字典数据。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
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

    /**
     * 删除字典数据。
     */
    @Override
    public void remove(Long id) {
        SysDictData entity = sysDictDataMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("字典数据不存在");
        }
        sysDictDataMapper.deleteById(id);
        refreshCache(entity.getDictType());
    }

    /**
     * 刷新字典数据缓存。
     */
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

    /**
     * 刷新字典数据缓存。
     *
     * @param dictType dictType，当前业务处理所需的输入值。
     */
    @Override
    public void refreshCache(String dictType) {
        redisTemplate.opsForValue().set(getCacheKey(dictType), queryActiveByType(dictType));
    }

    /**
     * 删除缓存。
     *
     * @param dictType dictType，当前业务处理所需的输入值。
     */
    @Override
    public void removeCache(String dictType) {
        redisTemplate.delete(getCacheKey(dictType));
    }

    /**
     * ensure字典类型Exists。
     *
     * @param dictType dictType，当前业务处理所需的输入值。
     */
    private void ensureDictTypeExists(String dictType) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictType::getDictType, dictType);
        if (sysDictTypeMapper.selectCount(wrapper) == 0) {
            throw new ServiceException("字典类型不存在");
        }
    }

    /**
     * check字典值Unique。
     *
     * @param dictType dictType，当前业务处理所需的输入值。
     * @param dictValue dictValue，当前业务处理所需的输入值。
     * @param excludeId exclude ID
     */
    private void checkDictValueUnique(String dictType, String dictValue, Long excludeId) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getDictValue, dictValue);
        SysDictData exists = sysDictDataMapper.selectOne(wrapper);
        if (exists != null && (excludeId == null || !exists.getId().equals(excludeId))) {
            throw new ServiceException("同一字典类型下键值不能重复");
        }
    }

    /**
     * 查询ActiveBy类型。
     *
     * @param dictType dictType，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
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

    /**
     * clearAll缓存。
     */
    private void clearAllCache() {
        Set<String> keys = redisTemplate.keys(CacheConstants.DICT_DATA_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 获取缓存Key。
     *
     * @param dictType dictType，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private String getCacheKey(String dictType) {
        return CacheConstants.DICT_DATA_KEY + dictType;
    }

    /**
     * to视图。
     *
     * @param entity entity，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private SysDictDataVO toVO(SysDictData entity) {
        return BeanUtil.copyProperties(entity, SysDictDataVO.class);
    }
}


