package com.jasic.aftersales.customer.service.impl;

import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.mapper.CUserMapper;
import com.jasic.aftersales.system.domain.model.WorkOrderNotifyReceiverInfo;
import com.jasic.aftersales.system.service.WorkOrderNotifyReceiverResolver;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 客户侧通知接收人解析器
 *
 * @author Codex
 * @date 2026/04/02
 */
@Component
public class CustomerNotifyReceiverResolver implements WorkOrderNotifyReceiverResolver {

    private static final String SYSTEM_CUSTOMER_OPENID_PREFIX = "SYS_WO_";

    @Resource
    private CUserMapper cUserMapper;

    @Override
    public boolean supports(String receiverType) {
        return "CUSTOMER".equals(receiverType);
    }

    @Override
    public WorkOrderNotifyReceiverInfo resolve(Long receiverId) {
        WorkOrderNotifyReceiverInfo info = new WorkOrderNotifyReceiverInfo();
        CUser user = cUserMapper.selectById(receiverId);
        if (user == null) {
            info.setFailReason("客户不存在");
            return info;
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            info.setFailReason("客户账号已停用");
            return info;
        }
        if (StrUtil.isBlank(user.getOpenid()) || user.getOpenid().startsWith(SYSTEM_CUSTOMER_OPENID_PREFIX)) {
            info.setFailReason("客户未完成微信登录绑定");
            return info;
        }
        info.setOpenid(user.getOpenid());
        return info;
    }
}
