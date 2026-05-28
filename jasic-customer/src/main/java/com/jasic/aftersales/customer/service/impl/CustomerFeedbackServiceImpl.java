package com.jasic.aftersales.customer.service.impl;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.service.ICustomerFeedbackService;
import com.jasic.aftersales.customer.service.ICUserService;
import com.jasic.aftersales.system.domain.dto.SysFeedbackCreateDTO;
import com.jasic.aftersales.system.domain.query.SysFeedbackMyQuery;
import com.jasic.aftersales.system.domain.vo.SysFeedbackVO;
import com.jasic.aftersales.system.service.ISysFeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * C端反馈单 Service 实现。
 *
 * <p>该实现不重复落库和查询规则，而是负责读取当前登录客户信息，
 * 组装终端用户提交所需的姓名、手机号快照后，委托系统侧通用反馈服务完成处理。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
@Service
public class CustomerFeedbackServiceImpl implements ICustomerFeedbackService {

    /** C端用户Service */
    @Resource
    private ICUserService cUserService;

    /** 平台反馈通用Service */
    @Resource
    private ISysFeedbackService sysFeedbackService;

    /**
     * 终端用户提交反馈。
     *
     * @param dto 提交参数
     * @return 新建反馈ID
     */
    @Override
    public Long create(SysFeedbackCreateDTO dto) {
        CUser currentUser = cUserService.getCurrentUser();
        return sysFeedbackService.createForCustomer(
                currentUser.getId(),
                resolveCustomerSubmitterName(currentUser),
                normalizeNullableText(currentUser.getPhone()),
                dto == null ? null : dto.getContent()
        );
    }

    /**
     * 分页查询终端用户自己的反馈列表。
     *
     * @param query 分页参数
     * @return 分页结果
     */
    @Override
    public PageResult<SysFeedbackVO> listPage(SysFeedbackMyQuery query) {
        CUser currentUser = cUserService.getCurrentUser();
        return sysFeedbackService.listCustomerPage(currentUser.getId(), query);
    }

    /**
     * 查询终端用户自己的反馈详情。
     *
     * @param id 反馈ID
     * @return 反馈详情
     */
    @Override
    public SysFeedbackVO getById(Long id) {
        CUser currentUser = cUserService.getCurrentUser();
        return sysFeedbackService.getCustomerDetail(currentUser.getId(), id);
    }

    /**
     * 解析终端用户姓名快照。
     *
     * @param user 当前终端用户
     * @return 提交人姓名
     */
    private String resolveCustomerSubmitterName(CUser user) {
        String nickname = normalizeNullableText(user == null ? null : user.getNickname());
        if (nickname != null) {
            return nickname;
        }
        return normalizeNullableText(user == null ? null : user.getPhone());
    }

    /**
     * 规范化可为空文本。
     *
     * @param text 原始文本
     * @return 去除首尾空白后的文本，空白返回null
     */
    private String normalizeNullableText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
