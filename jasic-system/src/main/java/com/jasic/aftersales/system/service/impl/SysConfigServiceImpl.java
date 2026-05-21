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
import com.jasic.aftersales.system.domain.vo.SysConfigGroupVO;
import com.jasic.aftersales.system.domain.vo.SysConfigVO;
import com.jasic.aftersales.system.mapper.SysConfigMapper;
import com.jasic.aftersales.system.service.ISysConfigService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 参数设置 Service 实现类。
 *
 * <p>该服务继续承接旧“参数设置”页的增删改查、按 key 读取配置和缓存刷新能力。
 * 本轮系统配置分组改造只在 sys_config 单表上补充 groupKey，服务层负责把旧页面未提交分组字段的请求
 * 按配置 key 自动归组，保证现有入口不因数据库新增非空字段而失效。</p>
 *
 * @author Codex
 * @date 2026/03/19
 */
@Service
public class SysConfigServiceImpl implements ISysConfigService {

    /** 组织配置分组，当前承载公司初始化管理员密码等组织基础参数。 */
    private static final String GROUP_ORG = "org";

    /** 微信配置分组，当前承载 B/C 端小程序接入和绑定跳转参数。 */
    private static final String GROUP_WECHAT = "wechat";

    /** 工单配置分组，当前承载工单业务默认归属等参数。 */
    private static final String GROUP_WORK_ORDER = "work_order";

    /** 历史配置分组，仅用于隔离已废弃的 wechat.notify.* 历史参数。 */
    private static final String GROUP_LEGACY = "legacy";

    /** 历史微信通知参数前缀，当前只归入 legacy，不重新启用旧通知链路。 */
    private static final String LEGACY_WECHAT_NOTIFY_PREFIX = "wechat.notify.";

