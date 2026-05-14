package com.jasic.aftersales.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.constant.CacheConstants;
import com.jasic.aftersales.common.enums.DataScopeEnum;
import com.jasic.aftersales.system.domain.entity.SysRole;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import com.jasic.aftersales.system.mapper.SysMenuMapper;
import com.jasic.aftersales.system.mapper.SysRoleMapper;
import com.jasic.aftersales.system.mapper.SysUserRoleMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限加载服务，用于登录和切换公司时加载用户权限到 Redis 缓存
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysPermissionService {

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    /**
     * 系统角色Mapper数据访问接口。
     *
     * @return 处理结果
     */
    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 加载用户在指定公司下的权限到 Redis
     *
     * @param userId    用户ID
     * @param companyId 公司ID
     * @return 权限标识集合
     */
    public Set<String> loadPermsToCache(Long userId, Long companyId) {
        // 说明：执行该步骤以保证业务流程正确。
        Set<String> perms = sysMenuMapper.selectPermsByUserIdAndCompanyId(userId, companyId);
        if (perms == null) {
            // 调用emptySet方法，复用统一能力并保证业务规则一致。
            perms = Collections.emptySet();
        }
        String key = CacheConstants.USER_PERMS_KEY + userId + ":" + companyId;
        // 调用delete方法，复用统一能力并保证业务规则一致。
        redisTemplate.delete(key);
        if (!perms.isEmpty()) {
            // 调用toArray方法，复用统一能力并保证业务规则一致。
            redisTemplate.opsForSet().add(key, perms.toArray(new Object[0]));
        }
        return perms;
    }

    /**
     * 清除用户在指定公司下的权限缓存
     *
     * @param userId    用户ID
     * @param companyId 公司ID
     */
    public void clearPermsCache(Long userId, Long companyId) {
        String permsKey = CacheConstants.USER_PERMS_KEY + userId + ":" + companyId;
        String menusKey = CacheConstants.USER_MENUS_KEY + userId + ":" + companyId;
        // 调用delete方法，复用统一能力并保证业务规则一致。
        redisTemplate.delete(permsKey);
        // 调用delete方法，复用统一能力并保证业务规则一致。
        redisTemplate.delete(menusKey);
    }

    /**
     * 清除用户所有公司的权限缓存
     *
     * @param userId 用户ID
     */
    public void clearAllPermsCache(Long userId) {
        String permsPattern = CacheConstants.USER_PERMS_KEY + userId + ":*";
        String menusPattern = CacheConstants.USER_MENUS_KEY + userId + ":*";
        // 调用keys方法，复用统一能力并保证业务规则一致。
        Set<String> permsKeys = redisTemplate.keys(permsPattern);
        // 调用keys方法，复用统一能力并保证业务规则一致。
        Set<String> menusKeys = redisTemplate.keys(menusPattern);
        if (permsKeys != null && !permsKeys.isEmpty()) {
            // 调用delete方法，复用统一能力并保证业务规则一致。
            redisTemplate.delete(permsKeys);
        }
        if (menusKeys != null && !menusKeys.isEmpty()) {
            // 调用delete方法，复用统一能力并保证业务规则一致。
            redisTemplate.delete(menusKeys);
        }
    }

    /**
     * 获取用户在指定公司下的有效 data_scope，多个角色取最大范围。
     *
     * @param userId    用户ID
     * @param companyId 公司ID
     * @return 有效数据范围
     */
    public DataScopeEnum getEffectiveDataScope(Long userId, Long companyId) {
        return getEffectiveDataScope(userId, companyId, null);
    }

    /**
     * 获取用户在指定公司下的有效 data_scope，并按主体类型收敛到合法值。
     *
     * @param userId      用户ID
     * @param companyId   公司ID
     * @param subjectType 主体类型编码
     * @return 有效数据范围
     */
    public DataScopeEnum getEffectiveDataScope(Long userId, Long companyId, String subjectType) {
        LambdaQueryWrapper<SysUserRole> userRoleQuery = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        userRoleQuery.eq(SysUserRole::getUserId, userId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleQuery);
        if (userRoles == null || userRoles.isEmpty()) {
            return DataScopeEnum.SELF;
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        LambdaQueryWrapper<SysRole> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.in(SysRole::getId, roleIds)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysRole::getCompanyId, companyId);
        // 调用selectList方法，复用统一能力并保证业务规则一致。
        List<SysRole> roles = sysRoleMapper.selectList(roleQuery);
        if (roles == null || roles.isEmpty()) {
            return DataScopeEnum.SELF;
        }

        DataScopeEnum result = null;
        for (SysRole role : roles) {
            // 调用getDataScope方法，复用统一能力并保证业务规则一致。
            DataScopeEnum scope = resolveRoleDataScope(role.getDataScope(), subjectType);
            // 调用max方法，复用统一能力并保证业务规则一致。
            result = result == null ? scope : result.max(scope);
        }
        return result == null ? DataScopeEnum.SELF : result;
    }

    /**
     * 解析角色数据范围。
     *
     * @param scopeCode 参数
     * @param subjectType 参数
     * @return 处理结果
     */
    private DataScopeEnum resolveRoleDataScope(String scopeCode, String subjectType) {
        if (subjectType == null) {
            return DataScopeEnum.getByCode(scopeCode);
        }
        return DataScopeEnum.normalize(scopeCode, subjectType);
    }
}


