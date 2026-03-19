package com.jasic.aftersales.customer.service;

import com.jasic.aftersales.customer.domain.entity.CUser;

/**
 * C端客户 Service 接口
 *
 * @author Zoro
 * @date 2026/03/18
 */
public interface ICUserService {

    /**
     * 根据openid查询客户
     *
     * @param openid 微信openid
     * @return 客户信息
     */
    CUser getByOpenid(String openid);

    /**
     * 微信登录（自动注册）
     *
     * @param openid 微信openid
     * @param phone  手机号
     * @return 客户信息
     */
    CUser loginOrRegister(String openid, String phone);
}
