package com.jasic.aftersales.system.notify.support;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Notify dispatch payload snapshot.
 *
 * @author Codex
 * @date 2026/04/21
 */
@Data
public class NotifyDispatchPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String templateCode;

    private String channelType;

    private NotifyTemplateChannelConfig channelConfig;

    private Map<String, Object> variables = new LinkedHashMap<>();
}
