package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * Notify template channel config DTO.
 *
 * @author Codex
 * @date 2026/04/21
 */
@ApiModel(description = "Notify template channel config DTO")
@Data
public class NotifyTemplateChannelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "Primary key")
    private Long id;

    @ApiModelProperty(value = "Template code", required = true)
    @NotBlank(message = "templateCode cannot be blank")
    private String templateCode;

    @ApiModelProperty(value = "Channel type", required = true)
    @NotBlank(message = "channelType cannot be blank")
    private String channelType;

    @ApiModelProperty(value = "Channel enabled", required = true)
    @NotNull(message = "channelEnabled cannot be null")
    private Integer channelEnabled;

    @ApiModelProperty(value = "Channel scene")
    private String channelScene;

    @ApiModelProperty(value = "Third-party template id")
    private String templateId;

    @ApiModelProperty(value = "Page path template")
    private String pagePathTemplate;

    @ApiModelProperty(value = "Field mappings")
    private List<NotifyChannelFieldMappingDTO> fieldMapping;

    @ApiModelProperty(value = "Raw config json")
    private String configJson;

    @ApiModelProperty(value = "Remark")
    private String remark;
}
