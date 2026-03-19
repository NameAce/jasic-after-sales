package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.ChooseCompanyDTO;
import com.jasic.aftersales.system.domain.dto.LoginDTO;
import com.jasic.aftersales.system.domain.vo.LoginVO;
import com.jasic.aftersales.system.domain.vo.SysMenuVO;
import com.jasic.aftersales.system.domain.vo.SysUserVO;
import com.jasic.aftersales.system.service.ISysAuthService;
import com.jasic.aftersales.system.service.ISysMenuService;
import com.jasic.aftersales.framework.security.SecurityContext;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 认证控制器（登录/登出/切换公司）
 *
 * @author Zoro
 * @date 2026/03/18
 */
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
    @SaIgnore
    @OperLog(title = "用户登录", operType = OperTypeEnum.LOGIN)
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        LoginVO vo = authService.login(dto);
        return Result.ok(vo);
    }

    /**
     * 选择/切换公司
     *
     * @param dto 公司选择参数
     * @return 用户信息
     */
    @OperLog(title = "切换公司", operType = OperTypeEnum.OTHER)
    @PostMapping("/choose-company")
    public Result<SysUserVO> chooseCompany(@Validated @RequestBody ChooseCompanyDTO dto) {
        SysUserVO vo = authService.chooseCompany(dto.getCompanyId());
        return Result.ok(vo);
    }

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/user-info")
    public Result<SysUserVO> getUserInfo() {
        SysUserVO vo = authService.getUserInfo();
        return Result.ok(vo);
    }

    /**
     * 获取当前用户菜单树（动态路由）
     *
     * @return 菜单树
     */
    @GetMapping("/menus")
    public Result<List<SysMenuVO>> getMenus() {
        Long userId = SecurityContext.getCurrentUserId();
        Long companyId = SecurityContext.getCurrentCompanyId();
        List<SysMenuVO> menus = menuService.listMenuTreeByUser(userId, companyId);
        return Result.ok(menus);
    }

    /**
     * 退出登录
     *
     * @return 操作结果
     */
    @OperLog(title = "用户登出", operType = OperTypeEnum.LOGOUT)
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }
}
