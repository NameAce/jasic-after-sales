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
        Set<String> perms = sysMenuMapper.selectPermsByUserIdAndCompanyId(userId, companyId);
        if (perms == null) {
            perms = Collections.emptySet();
        }
        String key = CacheConstants.USER_PERMS_KEY + userId + ":" + companyId;
        redisTemplate.delete(key);
        if (!perms.isEmpty()) {
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
        redisTemplate.delete(permsKey);
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
        Set<String> permsKeys = redisTemplate.keys(permsPattern);
        Set<String> menusKeys = redisTemplate.keys(menusPattern);
        if (permsKeys != null && !permsKeys.isEmpty()) {
            redisTemplate.delete(permsKeys);
        }
        if (menusKeys != null && !menusKeys.isEmpty()) {
            redisTemplate.delete(menusKeys);
        }
    }

    /**
     * 获取用户在指定公司下的有效 data_scope（多角色取最小范围）
     *
     * @param userId    用户ID
     * @param companyId 公司ID
     * @return 有效数据范围
     */
    public DataScopeEnum getEffectiveDataScope(Long userId, Long companyId) {
        LambdaQueryWrapper<SysUserRole> userRoleQuery = new LambdaQueryWrapper<>();
        userRoleQuery.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleQuery);
        if (userRoles == null || userRoles.isEmpty()) {
            return DataScopeEnum.SELF;
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<SysRole> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.in(SysRole::getId, roleIds)
                .eq(SysRole::getCompanyId, companyId);
        List<SysRole> roles = sysRoleMapper.selectList(roleQuery);
        if (roles == null || roles.isEmpty()) {
            return DataScopeEnum.SELF;
        }
        DataScopeEnum result = DataScopeEnum.SELF;
        for (SysRole role : roles) {
            DataScopeEnum scope = DataScopeEnum.getByCode(role.getDataScope());
            result = result.min(scope);
        }
        return result;
    }
}
