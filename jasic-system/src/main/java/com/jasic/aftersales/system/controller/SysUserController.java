package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.ResetPwdDTO;
import com.jasic.aftersales.system.domain.dto.SysUserDTO;
import com.jasic.aftersales.system.domain.query.SysUserQuery;
import com.jasic.aftersales.system.domain.vo.SysUserVO;
import com.jasic.aftersales.system.service.ISysUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户管理控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {

    @Resource
    private ISysUserService userService;

    /**
     * 分页查询用户列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @SaCheckPermission("system:user:list")
    @GetMapping("/list")
    public Result<PageResult<SysUserVO>> list(SysUserQuery query) {
        PageResult<SysUserVO> page = userService.listPage(query);
        return Result.ok(page);
    }

    /**
     * 查询用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    @SaCheckPermission("system:user:list")
    @GetMapping("/{userId}")
    public Result<SysUserVO> getById(@PathVariable Long userId) {
        SysUserVO vo = userService.getById(userId);
        return Result.ok(vo);
    }

    /**
     * 新增用户
     *
     * @param dto 用户参数
     * @return 用户ID
     */
    @SaCheckPermission("system:user:add")
    @OperLog(title = "用户管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysUserDTO dto) {
        Long id = userService.save(dto);
        return Result.ok(id);
    }

    /**
     * 修改用户
     *
     * @param dto 用户参数
     * @return 操作结果
     */
    @SaCheckPermission("system:user:update")
    @OperLog(title = "用户管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysUserDTO dto) {
        userService.update(dto);
        return Result.ok();
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @SaCheckPermission("system:user:remove")
    @OperLog(title = "用户管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{userId}")
    public Result<Void> remove(@PathVariable Long userId) {
        userService.remove(userId);
        return Result.ok();
    }

    /**
     * 重置密码
     *
     * @param dto 重置密码参数
     * @return 操作结果
     */
    @SaCheckPermission("system:user:resetPwd")
    @OperLog(title = "用户管理", operType = OperTypeEnum.UPDATE)
    @PutMapping("/reset-pwd")
    public Result<Void> resetPwd(@Validated @RequestBody ResetPwdDTO dto) {
        userService.resetPwd(dto);
        return Result.ok();
    }

    /**
     * 强制下线
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @SaCheckPermission("system:user:kickout")
    @OperLog(title = "用户管理", operType = OperTypeEnum.KICKOUT)
    @PostMapping("/{userId}/kickout")
    public Result<Void> kickout(@PathVariable Long userId) {
        userService.kickout(userId);
        return Result.ok();
    }

    /**
     * 分配用户角色
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     * @return 操作结果
     */
    @SaCheckPermission("system:user:update")
    @OperLog(title = "用户管理", operType = OperTypeEnum.GRANT)
    @PutMapping("/{userId}/roles")
    public Result<Void> assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        userService.assignRoles(userId, roleIds);
        return Result.ok();
    }
}
