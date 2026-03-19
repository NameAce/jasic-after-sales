package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jasic.aftersales.common.constant.CacheConstants;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.framework.web.ResultCode;
import com.jasic.aftersales.system.domain.dto.LoginDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.vo.LoginVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.domain.vo.SysUserVO;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysCompanyTypeMapper;
import com.jasic.aftersales.system.mapper.SysMenuMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.service.ISysAuthService;
import com.jasic.aftersales.system.service.SysPermissionService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * B端认证服务实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysAuthServiceImpl implements ISysAuthService {

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
    private SysPermissionService sysPermissionService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * B端登录
     *
     * @param dto 登录参数
     * @return 登录结果（含 token、用户信息、公司列表等）
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 根据用户名查询用户
        LambdaQueryWrapper<SysUser> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(SysUser::getUsername, dto.getUsername());
        SysUser user = sysUserMapper.selectOne(userQuery);
        if (user == null) {
            throw new ServiceException(ResultCode.LOGIN_ERROR, "用户名或密码错误");
        }

        // 2. 校验密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new ServiceException(ResultCode.LOGIN_ERROR, "用户名或密码错误");
        }

        // 3. 校验账号状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new ServiceException(ResultCode.ACCOUNT_DISABLED, "账号已停用");
        }

        // 4. Sa-Token 登录
        StpUtil.login(user.getId());

        // 5. 更新最后登录时间
        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, user.getId())
                .set(SysUser::getLastLoginTime, LocalDateTime.now());
        sysUserMapper.update(null, updateWrapper);

        // 6. 查询用户关联公司列表
        LambdaQueryWrapper<SysUserCompany> ucQuery = new LambdaQueryWrapper<>();
        ucQuery.eq(SysUserCompany::getUserId, user.getId());
        List<SysUserCompany> userCompanies = sysUserCompanyMapper.selectList(ucQuery);

        if (userCompanies == null || userCompanies.isEmpty()) {
            throw new ServiceException(ResultCode.USER_ERROR, "用户未关联任何公司");
        }

        List<SysCompanySimpleVO> companies = buildCompanySimpleList(
                userCompanies.stream().map(SysUserCompany::getCompanyId).collect(Collectors.toList()));

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(StpUtil.getTokenValue());

        if (companies.size() == 1) {
            // 7. 仅一个公司时自动选择
            SysUserVO userInfo = chooseCompany(companies.get(0).getId());
            loginVO.setUserInfo(userInfo);
            loginVO.setNeedChooseCompany(false);
            loginVO.setCompanies(null);
        } else {
            // 8. 多公司时需要选择
            loginVO.setUserInfo(buildBasicUserVO(user));
            loginVO.setNeedChooseCompany(true);
            loginVO.setCompanies(companies);
        }

        return loginVO;
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

        // 1. 校验用户-公司关联
        LambdaQueryWrapper<SysUserCompany> ucQuery = new LambdaQueryWrapper<>();
        ucQuery.eq(SysUserCompany::getUserId, userId)
                .eq(SysUserCompany::getCompanyId, companyId);
        SysUserCompany userCompany = sysUserCompanyMapper.selectOne(ucQuery);
        if (userCompany == null) {
            throw new ServiceException(ResultCode.NOT_PERMISSION, "无权限操作该公司");
        }

        // 2. 查询公司和公司类型
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "公司不存在");
        }

        LambdaQueryWrapper<SysCompanyType> typeQuery = new LambdaQueryWrapper<>();
        typeQuery.eq(SysCompanyType::getTypeCode, company.getTypeCode());
        SysCompanyType companyType = sysCompanyTypeMapper.selectOne(typeQuery);

        // 3. 设置 SecurityContext
        SecurityContext.setCurrentCompanyId(companyId);
        SecurityContext.setCurrentSubjectType(companyType != null ? companyType.getSubjectType() : null);
        SecurityContext.setCurrentTypeCode(company.getTypeCode());

        // 4. 加载权限到缓存
        Set<String> perms = sysPermissionService.loadPermsToCache(userId, companyId);

        // 5. 查询菜单树（用于缓存或后续接口）
        sysMenuMapper.selectMenuTreeByUserIdAndCompanyId(userId, companyId);

        // 6. 构建并返回 SysUserVO
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

                // 从 Redis 加载权限
                String permsKey = CacheConstants.USER_PERMS_KEY + userId + ":" + companyId;
                Set<Object> permObjects = redisTemplate.opsForSet().members(permsKey);
                if (permObjects != null && !permObjects.isEmpty()) {
                    Set<String> perms = permObjects.stream()
                            .map(String::valueOf)
                            .collect(Collectors.toSet());
                    vo.setPerms(perms);
                }
            }
        }

        // 加载用户关联公司列表
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
     * 退出登录
     */
    @Override
    public void logout() {
        Long userId = SecurityContext.getCurrentUserId();
        Long companyId = SecurityContext.getCurrentCompanyId();

        // 清除权限缓存
        if (companyId != null) {
            sysPermissionService.clearPermsCache(userId, companyId);
        }

        StpUtil.logout();
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
        vo.setPerms(perms);

        // 加载用户关联公司列表
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
}
