package com.jasic.aftersales.system.service.support;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * B端用户登录标识校验器
 *
 * @author Codex
 * @date 2026/04/02
 */
@Component
public class SysUserIdentityValidator {

    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 校验用户名、手机号及交叉登录标识唯一性
     *
     * @param excludeUserId 当前排除的用户ID（新增时为空）
     * @param username      用户名
     * @param phone         手机号
     */
    public void validateLoginIdentityUnique(Long excludeUserId, String username, String phone) {
        String normalizedUsername = StrUtil.trim(username);
        String normalizedPhone = StrUtil.trim(phone);
        if (StrUtil.isBlank(normalizedUsername)) {
            throw new ServiceException("用户名不能为空");
        }
        if (StrUtil.isBlank(normalizedPhone)) {
            throw new ServiceException("手机号不能为空");
        }
        if (existsByUsername(excludeUserId, normalizedUsername)) {
            throw new ServiceException("用户名已存在");
        }
        if (existsByPhone(excludeUserId, normalizedPhone)) {
            throw new ServiceException("手机号已存在");
        }
        if (existsOtherUserPhone(excludeUserId, normalizedUsername)) {
            throw new ServiceException("用户名（" + normalizedUsername + "）已被其他用户手机号占用，请调整");
        }
        if (!StrUtil.equals(normalizedUsername, normalizedPhone) && existsOtherUserUsername(excludeUserId, normalizedPhone)) {
            throw new ServiceException("手机号（" + normalizedPhone + "）已被其他用户用户名占用，请调整");
        }
    }

    private boolean existsByUsername(Long excludeUserId, String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        if (excludeUserId != null) {
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        return sysUserMapper.selectCount(wrapper) > 0;
    }

    private boolean existsByPhone(Long excludeUserId, String phone) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPhone, phone);
        if (excludeUserId != null) {
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        return sysUserMapper.selectCount(wrapper) > 0;
    }

    private boolean existsOtherUserPhone(Long excludeUserId, String phone) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPhone, phone);
        if (excludeUserId != null) {
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        return sysUserMapper.selectCount(wrapper) > 0;
    }

    private boolean existsOtherUserUsername(Long excludeUserId, String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        if (excludeUserId != null) {
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        return sysUserMapper.selectCount(wrapper) > 0;
    }
}
