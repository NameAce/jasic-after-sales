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
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        WechatAuthSession session = wechatMiniProgramService.code2Session(WechatMiniProgramScene.C, dto.getCode());
        String phone = null;
        if (StringUtils.hasText(dto.getPhoneCode())) {
            // 调用getPhoneCode方法，复用统一能力并保证业务规则一致。
            WechatPhoneInfo phoneInfo = wechatMiniProgramService.getPhoneNumber(WechatMiniProgramScene.C, dto.getPhoneCode());
            // 调用getPurePhoneNumber方法，复用统一能力并保证业务规则一致。
            phone = StringUtils.hasText(phoneInfo.getPhoneNumber()) ? phoneInfo.getPhoneNumber() : phoneInfo.getPurePhoneNumber();
        }

        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        CUser user = cUserService.loginOrRegister(session.getOpenid(), phone);
        // 调用getId方法，复用统一能力并保证业务规则一致。
        StpCustomerUtil.login(user.getId());

        // 调用CustomerLoginVO方法，复用统一能力并保证业务规则一致。
        CustomerLoginVO vo = new CustomerLoginVO();
        // 调用getTokenValue方法，复用统一能力并保证业务规则一致。
        vo.setToken(StpCustomerUtil.getTokenValue());
        // 调用buildUserInfo方法，复用统一能力并保证业务规则一致。
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
        // 调用logout方法，复用统一能力并保证业务规则一致。
        StpCustomerUtil.logout();
        return Result.ok();
    }

    /**
     * 构建用户Info。
     *
     * @param user 参数
     * @return 处理结果
     */
    private CustomerUserInfoVO buildUserInfo(CUser user) {
        // 调用CustomerUserInfoVO方法，复用统一能力并保证业务规则一致。
        CustomerUserInfoVO vo = new CustomerUserInfoVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setUserId(user.getId());
        // 调用getPhone方法，复用统一能力并保证业务规则一致。
        vo.setPhone(user.getPhone());
        // 调用getNickname方法，复用统一能力并保证业务规则一致。
        vo.setNickname(user.getNickname());
        // 调用getAvatar方法，复用统一能力并保证业务规则一致。
        vo.setAvatar(user.getAvatar());
        // 调用getNickname方法，复用统一能力并保证业务规则一致。
        vo.setNeedProfileComplete(!StringUtils.hasText(user.getNickname()));
        return vo;
    }
}




