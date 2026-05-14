package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jasic.aftersales.common.constant.CacheConstants;
import com.jasic.aftersales.common.constant.WechatConfigConstants;
import com.jasic.aftersales.common.enums.DataScopeEnum;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.framework.web.ResultCode;
import com.jasic.aftersales.system.domain.dto.ChangePasswordDTO;
import com.jasic.aftersales.system.domain.dto.LoginDTO;
import com.jasic.aftersales.system.domain.dto.MpBindLoginDTO;
import com.jasic.aftersales.system.domain.dto.MpLoginDTO;
import com.jasic.aftersales.system.domain.dto.UpdateProfileDTO;
import com.jasic.aftersales.system.domain.dto.WechatBindConfirmDTO;
import com.jasic.aftersales.system.domain.dto.WechatBindUnbindDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysMenu;
import com.jasic.aftersales.system.domain.entity.SysRole;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import com.jasic.aftersales.system.domain.entity.WechatBindRecord;
import com.jasic.aftersales.system.domain.enums.WechatMiniProgramScene;
import com.jasic.aftersales.system.domain.model.WechatAuthSession;
import com.jasic.aftersales.system.domain.model.WechatBindSession;
import com.jasic.aftersales.system.domain.model.WechatPhoneInfo;
import com.jasic.aftersales.system.domain.vo.LoginVO;
import com.jasic.aftersales.system.domain.vo.MpLoginVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.domain.vo.SysPermissionVO;
import com.jasic.aftersales.system.domain.vo.SysRoleVO;
import com.jasic.aftersales.system.domain.vo.SysUserVO;
import com.jasic.aftersales.system.domain.vo.WechatBindStatusVO;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysCompanyTypeMapper;
import com.jasic.aftersales.system.mapper.SysMenuMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.mapper.SysRoleMapper;
import com.jasic.aftersales.system.mapper.SysUserRoleMapper;
import com.jasic.aftersales.system.mapper.WechatBindRecordMapper;
import com.jasic.aftersales.system.service.ISysAuthService;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.ISysRegionService;
import com.jasic.aftersales.system.service.SysPermissionService;
import com.jasic.aftersales.system.service.WechatMiniProgramService;
import com.jasic.aftersales.system.service.support.SysUserIdentityValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * B端认证服务实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Slf4j
@Service
public class SysAuthServiceImpl implements ISysAuthService {

    private static final int BIND_TICKET_EXPIRE_MINUTES = 10;
    private static final String WECHAT_STATUS_BIND = "BIND";
    private static final String WECHAT_STATUS_UNBIND = "UNBIND";
    private static final String WECHAT_OPERATE_SOURCE_MP_BIND_LOGIN = "MP_BIND_LOGIN";
    private static final String WECHAT_OPERATE_SOURCE_PC_QR_BIND = "PC_QR_BIND";
    private static final String WECHAT_OPERATE_SOURCE_PC_SELF_UNBIND = "PC_SELF_UNBIND";

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysUserCompanyMapper sysUserCompanyMapper;

    @Resource
    private SysCompanyTypeMapper sysCompanyTypeMapper;

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionService sysPermissionService;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private ISysRegionService regionService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private WechatMiniProgramService wechatMiniProgramService;

    /**
     * 系统用户身份校验字段。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Resource
    private SysUserIdentityValidator userIdentityValidator;

    @Resource
    private WechatBindRecordMapper wechatBindRecordMapper;

    /**
     * B端登录
     *
     * @param dto 登录参数
     * @return 登录结果（含 token、用户信息、公司列表等）
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        // 调用getUsername方法，复用统一能力并保证业务规则一致。
        SysUser user = findByLoginIdentity(dto.getUsername());
        if (user == null) {
            throw new ServiceException(ResultCode.LOGIN_ERROR, "用户名或密码错误");
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new ServiceException(ResultCode.LOGIN_ERROR, "用户名或密码错误");
        }
        // 说明：执行该步骤以保证业务流程正确。
        ensureUserActive(user);
        return doLogin(user);
    }

    /**
     * B端小程序登录
     *
     * @param dto 登录参数
     * @return 登录结果
     */
    @Override
    public MpLoginVO mpLogin(MpLoginDTO dto) {
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        WechatAuthSession session = wechatMiniProgramService.code2Session(WechatMiniProgramScene.B, dto.getCode());
        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        SysUser user = findByOpenid(session.getOpenid());
        if (user == null) {
            // 调用MpLoginVO方法，复用统一能力并保证业务规则一致。
            MpLoginVO vo = new MpLoginVO();
            // 调用setStatus方法，复用统一能力并保证业务规则一致。
            vo.setStatus(WECHAT_STATUS_UNBIND);
            // 调用setNeedChooseCompany方法，复用统一能力并保证业务规则一致。
            vo.setNeedChooseCompany(false);
            return vo;
        }
        // 说明：执行该步骤以保证业务流程正确。
        ensureUserActive(user);
        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        refreshWechatIdentity(user.getId(), session.getOpenid(), null);
        return buildMpLoginVO(doLogin(requireActiveUser(user.getId())));
    }

