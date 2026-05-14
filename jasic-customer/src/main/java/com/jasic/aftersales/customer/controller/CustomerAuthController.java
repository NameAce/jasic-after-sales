package com.jasic.aftersales.customer.controller;

import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.customer.domain.dto.CustomerProfileUpdateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWechatLoginDTO;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.domain.vo.CustomerLoginVO;
import com.jasic.aftersales.customer.domain.vo.CustomerUserInfoVO;
import com.jasic.aftersales.customer.service.ICUserService;
import com.jasic.aftersales.framework.security.StpCustomerUtil;
import com.jasic.aftersales.system.domain.enums.WechatMiniProgramScene;
import com.jasic.aftersales.system.domain.model.WechatAuthSession;
import com.jasic.aftersales.system.domain.model.WechatPhoneInfo;
import com.jasic.aftersales.system.service.WechatMiniProgramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * C端客户认证控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Api(tags = "C端客户认证")
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
    @ApiOperation(value = "C端小程序登录")
    @PostMapping("/login")
    public Result<CustomerLoginVO> login(@Validated @RequestBody CustomerWechatLoginDTO dto) {
        WechatAuthSession session = wechatMiniProgramService.code2Session(WechatMiniProgramScene.C, dto.getCode());
        String phone = null;
        if (StringUtils.hasText(dto.getPhoneCode())) {
            WechatPhoneInfo phoneInfo = wechatMiniProgramService.getPhoneNumber(WechatMiniProgramScene.C, dto.getPhoneCode());
            phone = StringUtils.hasText(phoneInfo.getPhoneNumber()) ? phoneInfo.getPhoneNumber() : phoneInfo.getPurePhoneNumber();
        }

        CUser user = cUserService.loginOrRegister(session.getOpenid(), phone);
        StpCustomerUtil.login(user.getId());

        CustomerLoginVO vo = new CustomerLoginVO();
        vo.setToken(StpCustomerUtil.getTokenValue());
        vo.setUserInfo(buildUserInfo(user));
        return Result.ok(vo);
    }

    /**
     * 获取当前客户信息
     *
     * @return 客户信息
     */
    @ApiOperation(value = "获取当前客户信息")
    @GetMapping("/user-info")
    public Result<CustomerUserInfoVO> getUserInfo() {
        return Result.ok(buildUserInfo(cUserService.getCurrentUser()));
    }

    /**
     * 修改当前客户资料
     *
     * @param dto 资料参数
     * @return 客户信息
     */
    @ApiOperation(value = "修改当前客户资料")
    @PutMapping("/profile")
    public Result<CustomerUserInfoVO> updateProfile(@Validated @RequestBody CustomerProfileUpdateDTO dto) {
        return Result.ok(buildUserInfo(cUserService.updateProfile(dto)));
    }

    /**
     * C端退出登录
     *
     * @return 操作结果
     */
    @ApiOperation(value = "C端退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpCustomerUtil.logout();
        return Result.ok();
    }

    /**
     * ???????
     *
     * @param user ??
     * @return ????
     */
    private CustomerUserInfoVO buildUserInfo(CUser user) {
        CustomerUserInfoVO vo = new CustomerUserInfoVO();
        vo.setUserId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setNeedProfileComplete(!StringUtils.hasText(user.getNickname()));
        return vo;
    }
}
