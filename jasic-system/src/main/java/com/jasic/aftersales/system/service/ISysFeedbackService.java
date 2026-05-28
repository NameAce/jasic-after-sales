package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.SysFeedbackAcceptDTO;
import com.jasic.aftersales.system.domain.dto.SysFeedbackCreateDTO;
import com.jasic.aftersales.system.domain.query.SysFeedbackManageQuery;
import com.jasic.aftersales.system.domain.query.SysFeedbackMyQuery;
import com.jasic.aftersales.system.domain.vo.SysFeedbackVO;

/**
 * 平台反馈单 Service 接口。
 *
 * <p>该接口统一承载反馈单的提交、我的列表、我的详情、后台管理列表、
 * 后台管理详情、首次受理和修改受理能力。
 * 其中终端用户能力由 C 端模块调用这里的通用服务完成落库和查询。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
public interface ISysFeedbackService {

    /**
     * 终端用户提交反馈。
     *
     * @param customerId 当前终端用户 ID
     * @param submitterName 提交人姓名快照
     * @param contactPhone 联系电话快照
     * @param content 反馈内容
     * @return 新建反馈 ID
     */
    Long createForCustomer(Long customerId, String submitterName, String contactPhone, String content);

    /**
     * 分页查询终端用户自己的反馈列表。
     *
     * @param customerId 当前终端用户 ID
     * @param query 分页参数
     * @return 分页结果
     */
    PageResult<SysFeedbackVO> listCustomerPage(Long customerId, SysFeedbackMyQuery query);

    /**
     * 查询终端用户自己的反馈详情。
     *
     * @param customerId 当前终端用户 ID
     * @param feedbackId 反馈 ID
     * @return 反馈详情
     */
    SysFeedbackVO getCustomerDetail(Long customerId, Long feedbackId);

    /**
     * 当前网点用户提交反馈。
     *
     * @param dto 提交参数
     * @return 新建反馈 ID
     */
    Long createForCurrentServiceUser(SysFeedbackCreateDTO dto);

    /**
     * 分页查询当前网点用户自己的反馈列表。
     *
     * @param query 分页参数
     * @return 分页结果
     */
    PageResult<SysFeedbackVO> listCurrentServiceUserPage(SysFeedbackMyQuery query);

    /**
     * 查询当前网点用户自己的反馈详情。
     *
     * @param feedbackId 反馈 ID
     * @return 反馈详情
     */
    SysFeedbackVO getCurrentServiceUserDetail(Long feedbackId);

    /**
     * 分页查询后台管理列表。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<SysFeedbackVO> listManagePage(SysFeedbackManageQuery query);

    /**
     * 查询后台管理详情。
     *
     * @param feedbackId 反馈 ID
     * @return 反馈详情
     */
    SysFeedbackVO getManageDetail(Long feedbackId);

    /**
     * 总部后台首次受理反馈。
     *
     * <p>该动作只允许对未受理数据执行，
     * 会同时写入受理人、受理时间和受理回复。</p>
     *
     * @param dto 受理参数
     */
    void accept(SysFeedbackAcceptDTO dto);

    /**
     * 总部后台修改已受理数据的受理结果。
     *
     * <p>该动作只允许对已受理数据执行，
     * 会覆盖原有的受理人、受理时间和受理回复。</p>
     *
     * @param dto 修改参数
     */
    void updateAccept(SysFeedbackAcceptDTO dto);
}
