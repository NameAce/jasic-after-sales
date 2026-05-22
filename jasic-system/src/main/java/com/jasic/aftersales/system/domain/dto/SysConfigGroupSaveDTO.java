package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 系统配置分组保存参数。
 *
 * <p>该 DTO 服务于新的“系统配置分组页”保存动作，用于把同一分组下的多个配置项一次性提交给后端。
 * 本轮仍然保留旧参数设置页的单条新增、单条修改接口，因此这里不引入新的配置元数据模型，只负责承载
 * “同组批量修改现有配置项”的轻量入参。</p>
 *
 * @author Zoro
 * @date 2026/05/21
 */
@ApiModel(description = "系统配置分组保存参数")
@Data
public class SysConfigGroupSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置分组标识，只允许使用后端已确认的 org、wechat、work_order、legacy。 */
    @ApiModelProperty(value = "配置分组标识", required = true)
    @NotBlank(message = "配置分组标识不能为空")
    private String groupKey;

    /** 同一分组下待保存的配置项列表；分组保存只接受同组配置项，不允许跨组混提。 */
    @ApiModelProperty(value = "配置项列表", required = true)
    @Valid
    @NotEmpty(message = "配置项列表不能为空")
    private List<SysConfigDTO> configs;
}
