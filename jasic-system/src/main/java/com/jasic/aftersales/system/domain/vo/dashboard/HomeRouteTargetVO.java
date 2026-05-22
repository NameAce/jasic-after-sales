package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 首页卡片跳转目标。
 *
 * <p>该对象只描述“点击首页指标后应该进入哪个前端路由以及携带哪些查询条件”，
 * 不承载任何统计口径本身。首页接口把 routeName 与 query 一并返回，前端卡片组件只负责执行跳转，
 * 避免前端在多个卡片中硬编码工单筛选规则。</p>
 *
 * <p>query 中不得包含任意公司 ID 等权限字段，工单列表仍由后端根据当前登录上下文补齐数据边界。</p>
 *
 * @author Codex
 * @date 2026/05/21
 */
@ApiModel(description = "首页卡片跳转目标")
@Data
public class HomeRouteTargetVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 前端路由名称。
     *
     * <p>例如工单列表使用 after-sales_work-order；平台治理入口使用 org、system_user 等稳定路由名。</p>
     */
    @ApiModelProperty(value = "前端路由名称")
    private String routeName;

    /**
     * 前端路由查询参数。
     *
     * <p>该字段只表达列表筛选条件，如 viewScope、mainStatus、transferDirection。
     * 服务端不会信任前端传入权限边界，列表接口会继续按当前登录上下文兜底。</p>
     */
    @ApiModelProperty(value = "前端路由查询参数")
    private Map<String, Object> query = new LinkedHashMap<>();
}
