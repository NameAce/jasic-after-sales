package com.jasic.aftersales.customer.service;

import com.jasic.aftersales.customer.domain.dto.CustomerProfileUpdateDTO;
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
     * 根据手机号查询客户
     *
     * @param phone 手机号
     * @return 客户信息
     */
    CUser getByPhone(String phone);

    /**
     * 微信登录（自动注册）
     *
     * @param openid 微信openid
     * @param phone  手机号
     * @return 客户信息
     */
    CUser loginOrRegister(String openid, String phone);

    /**
     * 获取当前登录客户
     *
     * @return 客户信息
     */
    CUser getCurrentUser();

    /**
     * 修改当前客户资料
     *
     * @param dto 资料参数
     * @return 客户信息
     */
    CUser updateProfile(CustomerProfileUpdateDTO dto);
}
