package com.jasic.aftersales.customer.controller;

import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.customer.domain.dto.CustomerWechatLoginDTO;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.service.ICUserService;
import com.jasic.aftersales.framework.security.StpCustomerUtil;
import com.jasic.aftersales.system.domain.enums.WechatMiniProgramScene;
import com.jasic.aftersales.system.domain.model.WechatAuthSession;
import com.jasic.aftersales.system.domain.model.WechatPhoneInfo;
import com.jasic.aftersales.system.service.WechatMiniProgramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Resource
    private WechatMiniProgramService wechatMiniProgramService;

    /**
     * C端小程序登录
     *
     * @param dto 登录参数
     * @return Token和用户信息
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Validated @RequestBody CustomerWechatLoginDTO dto) {
        WechatAuthSession session = wechatMiniProgramService.code2Session(WechatMiniProgramScene.C, dto.getCode());
        String phone = null;
        if (StringUtils.hasText(dto.getPhoneCode())) {
            WechatPhoneInfo phoneInfo = wechatMiniProgramService.getPhoneNumber(WechatMiniProgramScene.C, dto.getPhoneCode());
            phone = StringUtils.hasText(phoneInfo.getPhoneNumber()) ? phoneInfo.getPhoneNumber() : phoneInfo.getPurePhoneNumber();
        }

        CUser user = cUserService.loginOrRegister(session.getOpenid(), session.getUnionid(), phone);
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
