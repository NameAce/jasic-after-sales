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

    /**
     * 系统字典数据Mapper数据访问接口。
     */
    @Resource
    private SysDictDataMapper sysDictDataMapper;

    @Resource
    private SysDictTypeMapper sysDictTypeMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理initCache业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     */
    @PostConstruct
    public void initCache() {
        // 调用refreshCache方法，复用统一能力并保证业务规则一致。
        refreshCache();
    }

    /**
     * 分页查询字典数据列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public PageResult<SysDictDataVO> listPage(SysDictDataQuery query) {
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<SysDictData> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getDictType())) {
            // 调用getDictType方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysDictData::getDictType, query.getDictType());
        }
        if (StrUtil.isNotBlank(query.getDictLabel())) {
            // 调用getDictLabel方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysDictData::getDictLabel, query.getDictLabel());
        }
        if (query.getStatus() != null) {
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysDictData::getStatus, query.getStatus());
        }
        // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByAsc(SysDictData::getDictSort, SysDictData::getId);
        // 说明：执行该步骤以保证业务流程正确。
        Page<SysDictData> result = sysDictDataMapper.selectPage(page, wrapper);
        List<SysDictDataVO> records = result.getRecords().stream()
                .map(this::toVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 根据ID查询字典数据详情。
     *
     * @return 处理结果
     */
    @Override
    public SysDictDataVO getById(Long id) {
        // 调用selectById方法，复用统一能力并保证业务规则一致。
        SysDictData entity = sysDictDataMapper.selectById(id);
        return entity == null ? null : toVO(entity);
    }

    /**
     * 分页查询By类型列表。
     *
     * @param dictType 参数
     * @return 处理结果
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<SysDictDataVO> listByType(String dictType) {
        // 调用getCacheKey方法，复用统一能力并保证业务规则一致。
        String cacheKey = getCacheKey(dictType);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            // 调用get方法，复用统一能力并保证业务规则一致。
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                return (List<SysDictDataVO>) cached;
            }
        }
        // 调用queryActiveByType方法，复用统一能力并保证业务规则一致。
        List<SysDictDataVO> list = queryActiveByType(dictType);
        // 调用set方法，复用统一能力并保证业务规则一致。
        redisTemplate.opsForValue().set(cacheKey, list);
        return list;
    }

    /**
     * 新增字典数据。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Override
    public Long save(SysDictDataDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        ensureDictTypeExists(dto.getDictType());
        // 调用getDictValue方法，复用统一能力并保证业务规则一致。
        checkDictValueUnique(dto.getDictType(), dto.getDictValue(), null);
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        SysDictData entity = BeanUtil.copyProperties(dto, SysDictData.class);
        // 说明：执行该步骤以保证业务流程正确。
        sysDictDataMapper.insert(entity);
        // 调用getDictType方法，复用统一能力并保证业务规则一致。
        refreshCache(entity.getDictType());
        return entity.getId();
    }

    /**
     * 更新字典数据。
     *
     * @param dto 参数
     */
    @Override
    public void update(SysDictDataDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("字典数据ID不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysDictData entity = sysDictDataMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("字典数据不存在");
        }
        // 说明：执行该步骤以保证业务流程正确。
        ensureDictTypeExists(dto.getDictType());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        checkDictValueUnique(dto.getDictType(), dto.getDictValue(), dto.getId());
        // 调用getDictType方法，复用统一能力并保证业务规则一致。
        String oldDictType = entity.getDictType();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, entity);
        // 说明：执行该步骤以保证业务流程正确。
        sysDictDataMapper.updateById(entity);
        if (!StrUtil.equals(oldDictType, entity.getDictType())) {
            // 调用removeCache方法，复用统一能力并保证业务规则一致。
            removeCache(oldDictType);
        }
        // 调用getDictType方法，复用统一能力并保证业务规则一致。
        refreshCache(entity.getDictType());
    }

    /**
     * 删除字典数据。
     */
    @Override
    public void remove(Long id) {
        // 说明：执行该步骤以保证业务流程正确。
        SysDictData entity = sysDictDataMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("字典数据不存在");
        }
        // 说明：执行该步骤以保证业务流程正确。
        sysDictDataMapper.deleteById(id);
        // 调用getDictType方法，复用统一能力并保证业务规则一致。
        refreshCache(entity.getDictType());
    }

    /**
     * 刷新字典数据缓存。
     */
    @Override
    public void refreshCache() {
        // 调用clearAllCache方法，复用统一能力并保证业务规则一致。
        clearAllCache();
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getStatus, 1)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysDictData::getDictType, SysDictData::getDictSort, SysDictData::getId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysDictDataVO> allData = sysDictDataMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .sorted(Comparator.comparing(SysDictDataVO::getDictSort).thenComparing(SysDictDataVO::getId))
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        Map<String, List<SysDictDataVO>> grouped = allData.stream()
                // 调用groupingBy方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.groupingBy(SysDictDataVO::getDictType));
        // 调用getCacheKey方法，复用统一能力并保证业务规则一致。
        grouped.forEach((dictType, list) -> redisTemplate.opsForValue().set(getCacheKey(dictType), list));
    }

    /**
     * 刷新字典数据缓存。
     *
     * @param dictType 参数
     */
    @Override
    public void refreshCache(String dictType) {
        // 调用queryActiveByType方法，复用统一能力并保证业务规则一致。
        redisTemplate.opsForValue().set(getCacheKey(dictType), queryActiveByType(dictType));
    }

    /**
     * 删除缓存。
     *
     * @param dictType 参数
     */
    @Override
    public void removeCache(String dictType) {
        // 调用getCacheKey方法，复用统一能力并保证业务规则一致。
        redisTemplate.delete(getCacheKey(dictType));
    }

    /**
     * ensure字典类型Exists。
     *
     * @param dictType 参数
     */
    private void ensureDictTypeExists(String dictType) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysDictType::getDictType, dictType);
        // 说明：执行该步骤以保证业务流程正确。
        if (sysDictTypeMapper.selectCount(wrapper) == 0) {
            throw new ServiceException("字典类型不存在");
        }
    }

    /**
     * check字典值Unique。
     *
     * @param dictType 参数
     * @param dictValue 参数
     * @param excludeId exclude ID
     */
    private void checkDictValueUnique(String dictType, String dictValue, Long excludeId) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysDictData::getDictValue, dictValue);
        // 说明：执行该步骤以保证业务流程正确。
        SysDictData exists = sysDictDataMapper.selectOne(wrapper);
        if (exists != null && (excludeId == null || !exists.getId().equals(excludeId))) {
            throw new ServiceException("同一字典类型下键值不能重复");
        }
    }

    /**
     * 查询ActiveBy类型。
     *
     * @param dictType 参数
     * @return 处理结果
     */
    private List<SysDictDataVO> queryActiveByType(String dictType) {
        if (StrUtil.isBlank(dictType)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysDictData::getDictSort, SysDictData::getId);
        // 说明：执行该步骤以保证业务流程正确。
        return sysDictDataMapper.selectList(wrapper).stream()
                .map(this::toVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * clearAll缓存。
     */
    private void clearAllCache() {
        // 调用keys方法，复用统一能力并保证业务规则一致。
        Set<String> keys = redisTemplate.keys(CacheConstants.DICT_DATA_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            // 调用delete方法，复用统一能力并保证业务规则一致。
            redisTemplate.delete(keys);
        }
    }

    /**
     * 获取缓存Key。
     *
     * @param dictType 参数
     * @return 处理结果
     */
    private String getCacheKey(String dictType) {
        return CacheConstants.DICT_DATA_KEY + dictType;
    }

    /**
     * to视图。
     *
     * @param entity 参数
     * @return 处理结果
     */
    private SysDictDataVO toVO(SysDictData entity) {
        return BeanUtil.copyProperties(entity, SysDictDataVO.class);
    }
}