    /**
     * B端小程序账号认领绑定并登录
     *
     * @param dto 绑定参数
     * @return 登录结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MpLoginVO mpBindLogin(MpBindLoginDTO dto) {
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        WechatAuthSession authSession = wechatMiniProgramService.code2Session(WechatMiniProgramScene.B, dto.getCode());
        // 调用getUsernameOrPhone方法，复用统一能力并保证业务规则一致。
        SysUser user = findByLoginIdentity(dto.getUsernameOrPhone());
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new ServiceException(ResultCode.LOGIN_ERROR, "用户名或密码错误");
        }
        // 说明：执行该步骤以保证业务流程正确。
        ensureUserActive(user);
        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        validateWechatBinding(user, authSession.getOpenid());

        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        boolean wasBound = StrUtil.isNotBlank(user.getOpenid());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        String wechatPhone = resolveWechatPhone(dto.getPhoneCode(), user.getId());
        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        refreshWechatIdentity(user.getId(), authSession.getOpenid(), wechatPhone);
        if (!wasBound) {
            saveWechatBindRecord(user, WECHAT_STATUS_BIND, WECHAT_OPERATE_SOURCE_MP_BIND_LOGIN,
                    // 调用resolveRecordWechatPhone方法，复用统一能力并保证业务规则一致。
                    authSession.getOpenid(), resolveRecordWechatPhone(user, wechatPhone));
        }
        // 调用getId方法，复用统一能力并保证业务规则一致。
        clearBindSession(getBindSession(user.getId()));
        return buildMpLoginVO(doLogin(requireActiveUser(user.getId())));
    }

    /**
     * 选择/切换公司
     *
     * @param companyId 公司ID
     * @return 用户信息（含当前公司、权限、菜单等）
     */
    @Override
    public SysUserVO chooseCompany(Long companyId) {
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        Long userId = SecurityContext.getCurrentUserId();

        LambdaQueryWrapper<SysUserCompany> ucQuery = new LambdaQueryWrapper<>();
        ucQuery.eq(SysUserCompany::getUserId, userId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysUserCompany::getCompanyId, companyId);
        // 说明：执行该步骤以保证业务流程正确。
        SysUserCompany userCompany = sysUserCompanyMapper.selectOne(ucQuery);
        if (userCompany == null) {
            throw new ServiceException(ResultCode.NOT_PERMISSION, "无权限操作该公司");
        }

        // 调用selectById方法，复用统一能力并保证业务规则一致。
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "公司不存在");
        }

        LambdaQueryWrapper<SysCompanyType> typeQuery = new LambdaQueryWrapper<>();
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        typeQuery.eq(SysCompanyType::getTypeCode, company.getTypeCode());
        // 调用selectOne方法，复用统一能力并保证业务规则一致。
        SysCompanyType companyType = sysCompanyTypeMapper.selectOne(typeQuery);

        // 调用setCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        SecurityContext.setCurrentCompanyId(companyId);
        // 调用getSubjectType方法，复用统一能力并保证业务规则一致。
        SecurityContext.setCurrentSubjectType(companyType != null ? companyType.getSubjectType() : null);
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        SecurityContext.setCurrentTypeCode(company.getTypeCode());
        // 调用initDataScopeContext方法，复用统一能力并保证业务规则一致。
        initDataScopeContext(userId, companyId, companyType);

        // 说明：执行该步骤以保证业务流程正确。
        Set<String> perms = sysPermissionService.loadPermsToCache(userId, companyId);
        // 调用selectMenuTreeByUserIdAndCompanyId方法，复用统一能力并保证业务规则一致。
        sysMenuMapper.selectMenuTreeByUserIdAndCompanyId(userId, companyId);

        // 调用selectById方法，复用统一能力并保证业务规则一致。
        SysUser user = sysUserMapper.selectById(userId);
        return buildSysUserVO(user, company, companyType, userId, perms);
    }

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    @Override
    public SysUserVO getUserInfo() {
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        Long userId = SecurityContext.getCurrentUserId();
        // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        Long companyId = SecurityContext.getCurrentCompanyId();

        // 说明：执行该步骤以保证业务流程正确。
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }

        // 调用buildBasicUserVO方法，复用统一能力并保证业务规则一致。
        SysUserVO vo = buildBasicUserVO(user);
        if (companyId != null) {
            // 调用selectById方法，复用统一能力并保证业务规则一致。
            SysCompany company = sysCompanyMapper.selectById(companyId);
            if (company != null) {
                // 调用getId方法，复用统一能力并保证业务规则一致。
                vo.setCurrentCompanyId(company.getId());
                // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
                vo.setCurrentCompanyName(company.getCompanyName());
                // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
                vo.setCurrentTypeCode(company.getTypeCode());

                LambdaQueryWrapper<SysCompanyType> typeQuery = new LambdaQueryWrapper<>();
                // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
                typeQuery.eq(SysCompanyType::getTypeCode, company.getTypeCode());
                // 调用selectOne方法，复用统一能力并保证业务规则一致。
                SysCompanyType companyType = sysCompanyTypeMapper.selectOne(typeQuery);
                if (companyType != null) {
                    // 调用getSubjectType方法，复用统一能力并保证业务规则一致。
                    vo.setCurrentSubjectType(companyType.getSubjectType());
                }

                // 调用loadCurrentPerms方法，复用统一能力并保证业务规则一致。
                vo.setPerms(loadCurrentPerms(userId, companyId));
                // 调用buildCurrentPermissionVos方法，复用统一能力并保证业务规则一致。
                vo.setPermissionVos(buildCurrentPermissionVos(userId, companyId));
                // 调用buildCurrentCompanyRoles方法，复用统一能力并保证业务规则一致。
                vo.setRoles(buildCurrentCompanyRoles(userId, companyId));
            }
        }

        LambdaQueryWrapper<SysUserCompany> ucQuery = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        ucQuery.eq(SysUserCompany::getUserId, userId);
        // 调用selectList方法，复用统一能力并保证业务规则一致。
        List<SysUserCompany> userCompanies = sysUserCompanyMapper.selectList(ucQuery);
        if (userCompanies != null && !userCompanies.isEmpty()) {
            List<Long> companyIds = userCompanies.stream()
                    .map(SysUserCompany::getCompanyId)
                    // 调用toList方法，复用统一能力并保证业务规则一致。
                    .collect(Collectors.toList());
            // 调用buildCompanySimpleList方法，复用统一能力并保证业务规则一致。
            vo.setCompanies(buildCompanySimpleList(companyIds));
        }
        return vo;
    }

    /**
     * 修改当前用户资料
     *
     * @param dto 资料参数
     * @return 用户信息
     */
    @Override
    public SysUserVO updateProfile(UpdateProfileDTO dto) {
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        Long userId = SecurityContext.getCurrentUserId();
        // 说明：执行该步骤以保证业务流程正确。
        SysUser user = requireActiveUser(userId);
        // 调用getCurrentPassword方法，复用统一能力并保证业务规则一致。
        verifyCurrentPassword(user, dto.getCurrentPassword());

        // 调用getRealName方法，复用统一能力并保证业务规则一致。
        String realName = StrUtil.trim(dto.getRealName());
        // 调用getPhone方法，复用统一能力并保证业务规则一致。
        String phone = StrUtil.trim(dto.getPhone());
        // 调用getEmail方法，复用统一能力并保证业务规则一致。
        String email = StrUtil.trim(dto.getEmail());
        // 调用getUsername方法，复用统一能力并保证业务规则一致。
        userIdentityValidator.validateLoginIdentityUnique(userId, user.getUsername(), phone);

        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, userId)
                .set(SysUser::getRealName, realName)
                .set(SysUser::getPhone, phone)
                // 调用isBlank方法，复用统一能力并保证业务规则一致。
                .set(SysUser::getEmail, StrUtil.isBlank(email) ? null : email);
        // 说明：执行该步骤以保证业务流程正确。
        sysUserMapper.update(null, updateWrapper);
        return getUserInfo();
    }

    /**
     * 修改当前用户密码
     *
     * @param dto 密码参数
     */
    @Override
    public void changePassword(ChangePasswordDTO dto) {
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        Long userId = SecurityContext.getCurrentUserId();
        // 说明：执行该步骤以保证业务流程正确。
        SysUser user = requireActiveUser(userId);
        // 调用getCurrentPassword方法，复用统一能力并保证业务规则一致。
        verifyCurrentPassword(user, dto.getCurrentPassword());
        if (BCrypt.checkpw(dto.getNewPassword(), user.getPassword())) {
            throw new ServiceException("新密码不能与当前密码相同");
        }

        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, userId)
                // 调用gensalt方法，复用统一能力并保证业务规则一致。
                .set(SysUser::getPassword, BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        // 说明：执行该步骤以保证业务流程正确。
        sysUserMapper.update(null, updateWrapper);
        // 说明：执行该步骤以保证业务流程正确。
        sysPermissionService.clearAllPermsCache(userId);
        // 调用kickout方法，复用统一能力并保证业务规则一致。
        StpUtil.kickout(userId);
    }

    /**
     * 生成当前用户的微信绑定二维码
     *
     * @return 绑定状态
     */
    @Override
    public WechatBindStatusVO createWechatBindQrcode() {
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        Long userId = SecurityContext.getCurrentUserId();
        // 说明：执行该步骤以保证业务流程正确。
        SysUser user = requireActiveUser(userId);
        // 调用getBindSession方法，复用统一能力并保证业务规则一致。
        WechatBindSession oldSession = getBindSession(userId);
        if (StrUtil.isNotBlank(user.getOpenid())) {
            // 调用clearBindSession方法，复用统一能力并保证业务规则一致。
            clearBindSession(oldSession);
            return buildWechatBindStatus(user, null);
        }

        if (oldSession != null) {
            // 调用clearBindSession方法，复用统一能力并保证业务规则一致。
            clearBindSession(oldSession);
        }

        // 调用WechatBindSession方法，复用统一能力并保证业务规则一致。
        WechatBindSession session = new WechatBindSession();
        // 调用setUserId方法，复用统一能力并保证业务规则一致。
        session.setUserId(userId);
        // 调用generateBindTicket方法，复用统一能力并保证业务规则一致。
        session.setBindTicket(generateBindTicket());
        // 调用plusMinutes方法，复用统一能力并保证业务规则一致。
        session.setExpireAt(LocalDateTime.now().plusMinutes(BIND_TICKET_EXPIRE_MINUTES));
        // 调用saveBindSession方法，复用统一能力并保证业务规则一致。
        saveBindSession(session);
        try {
            // 调用getValueByKey方法，复用统一能力并保证业务规则一致。
            String pagePath = StrUtil.trim(sysConfigService.getValueByKey(WechatConfigConstants.B_BIND_PAGE_PATH));
            // 调用buildWechatBindStatus方法，复用统一能力并保证业务规则一致。
            WechatBindStatusVO vo = buildWechatBindStatus(user, session);
            vo.setQrImageBase64(wechatMiniProgramService.createQrcodeBase64(
                    // 调用getBindTicket方法，复用统一能力并保证业务规则一致。
                    WechatMiniProgramScene.B, session.getBindTicket(), pagePath));
            return vo;
        } catch (RuntimeException ex) {
            // 调用clearBindSession方法，复用统一能力并保证业务规则一致。
            clearBindSession(session);
            throw ex;
        }
    }

    /**
     * 查询当前用户微信绑定状态
     *
     * @return 绑定状态
     */
    @Override
    public WechatBindStatusVO getWechatBindStatus() {
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        Long userId = SecurityContext.getCurrentUserId();
        // 说明：执行该步骤以保证业务流程正确。
        SysUser user = requireActiveUser(userId);
        // 调用getBindSession方法，复用统一能力并保证业务规则一致。
        WechatBindSession session = getBindSession(userId);
        if (StrUtil.isNotBlank(user.getOpenid())) {
            // 调用clearBindSession方法，复用统一能力并保证业务规则一致。
            clearBindSession(session);
            return buildWechatBindStatus(user, null);
        }
        return buildWechatBindStatus(user, session);
    }

    /**
     * 使用绑定票据确认微信绑定并登录
     *
     * @param dto 绑定参数
     * @return 登录结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MpLoginVO confirmWechatBind(WechatBindConfirmDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        WechatBindSession bindSession = requireBindSession(dto.getBindTicket());
        // 调用getUserId方法，复用统一能力并保证业务规则一致。
        SysUser user = requireActiveUser(bindSession.getUserId());
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        WechatAuthSession authSession = wechatMiniProgramService.code2Session(WechatMiniProgramScene.B, dto.getCode());
        if (StrUtil.isNotBlank(user.getOpenid()) && !StrUtil.equals(user.getOpenid(), authSession.getOpenid())) {
            // 调用clearBindSession方法，复用统一能力并保证业务规则一致。
            clearBindSession(bindSession);
            throw new ServiceException("当前账号已绑定微信");
        }
        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        validateWechatBinding(user, authSession.getOpenid());

        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        boolean wasBound = StrUtil.isNotBlank(user.getOpenid());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        String wechatPhone = resolveWechatPhone(dto.getPhoneCode(), user.getId());
        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        refreshWechatIdentity(user.getId(), authSession.getOpenid(), wechatPhone);
        if (!wasBound) {
            saveWechatBindRecord(user, WECHAT_STATUS_BIND, WECHAT_OPERATE_SOURCE_PC_QR_BIND,
                    // 调用resolveRecordWechatPhone方法，复用统一能力并保证业务规则一致。
                    authSession.getOpenid(), resolveRecordWechatPhone(user, wechatPhone));
        }
        // 调用clearBindSession方法，复用统一能力并保证业务规则一致。
        clearBindSession(bindSession);
        return buildMpLoginVO(doLogin(requireActiveUser(user.getId())));
    }

    /**
     * 解绑当前用户微信
     *
     * @param dto 解绑参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindWechat(WechatBindUnbindDTO dto) {
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        Long userId = SecurityContext.getCurrentUserId();
        // 说明：执行该步骤以保证业务流程正确。
        SysUser user = requireActiveUser(userId);
        // 调用getCurrentPassword方法，复用统一能力并保证业务规则一致。
        verifyCurrentPassword(user, dto.getCurrentPassword());
        if (StrUtil.isBlank(user.getOpenid())) {
            throw new ServiceException("当前账号未绑定微信");
        }

        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        String openid = user.getOpenid();
        // 调用getWechatPhone方法，复用统一能力并保证业务规则一致。
        String wechatPhone = user.getWechatPhone();
        // 调用getBindSession方法，复用统一能力并保证业务规则一致。
        clearBindSession(getBindSession(userId));
        // 调用clearWechatIdentity方法，复用统一能力并保证业务规则一致。
        clearWechatIdentity(userId);
        // 调用saveWechatBindRecord方法，复用统一能力并保证业务规则一致。
        saveWechatBindRecord(user, WECHAT_STATUS_UNBIND, WECHAT_OPERATE_SOURCE_PC_SELF_UNBIND, openid, wechatPhone);
        // 调用kickout方法，复用统一能力并保证业务规则一致。
        StpUtil.kickout(userId);
    }

    /**
     * 退出登录
     */
    @Override
    public void logout() {
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        Long userId = SecurityContext.getCurrentUserId();
        // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        Long companyId = SecurityContext.getCurrentCompanyId();
        if (companyId != null) {
            // 说明：执行该步骤以保证业务流程正确。
            sysPermissionService.clearPermsCache(userId, companyId);
        }
        // 调用logout方法，复用统一能力并保证业务规则一致。
        StpUtil.logout();
    }

    /**
     * 初始化当前公司下的数据权限上下文
     *
     * @param userId      用户ID
     * @param companyId   公司ID
     * @param companyType 公司类型
     */
    private void initDataScopeContext(Long userId, Long companyId, SysCompanyType companyType) {
        // 调用getSubjectType方法，复用统一能力并保证业务规则一致。
        String subjectType = companyType != null ? companyType.getSubjectType() : null;
        // 调用resolveEffectiveDataScope方法，复用统一能力并保证业务规则一致。
        DataScopeEnum effectiveDataScope = resolveEffectiveDataScope(userId, companyId, subjectType);
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        SecurityContext.setEffectiveDataScope(effectiveDataScope.getCode());
        // 调用resolveCurrentRegionIds方法，复用统一能力并保证业务规则一致。
        SecurityContext.setCurrentRegionIds(resolveCurrentRegionIds(userId, companyId, subjectType, effectiveDataScope));
    }

    /**
     * 计算当前公司下的有效数据范围
     *
     * @param userId      用户ID
     * @param companyId   公司ID
     * @param subjectType 主体类型
     * @return 有效数据范围
     */
    private DataScopeEnum resolveEffectiveDataScope(Long userId, Long companyId, String subjectType) {
        if (subjectType == null) {
            return DataScopeEnum.SELF;
        }
        if (SubjectTypeEnum.PLATFORM.getCode().equals(subjectType)) {
            return DataScopeEnum.ALL;
        }
        // 说明：执行该步骤以保证业务流程正确。
        return sysPermissionService.getEffectiveDataScope(userId, companyId, subjectType);
    }

    /**
     * 计算当前公司下的负责大区列表
     *
     * @param userId             用户ID
     * @param companyId          公司ID
     * @param subjectType        主体类型
     * @param effectiveDataScope 有效数据范围
     * @return 大区ID列表
     */
    private List<Long> resolveCurrentRegionIds(Long userId, Long companyId, String subjectType,
                                               DataScopeEnum effectiveDataScope) {
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectType)) {
            return Collections.emptyList();
        }
        if (effectiveDataScope != DataScopeEnum.REGION) {
            return Collections.emptyList();
        }
        return regionService.listRegionIdsByUserIdAndCompanyId(userId, companyId);
    }

    /**
     * 根据公司ID列表构建公司简要信息列表
     */
    private List<SysCompanySimpleVO> buildCompanySimpleList(List<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysCompanySimpleVO> result = new ArrayList<>();
        for (Long companyId : companyIds) {
            // 说明：执行该步骤以保证业务流程正确。
            SysCompany company = sysCompanyMapper.selectById(companyId);
            if (company == null) {
                continue;
            }
            // 调用SysCompanySimpleVO方法，复用统一能力并保证业务规则一致。
            SysCompanySimpleVO vo = new SysCompanySimpleVO();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setId(company.getId());
            // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
            vo.setCompanyName(company.getCompanyName());
            // 调用getCompanyCode方法，复用统一能力并保证业务规则一致。
            vo.setCompanyCode(company.getCompanyCode());
            // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
            vo.setTypeCode(company.getTypeCode());

            LambdaQueryWrapper<SysCompanyType> typeQuery = new LambdaQueryWrapper<>();
            // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
            typeQuery.eq(SysCompanyType::getTypeCode, company.getTypeCode());
            // 调用selectOne方法，复用统一能力并保证业务规则一致。
            SysCompanyType companyType = sysCompanyTypeMapper.selectOne(typeQuery);
            if (companyType != null) {
                // 调用getTypeName方法，复用统一能力并保证业务规则一致。
                vo.setTypeName(companyType.getTypeName());
            }
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.add(vo);
        }
        return result;
    }

    /**
     * 构建基础用户VO（不含公司、权限等）
     */
    private SysUserVO buildBasicUserVO(SysUser user) {
        // 调用SysUserVO方法，复用统一能力并保证业务规则一致。
        SysUserVO vo = new SysUserVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setId(user.getId());
        // 调用getUsername方法，复用统一能力并保证业务规则一致。
        vo.setUsername(user.getUsername());
        // 调用getRealName方法，复用统一能力并保证业务规则一致。
        vo.setRealName(user.getRealName());
        // 调用getPhone方法，复用统一能力并保证业务规则一致。
        vo.setPhone(user.getPhone());
        // 调用getEmail方法，复用统一能力并保证业务规则一致。
        vo.setEmail(user.getEmail());
        // 调用getAvatar方法，复用统一能力并保证业务规则一致。
        vo.setAvatar(user.getAvatar());
        // 调用getSex方法，复用统一能力并保证业务规则一致。
        vo.setSex(user.getSex());
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        vo.setStatus(user.getStatus());
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        vo.setRemark(user.getRemark());
        // 调用getCreateTime方法，复用统一能力并保证业务规则一致。
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 构建完整用户VO（含当前公司、权限等）
     */
    private SysUserVO buildSysUserVO(SysUser user, SysCompany company, SysCompanyType companyType,
                                     Long userId, Set<String> perms) {
        // 调用buildBasicUserVO方法，复用统一能力并保证业务规则一致。
        SysUserVO vo = buildBasicUserVO(user);
        if (company != null) {
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setCurrentCompanyId(company.getId());
            // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
            vo.setCurrentCompanyName(company.getCompanyName());
            // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
            vo.setCurrentTypeCode(company.getTypeCode());
        }
        if (companyType != null) {
            // 调用getSubjectType方法，复用统一能力并保证业务规则一致。
            vo.setCurrentSubjectType(companyType.getSubjectType());
        }
        // 调用setPerms方法，复用统一能力并保证业务规则一致。
        vo.setPerms(perms);
        if (company != null) {
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setPermissionVos(buildCurrentPermissionVos(userId, company.getId()));
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setRoles(buildCurrentCompanyRoles(userId, company.getId()));
        }

        LambdaQueryWrapper<SysUserCompany> ucQuery = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        ucQuery.eq(SysUserCompany::getUserId, userId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysUserCompany> userCompanies = sysUserCompanyMapper.selectList(ucQuery);
        if (userCompanies != null && !userCompanies.isEmpty()) {
            List<Long> companyIds = userCompanies.stream()
                    .map(SysUserCompany::getCompanyId)
                    // 调用toList方法，复用统一能力并保证业务规则一致。
                    .collect(Collectors.toList());
            // 调用buildCompanySimpleList方法，复用统一能力并保证业务规则一致。
            vo.setCompanies(buildCompanySimpleList(companyIds));
        }
        return vo;
    }

    /**
     * 优先读取缓存中的权限标识，缺失时回源并重建缓存。
     */
    private Set<String> loadCurrentPerms(Long userId, Long companyId) {
        if (userId == null || companyId == null) {
            return Collections.emptySet();
        }
        String permsKey = CacheConstants.USER_PERMS_KEY + userId + ":" + companyId;
        // 调用members方法，复用统一能力并保证业务规则一致。
        Set<Object> permObjects = redisTemplate.opsForSet().members(permsKey);
        if (permObjects != null && !permObjects.isEmpty()) {
            return permObjects.stream()
                    .map(String::valueOf)
                    // 调用toSet方法，复用统一能力并保证业务规则一致。
                    .collect(Collectors.toSet());
        }
        // 说明：执行该步骤以保证业务流程正确。
        return sysPermissionService.loadPermsToCache(userId, companyId);
    }

    /**
     * 构建当前公司下的轻量权限项集合。
     */
    private List<SysPermissionVO> buildCurrentPermissionVos(Long userId, Long companyId) {
        if (userId == null || companyId == null) {
            return Collections.emptyList();
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<SysMenu> permissionMenus = sysMenuMapper.selectPermissionMenusByUserIdAndCompanyId(userId, companyId);
        if (permissionMenus == null || permissionMenus.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionMenus.stream()
                .map(this::convertPermissionToVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * convert权限To视图。
     *
     * @param menu 参数
     * @return 处理结果
     */
    private SysPermissionVO convertPermissionToVO(SysMenu menu) {
        // 调用SysPermissionVO方法，复用统一能力并保证业务规则一致。
        SysPermissionVO vo = new SysPermissionVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setId(menu.getId());
        // 调用getMenuName方法，复用统一能力并保证业务规则一致。
        vo.setMenuName(menu.getMenuName());
        // 调用getParentId方法，复用统一能力并保证业务规则一致。
        vo.setParentId(menu.getParentId());
        // 调用getMenuType方法，复用统一能力并保证业务规则一致。
        vo.setMenuType(menu.getMenuType());
        // 调用getPerms方法，复用统一能力并保证业务规则一致。
        vo.setPerms(menu.getPerms());
        return vo;
    }

    /**
     * doLogin。
     *
     * @param user 参数
     * @return 处理结果
     */
    private LoginVO doLogin(SysUser user) {
        // 调用getId方法，复用统一能力并保证业务规则一致。
        StpUtil.login(user.getId());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        touchLastLoginTime(user.getId());

        // 调用getId方法，复用统一能力并保证业务规则一致。
        List<SysCompanySimpleVO> companies = listUserCompanies(user.getId());
        if (companies.isEmpty()) {
            throw new ServiceException(ResultCode.USER_ERROR, "用户未关联任何公司");
        }

        // 调用LoginVO方法，复用统一能力并保证业务规则一致。
        LoginVO loginVO = new LoginVO();
        // 调用getTokenValue方法，复用统一能力并保证业务规则一致。
        loginVO.setToken(StpUtil.getTokenValue());
        if (companies.size() == 1) {
            // 调用getId方法，复用统一能力并保证业务规则一致。
            SysUserVO userInfo = chooseCompany(companies.get(0).getId());
            // 调用setUserInfo方法，复用统一能力并保证业务规则一致。
            loginVO.setUserInfo(userInfo);
            // 调用setNeedChooseCompany方法，复用统一能力并保证业务规则一致。
            loginVO.setNeedChooseCompany(false);
            // 调用setCompanies方法，复用统一能力并保证业务规则一致。
            loginVO.setCompanies(null);
        } else {
            // 调用buildBasicUserVO方法，复用统一能力并保证业务规则一致。
            loginVO.setUserInfo(buildBasicUserVO(user));
            // 调用setNeedChooseCompany方法，复用统一能力并保证业务规则一致。
            loginVO.setNeedChooseCompany(true);
            // 调用setCompanies方法，复用统一能力并保证业务规则一致。
            loginVO.setCompanies(companies);
        }
        return loginVO;
    }

    /**
     * 分页查询用户Companies列表。
     *
     * @return 处理结果
     */
    private List<SysCompanySimpleVO> listUserCompanies(Long userId) {
        LambdaQueryWrapper<SysUserCompany> query = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        query.eq(SysUserCompany::getUserId, userId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysUserCompany> relations = sysUserCompanyMapper.selectList(query);
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }
        return buildCompanySimpleList(relations.stream()
                .map(SysUserCompany::getCompanyId)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList()));
    }

    /**
     * touchLastLoginTime。
     */
    private void touchLastLoginTime(Long userId) {
        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, userId)
                // 调用now方法，复用统一能力并保证业务规则一致。
                .set(SysUser::getLastLoginTime, LocalDateTime.now());
        // 说明：执行该步骤以保证业务流程正确。
        sysUserMapper.update(null, updateWrapper);
    }

    /**
     * requireActive用户。
     *
     * @return 处理结果
     */
    private SysUser requireActiveUser(Long userId) {
        // 说明：执行该步骤以保证业务流程正确。
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        // 说明：执行该步骤以保证业务流程正确。
        ensureUserActive(user);
        return user;
    }

    /**
     * ensure用户Active。
     *
     * @param user 参数
     */
    private void ensureUserActive(SysUser user) {
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new ServiceException(ResultCode.ACCOUNT_DISABLED, "账号已停用");
        }
    }

    /**
     * verifyCurrentPassword。
     *
     * @param user 参数
     * @param currentPassword 参数
     */
    private void verifyCurrentPassword(SysUser user, String currentPassword) {
        if (!BCrypt.checkpw(currentPassword, user.getPassword())) {
            throw new ServiceException(ResultCode.LOGIN_ERROR, "当前密码错误");
        }
    }

    /**
     * findByOpenid。
     *
     * @return 处理结果
     */
    private SysUser findByOpenid(String openid) {
        if (StrUtil.isBlank(openid)) {
            return null;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysUser::getOpenid, openid);
        // 说明：执行该步骤以保证业务流程正确。
        return sysUserMapper.selectOne(wrapper);
    }

    /**
     * findByLogin身份。
     *
     * @param loginIdentity 参数
     * @return 处理结果
     */
    private SysUser findByLoginIdentity(String loginIdentity) {
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trim(loginIdentity);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.and(q -> q.eq(SysUser::getUsername, normalized).or().eq(SysUser::getPhone, normalized));
        // 说明：执行该步骤以保证业务流程正确。
        List<SysUser> users = sysUserMapper.selectList(wrapper);
        if (users == null || users.isEmpty()) {
            return null;
        }
        if (users.size() > 1) {
            throw new ServiceException("登录标识存在冲突，请联系管理员处理");
        }
        return users.get(0);
    }

    /**
     * 构建Current公司Roles。
     *
     * @return 处理结果
     */
    private List<SysRoleVO> buildCurrentCompanyRoles(Long userId, Long companyId) {
        if (userId == null || companyId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysUserRole> userRoleQuery = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        userRoleQuery.eq(SysUserRole::getUserId, userId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleQuery);
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        LambdaQueryWrapper<SysRole> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.in(SysRole::getId, roleIds)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysRole::getCompanyId, companyId);
        // 调用selectList方法，复用统一能力并保证业务规则一致。
        List<SysRole> roles = sysRoleMapper.selectList(roleQuery);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(this::convertRoleToVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * convert角色To视图。
     *
     * @param role 参数
     * @return 处理结果
     */
    private SysRoleVO convertRoleToVO(SysRole role) {
        // 调用SysRoleVO方法，复用统一能力并保证业务规则一致。
        SysRoleVO vo = new SysRoleVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setId(role.getId());
        // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
        vo.setCompanyId(role.getCompanyId());
        // 调用getRoleName方法，复用统一能力并保证业务规则一致。
        vo.setRoleName(role.getRoleName());
        // 调用getRoleKey方法，复用统一能力并保证业务规则一致。
        vo.setRoleKey(role.getRoleKey());
        // 调用getDataScope方法，复用统一能力并保证业务规则一致。
        vo.setDataScope(role.getDataScope());
        // 调用getRoleType方法，复用统一能力并保证业务规则一致。
        vo.setRoleType(role.getRoleType());
        // 调用getIsSystem方法，复用统一能力并保证业务规则一致。
        vo.setIsSystem(role.getIsSystem());
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        vo.setStatus(role.getStatus());
        // 调用getOrderNum方法，复用统一能力并保证业务规则一致。
        vo.setOrderNum(role.getOrderNum());
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        vo.setRemark(role.getRemark());
        // 调用getCreateTime方法，复用统一能力并保证业务规则一致。
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }

    /**
     * refresh微信身份。
     *
     * @param wechatPhone 参数
     */
    private void refreshWechatIdentity(Long userId, String openid, String wechatPhone) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
                // 调用set方法，复用统一能力并保证业务规则一致。
                .set(SysUser::getOpenid, openid);
        if (StrUtil.isNotBlank(wechatPhone)) {
            // 调用set方法，复用统一能力并保证业务规则一致。
            wrapper.set(SysUser::getWechatPhone, wechatPhone);
        }
        // 说明：执行该步骤以保证业务流程正确。
        sysUserMapper.update(null, wrapper);
    }

    /**
     * clear微信身份。
     */
    private void clearWechatIdentity(Long userId) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
                .set(SysUser::getOpenid, null)
                // 调用set方法，复用统一能力并保证业务规则一致。
                .set(SysUser::getWechatPhone, null);
        // 说明：执行该步骤以保证业务流程正确。
        sysUserMapper.update(null, wrapper);
    }

    /**
     * 校验微信绑定ing。
     *
     * @param user 参数
     */
    private void validateWechatBinding(SysUser user, String openid) {
        if (StrUtil.isBlank(openid)) {
            throw new ServiceException("微信登录失败，未获取到用户标识");
        }
        if (StrUtil.isNotBlank(user.getOpenid()) && !StrUtil.equals(user.getOpenid(), openid)) {
            throw new ServiceException("当前账号已绑定微信");
        }
        // 调用findByOpenid方法，复用统一能力并保证业务规则一致。
        SysUser boundUser = findByOpenid(openid);
        if (boundUser != null && !boundUser.getId().equals(user.getId())) {
            throw new ServiceException("该微信已绑定其他账号，请联系管理员");
        }
    }

    /**
     * 解析微信Phone。
     *
     * @param phoneCode 参数
     * @return 处理结果
     */
    private String resolveWechatPhone(String phoneCode, Long userId) {
        if (StrUtil.isBlank(phoneCode)) {
            return null;
        }
        try {
            // 调用getPhoneNumber方法，复用统一能力并保证业务规则一致。
            WechatPhoneInfo phoneInfo = wechatMiniProgramService.getPhoneNumber(WechatMiniProgramScene.B, phoneCode);
            return StrUtil.blankToDefault(phoneInfo.getPhoneNumber(), phoneInfo.getPurePhoneNumber());
        } catch (Exception ex) {
            // 调用warn方法，复用统一能力并保证业务规则一致。
            log.warn("获取 B 端微信手机号失败，userId={}", userId, ex);
            return null;
        }
    }

    /**
     * 解析Record微信Phone。
     *
     * @param user 参数
     * @param latestWechatPhone 参数
     * @return 处理结果
     */
    private String resolveRecordWechatPhone(SysUser user, String latestWechatPhone) {
        if (StrUtil.isNotBlank(latestWechatPhone)) {
            return latestWechatPhone;
        }
        return user.getWechatPhone();
    }

    /**
     * generate绑定Ticket。
     *
     * @return 处理结果
     */
    private String generateBindTicket() {
        for (int i = 0; i < 10; i++) {
            // 调用randomString方法，复用统一能力并保证业务规则一致。
            String bindTicket = RandomUtil.randomString(24);
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(CacheConstants.WECHAT_BIND_TICKET_KEY + bindTicket))) {
                return bindTicket;
            }
        }
        throw new ServiceException("生成绑定二维码失败，请稍后重试");
    }

    /**
     * 新增绑定Session。
     *
     * @param session 参数
     */
    private void saveBindSession(WechatBindSession session) {
        // 调用toJsonStr方法，复用统一能力并保证业务规则一致。
        String sessionJson = JSONUtil.toJsonStr(session);
        redisTemplate.opsForValue().set(CacheConstants.WECHAT_BIND_USER_KEY + session.getUserId(), sessionJson,
                BIND_TICKET_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(CacheConstants.WECHAT_BIND_TICKET_KEY + session.getBindTicket(), sessionJson,
                BIND_TICKET_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 获取绑定Session。
     *
     * @return 处理结果
     */
    private WechatBindSession getBindSession(Long userId) {
        // 调用get方法，复用统一能力并保证业务规则一致。
        Object raw = redisTemplate.opsForValue().get(CacheConstants.WECHAT_BIND_USER_KEY + userId);
        return parseBindSession(raw, userId);
    }

    /**
     * 解析绑定Session。
     *
     * @param raw 参数
     * @return 处理结果
     */
    private WechatBindSession parseBindSession(Object raw, Long userId) {
        if (raw == null || StrUtil.isBlank(String.valueOf(raw))) {
            return null;
        }
        try {
            // 调用valueOf方法，复用统一能力并保证业务规则一致。
            WechatBindSession session = JSONUtil.toBean(String.valueOf(raw), WechatBindSession.class);
            if (session == null || session.getExpireAt() == null || session.getExpireAt().isBefore(LocalDateTime.now())) {
                // 调用clearBindSession方法，复用统一能力并保证业务规则一致。
                clearBindSession(session);
                if (userId != null) {
                    // 调用delete方法，复用统一能力并保证业务规则一致。
                    redisTemplate.delete(CacheConstants.WECHAT_BIND_USER_KEY + userId);
                }
                return null;
            }
            return session;
        } catch (Exception ex) {
            if (userId != null) {
                // 调用delete方法，复用统一能力并保证业务规则一致。
                redisTemplate.delete(CacheConstants.WECHAT_BIND_USER_KEY + userId);
            }
            return null;
        }
    }

    /**
     * require绑定Session。
     *
     * @param bindTicket 参数
     * @return 处理结果
     */
    private WechatBindSession requireBindSession(String bindTicket) {
        // 调用get方法，复用统一能力并保证业务规则一致。
        Object rawSession = redisTemplate.opsForValue().get(CacheConstants.WECHAT_BIND_TICKET_KEY + bindTicket);
        // 调用parseBindSession方法，复用统一能力并保证业务规则一致。
        WechatBindSession session = parseBindSession(rawSession, null);
        if (session == null || !StrUtil.equals(bindTicket, session.getBindTicket())) {
            throw new ServiceException("二维码已失效，请回 PC 端重新生成");
        }
        // 调用getUserId方法，复用统一能力并保证业务规则一致。
        WechatBindSession currentUserSession = getBindSession(session.getUserId());
        if (currentUserSession == null || !StrUtil.equals(bindTicket, currentUserSession.getBindTicket())) {
            throw new ServiceException("二维码已失效，请回 PC 端重新生成");
        }
        return session;
    }

    /**
     * clear绑定Session。
     *
     * @param session 参数
     */
    private void clearBindSession(WechatBindSession session) {
        if (session == null) {
            return;
        }
        // 调用getUserId方法，复用统一能力并保证业务规则一致。
        redisTemplate.delete(CacheConstants.WECHAT_BIND_USER_KEY + session.getUserId());
        if (StrUtil.isNotBlank(session.getBindTicket())) {
            // 调用getBindTicket方法，复用统一能力并保证业务规则一致。
            redisTemplate.delete(CacheConstants.WECHAT_BIND_TICKET_KEY + session.getBindTicket());
        }
    }

    /**
     * 新增微信绑定Record。
     *
     * @param user 参数
     * @param operateType 参数
     * @param operateSource 参数
     * @param wechatPhone 参数
     */
    private void saveWechatBindRecord(SysUser user, String operateType, String operateSource,
                                      String openid, String wechatPhone) {
        // 调用WechatBindRecord方法，复用统一能力并保证业务规则一致。
        WechatBindRecord record = new WechatBindRecord();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        record.setUserId(user.getId());
        // 调用setOperateType方法，复用统一能力并保证业务规则一致。
        record.setOperateType(operateType);
        // 调用setOperateSource方法，复用统一能力并保证业务规则一致。
        record.setOperateSource(operateSource);
        // 调用setOpenid方法，复用统一能力并保证业务规则一致。
        record.setOpenid(openid);
        // 调用setWechatPhone方法，复用统一能力并保证业务规则一致。
        record.setWechatPhone(wechatPhone);
        // 调用getId方法，复用统一能力并保证业务规则一致。
        record.setOperatorUserId(user.getId());
        // 调用getUsername方法，复用统一能力并保证业务规则一致。
        record.setOperatorUsername(user.getUsername());
        // 调用now方法，复用统一能力并保证业务规则一致。
        record.setOperateTime(LocalDateTime.now());
        // 说明：执行该步骤以保证业务流程正确。
        wechatBindRecordMapper.insert(record);
    }

    /**
     * 构建微信绑定状态。
     *
     * @param user 参数
     * @param session 参数
     * @return 处理结果
     */
    private WechatBindStatusVO buildWechatBindStatus(SysUser user, WechatBindSession session) {
        // 调用WechatBindStatusVO方法，复用统一能力并保证业务规则一致。
        WechatBindStatusVO vo = new WechatBindStatusVO();
        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        vo.setBound(StrUtil.isNotBlank(user.getOpenid()));
        // 调用getOpenid方法，复用统一能力并保证业务规则一致。
        vo.setMaskedOpenid(maskOpenid(user.getOpenid()));
        // 调用getWechatPhone方法，复用统一能力并保证业务规则一致。
        vo.setWechatPhone(user.getWechatPhone());
        // 调用getBound方法，复用统一能力并保证业务规则一致。
        vo.setHasActiveTicket(Boolean.FALSE.equals(vo.getBound()) && session != null);
        if (session != null && Boolean.FALSE.equals(vo.getBound())) {
            // 调用getExpireAt方法，复用统一能力并保证业务规则一致。
            vo.setExpireAt(session.getExpireAt());
        }
        return vo;
    }

    /**
     * maskOpenid。
     *
     * @return 处理结果
     */
    private String maskOpenid(String openid) {
        if (StrUtil.isBlank(openid)) {
            return null;
        }
        if (openid.length() <= 8) {
            return openid;
        }
        return openid.substring(0, 4) + "****" + openid.substring(openid.length() - 4);
    }

    /**
     * 构建MpLogin视图。
     *
     * @param loginVO 参数
     * @return 处理结果
     */
    private MpLoginVO buildMpLoginVO(LoginVO loginVO) {
        // 调用MpLoginVO方法，复用统一能力并保证业务规则一致。
        MpLoginVO vo = new MpLoginVO();
        // 调用setStatus方法，复用统一能力并保证业务规则一致。
        vo.setStatus(WECHAT_STATUS_BIND);
        // 调用getToken方法，复用统一能力并保证业务规则一致。
        vo.setToken(loginVO.getToken());
        // 调用getUserInfo方法，复用统一能力并保证业务规则一致。
        vo.setUserInfo(loginVO.getUserInfo());
        // 调用getCompanies方法，复用统一能力并保证业务规则一致。
        vo.setCompanies(loginVO.getCompanies());
        // 调用getNeedChooseCompany方法，复用统一能力并保证业务规则一致。
        vo.setNeedChooseCompany(loginVO.getNeedChooseCompany());
        return vo;
    }
}




