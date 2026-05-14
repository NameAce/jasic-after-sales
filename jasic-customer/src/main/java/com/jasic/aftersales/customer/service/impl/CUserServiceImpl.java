package com.jasic.aftersales.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.customer.domain.dto.CustomerProfileUpdateDTO;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.mapper.CUserMapper;
import com.jasic.aftersales.customer.service.ICUserService;
import com.jasic.aftersales.framework.security.StpCustomerUtil;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * C端客户 Service 实现
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class CUserServiceImpl implements ICUserService {

    /**
     * C用户Mapper数据访问接口。
     *
     * @return 处理结果
     */
    @Resource
    private CUserMapper cUserMapper;

    /**
     * 根据openid查询客户
     *
     * @param openid 微信openid
     * @return 客户信息
     */
    @Override
    public CUser getByOpenid(String openid) {
        return cUserMapper.selectOne(
                new LambdaQueryWrapper<CUser>().eq(CUser::getOpenid, openid)
        );
    }

    /**
     * 根据手机号查询客户
     *
     * @param phone 手机号
     * @return 客户信息
     */
    @Override
    public CUser getByPhone(String phone) {
        // 说明：执行该步骤以保证业务流程正确。
        List<CUser> users = cUserMapper.selectList(
                new LambdaQueryWrapper<CUser>().eq(CUser::getPhone, phone).orderByAsc(CUser::getId)
        );
        if (users.size() > 1) {
            throw new ServiceException("客户手机号存在重复数据，请联系管理员处理");
        }
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 微信登录（自动注册），openid 不存在则创建新客户
     *
     * @param openid 微信openid
     * @param phone  手机号
     * @return 客户信息
     */
    @Override
    public CUser loginOrRegister(String openid, String phone) {
        // 调用getByOpenid方法，复用统一能力并保证业务规则一致。
        CUser user = getByOpenid(openid);
        if (user != null) {
            return updateLoginSnapshot(user, phone);
        }
        if (!StringUtils.hasText(phone)) {
            throw new ServiceException("首次登录请先授权手机号");
        }
        // 调用getByPhone方法，复用统一能力并保证业务规则一致。
        CUser phoneUser = getByPhone(phone);
        if (phoneUser != null) {
            // 调用setOpenid方法，复用统一能力并保证业务规则一致。
            phoneUser.setOpenid(openid);
            return updateLoginSnapshot(phoneUser, phone);
        }
        return createUser(openid, phone);
    }

    /**
     * 获取当前登录客户
     *
     * @return 客户信息
     */
    @Override
    public CUser getCurrentUser() {
        return requireCurrentUser();
    }

    /**
     * 修改当前客户资料
     *
     * @param dto 资料参数
     * @return 客户信息
     */
    @Override
    public CUser updateProfile(CustomerProfileUpdateDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        CUser user = requireCurrentUser();
        boolean changed = false;
        if (dto.getNickname() != null) {
            // 调用getNickname方法，复用统一能力并保证业务规则一致。
            user.setNickname(normalizeText(dto.getNickname()));
            changed = true;
        }
        if (dto.getAvatar() != null) {
            // 调用getAvatar方法，复用统一能力并保证业务规则一致。
            user.setAvatar(normalizeText(dto.getAvatar()));
            changed = true;
        }
        if (changed) {
            // 说明：执行该步骤以保证业务流程正确。
            cUserMapper.updateById(user);
        }
        return user;
    }

    /**
     * 更新客户最近一次微信登录快照，保持手机号与微信侧同步。
     *
     * @param user 客户实体
     * @param phone 手机号
     * @return 更新后的客户
     */
    private CUser updateLoginSnapshot(CUser user, String phone) {
        // 说明：执行该步骤以保证业务流程正确。
        ensureUserActive(user);
        if (StringUtils.hasText(phone)) {
            // 调用setPhone方法，复用统一能力并保证业务规则一致。
            user.setPhone(phone);
        }
        // 调用now方法，复用统一能力并保证业务规则一致。
        user.setLastLoginTime(LocalDateTime.now());
        // 说明：执行该步骤以保证业务流程正确。
        cUserMapper.updateById(user);
        return user;
    }

    /**
     * 首次登录时创建客户账号。
     *
     * @param openid 微信 openid
     * @param phone 手机号
     * @return 新建客户
     */
    private CUser createUser(String openid, String phone) {
        // 调用CUser方法，复用统一能力并保证业务规则一致。
        CUser newUser = new CUser();
        // 调用setOpenid方法，复用统一能力并保证业务规则一致。
        newUser.setOpenid(openid);
        // 调用setPhone方法，复用统一能力并保证业务规则一致。
        newUser.setPhone(phone);
        // 调用setStatus方法，复用统一能力并保证业务规则一致。
        newUser.setStatus(1);
        // 调用now方法，复用统一能力并保证业务规则一致。
        newUser.setLastLoginTime(LocalDateTime.now());
        // 说明：执行该步骤以保证业务流程正确。
        cUserMapper.insert(newUser);
        return newUser;
    }

    /**
     * 校验登录态并返回当前客户。
     *
     * @return 当前客户
     */
    private CUser requireCurrentUser() {
        // 调用checkLogin方法，复用统一能力并保证业务规则一致。
        StpCustomerUtil.checkLogin();
        // 调用getLoginIdAsLong方法，复用统一能力并保证业务规则一致。
        Long userId = StpCustomerUtil.getLoginIdAsLong();
        // 说明：执行该步骤以保证业务流程正确。
        CUser user = cUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("当前客户不存在");
        }
        // 说明：执行该步骤以保证业务流程正确。
        ensureUserActive(user);
        return user;
    }

    /**
     * 拦截已停用客户，避免继续登录或修改资料。
     *
     * @param user 客户实体
     */
    private void ensureUserActive(CUser user) {
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new ServiceException("当前客户账号已停用");
        }
    }

    /**
     * 统一清洗文本输入，空白字符按 null 处理。
     *
     * @param text 原始文本
     * @return 清洗后的文本
     */
    private String normalizeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}


