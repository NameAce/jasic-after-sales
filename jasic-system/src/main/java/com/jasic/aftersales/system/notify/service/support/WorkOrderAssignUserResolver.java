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
 * B 端待派单通知接收人解析器。
 *
 * <p>该解析器专门服务“工单创建后进入待派单池”的网点级通知场景，
 * 当前统一按照以下口径收口接收人：
 * 1. 当前目标公司下的启用用户
 * 2. 具备 `workorder:assign` 权限
 * 3. 已绑定小程序 openid
 *
 * <p>由于后端当前仍未单独持久化“用户已授权订阅模板”的结果，
 * 因此这里只负责收口“谁有资格作为待派单通知接收人”，
 * 真实发送时如果微信返回未订阅，仍由 sender 统一记跳过结果。</p>
 *
 * @author Codex
 * @date 2026/05/18
 */
@Component
public class WorkOrderAssignUserResolver {

    private static final String WORKORDER_ASSIGN_PERMISSION = "workorder:assign";

    @Resource
    private SysUserCompanyMapper sysUserCompanyMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private CompanyDataAccessContext companyDataAccessContext;

    /**
     * 解析指定公司下满足当前待派单通知口径的接收人快照列表。
     *
     * @param companyId 目标公司ID
     * @return 接收人快照列表
     */
    public List<NotifyReceiverSnapshot> resolveAssignUserSnapshots(Long companyId) {
        if (companyId == null) {
            return Collections.emptyList();
        }
        // 接收人解析运行在通知事件异步消费链路里，不能再依赖当前登录请求上下文。
        // 这里显式压入目标网点公司上下文，确保角色权限查询命中正确的 company_id 隔离条件。
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
                            && hasCompanyPermission(user.getId(), companyId, WORKORDER_ASSIGN_PERMISSION)
                            && StrUtil.isNotBlank(StrUtil.trim(user.getOpenid())))
                    .sorted(java.util.Comparator.comparing(SysUser::getId))
                    .map(user -> NotifyReceiverSnapshot.of(
                            NotifyReceiverTypeEnum.ASSIGN_USER.getCode(),
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
