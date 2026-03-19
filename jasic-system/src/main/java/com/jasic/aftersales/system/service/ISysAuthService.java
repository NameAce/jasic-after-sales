package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.dto.LoginDTO;
import com.jasic.aftersales.system.domain.vo.LoginVO;
import com.jasic.aftersales.system.domain.vo.SysUserVO;

/**
 * B端认证服务接口
 *
 * @author Zoro
 * @date 2026/03/18
 */
public interface ISysAuthService {

    /**
     * B端登录
     *
     * @param dto 登录参数
     * @return 登录结果（含 token、用户信息、公司列表等）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 选择/切换公司
     *
     * @param companyId 公司ID
     * @return 用户信息（含当前公司、权限、菜单等）
     */
    SysUserVO chooseCompany(Long companyId);

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    SysUserVO getUserInfo();

    /**
     * 退出登录
     */
    void logout();
}
