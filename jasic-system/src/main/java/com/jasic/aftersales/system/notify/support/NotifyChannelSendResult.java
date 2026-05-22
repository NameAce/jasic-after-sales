package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * Notify channel send result.
 *
 * @author Zoro
 * @date 2026/04/21
 */
@Data
public class NotifyChannelSendResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部字段，用于保存当前流程需要复用的业务值。
     *
     * @return 业务处理结果
     */
    private String dispatchStatus;

    /**resultCode 字段，用于当前类内部业务处理。*/
    private String resultCode;

    /**resultMessage 字段，用于当前类内部业务处理。*/
    private String resultMessage;

    /**channelResponseJson 字段，用于当前类内部业务处理。*/
    private String channelResponseJson;

    /**
     * 处理success业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 业务处理结果
     */
    public static NotifyChannelSendResult success() {
        /**
         * 通知渠道发送结果。
         */
        NotifyChannelSendResult result = new NotifyChannelSendResult();
        result.setDispatchStatus(NotifyDispatchStatusEnum.SUCCESS.getCode());
        return result;
    }

    /**
     * skipped。
     *
     * @param resultCode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param resultMessage 提示或消息文本，用于异常返回或通知内容。
     * @return 业务处理结果
     */
    public static NotifyChannelSendResult skipped(String resultCode, String resultMessage) {
        /**
         * 通知渠道发送结果。
         */
        NotifyChannelSendResult result = new NotifyChannelSendResult();
        result.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
        result.setResultCode(resultCode);
        result.setResultMessage(resultMessage);
        return result;
    }

    /**
     * failed。
     *
     * @param resultCode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param resultMessage 提示或消息文本，用于异常返回或通知内容。
     * @return 业务处理结果
     */
    public static NotifyChannelSendResult failed(String resultCode, String resultMessage) {
        /**
         * 通知渠道发送结果。
         */
        NotifyChannelSendResult result = new NotifyChannelSendResult();
        result.setDispatchStatus(NotifyDispatchStatusEnum.FAILED.getCode());
        result.setResultCode(resultCode);
        result.setResultMessage(resultMessage);
        return result;
    }
}




