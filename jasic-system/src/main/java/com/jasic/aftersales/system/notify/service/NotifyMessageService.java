package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoCompleteDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoInvalidateDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessage;
import com.jasic.aftersales.system.notify.domain.query.NotifyMessageQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyMessagePageVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知消息 Service。
 *
 * @author Codex
 * @date 2026/04/18
 */
public interface NotifyMessageService {

    /**
     * 创建通知消息。
     *
     * @param notifyMessage 通知消息
     * @return 主键ID
     */
    Long createMessage(SysNotifyMessage notifyMessage);

    /**
     * 根据ID查询通知消息。
     *
     * @param id 主键ID
     * @return 通知消息
     */
    SysNotifyMessage getById(Long id);

    /**
     * 根据事件ID查询通知消息。
     *
     * @param eventId 事件ID
     * @return 通知消息
     */
    SysNotifyMessage getByEventId(Long eventId);

    /**
     * 查询接收人当前有效待办。
     *
     * @param bizType    业务类型
     * @param bizId      业务ID
     * @param receiverId 接收人ID
     * @param receiverCompanyId 接收公司ID
     * @return 有效待办列表
     */
    List<SysNotifyMessage> listActiveTodoByBizAndReceiver(String bizType, Long bizId, Long receiverId,
                                                          Long receiverCompanyId);

    /**
     * 失效消息。
     *
     * @param messageId     消息ID
     * @param invalidReason 失效原因
     * @param invalidTime   失效时间
     * @return 是否更新成功
     */
    boolean invalidateMessage(Long messageId, String invalidReason, LocalDateTime invalidTime);

    void markRead(Long id, Long receiverId, Long receiverCompanyId);

    /**
     * 按业务对象标记已读。
     *
     * @param dto 已读参数
     */
    void markReadByBiz(NotifyReadByBizDTO dto);

    /**
     * 按业务对象和接收人完成待办。
     *
     * @param dto 完成参数
     */
    void completeTodoByBizAndReceiver(NotifyTodoCompleteDTO dto);

    /**
     * 按业务对象失效待办。
     *
     * @param dto 失效参数
     */
    void invalidateTodoByBiz(NotifyTodoInvalidateDTO dto);

    /**
     * 分页查询通知消息。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<NotifyMessagePageVO> listPage(NotifyMessageQuery query);

    /**
     * 统计有效待办数量。
     *
     * @param receiverId 接收人ID
     * @param receiverCompanyId 接收公司ID
     * @return 待办数量
     */
    Long countTodo(Long receiverId, Long receiverCompanyId);
}