    /** 允许保存的配置分组集合，避免配置项被自由写入未知分组导致后续页面化改造无法识别。 */
    private static final Set<String> ALLOWED_GROUP_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            GROUP_ORG,
            GROUP_WECHAT,
            GROUP_WORK_ORDER,
            GROUP_LEGACY
    )));

    /** 配置分组固定返回顺序，前端可直接按该顺序渲染 tab、分组块或折叠面板。 */
    private static final List<String> CONFIG_GROUP_ORDER = Collections.unmodifiableList(Arrays.asList(
            GROUP_ORG,
            GROUP_WECHAT,
            GROUP_WORK_ORDER,
            GROUP_LEGACY
    ));

    /** 配置分组展示名称映射，避免前端硬编码分组中文名后和后端语义漂移。 */
    private static final Map<String, String> CONFIG_GROUP_NAME_MAP;

    /** 固定配置 key 到业务分组的映射，覆盖当前已确认的真实配置项。 */
    private static final Map<String, String> CONFIG_KEY_GROUP_MAP;

    static {
        Map<String, String> groupNameMap = new HashMap<>();
        groupNameMap.put(GROUP_ORG, "组织配置");
        groupNameMap.put(GROUP_WECHAT, "微信配置");
        groupNameMap.put(GROUP_WORK_ORDER, "工单配置");
        groupNameMap.put(GROUP_LEGACY, "历史配置");
        CONFIG_GROUP_NAME_MAP = Collections.unmodifiableMap(groupNameMap);

        Map<String, String> groupMap = new HashMap<>();
        groupMap.put("org.company.adminInitPassword", GROUP_ORG);
        groupMap.put("wechat.mp.b.appid", GROUP_WECHAT);
        groupMap.put("wechat.mp.b.secret", GROUP_WECHAT);
        groupMap.put("wechat.mp.c.appid", GROUP_WECHAT);
        groupMap.put("wechat.mp.c.secret", GROUP_WECHAT);
        groupMap.put("wechat.mp.b.bind.pagePath", GROUP_WECHAT);
        groupMap.put("default.hq.company.id", GROUP_WORK_ORDER);
        CONFIG_KEY_GROUP_MAP = Collections.unmodifiableMap(groupMap);
    }

    /**
     * 系统配置 Mapper 数据访问接口，负责 sys_config 单表读写。
     */
    @Resource
    private SysConfigMapper sysConfigMapper;

    /**
     * Redis 操作模板，沿用现有参数缓存能力，保存成功或刷新缓存时按配置 key 写入缓存。
     */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 初始化参数缓存。
     *
     * <p>应用启动后统一加载 sys_config 到 Redis，避免业务首次读取配置时反复访问数据库。
     * 本次新增 groupKey 不改变缓存 key 结构，缓存仍然只服务按 configKey 读取配置值的既有链路。</p>
     */
    @PostConstruct
    public void initCache() {
        // 启动阶段复用全量刷新逻辑，确保新增分组字段不影响原有按 key 缓存配置值的行为。
        refreshCache();
    }

    /**
     * 分页查询配置列表。
     *
     * @param query 参数名称、键名、内置状态和可选分组查询条件
     * @return 按分组和主键稳定排序后的配置分页结果
     */
    @Override
    public PageResult<SysConfigVO> listPage(SysConfigQuery query) {
        // 旧参数设置页仍使用分页列表，分页参数继续沿用 PageQuery，避免本次分组改造改变前端调用方式。
        Page<SysConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getConfigName())) {
            // 参数名称是旧页面已有筛选项，继续保留模糊匹配以兼容现有维护习惯。
            wrapper.like(SysConfig::getConfigName, query.getConfigName());
        }
        if (StrUtil.isNotBlank(query.getConfigKey())) {
            // 参数键名是业务代码读取配置的稳定标识，按键名模糊查询便于定位具体配置项。
            wrapper.like(SysConfig::getConfigKey, query.getConfigKey());
        }
        if (query.getConfigType() != null) {
            // 内置状态决定是否允许删除，保留该筛选条件不改变旧参数设置页能力。
            wrapper.eq(SysConfig::getConfigType, query.getConfigType());
        }
        if (StrUtil.isNotBlank(query.getGroupKey())) {
            // groupKey 是本轮新增的后端分组能力，查询前校验可防止未知分组进入后续页面化改造口径。
            wrapper.eq(SysConfig::getGroupKey, normalizeExplicitGroupKey(query.getGroupKey()));
        }
        // 按方案优先以分组排序，再按主键稳定排序；当前不引入 sort_num，避免扩大数据模型。
        wrapper.orderByAsc(SysConfig::getGroupKey, SysConfig::getId);
        // 执行分页查询后统一转换 VO，让新增 groupKey 随列表返回给调用方。
        Page<SysConfig> result = sysConfigMapper.selectPage(page, wrapper);
        List<SysConfigVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 查询系统配置分组及各分组全部配置项。
     *
     * <p>旧分页列表只适合“参数设置”维护页，不适合前端一次性展示所有配置分组。
     * 该方法返回固定分组容器，并在每个容器内放入当前分组的全部配置项，前端不需要循环拉取分页数据。</p>
     *
     * @param includeLegacy 是否包含 legacy 历史废弃分组；false 时用于新系统配置页，true 时用于历史配置排查
     * @return 固定顺序的配置分组列表
     */
    @Override
    public List<SysConfigGroupVO> listGroups(Boolean includeLegacy) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (!Boolean.TRUE.equals(includeLegacy)) {
            // 新配置页默认不展示 legacy，避免历史废弃通知配置被误认为当前正式配置入口。
            wrapper.ne(SysConfig::getGroupKey, GROUP_LEGACY);
        }
        // 查询全部配置项后由服务层按固定分组顺序组装，避免前端受分页或数据库字典序影响。
        wrapper.orderByAsc(SysConfig::getGroupKey, SysConfig::getId);
        List<SysConfig> configs = sysConfigMapper.selectList(wrapper);
        Map<String, List<SysConfigVO>> groupedConfigMap = configs.stream()
                .collect(Collectors.groupingBy(SysConfig::getGroupKey, Collectors.mapping(this::toVO, Collectors.toList())));

        List<SysConfigGroupVO> groups = new ArrayList<>();
        for (String groupKey : CONFIG_GROUP_ORDER) {
            if (GROUP_LEGACY.equals(groupKey) && !Boolean.TRUE.equals(includeLegacy)) {
                // 未显式要求 legacy 时跳过历史分组，保持新系统配置页只展示当前有效配置域。
                continue;
            }
            SysConfigGroupVO groupVO = new SysConfigGroupVO();
            groupVO.setGroupKey(groupKey);
            groupVO.setGroupName(CONFIG_GROUP_NAME_MAP.get(groupKey));
            groupVO.setLegacy(GROUP_LEGACY.equals(groupKey));
            groupVO.setConfigs(groupedConfigMap.getOrDefault(groupKey, Collections.emptyList()));
            groups.add(groupVO);
        }
        return groups;
    }

    /**
     * 根据ID查询配置详情。
     *
     * @param id 参数设置主键
     * @return 配置详情；记录不存在时返回 null，保持原接口语义
     */
    @Override
    public SysConfigVO getById(Long id) {
        // 详情接口继续按主键读取，groupKey 通过 VO 自动返回，旧页面可忽略该新增字段。
        SysConfig entity = sysConfigMapper.selectById(id);
        return entity == null ? null : toVO(entity);
    }

    /**
     * 根据参数键名获取参数值。
     *
     * @param configKey 参数键名
     * @return 参数值；未配置时返回空字符串以兼容现有业务调用
     */
    @Override
    public String getValueByKey(String configKey) {
        // 配置读取链路仍以 configKey 作为唯一缓存 key，groupKey 只影响维护和展示分组，不参与运行时取值。
        String cacheKey = getCacheKey(configKey);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            // 缓存存在时直接返回，避免高频业务配置读取反复访问数据库。
            Object value = redisTemplate.opsForValue().get(cacheKey);
            return value == null ? "" : String.valueOf(value);
        }
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        // 缓存未命中时回源数据库，按唯一配置 key 查询当前有效配置值。
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig entity = sysConfigMapper.selectOne(wrapper);
        if (entity == null) {
            return "";
        }
        // 回源成功后写入缓存，后续业务读取继续沿用原有快速路径。
        redisTemplate.opsForValue().set(cacheKey, entity.getConfigValue());
        return entity.getConfigValue();
    }

    /**
     * 新增配置。
     *
     * @param dto 参数设置新增入参
     * @return 新增配置主键
     */
    @Override
    public Long save(SysConfigDTO dto) {
        // 新增前先校验配置 key 唯一，避免运行时按 key 读取配置时出现多条记录的歧义。
        checkConfigKeyUnique(dto.getConfigKey(), null);
        // 旧参数设置页不会提交 groupKey，因此复制属性后必须由服务层补齐分组，保证数据库非空约束可稳定通过。
        SysConfig entity = BeanUtil.copyProperties(dto, SysConfig.class);
        entity.setGroupKey(resolveGroupKey(dto.getGroupKey(), dto.getConfigKey(), null));
        // 写入 sys_config 后，新增配置即可被旧页面和业务读取链路共同识别。
        sysConfigMapper.insert(entity);
        // 保存成功后立即刷新当前 key 的缓存，避免业务读取到旧值或空值。
        redisTemplate.opsForValue().set(getCacheKey(entity.getConfigKey()), entity.getConfigValue());
        return entity.getId();
    }

    /**
     * 更新配置。
     *
     * @param dto 参数设置修改入参
     */
    @Override
    public void update(SysConfigDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("参数ID不能为空");
        }
        // 先读取旧记录，用于判断配置是否存在、保留旧分组以及清理旧配置 key 的缓存。
        SysConfig entity = sysConfigMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("参数不存在");
        }
        // 修改时仍要保证 configKey 唯一，否则按 key 获取配置会出现不可控结果。
        checkConfigKeyUnique(dto.getConfigKey(), dto.getId());
        // 记录旧 key 用于后续缓存清理，避免改名后旧 key 仍能命中历史缓存。
        String oldConfigKey = entity.getConfigKey();
        // 旧页面不传 groupKey 时按新 key 自动归组或保留旧分组，兼容新旧页面并存的过渡状态。
        String resolvedGroupKey = resolveGroupKey(dto.getGroupKey(), dto.getConfigKey(), entity.getGroupKey());
        // 复制普通可维护字段，随后回写归一化后的分组，避免空分组覆盖已有数据。
        BeanUtil.copyProperties(dto, entity);
        entity.setGroupKey(resolvedGroupKey);
        // 持久化修改后的配置项，保证新增 groupKey 与参数值、备注等字段一起保存。
        sysConfigMapper.updateById(entity);
        if (!StrUtil.equals(oldConfigKey, entity.getConfigKey())) {
            // key 发生变化时删除旧缓存，不删除会导致业务继续通过旧 key 读到已改名配置。
            redisTemplate.delete(getCacheKey(oldConfigKey));
        }
        // 更新当前 key 缓存，保证保存成功后业务读取立即生效。
        redisTemplate.opsForValue().set(getCacheKey(entity.getConfigKey()), entity.getConfigValue());
    }

    /**
     * 删除配置。
     *
     * @param id 参数设置主键
     */
    @Override
    public void remove(Long id) {
        // 删除前读取配置记录，既用于存在性校验，也用于判断内置配置是否允许删除。
        SysConfig entity = sysConfigMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("参数不存在");
        }
        if (entity.getConfigType() != null && entity.getConfigType() == 1) {
            throw new ServiceException("内置参数不允许删除");
        }
        // 删除普通配置记录后同步删除缓存，避免运行时继续读取已删除配置。
        sysConfigMapper.deleteById(id);
        redisTemplate.delete(getCacheKey(entity.getConfigKey()));
    }

    /**
     * 刷新配置缓存。
     *
     * <p>当前缓存以 configKey 为维度，本轮分组改造不强制引入按组缓存。全量刷新优先保证配置值稳定生效，
     * 后续如需要更细粒度缓存，再单独评估缓存结构。</p>
     */
    @Override
    public void refreshCache() {
        // 先清理旧缓存，避免数据库中已删除或改名的配置继续留在 Redis 中。
        clearAllCache();
        // 再全量加载 sys_config，沿用原有按配置 key 缓存配置值的运行时读取方式。
        List<SysConfig> configs = sysConfigMapper.selectList(new LambdaQueryWrapper<>());
        for (SysConfig config : configs) {
            // groupKey 不参与缓存 key，避免本次分组字段影响现有业务读取配置值。
            redisTemplate.opsForValue().set(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
    }

    /**
     * 清理全部参数缓存。
     *
     * <p>缓存 key 仍沿用 config:key:{configKey} 格式。清理时按统一前缀删除，确保保存或刷新缓存后
     * 不会残留已废弃、改名或删除的配置项。</p>
     */
    private void clearAllCache() {
        // 使用现有缓存前缀统一定位参数缓存，避免误删其它业务缓存。
        Set<String> keys = redisTemplate.keys(CacheConstants.CONFIG_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            // 只有存在参数缓存时才执行删除，减少空集合调用带来的无意义 Redis 操作。
            redisTemplate.delete(keys);
        }
    }

    /**
     * 校验配置键名唯一。
     *
     * @param configKey 参数键名
     * @param excludeId 修改场景需要排除的当前记录主键；新增场景传 null
     */
    private void checkConfigKeyUnique(String configKey, Long excludeId) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        // configKey 是运行时读取配置的唯一入口，必须先按 key 查询是否已有其它记录占用。
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig exists = sysConfigMapper.selectOne(wrapper);
        if (exists != null && (excludeId == null || !exists.getId().equals(excludeId))) {
            throw new ServiceException("参数键名已存在");
        }
    }

    /**
     * 获取参数缓存 key。
     *
     * @param configKey 参数键名
     * @return Redis 中保存参数值的完整缓存 key
     */
    private String getCacheKey(String configKey) {
        return CacheConstants.CONFIG_KEY + configKey;
    }

    /**
     * 转换参数设置视图对象。
     *
     * @param entity 参数设置实体
     * @return 包含 groupKey 的参数设置返回对象
     */
    private SysConfigVO toVO(SysConfig entity) {
        return BeanUtil.copyProperties(entity, SysConfigVO.class);
    }

    /**
     * 解析并校验配置分组。
     *
     * <p>旧“参数设置”页不会传 groupKey，新页面或接口调用也可能只提交配置 key。
     * 该方法按“显式分组优先、已确认 key 自动归组、已有分组保留、默认 org 兜底”的顺序处理，
     * 既保证新增 NOT NULL 字段有值，也避免未知配置项被写入不可识别分组。</p>
     *
     * @param requestedGroupKey 入参中显式提交的分组标识
     * @param configKey 参数键名
     * @param existingGroupKey 修改场景中的原分组标识；新增场景传 null
     * @return 归一化后的合法分组标识
     */
    private String resolveGroupKey(String requestedGroupKey, String configKey, String existingGroupKey) {
        if (StrUtil.isNotBlank(requestedGroupKey)) {
            // 调用方显式提交分组时必须先校验，避免未知分组污染 sys_config 数据。
            return normalizeExplicitGroupKey(requestedGroupKey);
        }
        String mappedGroupKey = resolveGroupKeyByConfigKey(configKey);
        if (StrUtil.isNotBlank(mappedGroupKey)) {
            // 已确认配置项按方案固定归组，旧页面不传 groupKey 时也能保存到正确分组。
            return mappedGroupKey;
        }
        if (StrUtil.isNotBlank(existingGroupKey)) {
            // 未纳入本轮映射的历史或自定义配置，修改时保留原分组，避免旧页面编辑造成分组漂移。
            return normalizeExplicitGroupKey(existingGroupKey);
        }
        // 新增未知配置时默认归入 org，保证字段非空且不引入方案外的新分组。
        return GROUP_ORG;
    }

    /**
     * 根据配置键名解析已确认分组。
     *
     * @param configKey 参数键名
     * @return 已确认分组；未命中固定映射且不是历史微信通知 key 时返回 null
     */
    private String resolveGroupKeyByConfigKey(String configKey) {
        if (StrUtil.isBlank(configKey)) {
            return null;
        }
        if (configKey.startsWith(LEGACY_WECHAT_NOTIFY_PREFIX)) {
            // wechat.notify.* 已明确废弃，本轮只做 legacy 隔离，不恢复旧通知配置链路。
            return GROUP_LEGACY;
        }
        return CONFIG_KEY_GROUP_MAP.get(configKey);
    }

    /**
     * 归一化显式分组值。
     *
     * @param groupKey 待校验分组标识
     * @return 去除首尾空白后的合法分组标识
     */
    private String normalizeExplicitGroupKey(String groupKey) {
        String normalizedGroupKey = StrUtil.trim(groupKey);
        if (!ALLOWED_GROUP_KEYS.contains(normalizedGroupKey)) {
            throw new ServiceException("配置分组标识不合法");
        }
        return normalizedGroupKey;
    }
}


