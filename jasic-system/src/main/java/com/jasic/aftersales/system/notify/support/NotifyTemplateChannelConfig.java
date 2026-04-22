package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Channel config snapshot for template channel.
 *
 * @author Codex
 * @date 2026/04/21
 */
@Data
public class NotifyTemplateChannelConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String scene;

    private String templateId;

    private String pagePathTemplate;

    private List<NotifyChannelFieldMappingDTO> fieldMapping = new ArrayList<>();
}
