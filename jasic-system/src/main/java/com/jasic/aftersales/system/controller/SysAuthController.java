package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.ChooseCompanyDTO;
import com.jasic.aftersales.system.domain.dto.LoginDTO;
import com.jasic.aftersales.system.domain.dto.MpBindLoginDTO;
import com.jasic.aftersales.system.domain.dto.MpLoginDTO;
import com.jasic.aftersales.system.domain.dto.WechatBindConfirmDTO;
import com.jasic.aftersales.system.domain.dto.WechatBindUnbindDTO;
import com.jasic.aftersales.system.domain.dto.ChangePasswordDTO;
import com.jasic.aftersales.system.domain.dto.UpdateProfileDTO;
import com.jasic.aftersales.system.domain.vo.LoginVO;
import com.jasic.aftersales.system.domain.vo.MpLoginVO;
import com.jasic.aftersales.system.domain.vo.SysMenuVO;
import com.jasic.aftersales.system.domain.vo.SysUserVO;
import com.jasic.aftersales.system.domain.vo.WechatBindStatusVO;
import com.jasic.aftersales.system.service.ISysAuthService;
import com.jasic.aftersales.system.service.ISysMenuService;
import com.jasic.aftersales.framework.security.SecurityContext;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 认证控制器（登录/登出/切换公司）
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Api(tags = "认证（登录/登出/切换公司）")
@RestController
@RequestMapping("/auth")
public class SysAuthController {

    @Resource
    private ISysAuthService authService;

    @Resource
    private ISysMenuService menuService;

    /**
     * B端登录
     *
     * @param dto 登录参数
     * @return 登录结果
     */
    @ApiOperation(value = "B端登录")
    @SaIgnore
    @OperLog(title = "用户登录", operType = OperTypeEnum.LOGIN)
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        // 调用login方法，复用统一能力并保证业务规则一致。
        LoginVO vo = authService.login(dto);
        return Result.ok(vo);
    }

    /**
     * B端小程序登录
     *
     * @param dto 登录参数
     * @return 登录结果
     */
    @ApiOperation(value = "B端小程序登录")
    @SaIgnore
    @PostMapping("/mp-login")
    public Result<MpLoginVO> mpLogin(@Validated @RequestBody MpLoginDTO dto) {
        // 调用mpLogin方法，复用统一能力并保证业务规则一致。
        MpLoginVO vo = authService.mpLogin(dto);
        return Result.ok(vo);
    }

    /**
     * B端小程序账号认领绑定并登录
     *
     * @param dto 绑定参数
     * @return 登录结果
     */
    @ApiOperation(value = "B端小程序账号认领绑定并登录")
    @SaIgnore
    @PostMapping("/mp-bind-login")
    public Result<MpLoginVO> mpBindLogin(@Validated @RequestBody MpBindLoginDTO dto) {
        return Result.ok(authService.mpBindLogin(dto));
    }

    /**
     * 选择/切换公司
     *
     * @param dto 公司选择参数
     * @return 用户信息
     */
    @ApiOperation(value = "选择/切换公司")
    @OperLog(title = "切换公司", operType = OperTypeEnum.OTHER)
    @PostMapping("/choose-company")
    public Result<SysUserVO> chooseCompany(@Validated @RequestBody ChooseCompanyDTO dto) {
        // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
        SysUserVO vo = authService.chooseCompany(dto.getCompanyId());
        return Result.ok(vo);
    }

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    @ApiOperation(value = "获取当前用户信息")
    @GetMapping("/user-info")
    public Result<SysUserVO> getUserInfo() {
        // 调用getUserInfo方法，复用统一能力并保证业务规则一致。
        SysUserVO vo = authService.getUserInfo();
        return Result.ok(vo);
    }

    /**
     * 修改当前用户资料
     *
     * @param dto 资料参数
     * @return 用户信息
     */
    @ApiOperation(value = "修改当前用户资料")
    @OperLog(title = "账号中心", operType = OperTypeEnum.UPDATE)
    @PutMapping("/profile")
    public Result<SysUserVO> updateProfile(@Validated @RequestBody UpdateProfileDTO dto) {
        return Result.ok(authService.updateProfile(dto));
    }

    /**
     * 修改当前用户密码
     *
     * @param dto 密码参数
     * @return 操作结果
     */
    @ApiOperation(value = "修改当前用户密码")
    @OperLog(title = "账号中心", operType = OperTypeEnum.UPDATE)
    @PutMapping("/change-password")
    public Result<Void> changePassword(@Validated @RequestBody ChangePasswordDTO dto) {
        // 调用changePassword方法，复用统一能力并保证业务规则一致。
        authService.changePassword(dto);
        return Result.ok();
    }

    /**
     * 查询当前用户微信绑定状态
     *
     * @return 绑定状态
     */
    @ApiOperation(value = "查询当前用户微信绑定状态")
    @GetMapping("/wechat-bind/status")
    public Result<WechatBindStatusVO> getWechatBindStatus() {
        return Result.ok(authService.getWechatBindStatus());
    }

    /**
     * 生成当前用户微信绑定二维码
     *
     * @return 绑定状态
     */
    @ApiOperation(value = "生成当前用户微信绑定二维码")
    @PostMapping("/wechat-bind/qrcode")
    public Result<WechatBindStatusVO> createWechatBindQrcode() {
        return Result.ok(authService.createWechatBindQrcode());
    }

    /**
     * 解绑当前用户微信
     *
     * @param dto 解绑参数
     * @return 操作结果
     */
    @ApiOperation(value = "解绑当前用户微信")
    @OperLog(title = "账号中心", operType = OperTypeEnum.UPDATE)
    @PostMapping("/wechat-bind/unbind")
    public Result<Void> unbindWechat(@Validated @RequestBody WechatBindUnbindDTO dto) {
        // 调用unbindWechat方法，复用统一能力并保证业务规则一致。
        authService.unbindWechat(dto);
        return Result.ok();
    }

    /**
     * 小程序侧确认绑定并登录
     *
     * @param dto 绑定参数
     * @return 登录结果
     */
    @ApiOperation(value = "小程序侧确认绑定并登录")
    @SaIgnore
    @PostMapping("/mp-bind-confirm")
    public Result<MpLoginVO> confirmWechatBind(@Validated @RequestBody WechatBindConfirmDTO dto) {
        return Result.ok(authService.confirmWechatBind(dto));
    }

    /**
     * 获取当前用户菜单树（动态路由）
     *
     * @return 菜单树
     */
    @ApiOperation(value = "获取当前用户菜单树（动态路由）")
    @GetMapping("/menus")
    public Result<List<SysMenuVO>> getMenus() {
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        Long userId = SecurityContext.getCurrentUserId();
        // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        Long companyId = SecurityContext.getCurrentCompanyId();
        // 调用listMenuTreeByUser方法，复用统一能力并保证业务规则一致。
        List<SysMenuVO> menus = menuService.listMenuTreeByUser(userId, companyId);
        return Result.ok(menus);
    }

    /**
     * 退出登录
     *
     * @return 操作结果
     */
    @ApiOperation(value = "退出登录")
    @OperLog(title = "用户登出", operType = OperTypeEnum.LOGOUT)
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 调用logout方法，复用统一能力并保证业务规则一致。
        authService.logout();
        return Result.ok();
    }
}


