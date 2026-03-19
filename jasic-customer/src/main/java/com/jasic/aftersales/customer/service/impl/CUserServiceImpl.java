package com.jasic.aftersales.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.mapper.CUserMapper;
import com.jasic.aftersales.customer.service.ICUserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

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
            user.setLastLoginTime(LocalDateTime.now());
            cUserMapper.updateById(user);
            return user;
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
