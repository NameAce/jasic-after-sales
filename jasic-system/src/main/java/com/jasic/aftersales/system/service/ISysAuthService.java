package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.dto.LoginDTO;
import com.jasic.aftersales.system.domain.dto.MpBindLoginDTO;
import com.jasic.aftersales.system.domain.dto.MpLoginDTO;
import com.jasic.aftersales.system.domain.dto.WechatBindConfirmDTO;
import com.jasic.aftersales.system.domain.dto.WechatBindUnbindDTO;
import com.jasic.aftersales.system.domain.dto.ChangePasswordDTO;
import com.jasic.aftersales.system.domain.dto.UpdateProfileDTO;
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
     * B端小程序账号认领绑定并登录
     *
     * @param dto 绑定参数
     * @return 登录结果
     */
    MpLoginVO mpBindLogin(MpBindLoginDTO dto);

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
     * 修改当前用户资料
     *
     * @param dto 资料参数
     * @return 用户信息
     */
    SysUserVO updateProfile(UpdateProfileDTO dto);

    /**
     * 修改当前用户密码
     *
     * @param dto 密码参数
     */
    void changePassword(ChangePasswordDTO dto);

    /**
     * 生成当前用户的微信绑定二维码
     *
     * @return 绑定状态
     */
    WechatBindStatusVO createWechatBindQrcode();

    /**
     * 查询当前用户微信绑定状态
     *
     * @return 绑定状态
     */
    WechatBindStatusVO getWechatBindStatus();

    /**
     * 使用绑定票据确认微信绑定并登录
     *
     * @param dto 绑定参数
     * @return 登录结果
     */
    MpLoginVO confirmWechatBind(WechatBindConfirmDTO dto);

    /**
     * 解绑当前用户微信
     *
     * @param dto 解绑参数
     */
    void unbindWechat(WechatBindUnbindDTO dto);

    /**
     * 退出登录
     */
    void logout();
}
