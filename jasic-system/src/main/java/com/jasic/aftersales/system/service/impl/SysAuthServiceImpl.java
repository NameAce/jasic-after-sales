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
     * ?? login ?????
     *
     * @param dto ????
     * @return ????
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
        SysUser user = findByLoginIdentity(dto.getUsername());
        if (user == null) {
            throw new ServiceException(ResultCode.LOGIN_ERROR, "用户名或密码错误");
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new ServiceException(ResultCode.LOGIN_ERROR, "用户名或密码错误");
        }
        // ?????????????????????????????
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
        WechatAuthSession session = wechatMiniProgramService.code2Session(WechatMiniProgramScene.B, dto.getCode());
        SysUser user = findByOpenid(session.getOpenid());
        if (user == null) {
            MpLoginVO vo = new MpLoginVO();
            vo.setStatus(WECHAT_STATUS_UNBIND);
            vo.setNeedChooseCompany(false);
            return vo;
        }
        // ?????????????????????????????
        ensureUserActive(user);
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
        WechatAuthSession authSession = wechatMiniProgramService.code2Session(WechatMiniProgramScene.B, dto.getCode());
        SysUser user = findByLoginIdentity(dto.getUsernameOrPhone());
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new ServiceException(ResultCode.LOGIN_ERROR, "用户名或密码错误");
        }
        // ?????????????????????????????
        ensureUserActive(user);
        validateWechatBinding(user, authSession.getOpenid());

        boolean wasBound = StrUtil.isNotBlank(user.getOpenid());
        String wechatPhone = resolveWechatPhone(dto.getPhoneCode(), user.getId());
        refreshWechatIdentity(user.getId(), authSession.getOpenid(), wechatPhone);
        if (!wasBound) {
            saveWechatBindRecord(user, WECHAT_STATUS_BIND, WECHAT_OPERATE_SOURCE_MP_BIND_LOGIN,
                    authSession.getOpenid(), resolveRecordWechatPhone(user, wechatPhone));
        }
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
        Long userId = SecurityContext.getCurrentUserId();

        LambdaQueryWrapper<SysUserCompany> ucQuery = new LambdaQueryWrapper<>();
        ucQuery.eq(SysUserCompany::getUserId, userId)
                .eq(SysUserCompany::getCompanyId, companyId);
        // ??????????????????????????
        SysUserCompany userCompany = sysUserCompanyMapper.selectOne(ucQuery);
        if (userCompany == null) {
            throw new ServiceException(ResultCode.NOT_PERMISSION, "无权限操作该公司");
        }

        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "公司不存在");
        }

        LambdaQueryWrapper<SysCompanyType> typeQuery = new LambdaQueryWrapper<>();
        typeQuery.eq(SysCompanyType::getTypeCode, company.getTypeCode());
        SysCompanyType companyType = sysCompanyTypeMapper.selectOne(typeQuery);

        SecurityContext.setCurrentCompanyId(companyId);
        SecurityContext.setCurrentSubjectType(companyType != null ? companyType.getSubjectType() : null);
        SecurityContext.setCurrentTypeCode(company.getTypeCode());
        initDataScopeContext(userId, companyId, companyType);

        // ??????????????????????
        Set<String> perms = sysPermissionService.loadPermsToCache(userId, companyId);
        sysMenuMapper.selectMenuTreeByUserIdAndCompanyId(userId, companyId);

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
        Long userId = SecurityContext.getCurrentUserId();
        Long companyId = SecurityContext.getCurrentCompanyId();

        // ??????????????????????????
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }

        SysUserVO vo = buildBasicUserVO(user);
        if (companyId != null) {
            SysCompany company = sysCompanyMapper.selectById(companyId);
            if (company != null) {
                vo.setCurrentCompanyId(company.getId());
                vo.setCurrentCompanyName(company.getCompanyName());
                vo.setCurrentTypeCode(company.getTypeCode());

                LambdaQueryWrapper<SysCompanyType> typeQuery = new LambdaQueryWrapper<>();
                typeQuery.eq(SysCompanyType::getTypeCode, company.getTypeCode());
                SysCompanyType companyType = sysCompanyTypeMapper.selectOne(typeQuery);
                if (companyType != null) {
                    vo.setCurrentSubjectType(companyType.getSubjectType());
                }

                vo.setPerms(loadCurrentPerms(userId, companyId));
                vo.setPermissionVos(buildCurrentPermissionVos(userId, companyId));
                vo.setRoles(buildCurrentCompanyRoles(userId, companyId));
            }
        }

        LambdaQueryWrapper<SysUserCompany> ucQuery = new LambdaQueryWrapper<>();
        ucQuery.eq(SysUserCompany::getUserId, userId);
        List<SysUserCompany> userCompanies = sysUserCompanyMapper.selectList(ucQuery);
        if (userCompanies != null && !userCompanies.isEmpty()) {
            List<Long> companyIds = userCompanies.stream()
                    .map(SysUserCompany::getCompanyId)
                    .collect(Collectors.toList());
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
        Long userId = SecurityContext.getCurrentUserId();
        // ?????????????????????????????
        SysUser user = requireActiveUser(userId);
        verifyCurrentPassword(user, dto.getCurrentPassword());

        String realName = StrUtil.trim(dto.getRealName());
        String phone = StrUtil.trim(dto.getPhone());
        String email = StrUtil.trim(dto.getEmail());
        userIdentityValidator.validateLoginIdentityUnique(userId, user.getUsername(), phone);

        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, userId)
                .set(SysUser::getRealName, realName)
                .set(SysUser::getPhone, phone)
                .set(SysUser::getEmail, StrUtil.isBlank(email) ? null : email);
        // ???????????????????????
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
        Long userId = SecurityContext.getCurrentUserId();
        // ?????????????????????????????
        SysUser user = requireActiveUser(userId);
        verifyCurrentPassword(user, dto.getCurrentPassword());
        if (BCrypt.checkpw(dto.getNewPassword(), user.getPassword())) {
            throw new ServiceException("新密码不能与当前密码相同");
        }

        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, userId)
                .set(SysUser::getPassword, BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        // ???????????????????????
        sysUserMapper.update(null, updateWrapper);
        // ??????????????????????
        sysPermissionService.clearAllPermsCache(userId);
        StpUtil.kickout(userId);
    }

    /**
     * 生成当前用户的微信绑定二维码
     *
     * @return 绑定状态
     */
    @Override
    public WechatBindStatusVO createWechatBindQrcode() {
        Long userId = SecurityContext.getCurrentUserId();
        // ?????????????????????????????
        SysUser user = requireActiveUser(userId);
        WechatBindSession oldSession = getBindSession(userId);
        if (StrUtil.isNotBlank(user.getOpenid())) {
            clearBindSession(oldSession);
            return buildWechatBindStatus(user, null);
        }

        if (oldSession != null) {
            clearBindSession(oldSession);
        }

        WechatBindSession session = new WechatBindSession();
        session.setUserId(userId);
        session.setBindTicket(generateBindTicket());
        session.setExpireAt(LocalDateTime.now().plusMinutes(BIND_TICKET_EXPIRE_MINUTES));
        saveBindSession(session);
        try {
            String pagePath = StrUtil.trim(sysConfigService.getValueByKey(WechatConfigConstants.B_BIND_PAGE_PATH));
            WechatBindStatusVO vo = buildWechatBindStatus(user, session);
            vo.setQrImageBase64(wechatMiniProgramService.createQrcodeBase64(
                    WechatMiniProgramScene.B, session.getBindTicket(), pagePath));
            return vo;
        } catch (RuntimeException ex) {
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
        Long userId = SecurityContext.getCurrentUserId();
        // ?????????????????????????????
        SysUser user = requireActiveUser(userId);
        WechatBindSession session = getBindSession(userId);
        if (StrUtil.isNotBlank(user.getOpenid())) {
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
        // ?????????????????????????????
        WechatBindSession bindSession = requireBindSession(dto.getBindTicket());
        SysUser user = requireActiveUser(bindSession.getUserId());
        WechatAuthSession authSession = wechatMiniProgramService.code2Session(WechatMiniProgramScene.B, dto.getCode());
        if (StrUtil.isNotBlank(user.getOpenid()) && !StrUtil.equals(user.getOpenid(), authSession.getOpenid())) {
            clearBindSession(bindSession);
            throw new ServiceException("当前账号已绑定微信");
        }
        validateWechatBinding(user, authSession.getOpenid());

        boolean wasBound = StrUtil.isNotBlank(user.getOpenid());
        String wechatPhone = resolveWechatPhone(dto.getPhoneCode(), user.getId());
        refreshWechatIdentity(user.getId(), authSession.getOpenid(), wechatPhone);
        if (!wasBound) {
            saveWechatBindRecord(user, WECHAT_STATUS_BIND, WECHAT_OPERATE_SOURCE_PC_QR_BIND,
                    authSession.getOpenid(), resolveRecordWechatPhone(user, wechatPhone));
        }
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
        Long userId = SecurityContext.getCurrentUserId();
        // ?????????????????????????????
        SysUser user = requireActiveUser(userId);
        verifyCurrentPassword(user, dto.getCurrentPassword());
        if (StrUtil.isBlank(user.getOpenid())) {
            throw new ServiceException("当前账号未绑定微信");
        }

        String openid = user.getOpenid();
        String wechatPhone = user.getWechatPhone();
        clearBindSession(getBindSession(userId));
        clearWechatIdentity(userId);
        saveWechatBindRecord(user, WECHAT_STATUS_UNBIND, WECHAT_OPERATE_SOURCE_PC_SELF_UNBIND, openid, wechatPhone);
        StpUtil.kickout(userId);
    }

    /**
     * 退出登录
     */
    @Override
    public void logout() {
        Long userId = SecurityContext.getCurrentUserId();
        Long companyId = SecurityContext.getCurrentCompanyId();
        if (companyId != null) {
            // ??????????????????????
            sysPermissionService.clearPermsCache(userId, companyId);
        }
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
        String subjectType = companyType != null ? companyType.getSubjectType() : null;
        DataScopeEnum effectiveDataScope = resolveEffectiveDataScope(userId, companyId, subjectType);
        SecurityContext.setEffectiveDataScope(effectiveDataScope.getCode());
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
        // ??????????????????????
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
            // ??????????????????????????
            SysCompany company = sysCompanyMapper.selectById(companyId);
            if (company == null) {
                continue;
            }
            SysCompanySimpleVO vo = new SysCompanySimpleVO();
            vo.setId(company.getId());
            vo.setCompanyName(company.getCompanyName());
            vo.setCompanyCode(company.getCompanyCode());
            vo.setTypeCode(company.getTypeCode());

            LambdaQueryWrapper<SysCompanyType> typeQuery = new LambdaQueryWrapper<>();
            typeQuery.eq(SysCompanyType::getTypeCode, company.getTypeCode());
            SysCompanyType companyType = sysCompanyTypeMapper.selectOne(typeQuery);
            if (companyType != null) {
                vo.setTypeName(companyType.getTypeName());
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 构建基础用户VO（不含公司、权限等）
     */
    private SysUserVO buildBasicUserVO(SysUser user) {
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setSex(user.getSex());
        vo.setStatus(user.getStatus());
        vo.setRemark(user.getRemark());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 构建完整用户VO（含当前公司、权限等）
     */
    private SysUserVO buildSysUserVO(SysUser user, SysCompany company, SysCompanyType companyType,
                                     Long userId, Set<String> perms) {
        SysUserVO vo = buildBasicUserVO(user);
        if (company != null) {
            vo.setCurrentCompanyId(company.getId());
            vo.setCurrentCompanyName(company.getCompanyName());
            vo.setCurrentTypeCode(company.getTypeCode());
        }
        if (companyType != null) {
            vo.setCurrentSubjectType(companyType.getSubjectType());
        }
        vo.setPerms(perms);
        if (company != null) {
            vo.setPermissionVos(buildCurrentPermissionVos(userId, company.getId()));
            vo.setRoles(buildCurrentCompanyRoles(userId, company.getId()));
        }

        LambdaQueryWrapper<SysUserCompany> ucQuery = new LambdaQueryWrapper<>();
        ucQuery.eq(SysUserCompany::getUserId, userId);
        // ??????????????????????????
        List<SysUserCompany> userCompanies = sysUserCompanyMapper.selectList(ucQuery);
        if (userCompanies != null && !userCompanies.isEmpty()) {
            List<Long> companyIds = userCompanies.stream()
                    .map(SysUserCompany::getCompanyId)
                    .collect(Collectors.toList());
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
        Set<Object> permObjects = redisTemplate.opsForSet().members(permsKey);
        if (permObjects != null && !permObjects.isEmpty()) {
            return permObjects.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toSet());
        }
        // ??????????????????????
        return sysPermissionService.loadPermsToCache(userId, companyId);
    }

    /**
     * 构建当前公司下的轻量权限项集合。
     */
    private List<SysPermissionVO> buildCurrentPermissionVos(Long userId, Long companyId) {
        if (userId == null || companyId == null) {
            return Collections.emptyList();
        }
        // ??????????????????????????
        List<SysMenu> permissionMenus = sysMenuMapper.selectPermissionMenusByUserIdAndCompanyId(userId, companyId);
        if (permissionMenus == null || permissionMenus.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionMenus.stream()
                .map(this::convertPermissionToVO)
                .collect(Collectors.toList());
    }

    /**
     * ???????
     *
     * @param menu ??
     * @return ????
     */
    private SysPermissionVO convertPermissionToVO(SysMenu menu) {
        SysPermissionVO vo = new SysPermissionVO();
        vo.setId(menu.getId());
        vo.setMenuName(menu.getMenuName());
        vo.setParentId(menu.getParentId());
        vo.setMenuType(menu.getMenuType());
        vo.setPerms(menu.getPerms());
        return vo;
    }

    /**
     * ?? doLogin ?????
     *
     * @param user ??
     * @return ????
     */
    private LoginVO doLogin(SysUser user) {
        StpUtil.login(user.getId());
        touchLastLoginTime(user.getId());

        List<SysCompanySimpleVO> companies = listUserCompanies(user.getId());
        if (companies.isEmpty()) {
            throw new ServiceException(ResultCode.USER_ERROR, "用户未关联任何公司");
        }

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(StpUtil.getTokenValue());
        if (companies.size() == 1) {
            SysUserVO userInfo = chooseCompany(companies.get(0).getId());
            loginVO.setUserInfo(userInfo);
            loginVO.setNeedChooseCompany(false);
            loginVO.setCompanies(null);
        } else {
            loginVO.setUserInfo(buildBasicUserVO(user));
            loginVO.setNeedChooseCompany(true);
            loginVO.setCompanies(companies);
        }
        return loginVO;
    }

    /**
     * ???????
     *
     * @param userId ??ID
     * @return ????
     */
    private List<SysCompanySimpleVO> listUserCompanies(Long userId) {
        LambdaQueryWrapper<SysUserCompany> query = new LambdaQueryWrapper<>();
        query.eq(SysUserCompany::getUserId, userId);
        // ??????????????????????????
        List<SysUserCompany> relations = sysUserCompanyMapper.selectList(query);
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }
        return buildCompanySimpleList(relations.stream()
                .map(SysUserCompany::getCompanyId)
                .collect(Collectors.toList()));
    }

    /**
     * ?? touchLastLoginTime ?????
     *
     * @param userId ??ID
     */
    private void touchLastLoginTime(Long userId) {
        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, userId)
                .set(SysUser::getLastLoginTime, LocalDateTime.now());
        // ???????????????????????
        sysUserMapper.update(null, updateWrapper);
    }

    /**
     * ??????????
     *
     * @param userId ??ID
     * @return ????
     */
    private SysUser requireActiveUser(Long userId) {
        // ??????????????????????????
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        // ?????????????????????????????
        ensureUserActive(user);
        return user;
    }

    /**
     * ?????????
     *
     * @param user ??
     */
    private void ensureUserActive(SysUser user) {
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new ServiceException(ResultCode.ACCOUNT_DISABLED, "账号已停用");
        }
    }

    /**
     * ?? verifyCurrentPassword ?????
     *
     * @param user ??
     * @param currentPassword ??
     */
    private void verifyCurrentPassword(SysUser user, String currentPassword) {
        if (!BCrypt.checkpw(currentPassword, user.getPassword())) {
            throw new ServiceException(ResultCode.LOGIN_ERROR, "当前密码错误");
        }
    }

    /**
     * ?? findByOpenid ?????
     *
     * @param openid ??openid
     * @return ????
     */
    private SysUser findByOpenid(String openid) {
        if (StrUtil.isBlank(openid)) {
            return null;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getOpenid, openid);
        // ??????????????????????????
        return sysUserMapper.selectOne(wrapper);
    }

    /**
     * ?? findByLoginIdentity ?????
     *
     * @param loginIdentity ??
     * @return ????
     */
    private SysUser findByLoginIdentity(String loginIdentity) {
        String normalized = StrUtil.trim(loginIdentity);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(q -> q.eq(SysUser::getUsername, normalized).or().eq(SysUser::getPhone, normalized));
        // ??????????????????????????
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
     * ???????
     *
     * @param userId ??ID
     * @param companyId ??ID
     * @return ????
     */
    private List<SysRoleVO> buildCurrentCompanyRoles(Long userId, Long companyId) {
        if (userId == null || companyId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysUserRole> userRoleQuery = new LambdaQueryWrapper<>();
        userRoleQuery.eq(SysUserRole::getUserId, userId);
        // ??????????????????????????
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleQuery);
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<SysRole> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.in(SysRole::getId, roleIds)
                .eq(SysRole::getCompanyId, companyId);
        List<SysRole> roles = sysRoleMapper.selectList(roleQuery);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(this::convertRoleToVO)
                .collect(Collectors.toList());
    }

    /**
     * ???????
     *
     * @param role ??
     * @return ????
     */
    private SysRoleVO convertRoleToVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        vo.setId(role.getId());
        vo.setCompanyId(role.getCompanyId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleKey(role.getRoleKey());
        vo.setDataScope(role.getDataScope());
        vo.setRoleType(role.getRoleType());
        vo.setIsSystem(role.getIsSystem());
        vo.setStatus(role.getStatus());
        vo.setOrderNum(role.getOrderNum());
        vo.setRemark(role.getRemark());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }

    /**
     * ?? refreshWechatIdentity ?????
     *
     * @param userId ??ID
     * @param openid ??openid
     * @param wechatPhone ??
     */
    private void refreshWechatIdentity(Long userId, String openid, String wechatPhone) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
                .set(SysUser::getOpenid, openid);
        if (StrUtil.isNotBlank(wechatPhone)) {
            wrapper.set(SysUser::getWechatPhone, wechatPhone);
        }
        // ???????????????????????
        sysUserMapper.update(null, wrapper);
    }

    /**
     * ?????
     *
     * @param userId ??ID
     */
    private void clearWechatIdentity(Long userId) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
                .set(SysUser::getOpenid, null)
                .set(SysUser::getWechatPhone, null);
        // ???????????????????????
        sysUserMapper.update(null, wrapper);
    }

    /**
     * ???????
     *
     * @param user ??
     * @param openid ??openid
     */
    private void validateWechatBinding(SysUser user, String openid) {
        if (StrUtil.isBlank(openid)) {
            throw new ServiceException("微信登录失败，未获取到用户标识");
        }
        if (StrUtil.isNotBlank(user.getOpenid()) && !StrUtil.equals(user.getOpenid(), openid)) {
            throw new ServiceException("当前账号已绑定微信");
        }
        SysUser boundUser = findByOpenid(openid);
        if (boundUser != null && !boundUser.getId().equals(user.getId())) {
            throw new ServiceException("该微信已绑定其他账号，请联系管理员");
        }
    }

    /**
     * ???????
     *
     * @param phoneCode ??
     * @param userId ??ID
     * @return ?????
     */
    private String resolveWechatPhone(String phoneCode, Long userId) {
        if (StrUtil.isBlank(phoneCode)) {
            return null;
        }
        try {
            WechatPhoneInfo phoneInfo = wechatMiniProgramService.getPhoneNumber(WechatMiniProgramScene.B, phoneCode);
            return StrUtil.blankToDefault(phoneInfo.getPhoneNumber(), phoneInfo.getPurePhoneNumber());
        } catch (Exception ex) {
            log.warn("获取 B 端微信手机号失败，userId={}", userId, ex);
            return null;
        }
    }

    /**
     * ???????
     *
     * @param user ??
     * @param latestWechatPhone ??
     * @return ?????
     */
    private String resolveRecordWechatPhone(SysUser user, String latestWechatPhone) {
        if (StrUtil.isNotBlank(latestWechatPhone)) {
            return latestWechatPhone;
        }
        return user.getWechatPhone();
    }

    /**
     * ?? generateBindTicket ?????
     *
     * @return ?????
     */
    private String generateBindTicket() {
        for (int i = 0; i < 10; i++) {
            String bindTicket = RandomUtil.randomString(24);
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(CacheConstants.WECHAT_BIND_TICKET_KEY + bindTicket))) {
                return bindTicket;
            }
        }
        throw new ServiceException("生成绑定二维码失败，请稍后重试");
    }

    /**
     * ?????
     *
     * @param session ??
     */
    private void saveBindSession(WechatBindSession session) {
        String sessionJson = JSONUtil.toJsonStr(session);
        redisTemplate.opsForValue().set(CacheConstants.WECHAT_BIND_USER_KEY + session.getUserId(), sessionJson,
                BIND_TICKET_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(CacheConstants.WECHAT_BIND_TICKET_KEY + session.getBindTicket(), sessionJson,
                BIND_TICKET_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * ??Bind Session?
     *
     * @param userId ??ID
     * @return ????
     */
    private WechatBindSession getBindSession(Long userId) {
        Object raw = redisTemplate.opsForValue().get(CacheConstants.WECHAT_BIND_USER_KEY + userId);
        return parseBindSession(raw, userId);
    }

    /**
     * ?? parseBindSession ?????
     *
     * @param raw ??
     * @param userId ??ID
     * @return ????
     */
    private WechatBindSession parseBindSession(Object raw, Long userId) {
        if (raw == null || StrUtil.isBlank(String.valueOf(raw))) {
            return null;
        }
        try {
            WechatBindSession session = JSONUtil.toBean(String.valueOf(raw), WechatBindSession.class);
            if (session == null || session.getExpireAt() == null || session.getExpireAt().isBefore(LocalDateTime.now())) {
                clearBindSession(session);
                if (userId != null) {
                    redisTemplate.delete(CacheConstants.WECHAT_BIND_USER_KEY + userId);
                }
                return null;
            }
            return session;
        } catch (Exception ex) {
            if (userId != null) {
                redisTemplate.delete(CacheConstants.WECHAT_BIND_USER_KEY + userId);
            }
            return null;
        }
    }

    /**
     * ??????????
     *
     * @param bindTicket ??
     * @return ????
     */
    private WechatBindSession requireBindSession(String bindTicket) {
        Object rawSession = redisTemplate.opsForValue().get(CacheConstants.WECHAT_BIND_TICKET_KEY + bindTicket);
        WechatBindSession session = parseBindSession(rawSession, null);
        if (session == null || !StrUtil.equals(bindTicket, session.getBindTicket())) {
            throw new ServiceException("二维码已失效，请回 PC 端重新生成");
        }
        WechatBindSession currentUserSession = getBindSession(session.getUserId());
        if (currentUserSession == null || !StrUtil.equals(bindTicket, currentUserSession.getBindTicket())) {
            throw new ServiceException("二维码已失效，请回 PC 端重新生成");
        }
        return session;
    }

    /**
     * ?????
     *
     * @param session ??
     */
    private void clearBindSession(WechatBindSession session) {
        if (session == null) {
            return;
        }
        redisTemplate.delete(CacheConstants.WECHAT_BIND_USER_KEY + session.getUserId());
        if (StrUtil.isNotBlank(session.getBindTicket())) {
            redisTemplate.delete(CacheConstants.WECHAT_BIND_TICKET_KEY + session.getBindTicket());
        }
    }

    /**
     * ?????
     *
     * @param user ??
     * @param operateType ??
     * @param operateSource ??
     * @param openid ??openid
     * @param wechatPhone ??
     */
    private void saveWechatBindRecord(SysUser user, String operateType, String operateSource,
                                      String openid, String wechatPhone) {
        WechatBindRecord record = new WechatBindRecord();
        record.setUserId(user.getId());
        record.setOperateType(operateType);
        record.setOperateSource(operateSource);
        record.setOpenid(openid);
        record.setWechatPhone(wechatPhone);
        record.setOperatorUserId(user.getId());
        record.setOperatorUsername(user.getUsername());
        record.setOperateTime(LocalDateTime.now());
        // ???????????????????????
        wechatBindRecordMapper.insert(record);
    }

    /**
     * ???????
     *
     * @param user ??
     * @param session ??
     * @return ????
     */
    private WechatBindStatusVO buildWechatBindStatus(SysUser user, WechatBindSession session) {
        WechatBindStatusVO vo = new WechatBindStatusVO();
        vo.setBound(StrUtil.isNotBlank(user.getOpenid()));
        vo.setMaskedOpenid(maskOpenid(user.getOpenid()));
        vo.setWechatPhone(user.getWechatPhone());
        vo.setHasActiveTicket(Boolean.FALSE.equals(vo.getBound()) && session != null);
        if (session != null && Boolean.FALSE.equals(vo.getBound())) {
            vo.setExpireAt(session.getExpireAt());
        }
        return vo;
    }

    /**
     * ?? maskOpenid ?????
     *
     * @param openid ??openid
     * @return ?????
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
     * ???????
     *
     * @param loginVO ??
     * @return ????
     */
    private MpLoginVO buildMpLoginVO(LoginVO loginVO) {
        MpLoginVO vo = new MpLoginVO();
        vo.setStatus(WECHAT_STATUS_BIND);
        vo.setToken(loginVO.getToken());
        vo.setUserInfo(loginVO.getUserInfo());
        vo.setCompanies(loginVO.getCompanies());
        vo.setNeedChooseCompany(loginVO.getNeedChooseCompany());
        return vo;
    }
}
