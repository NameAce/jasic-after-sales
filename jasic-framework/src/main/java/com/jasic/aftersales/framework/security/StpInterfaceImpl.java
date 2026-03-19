package com.jasic.aftersales.framework.security;

import cn.dev33.satoken.stp.StpInterface;
import com.jasic.aftersales.common.constant.CacheConstants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Sa-Token 权限接口实现，从 Redis 加载用户权限和角色
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取用户权限集合
     *
     * @param loginId   登录用户ID
     * @param loginType 登录类型
     * @return 权限标识列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long companyId = SecurityContext.getCurrentCompanyId();
        if (companyId == null) {
            return Collections.emptyList();
        }
        String key = CacheConstants.USER_PERMS_KEY + loginId + ":" + companyId;
        Set<Object> perms = redisTemplate.opsForSet().members(key);
        if (perms == null || perms.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> permList = new ArrayList<>(perms.size());
        for (Object perm : perms) {
            permList.add(perm.toString());
        }
        return permList;
    }

    /**
     * 获取用户角色集合（暂不使用角色标识校验，返回空）
     *
     * @param loginId   登录用户ID
     * @param loginType 登录类型
     * @return 角色标识列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return Collections.emptyList();
    }
}
