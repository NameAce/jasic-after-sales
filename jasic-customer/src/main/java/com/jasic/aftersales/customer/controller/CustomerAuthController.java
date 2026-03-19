package com.jasic.aftersales.customer.controller;

import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.service.ICUserService;
import com.jasic.aftersales.framework.security.StpCustomerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * C端客户认证控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Slf4j
@RestController
@RequestMapping("/customer/auth")
public class CustomerAuthController {

    @Resource
    private ICUserService cUserService;

    /**
     * C端小程序登录
     * <p>
     * 前端传入微信 code 和手机号 code，后端换取 openid 和手机号后自动注册/登录。
     * 当前为简化版，直接传入 openid 和 phone（实际对接微信时需换取）。
     * </p>
     *
     * @param openid 微信openid（实际场景由后端通过code换取）
     * @param phone  手机号（实际场景由后端通过phoneCode换取）
     * @return Token和用户信息
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String openid, @RequestParam String phone) {
        CUser user = cUserService.loginOrRegister(openid, phone);
        StpCustomerUtil.login(user.getId());

        Map<String, Object> result = new HashMap<>(4);
        result.put("token", StpCustomerUtil.getTokenValue());
        result.put("userId", user.getId());
        result.put("phone", user.getPhone());
        result.put("nickname", user.getNickname());
        return Result.ok(result);
    }

    /**
     * C端退出登录
     *
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpCustomerUtil.logout();
        return Result.ok();
    }
}
