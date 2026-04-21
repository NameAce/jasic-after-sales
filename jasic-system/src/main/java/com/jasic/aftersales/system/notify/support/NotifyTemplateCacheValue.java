package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.system.notify.domain.entity.SysNotifyTemplate;
import lombok.Data;

import java.io.Serializable;

/**
 * Cached built-in/custom template pair.
 *
 * @author Codex
 * @date 2026/04/20
 */
@Data
public class NotifyTemplateCacheValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private SysNotifyTemplate builtInTemplate;

    private SysNotifyTemplate customTemplate;
}
