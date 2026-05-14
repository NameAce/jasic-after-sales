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

    /**
     * 系统配置Mapper数据访问接口。
     */
    @Resource
    private SysConfigMapper sysConfigMapper;

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
     * 分页查询配置列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public PageResult<SysConfigVO> listPage(SysConfigQuery query) {
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<SysConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getConfigName())) {
            // 调用getConfigName方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysConfig::getConfigName, query.getConfigName());
        }
        if (StrUtil.isNotBlank(query.getConfigKey())) {
            // 调用getConfigKey方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysConfig::getConfigKey, query.getConfigKey());
        }
        if (query.getConfigType() != null) {
            // 调用getConfigType方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysConfig::getConfigType, query.getConfigType());
        }
        // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByDesc(SysConfig::getId);
        // 说明：执行该步骤以保证业务流程正确。
        Page<SysConfig> result = sysConfigMapper.selectPage(page, wrapper);
        List<SysConfigVO> records = result.getRecords().stream()
                .map(this::toVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 根据ID查询配置详情。
     *
     * @return 处理结果
     */
    @Override
    public SysConfigVO getById(Long id) {
        // 调用selectById方法，复用统一能力并保证业务规则一致。
        SysConfig entity = sysConfigMapper.selectById(id);
        return entity == null ? null : toVO(entity);
    }

    /**
     * 获取值ByKey。
     *
     * @param configKey 参数
     * @return 处理结果
     */
    @Override
    public String getValueByKey(String configKey) {
        // 调用getCacheKey方法，复用统一能力并保证业务规则一致。
        String cacheKey = getCacheKey(configKey);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            // 调用get方法，复用统一能力并保证业务规则一致。
            Object value = redisTemplate.opsForValue().get(cacheKey);
            return value == null ? "" : String.valueOf(value);
        }
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysConfig::getConfigKey, configKey);
        // 说明：执行该步骤以保证业务流程正确。
        SysConfig entity = sysConfigMapper.selectOne(wrapper);
        if (entity == null) {
            return "";
        }
        // 调用getConfigValue方法，复用统一能力并保证业务规则一致。
        redisTemplate.opsForValue().set(cacheKey, entity.getConfigValue());
        return entity.getConfigValue();
    }

    /**
     * 新增配置。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Override
    public Long save(SysConfigDTO dto) {
        // 调用getConfigKey方法，复用统一能力并保证业务规则一致。
        checkConfigKeyUnique(dto.getConfigKey(), null);
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        SysConfig entity = BeanUtil.copyProperties(dto, SysConfig.class);
        // 说明：执行该步骤以保证业务流程正确。
        sysConfigMapper.insert(entity);
        // 调用getConfigValue方法，复用统一能力并保证业务规则一致。
        redisTemplate.opsForValue().set(getCacheKey(entity.getConfigKey()), entity.getConfigValue());
        return entity.getId();
    }

    /**
     * 更新配置。
     *
     * @param dto 参数
     */
    @Override
    public void update(SysConfigDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("参数ID不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysConfig entity = sysConfigMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("参数不存在");
        }
        // 调用getId方法，复用统一能力并保证业务规则一致。
        checkConfigKeyUnique(dto.getConfigKey(), dto.getId());
        // 调用getConfigKey方法，复用统一能力并保证业务规则一致。
        String oldConfigKey = entity.getConfigKey();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, entity);
        // 说明：执行该步骤以保证业务流程正确。
        sysConfigMapper.updateById(entity);
        if (!StrUtil.equals(oldConfigKey, entity.getConfigKey())) {
            // 调用getCacheKey方法，复用统一能力并保证业务规则一致。
            redisTemplate.delete(getCacheKey(oldConfigKey));
        }
        // 调用getConfigValue方法，复用统一能力并保证业务规则一致。
        redisTemplate.opsForValue().set(getCacheKey(entity.getConfigKey()), entity.getConfigValue());
    }

    /**
     * 删除配置。
     */
    @Override
    public void remove(Long id) {
        // 说明：执行该步骤以保证业务流程正确。
        SysConfig entity = sysConfigMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("参数不存在");
        }
        if (entity.getConfigType() != null && entity.getConfigType() == 1) {
            throw new ServiceException("内置参数不允许删除");
        }
        // 说明：执行该步骤以保证业务流程正确。
        sysConfigMapper.deleteById(id);
        // 调用getConfigKey方法，复用统一能力并保证业务规则一致。
        redisTemplate.delete(getCacheKey(entity.getConfigKey()));
    }

    /**
     * 刷新配置缓存。
     */
    @Override
    public void refreshCache() {
        // 调用clearAllCache方法，复用统一能力并保证业务规则一致。
        clearAllCache();
        // 说明：执行该步骤以保证业务流程正确。
        List<SysConfig> configs = sysConfigMapper.selectList(new LambdaQueryWrapper<>());
        for (SysConfig config : configs) {
            // 调用getConfigValue方法，复用统一能力并保证业务规则一致。
            redisTemplate.opsForValue().set(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
    }

    /**
     * clearAll缓存。
     */
    private void clearAllCache() {
        // 调用keys方法，复用统一能力并保证业务规则一致。
        Set<String> keys = redisTemplate.keys(CacheConstants.CONFIG_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            // 调用delete方法，复用统一能力并保证业务规则一致。
            redisTemplate.delete(keys);
        }
    }

    /**
     * check配置KeyUnique。
     *
     * @param configKey 参数
     * @param excludeId exclude ID
     */
    private void checkConfigKeyUnique(String configKey, Long excludeId) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysConfig::getConfigKey, configKey);
        // 说明：执行该步骤以保证业务流程正确。
        SysConfig exists = sysConfigMapper.selectOne(wrapper);
        if (exists != null && (excludeId == null || !exists.getId().equals(excludeId))) {
            throw new ServiceException("参数键名已存在");
        }
    }

    /**
     * 获取缓存Key。
     *
     * @param configKey 参数
     * @return 处理结果
     */
    private String getCacheKey(String configKey) {
        return CacheConstants.CONFIG_KEY + configKey;
    }

    /**
     * to视图。
     *
     * @param entity 参数
     * @return 处理结果
     */
    private SysConfigVO toVO(SysConfig entity) {
        return BeanUtil.copyProperties(entity, SysConfigVO.class);
    }
}


