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

    /**
     * 系统用户Mapper数据访问接口。
     *
     * @param excludeUserId exclude User ID
     * @param username 参数
     * @param phone 参数
     */
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
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedUsername = StrUtil.trim(username);
        // 调用trim方法，复用统一能力并保证业务规则一致。
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

    /**
     * existsByUsername。
     *
     * @param excludeUserId exclude User ID
     * @param username 参数
     */
    private boolean existsByUsername(Long excludeUserId, String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysUser::getUsername, username);
        if (excludeUserId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        // 说明：执行该步骤以保证业务流程正确。
        return sysUserMapper.selectCount(wrapper) > 0;
    }

    /**
     * existsByPhone。
     *
     * @param excludeUserId exclude User ID
     * @param phone 参数
     */
    private boolean existsByPhone(Long excludeUserId, String phone) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysUser::getPhone, phone);
        if (excludeUserId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        // 说明：执行该步骤以保证业务流程正确。
        return sysUserMapper.selectCount(wrapper) > 0;
    }

    /**
     * existsOther用户Phone。
     *
     * @param excludeUserId exclude User ID
     * @param phone 参数
     */
    private boolean existsOtherUserPhone(Long excludeUserId, String phone) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysUser::getPhone, phone);
        if (excludeUserId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        // 说明：执行该步骤以保证业务流程正确。
        return sysUserMapper.selectCount(wrapper) > 0;
    }

    /**
     * existsOther用户Username。
     *
     * @param excludeUserId exclude User ID
     * @param username 参数
     */
    private boolean existsOtherUserUsername(Long excludeUserId, String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysUser::getUsername, username);
        if (excludeUserId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        // 说明：执行该步骤以保证业务流程正确。
        return sysUserMapper.selectCount(wrapper) > 0;
    }
}


