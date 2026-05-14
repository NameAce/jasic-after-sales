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
     * ?? success ?????
     *
     * @return ????
     */
    private String dispatchStatus;

    private String resultCode;

    private String resultMessage;

    private String channelResponseJson;

    public static NotifyChannelSendResult success() {
        NotifyChannelSendResult result = new NotifyChannelSendResult();
        result.setDispatchStatus(NotifyDispatchStatusEnum.SUCCESS.getCode());
        return result;
    }

    /**
     * ?? skipped ?????
     *
     * @param resultCode ??
     * @param resultMessage ??
     * @return ????
     */
    public static NotifyChannelSendResult skipped(String resultCode, String resultMessage) {
        NotifyChannelSendResult result = new NotifyChannelSendResult();
        result.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
        result.setResultCode(resultCode);
        result.setResultMessage(resultMessage);
        return result;
    }

    /**
     * ?? failed ?????
     *
     * @param resultCode ??
     * @param resultMessage ??
     * @return ????
     */
    public static NotifyChannelSendResult failed(String resultCode, String resultMessage) {
        NotifyChannelSendResult result = new NotifyChannelSendResult();
        result.setDispatchStatus(NotifyDispatchStatusEnum.FAILED.getCode());
        result.setResultCode(resultCode);
        result.setResultMessage(resultMessage);
        return result;
    }
}
