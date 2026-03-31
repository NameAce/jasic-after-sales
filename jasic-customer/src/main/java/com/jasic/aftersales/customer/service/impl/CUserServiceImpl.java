package com.jasic.aftersales.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.mapper.CUserMapper;
import com.jasic.aftersales.customer.service.ICUserService;
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
     * @param openid 微信openid
     * @param phone  手机号
     * @return 客户信息
     */
    @Override
    public CUser loginOrRegister(String openid, String phone) {
        CUser user = getByOpenid(openid);
        if (user != null) {
            user.setPhone(phone);
            user.setLastLoginTime(LocalDateTime.now());
            cUserMapper.updateById(user);
            return user;
        }
        CUser phoneUser = getByPhone(phone);
        if (phoneUser != null) {
            phoneUser.setOpenid(openid);
            phoneUser.setPhone(phone);
            phoneUser.setStatus(1);
            phoneUser.setLastLoginTime(LocalDateTime.now());
            cUserMapper.updateById(phoneUser);
            return phoneUser;
        }
        CUser newUser = new CUser();
        newUser.setOpenid(openid);
        newUser.setPhone(phone);
        newUser.setStatus(1);
        newUser.setLastLoginTime(LocalDateTime.now());
        cUserMapper.insert(newUser);
        return newUser;
    }
}
