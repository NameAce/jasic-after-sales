package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.dto.LoginDTO;
import com.jasic.aftersales.system.domain.dto.MpLoginDTO;
import com.jasic.aftersales.system.domain.dto.WechatBindConfirmDTO;
import com.jasic.aftersales.system.domain.vo.LoginVO;
import com.jasic.aftersales.system.domain.vo.MpLoginVO;
import com.jasic.aftersales.system.domain.vo.SysUserVO;
import com.jasic.aftersales.system.domain.vo.WechatBindStatusVO;

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
     * B端小程序登录
     *
     * @param dto 登录参数
     * @return 登录结果
     */
    MpLoginVO mpLogin(MpLoginDTO dto);

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
     * 生成当前用户的微信绑定码
     *
     * @return 绑定状态
     */
    WechatBindStatusVO createWechatBindCode();

    /**
     * 查询当前用户微信绑定状态
     *
     * @return 绑定状态
     */
    WechatBindStatusVO getWechatBindStatus();

    /**
     * 使用绑定码确认微信绑定并登录
     *
     * @param dto 绑定参数
     * @return 登录结果
     */
    MpLoginVO confirmWechatBind(WechatBindConfirmDTO dto);

    /**
     * 退出登录
     */
    void logout();
}
