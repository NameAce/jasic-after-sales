package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * Notify channel send result.
 *
 * @author Codex
 * @date 2026/04/21
 */
@Data
public class NotifyChannelSendResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * String字段。
     *
     * @return 处理结果
     */
    private String dispatchStatus;

    private String resultCode;

    private String resultMessage;

    private String channelResponseJson;

    /**
     * 处理success业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 处理结果
     */
    public static NotifyChannelSendResult success() {
        // 调用NotifyChannelSendResult方法，复用统一能力并保证业务规则一致。
        /**
         * 通知渠道发送结果。
         */
        NotifyChannelSendResult result = new NotifyChannelSendResult();
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        result.setDispatchStatus(NotifyDispatchStatusEnum.SUCCESS.getCode());
        return result;
    }

    /**
     * skipped。
     *
     * @param resultCode 参数
     * @param resultMessage 参数
     * @return 处理结果
     */
    public static NotifyChannelSendResult skipped(String resultCode, String resultMessage) {
        // 调用NotifyChannelSendResult方法，复用统一能力并保证业务规则一致。
        /**
         * 通知渠道发送结果。
         */
        NotifyChannelSendResult result = new NotifyChannelSendResult();
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        result.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
        // 调用setResultCode方法，复用统一能力并保证业务规则一致。
        result.setResultCode(resultCode);
        // 调用setResultMessage方法，复用统一能力并保证业务规则一致。
        result.setResultMessage(resultMessage);
        return result;
    }

    /**
     * failed。
     *
     * @param resultCode 参数
     * @param resultMessage 参数
     * @return 处理结果
     */
    public static NotifyChannelSendResult failed(String resultCode, String resultMessage) {
        // 调用NotifyChannelSendResult方法，复用统一能力并保证业务规则一致。
        /**
         * 通知渠道发送结果。
         */
        NotifyChannelSendResult result = new NotifyChannelSendResult();
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        result.setDispatchStatus(NotifyDispatchStatusEnum.FAILED.getCode());
        // 调用setResultCode方法，复用统一能力并保证业务规则一致。
        result.setResultCode(resultCode);
        // 调用setResultMessage方法，复用统一能力并保证业务规则一致。
        result.setResultMessage(resultMessage);
        return result;
    }
}




