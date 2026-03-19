package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.ResetPwdDTO;
import com.jasic.aftersales.system.domain.dto.SysUserDTO;
import com.jasic.aftersales.system.domain.query.SysUserQuery;
import com.jasic.aftersales.system.domain.vo.SysUserVO;

/**
 * 用户管理 Service 接口
 *
 * @author Zoro
 * @date 2026/03/18
 */
public interface ISysUserService {

    /**
     * 分页查询用户列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<SysUserVO> listPage(SysUserQuery query);

    /**
     * 查询用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    SysUserVO getById(Long userId);

    /**
     * 新增用户
     *
     * @param dto 用户参数
     * @return 用户ID
     */
    Long save(SysUserDTO dto);

    /**
     * 修改用户
     *
     * @param dto 用户参数
     */
    void update(SysUserDTO dto);

    /**
     * 删除用户（逻辑删除）
     *
     * @param userId 用户ID
     */
    void remove(Long userId);

    /**
     * 重置密码
     *
     * @param dto 重置密码参数
     */
    void resetPwd(ResetPwdDTO dto);

    /**
     * 强制下线指定用户
     *
     * @param userId 用户ID
     */
    void kickout(Long userId);

    /**
     * 分配用户角色
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     */
    void assignRoles(Long userId, java.util.List<Long> roleIds);
}
