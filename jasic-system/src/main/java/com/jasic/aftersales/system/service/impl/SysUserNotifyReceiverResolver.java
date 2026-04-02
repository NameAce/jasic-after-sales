package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.model.WorkOrderNotifyReceiverInfo;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.service.WorkOrderNotifyReceiverResolver;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 公司侧通知接收人解析器
 *
 * @author Codex
 * @date 2026/04/02
 */
@Component
public class SysUserNotifyReceiverResolver implements WorkOrderNotifyReceiverResolver {

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public boolean supports(String receiverType) {
        return "USER".equals(receiverType);
    }

    @Override
    public WorkOrderNotifyReceiverInfo resolve(Long receiverId) {
        WorkOrderNotifyReceiverInfo info = new WorkOrderNotifyReceiverInfo();
        SysUser user = sysUserMapper.selectById(receiverId);
        if (user == null) {
            info.setFailReason("员工不存在");
            return info;
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            info.setFailReason("员工账号已停用");
            return info;
        }
        if (StrUtil.isBlank(user.getOpenid())) {
            info.setFailReason("员工未绑定微信");
            return info;
        }
        info.setOpenid(user.getOpenid());
        return info;
    }
}
