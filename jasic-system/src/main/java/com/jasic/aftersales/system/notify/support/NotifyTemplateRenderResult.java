package com.jasic.aftersales.system.notify.support;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime render result for notify template.
 *
 * @author Codex
 * @date 2026/04/20
 */
@Data
public class NotifyTemplateRenderResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean notifyEnabled;

    private String templateCode;

    private String templateSource;

    private String title;

    private String summary;

    private String routeType;

    private String routeValue;

    private final List<String> errors = new ArrayList<>();

    /**
     * ?? addError ?????
     *
     * @param error ??
     */
    public void addError(String error) {
        if (error != null && !error.trim().isEmpty()) {
            errors.add(error);
        }
    }
}
