package com.jasic.aftersales.customer.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.SysFeedbackCreateDTO;
import com.jasic.aftersales.system.domain.query.SysFeedbackMyQuery;
import com.jasic.aftersales.system.domain.vo.SysFeedbackVO;

/**
 * C端反馈单 Service 接口。
 *
 * <p>该接口只负责终端用户入口的能力编排：
 * 获取当前登录客户、整理提交人快照，再调用系统侧通用反馈服务完成真正的业务处理。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
public interface ICustomerFeedbackService {

    /**
     * 终端用户提交反馈。
     *
     * @param dto 提交参数
     * @return 新建反馈ID
     */
    Long create(SysFeedbackCreateDTO dto);

    /**
     * 分页查询终端用户自己的反馈列表。
     *
     * @param query 分页参数
     * @return 分页结果
     */
    PageResult<SysFeedbackVO> listPage(SysFeedbackMyQuery query);

    /**
     * 查询终端用户自己的反馈详情。
     *
     * @param id 反馈ID
     * @return 反馈详情
     */
    SysFeedbackVO getById(Long id);
}
