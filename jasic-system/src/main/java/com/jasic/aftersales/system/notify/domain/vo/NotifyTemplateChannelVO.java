package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Notify template channel config view object.
 *
 * @author Codex
 * @date 2026/04/21
 */
@ApiModel(description = "閫氱煡妯℃澘娓犻亾閰嶇疆杩斿洖瀵硅薄")
@Data
public class NotifyTemplateChannelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "涓婚敭")
    private Long id;

    @ApiModelProperty(value = "妯℃澘缂栫爜")
    private String templateCode;

    @ApiModelProperty(value = "娓犻亾绫诲瀷")
    private String channelType;

    @ApiModelProperty(value = "娓犻亾鏄惁鍚敤")
    private Integer channelEnabled;

    @ApiModelProperty(value = "娓犻亾鍦烘櫙")
    private String channelScene;

    @ApiModelProperty(value = "灏忕▼搴忔ā鏉?ID")
    private String templateId;

    @ApiModelProperty(value = "椤甸潰璺宠浆妯℃澘")
    private String pagePathTemplate;

    @ApiModelProperty(value = "瀛楁鏄犲皠")
    private List<com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO> fieldMapping;

    @ApiModelProperty(value = "鍘熷閰嶇疆 JSON")
    private String configJson;

    @ApiModelProperty(value = "澶囨敞")
    private String remark;

    @ApiModelProperty(value = "鍒涘缓鏃堕棿")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "鏇存柊鏃堕棿")
    private LocalDateTime updateTime;
}
