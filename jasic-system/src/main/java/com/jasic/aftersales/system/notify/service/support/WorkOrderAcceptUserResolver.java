package com.jasic.aftersales.system.notify.service.support;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.framework.datapermission.CompanyDataAccessContext;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.mapper.SysMenuMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.support.NotifyReceiverSnapshot;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * B 端接单类通知接收人解析器。
 *
 * <p>当前阶段统一负责“网点级通知”的接收人筛选口径：
 * 1. 当前目标公司下的启用用户
 * 2. 具备 `workorder:accept` 权限
 * 3. 已绑定小程序 openid
 *
 * <p>由于当前后端还没有模板订阅授权持久化记录，
 * 因此这里无法在事件发布前进一步预筛“已完成模板订阅授权”的用户。
 * 真实发送时如果微信返回用户未订阅，仍由 sender 统一跳过并留痕。</p>
 *
 * @author Zoro
 * @date 2026/05/16
 */
@Component
public class WorkOrderAcceptUserResolver {

    /**WORKORDER_ACCEPT_PERMISSION 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String WORKORDER_ACCEPT_PERMISSION = "workorder:accept";

    /**sysUserCompanyMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysUserCompanyMapper sysUserCompanyMapper;

    /**sysUserMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysUserMapper sysUserMapper;

    /**sysMenuMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysMenuMapper sysMenuMapper;

    /**companyDataAccessContext 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private CompanyDataAccessContext companyDataAccessContext;

    /**
     * 解析指定公司下满足当前阶段通知口径的接收人快照列表。
     *
     * @param companyId 目标公司ID
     * @return 接收人快照列表
     */
    public List<NotifyReceiverSnapshot> resolveAcceptUserSnapshots(Long companyId) {
        if (companyId == null) {
            return Collections.emptyList();
        }
        // 转单转入通知同样走异步消费，权限查询必须显式绑定目标网点公司上下文，
        // 避免租户拦截器回退读取当前请求登录态，导致非 Web 线程下抛错。
        return companyDataAccessContext.runWithTargetCompany(companyId, () -> {
            Set<Long> userIds = listCompanyUserIds(companyId);
            if (userIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            if (users == null || users.isEmpty()) {
                return Collections.emptyList();
            }
            return users.stream()
                    .filter(user -> user != null
                            && user.getId() != null
                            && Integer.valueOf(1).equals(user.getStatus())
                            && hasCompanyPermission(user.getId(), companyId, WORKORDER_ACCEPT_PERMISSION)
                            && StrUtil.isNotBlank(StrUtil.trim(user.getOpenid())))
                    .sorted(java.util.Comparator.comparing(SysUser::getId))
                    .map(user -> NotifyReceiverSnapshot.of(
                            NotifyReceiverTypeEnum.ACCEPT_USER.getCode(),
                            user.getId(),
                            companyId,
                            resolveUserName(user),
                            StrUtil.trim(user.getOpenid())
                    ))
                    .collect(Collectors.toList());
        });
    }

    /**
     * 查询公司关联用户ID集合。
     *
     * @param companyId 公司ID
     * @return 用户ID集合
     */
    private Set<Long> listCompanyUserIds(Long companyId) {
        LambdaQueryWrapper<SysUserCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserCompany::getCompanyId, companyId);
        List<SysUserCompany> relations = sysUserCompanyMapper.selectList(wrapper);
        if (relations == null || relations.isEmpty()) {
            return Collections.emptySet();
        }
        return relations.stream()
                .map(SysUserCompany::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 判断用户在指定公司下是否具备目标权限。
     *
     * @param userId 用户ID
     * @param companyId 公司ID
     * @param permission 权限标识
     * @return 是否具备权限
     */
    private boolean hasCompanyPermission(Long userId, Long companyId, String permission) {
        Set<String> perms = sysMenuMapper.selectPermsByUserIdAndCompanyId(userId, companyId);
        return perms != null && perms.contains(permission);
    }

    /**
     * 解析用户名称快照。
     *
     * @param user 用户实体
     * @return 用户名称
     */
    private String resolveUserName(SysUser user) {
        String realName = StrUtil.trim(user.getRealName());
        if (StrUtil.isNotBlank(realName)) {
            return realName;
        }
        String username = StrUtil.trim(user.getUsername());
        if (StrUtil.isNotBlank(username)) {
            return username;
        }
        return String.valueOf(user.getId());
    }
}
