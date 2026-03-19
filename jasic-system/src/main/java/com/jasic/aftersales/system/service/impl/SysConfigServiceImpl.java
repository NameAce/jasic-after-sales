package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.constant.CacheConstants;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysConfigDTO;
import com.jasic.aftersales.system.domain.entity.SysConfig;
import com.jasic.aftersales.system.domain.query.SysConfigQuery;
import com.jasic.aftersales.system.domain.vo.SysConfigVO;
import com.jasic.aftersales.system.mapper.SysConfigMapper;
import com.jasic.aftersales.system.service.ISysConfigService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 参数设置 Service 实现类
 *
 * @author Codex
 * @date 2026/03/19
 */
@Service
public class SysConfigServiceImpl implements ISysConfigService {

    @Resource
    private SysConfigMapper sysConfigMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void initCache() {
        refreshCache();
    }

    @Override
    public PageResult<SysConfigVO> listPage(SysConfigQuery query) {
        Page<SysConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getConfigName())) {
            wrapper.like(SysConfig::getConfigName, query.getConfigName());
        }
        if (StrUtil.isNotBlank(query.getConfigKey())) {
            wrapper.like(SysConfig::getConfigKey, query.getConfigKey());
        }
        if (query.getConfigType() != null) {
            wrapper.eq(SysConfig::getConfigType, query.getConfigType());
        }
        wrapper.orderByDesc(SysConfig::getId);
        Page<SysConfig> result = sysConfigMapper.selectPage(page, wrapper);
        List<SysConfigVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public SysConfigVO getById(Long id) {
        SysConfig entity = sysConfigMapper.selectById(id);
        return entity == null ? null : toVO(entity);
    }

    @Override
    public String getValueByKey(String configKey) {
        String cacheKey = getCacheKey(configKey);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            Object value = redisTemplate.opsForValue().get(cacheKey);
            return value == null ? "" : String.valueOf(value);
        }
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig entity = sysConfigMapper.selectOne(wrapper);
        if (entity == null) {
            return "";
        }
        redisTemplate.opsForValue().set(cacheKey, entity.getConfigValue());
        return entity.getConfigValue();
    }

    @Override
    public Long save(SysConfigDTO dto) {
        checkConfigKeyUnique(dto.getConfigKey(), null);
        SysConfig entity = BeanUtil.copyProperties(dto, SysConfig.class);
        sysConfigMapper.insert(entity);
        redisTemplate.opsForValue().set(getCacheKey(entity.getConfigKey()), entity.getConfigValue());
        return entity.getId();
    }

    @Override
    public void update(SysConfigDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("参数ID不能为空");
        }
        SysConfig entity = sysConfigMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("参数不存在");
        }
        checkConfigKeyUnique(dto.getConfigKey(), dto.getId());
        String oldConfigKey = entity.getConfigKey();
        BeanUtil.copyProperties(dto, entity);
        sysConfigMapper.updateById(entity);
        if (!StrUtil.equals(oldConfigKey, entity.getConfigKey())) {
            redisTemplate.delete(getCacheKey(oldConfigKey));
        }
        redisTemplate.opsForValue().set(getCacheKey(entity.getConfigKey()), entity.getConfigValue());
    }

    @Override
    public void remove(Long id) {
        SysConfig entity = sysConfigMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("参数不存在");
        }
        if (entity.getConfigType() != null && entity.getConfigType() == 1) {
            throw new ServiceException("内置参数不允许删除");
        }
        sysConfigMapper.deleteById(id);
        redisTemplate.delete(getCacheKey(entity.getConfigKey()));
    }

    @Override
    public void refreshCache() {
        clearAllCache();
        List<SysConfig> configs = sysConfigMapper.selectList(new LambdaQueryWrapper<>());
        for (SysConfig config : configs) {
            redisTemplate.opsForValue().set(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
    }

    private void clearAllCache() {
        Set<String> keys = redisTemplate.keys(CacheConstants.CONFIG_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void checkConfigKeyUnique(String configKey, Long excludeId) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig exists = sysConfigMapper.selectOne(wrapper);
        if (exists != null && (excludeId == null || !exists.getId().equals(excludeId))) {
            throw new ServiceException("参数键名已存在");
        }
    }

    private String getCacheKey(String configKey) {
        return CacheConstants.CONFIG_KEY + configKey;
    }

    private SysConfigVO toVO(SysConfig entity) {
        return BeanUtil.copyProperties(entity, SysConfigVO.class);
    }
}
