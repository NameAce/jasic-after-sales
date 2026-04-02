package com.jasic.aftersales.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.mapper.CUserMapper;
import com.jasic.aftersales.customer.service.ICUserService;
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
        List<CUser> users = cUserMapper.selectList(
                new LambdaQueryWrapper<CUser>().eq(CUser::getPhone, phone).orderByAsc(CUser::getId)
        );
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 微信登录（自动注册），openid 不存在则创建新客户
     *
     * @param openid  微信openid
     * @param unionid 微信unionid
     * @param phone   手机号
     * @return 客户信息
     */
    @Override
    public CUser loginOrRegister(String openid, String unionid, String phone) {
        CUser user = getByOpenid(openid);
        if (user != null) {
            ensureUserActive(user);
            if (StringUtils.hasText(phone)) {
                user.setPhone(phone);
            }
            if (StringUtils.hasText(unionid)) {
                user.setUnionid(unionid);
            }
            user.setLastLoginTime(LocalDateTime.now());
            cUserMapper.updateById(user);
            return user;
        }
        if (!StringUtils.hasText(phone)) {
            throw new ServiceException("首次登录请先授权手机号");
        }
        CUser phoneUser = getByPhone(phone);
        if (phoneUser != null) {
            ensureUserActive(phoneUser);
            phoneUser.setOpenid(openid);
            phoneUser.setUnionid(unionid);
            phoneUser.setPhone(phone);
            phoneUser.setLastLoginTime(LocalDateTime.now());
            cUserMapper.updateById(phoneUser);
            return phoneUser;
        }
        CUser newUser = new CUser();
        newUser.setOpenid(openid);
        newUser.setUnionid(unionid);
        newUser.setPhone(phone);
        newUser.setStatus(1);
        newUser.setLastLoginTime(LocalDateTime.now());
        cUserMapper.insert(newUser);
        return newUser;
    }

    private void ensureUserActive(CUser user) {
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new ServiceException("当前客户账号已停用");
        }
    }
}
